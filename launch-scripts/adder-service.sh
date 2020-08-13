#!/bin/bash

# Uses container hostname if not provided in the command line args
exec java -jar $JAR $0 $@ -m adder-service --host $(hostname) --zookeepers zookeeper
