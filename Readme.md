# Q-Rapids Kafka Connector for Github ![](https://img.shields.io/badge/License-Apache2.0-blue.svg)

An Apache Kafka Connector that collects Issues and Commits from Github.

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

#### Github Configuration
Configuration for Github Source Connector Worker (github.properties)

```properties
name=kafka-github-source-connector
connector.class=connect.github.GithubSourceConnector
tasks.max=1

## Common fields

username=<githubUser> or XXXX
password=<githubPass> or XXXX
github.secret=<githubToken>

github.created.since=2022-01-01

github.interval.seconds=86400
github.teams.num=<numberOfTeams>
github.teams.interval.seconds=120

## Particular fields (until tasks.<numberOfTeams> - 1)

tasks.0.github.url=https://api.github.com/orgs/<githubOrgName>
tasks.0.github.commit.topic=github_0.commits
tasks.0.github.issue.topic=github_0.issues
tasks.0.taiga.task.topic=taiga_0.tasks

tasks.1.github.url=https://api.github.com/orgs/<githubOrgName>
tasks.1.github.commit.topic=github_1.commits
tasks.1.github.issue.topic=github_1.issues
tasks.1.taiga.task.topic=taiga_1.tasks

...
```

Configuration for Elasticsearch Sink Connector Worker (elasticsearch.properties)

```properties
name=kafka-github-elasticsearch
connector.class=io.confluent.connect.elasticsearch.ElasticsearchSinkConnector
tasks.max=1
topics=github_0.commits, github_0.issues, \
    github_1.commits, github_1.issues, \
    ...
key.ignore=false
connection.url=http://elasticsearch:9200
type.name=github
```

## Running the Connector

```
<path-to-kafka>/bin/connect-standalone standalone.properties sonarqube.properties elasticsearch.properties
```

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Axel Wickenkamp, Fraunhofer IESE**
* **Laura Cazorla, UPC - FIB**

