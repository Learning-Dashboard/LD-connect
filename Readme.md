# Q-Rapids Kafka Connector for Google Sheets ![](https://img.shields.io/badge/License-Apache2.0-blue.svg)

An Apache Kafka Connector that collects Imputations from Google Sheets.

More specifically, this connector is designed to retrieve individual hour imputations from a Project Record Track, hosted in a Google Sheets file.

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

#### Google Sheets Configuration

Configuration for Google Sheets Source Connector Worker (sheets.properties)

```properties
name=kafka-sheet-source-connector
connector.class=connect.sheets.googlesheets.SheetsSourceConnector
tasks.max=1

## Common fields

account.type=<accountType>
project.id=<projectID>
private.key.id=<privateKeyID>
private.key=<privateKey>
client.email=<clientEmail>
client.id=<clientID>
auth.uri=<authenticationURI>
token.uri=<tokenURI>
auth.provider.x509.cert.url=<authCertificateProvider>
client.x509.cert.url=<clientCertificateURL>

sprint.names=<sprintNames>

sheets.interval.seconds=86400
sheets.teams.num=<numberOfTeams>
sheets.teams.interval.seconds=120

## Particular fields (until tasks.<numberOfTeams> - 1)

tasks.0.spreadsheet.id=<spreadsheetID>
tasks.0.team.name=<teamName>
tasks.0.imputations.topic=sheets_0.imputations

tasks.1.spreadsheet.id=<spreadsheetID>
tasks.1.team.name=<teamName>
tasks.1.imputations.topic=sheets_1.imputations

...
```

Configuration for Elasticsearch Sink Connector Worker (elasticsearch.properties)

```properties
name=kafka-sheets-elasticsearch
connector.class=io.confluent.connect.elasticsearch.ElasticsearchSinkConnector
tasks.max=1
topics=sheets_0.imputations, sheets_1.imputations, \
    ...
key.ignore=false
connection.url=http://elasticsearch:9200
type.name=sheets
```

## Running the Connector

```
<path-to-kafka>/bin/connect-standalone standalone.properties sheets.properties elasticsearch.properties
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management


## Authors

* **Axel Wickenkamp, Fraunhofer IESE**
* **Laura Cazorla, UPC - FIB**

