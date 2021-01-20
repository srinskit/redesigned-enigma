#!/bin/bash

docker build -t local/calc-api-server:latest -f docker/api-server.dockerfile .
docker build -t local/calc-adder-service:latest -f docker/adder-service.dockerfile .
docker build -t local/calc-divider-service:latest -f docker/divider-service.dockerfile .
