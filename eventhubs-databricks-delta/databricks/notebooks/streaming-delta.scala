// Databricks notebook source
dbutils.widgets.text("eventhub-consumergroup", "delta", "Event Hubs consumer group")
dbutils.widgets.text("eventhub-maxEventsPerTrigger", "1000", "Event Hubs max events per trigger")

// COMMAND ----------

import org.apache.spark.eventhubs.{ EventHubsConf, EventPosition }

val eventHubsConf = EventHubsConf(dbutils.secrets.get(scope = "MAIN", key = "event-hubs-read-connection-string"))
  .setStartingPosition(EventPosition.fromStartOfStream)
  .setConsumerGroup(dbutils.widgets.get("eventhub-consumergroup"))
  .setMaxEventsPerTrigger(dbutils.widgets.get("eventhub-maxEventsPerTrigger").toLong)

val eventhubs = spark.readStream
  .format("eventhubs")
  .options(eventHubsConf.toMap)
  .load()

// COMMAND ----------

import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
val schema = StructType(
  StructField("eventId", StringType) ::
  StructField("complexData", StructType((1 to 22).map(i => StructField(s"moreData$i", DoubleType)))) ::
  StructField("value", StringType) ::
  StructField("type", StringType) ::
  StructField("deviceId", StringType) ::
  StructField("createdAt", StringType) :: Nil)
val jsons = eventhubs
      .select(from_json(decode($"body", "UTF-8"), schema).as("eventData"), $"*")
      .select($"eventData.*", $"offset", $"sequenceNumber", $"publisher", $"partitionKey") 

// COMMAND ----------

// You can also use a path instead of a table, see https://docs.azuredatabricks.net/delta/delta-streaming.html#append-mode
jsons.writeStream
  .outputMode("append")
  .option("checkpointLocation", "dbfs:/checkpoints/streaming-delta")
  .format("delta")
  .table("stream_scale_events")

// COMMAND ----------

// MAGIC %sql select * from stream_scale_events limit 10
