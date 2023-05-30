# Weather-Stations-Monitoring
Each weather station emits readings for the current weather status to the “central base station” for persistence and
analysis. In this project, it will be required to implement the architecture of a weather monitoring system.
![image](https://github.com/ziadalbana/Weather-Stations-Monitoring/assets/58531158/aab4fe43-613f-4791-a4b8-618c828aa935)
- The system is composed of three stages:
  - Data Acquisition: multiple weather stations that feed a queueing service (Kafka) with their readings.
  - Data Processing & Archiving: The base central station is consuming the streamed data and archiving all data in the form of Parquet files.
  - Indexing: two variants of index are maintained
     - Key-value store (Bitcask) for the latest reading from each individual station
     - ElasticSearch / Kibana that are running over the Parquet files
