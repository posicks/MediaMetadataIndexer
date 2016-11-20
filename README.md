* Media Metadata Indexer


* Building the project
mvn clean package assembly:single


* Run
java -jar target/MediaMetadataIndexer-1.0.0-jar-with-dependencies.jar -i http://s3.amazonaws.com/waldo-recruiting -o localhost -t 100