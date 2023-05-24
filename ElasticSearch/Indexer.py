import os
from elasticsearch import Elasticsearch
import pandas as pd

# Hashset containing all the processed file up until now
processed_files = set()

# Configure Elasticsearch connection
es = Elasticsearch(['http://Elastic-service:9200'])

index_name = "weather_stations"

# Path to the parent directory that contains all the parquet files
parquet_dir = "./archive"

# Define the index mapping
index_mapping = {
    "mappings": {
        "properties": {
            "station_id": {
                "type": "integer"
            },
            "s_no": {
                "type": "integer"
            },
            "battery_status": {
                "type": "text"
            },
            "status_timestamp": {
                "type": "long"
            },
            "weather": {
                "type": "nested",
                "properties": {
                    "humidity": {
                        "type": "integer"
                    },
                    "temperature": {
                        "type": "integer"
                    },
                    "wind_speed": {
                        "type": "integer"
                    }
                }
            }
        }
    }
}


def readAllParquetFiles():

    # Create the index if it doesn't exist
    parquet_files = [os.path.join(parquet_dir, file)
                     for file in os.listdir(parquet_dir)
                     if os.path.join(parquet_dir, file) not in processed_files]

    if not len(parquet_files):
        return pd.DataFrame()

    # Read Parquet files using pandas
    df = pd.concat([pd.read_parquet(file) for file in parquet_files if file not in processed_files])

    # Mark all files as processed.
    for file in parquet_files:
        processed_files.add(file)

    return df


def indexData():
    if not es.indices.exists(index=index_name):
        es.indices.create(index=index_name, body=index_mapping)

    df = readAllParquetFiles()

    if df.empty:
        return False

    for _, row in df.iterrows():
        humidity = row["weather"]["humidity"]
        temperature = row["weather"]["temperature"]
        wind_speed = row["weather"]["windSpeed"]
        doc = {
            "station_id": row["stationId"],
            "s_no": row["sNo"],
            "battery_status": row["batteryStatus"],
            "status_timestamp": row["statusTimestamp"],
            "weather": [{
                "humidity": humidity,
                "temperature": temperature,
                "wind_speed": wind_speed
            }]
        }

        # Adding each row to the existing index
        es.index(index=index_name, document=doc)

        return True
