
# Azure Time Series Insights

> see https://aka.ms/autorest

This is the AutoRest configuration file for Azure Time Series Insights Gen2 Data Plane API.

---

## Getting Started

To build the SDK for Azure Time Series Insights Gen2 Data Plane API, simply [Install AutoRest](https://aka.ms/autorest/install) and in this folder, run:

> `autorest`

To see additional help and options, run:

> `autorest --help`
---

## Configuration

### Basic Information

These are the global settings for Azure Time Series Insights Gen2 Data Plane API.

``` yaml
openapi-type: data-plane
add-credentials: true
license-header: MICROSOFT_MIT
tag: package-2020-07-31
```

### Tag: package-2020-07-31

These settings apply only when `--tag=package-2020-07-31` is specified on the command line.

``` yaml $(tag) == 'package-2020-07-31'
input-file:
- https://raw.githubusercontent.com/Azure/azure-rest-api-specs/master/specification/timeseriesinsights/data-plane/Microsoft.TimeSeriesInsights/stable/2020-07-31/timeseriesinsights.json
```

## Suppression

``` yaml
directive:
  - suppress: R2001
    reason: Adding flattening does not improve the code - array of properties is not supported by flattening. See https://github.com/Azure/oav/issues/416
```

``` yaml
directive:
  - suppress: R3017
    reason: GUIDs are required to enforce referential constraints and reduce number of updates.
```

---
# Code Generation

## C#

These settings apply only when `--csharp` is specified on the command line.
Please also specify `--csharp-sdks-folder=<path to "SDKs" directory of your azure-sdk-for-net clone>`.

``` yaml $(csharp)
csharp:
  sync-methods: none
  azure-arm: false
  license-header: MICROSOFT_MIT_NO_VERSION
  namespace: Microsoft.Azure.TimeSeriesInsights
  output-folder: generated
```

## Multi-API/Profile support for AutoRest v3 generators 

AutoRest V3 generators require the use of `--tag=all-api-versions` to select api files.


If there are files that should not be in the `all-api-versions` set, 
uncomment the  `exclude-file` section below and add the file paths.

``` yaml $(tag) == 'all-api-versions'
#exclude-file: 
#  - $(this-folder)/Microsoft.Example/stable/2010-01-01/somefile.json
```

