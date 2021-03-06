// Databricks notebook source
dbutils.widgets.text("test-output-path", "dbfs:/test-output/test-output.txt", "DBFS location to store assertion results")
dbutils.widgets.text("eventhub-consumergroup", "$Default", "Event Hubs consumer group")
dbutils.widgets.text("eventhub-maxEventsPerTrigger", "1000000", "Event Hubs max events per trigger")
dbutils.widgets.text("assert-events-per-second", "900", "Assert min events per second (computed over 1 min windows)")
dbutils.widgets.text("assert-latency-milliseconds", "15000", "Assert max latency in milliseconds (averaged over 1 min windows)")
dbutils.widgets.text("assert-duplicate-fraction", "0", "Assert max proportion of duplicate events")
dbutils.widgets.text("assert-outofsequence-fraction", "0", "Assert max proportion of out-of-sequence events")

// COMMAND ----------

import org.apache.spark.eventhubs.{ EventHubsConf, EventPosition }

val eventHubsConf = EventHubsConf(dbutils.secrets.get(scope = "MAIN", key = "event-hubs-read-connection-string"))
  .setConsumerGroup(dbutils.widgets.get("eventhub-consumergroup"))
  .setStartingPosition(EventPosition.fromStartOfStream)
  .setMaxEventsPerTrigger(dbutils.widgets.get("eventhub-maxEventsPerTrigger").toLong)

val streamingData = spark
  .readStream
  .format("eventhubs")
  .options(eventHubsConf.toMap)
  .load()

// COMMAND ----------

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import java.util.UUID.randomUUID

val schema = StructType(
  StructField("eventId", StringType, false) ::
  StructField("complexData", StructType((0 to 22).map(i => StructField(s"moreData$i", DoubleType, false)))) ::
  StructField("value", StringType, false) ::
  StructField("type", StringType, false) ::
  StructField("deviceId", StringType, false) ::
  StructField("deviceSequenceNumber", LongType, false) ::
  StructField("PartitionId", IntegerType, false) ::
  // Parse time fields as string as workaround for https://issues.apache.org/jira/browse/SPARK-17914 
  // (incorrectly marked as resolved as of Spark 2.4.3, see Jira comments)
  StructField("createdAt", StringType, false) ::
  StructField("enqueuedAt", StringType, false) ::
  StructField("processedAt", StringType, false) ::
  StructField("processedAt2", StringType, false) ::
  Nil)

val arrayOfEventsSchema = ArrayType(schema)

val stagingTable = "tempresult_" + randomUUID().toString.replace("-","_")

var query = streamingData
  .select(explode(from_json(decode($"body", "UTF-8"), arrayOfEventsSchema)).as("eventData"), $"*")
  // When consuming from the output of eventhubs-streamanalytics-eventhubs pipeline, 'enqueuedAt' will haven been
  // set when reading from the first eventhub, and the enqueued timestamp of the second eventhub is then the 'storedAt' time
  .select($"eventData.*", $"offset", $"sequenceNumber", $"publisher", $"partitionKey", $"enqueuedTime".as("storedAt"))
  // Continue workaround for https://issues.apache.org/jira/browse/SPARK-17914 
  .withColumn("createdAt", $"createdAt".cast(TimestampType))
  .withColumn("enqueuedAt", $"enqueuedAt".cast(TimestampType))
  .withColumn("processedAt", $"processedAt".cast(TimestampType))
  .withColumn("processedAt2", $"processedAt2".cast(TimestampType))
  // Write stream as a delta table
  .writeStream
  .format("delta")
  .option("checkpointLocation", "dbfs:/streaming_at_scale/checkpoints/verify-eventhubs/" + stagingTable)
  .table(stagingTable)

// COMMAND ----------

println("Waiting while stream collects data")
while (query.isActive) {
  if (!query.status.isDataAvailable) {
    println("No more data available")
    // https://stackoverflow.com/questions/45717433/stop-structured-streaming-query-gracefully
    while (query.status.isTriggerActive) {}
    query.stop
  }
  Thread.sleep(1000)
}
println("Query stopped")
if (query.exception.nonEmpty) {
  throw query.exception.get
}
if (table(stagingTable).count == 0) {
  throw new AssertionError("No data collected")
}

// COMMAND ----------

dbutils.notebook.run("verify-common", 0, Map(
    "test-output-path" -> dbutils.widgets.get("test-output-path"),
    "input-table" -> stagingTable,
    "assert-events-per-second" -> dbutils.widgets.get("assert-events-per-second"),
    "assert-latency-milliseconds" -> dbutils.widgets.get("assert-latency-milliseconds"),
    "assert-duplicate-fraction" -> dbutils.widgets.get("assert-duplicate-fraction"),
    "assert-outofsequence-fraction" -> dbutils.widgets.get("assert-outofsequence-fraction")
))

// COMMAND ----------

sql("DROP TABLE `" + stagingTable + "`")
