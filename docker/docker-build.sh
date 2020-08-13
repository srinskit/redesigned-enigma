#!/bin/bash

docker build -t srinskit/calc-api-server:latest -f docker/api-server.dockerfile .
docker build -t srinskit/calc-adder-service:latest -f docker/adder-service.dockerfile .
docker build -t srinskit/calc-divider-service:latest -f docker/divider-service.dockerfile .
