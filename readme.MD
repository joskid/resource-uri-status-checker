# Resource-URI-status-checker

## Goal
This simple crawler visits URIs specified in RDF resources and checks their status, i.e. it tries to open and download the contents of the corresponding webpage. If the webpage contains any warning words or shows some error codes such as 404, the URIs of such webpages are collected in the log file (output.log).

## Getting Started
### Installation and Usage
Run the following commands for building and running the executable jar file.

~~~~
    mvn clean compile assembly:single
    java -jar target/resourceuristatuschecker-0.0.1-SNAPSHOT-jar-with-dependencies.jar -i neoplasm-core.owl (check the status of URIs in neoplasm-core.owl) 
~~~~

## Contributors
- [HyeongSik Kim](https://github.com/yy20716)
