#!/bin/bash

PORT=${1:-8087}

docker rm -f mocks$PORT
docker run -d -p $PORT:8087 --name mocks$PORT --env MOCKPORT=8087 -v /$PWD/mocks:/mocks nodo-dei-pagamenti-mock