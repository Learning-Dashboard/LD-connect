# Q-Rapids Kafka Connector for Taiga ![](https://img.shields.io/badge/License-Apache2.0-blue.svg)

An Apache Kafka Connector that collects Issues, Epics, Userstories and Tasks from Taiga.

## Running the Connector

### Prerequisites

* Kafka has to be setup and running (see [Kafka Connect](https://docs.confluent.io/current/connect/index.html))
* If you want your data to be transfered to Elasticsearch, Elasticsearch has to be setup and running. (see [Set up Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/setup.html))

### Build the connector

```
mvn package assembly:single
```
After build, you'll find the generated jar in the target folder

### Configuration files

Example Configuration for Kafka Standalone Connector (standalone.properties)

```properties
bootstrap.servers=<kafka-ip>:9092

key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=true
value.converter.schemas.enable=true

internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false

offset.storage.file.filename=/tmp/connect.offsets
offset.flush.interval.ms=60000
offset.flush.timeout.ms=10000
rest.port=8189
```

#### Taiga Configuration

Configuration for Taiga Source Connector Worker (taiga.properties)

```properties
name=kafka-taiga-source-connector
connector.class=connect.taiga.TaigaSourceConnector
tasks.max=1

## Common fields

taiga.url=<taigaApiURL>, currently it is https://api.taiga.io/api/v1
username=<taigaUser> or XXXX
password=<taigaPass> or XXXX

taskCustomAttributes=Estimated Effort,Actual Effort,
userstoryCustomAttributes=Acceptance Criteria,Priority,

taiga.interval.seconds=86400
taiga.teams.num=<numberOfTeams>
taiga.teams.interval.seconds=120

## Particular fields (until tasks.<numberOfTeams> - 1)

tasks.0.slug=<projectSlug>
tasks.0.taiga.issue.topic=taiga_0.issues
tasks.0.taiga.epic.topic=taiga_0.epics
tasks.0.taiga.userstory.topic=taiga_0.userstories
tasks.0.taiga.task.topic=taiga_0.tasks

tasks.1.slug=<projectSlug>
tasks.1.taiga.issue.topic=taiga_1.issues
tasks.1.taiga.epic.topic=taiga_1.epics
tasks.1.taiga.userstory.topic=taiga_1.userstories
tasks.1.taiga.task.topic=taiga_1.tasks

...
```

Configuration for Elasticsearch Sink Connector Worker (elasticsearch.properties)

```properties
name=kafka-taiga-elasticsearch
connector.class=io.confluent.connect.elasticsearch.ElasticsearchSinkConnector
tasks.max=1
topics=taiga_0.issues, taiga_0.epics, \
    taiga_0.userstories, taiga_0.tasks, \
    taiga_1.issues, taiga_1.epics, \
    taiga_1.userstories, taiga_1.tasks, \
    ...
key.ignore=false
connection.url=http://elasticsearch:9200
type.name=taiga
```

## Running the Connector

```
<path-to-kafka>/bin/connect-standalone standalone.properties taiga.properties elasticsearch.properties
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management


## Authors

* **Axel Wickenkamp, Fraunhofer IESE**
* **Laura Cazorla, UPC - FIB**

