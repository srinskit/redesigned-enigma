#!/bin/bash

# Uses container hostname if not provided in the command line args
exec java -jar $JAR $0 $@ -m api-server --host $(hostname) --zookeepers zookeeper
