#!/bin/bash

docker rm -f mocks
docker run -p 8087:8087 --name mocks -v /$PWD/mocks:/mocks nodo-dei-pagamenti-mock