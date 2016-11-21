[components]: https://github.com/posicks/MediaMetadataIndexer/raw/master/docs/Components%20and%20Flow.png "Components and Flow"

# Media Metadata Indexer

## Overview
The Media Metadata Indexer is an application that reads the media files contained withing the specified S3 directory, 
parses the EXIF metadata out of the files, and writes the metadata to a CSQL output. Both ScyllaDB and Apache Cassandra 
are supported and have been tested with ScyllaDB 1.4 and Cassandra 3.9. 

The [Metadata Extractor](https://drewnoakes.com/code/exif/) is used to parse the metadata allowing for the support of a 
wide range of files.

<div align="center">
    <img src="https://github.com/posicks/MediaMetadataIndexer/raw/master/docs/Components%20and%20Flow.png" />
</div>

## Building the Application
The project uses the Maven build system to create a small dependenciless Jar file and a "Fat" executable Jar.

###To build the small dependenciless Jar file use the command line:
`mvn clean package`
 
###To build the "Fat" executable Jar file use the command line:
`mvn clean package assembly:single`

## Setting up the Environment
There are 2 ways to setup the ScyallaDB or Cassandra datastore to use with this application, the datastore software can
be downloaded and installed or [Docker](https://www.docker.com/) may be used to download and execute the containerized version.

### Using Docker

#### ScyllaDB
`docker pull scylladb/scylla:1.4.1`
`docker run -d -p 9042:9042 -p 10000:10000 -p 9160:9160 -p 7199:7199 --name scylladb scylladb/scylla:1.4.1`

#### Apache Cassandra
`docker pull cassandra:3.9`
```
docker run -d -p 9042:9042 -p 10000:10000 -p 9160:9160 -p 7199:7199 --name cassandra cassandra:3.9
```

## Running the "Fat" executable Jar
```
java -jar target/MediaMetadataIndexer-1.0.0-jar-with-dependencies.jar -i http://s3.amazonaws.com/waldo-recruiting -o localhost -t 100
```

### Command Line:
```
usage: Indexer
 -f,--filter <Input URI>                Specifies a regular expression filter for selecting input files
    --file_handler <File Handler>       Specifies the File Handler to be used to extract the EXIF metadata from the input media files
 -i,--input <Input URI>                 Specifies the URI for the input directory listing
    --input_handler <Input Handler>     Specifies the Input Handler to be used to read the input media files
 -o,--output <Datasource URI>           Specifies the output URI to send EXIF metadata to
    --output_handler <Output Handler>   Specifies the Output Handler to be used to send the processed data to
 -t,--threads <Processing Threads>      Specifies the maximum number of processing threads
```

## Example Execution from the Beginning
```
docker pull scylladb/scylla:1.4.1
docker run -d -p 9042:9042 -p 10000:10000 -p 9160:9160 -p 7199:7199 --name scylladb scylladb/scylla:1.4.1
git clone https://github.com/posicks/MediaMetadataIndexer.git
cd MediaMetadataIndexer
mvn clean package assembly:single
java -jar target/MediaMetadataIndexer-1.0.0-jar-with-dependencies.jar -i http://s3.amazonaws.com/waldo-recruiting -o localhost -t 100
```

### Querying the Datastore
ScyllaDB 1.4.1 and Cassandra 3.9 support different versions of CQL. To query the results within the datastore use the following commands:

#### ScyllaDB 1.4.1
`cqlsh --cqlversion=3.2.1 [host] [port]`

#### Cassandra 3.9
`cqlsh [host] [port]`

#### Querying Data
After cqlsh is successfully started the following commands may be used to query that date within the datastore:
```
use exif;
select count(*) from metadata;
select * from metadata limit 10;
```

`truncate metadata` or `truncate exif.metadata` can be used to clear the datastore between executions. 
