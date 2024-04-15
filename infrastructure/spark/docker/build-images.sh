#!/bin/bash

set -e

export SPARKBASENAME=spark351-scala213

docker build -t $SPARKBASENAME-base:latest ./base
docker build -t $SPARKBASENAME-master:latest ./spark-master
docker build -t $SPARKBASENAME-worker:latest ./spark-worker
