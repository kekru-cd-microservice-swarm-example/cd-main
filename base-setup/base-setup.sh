#!/bin/bash

docker network create --attachable --driver overlay app-net
docker run --name redis -d -p 6379:6379 --net app-net redis:alpine
docker run --name webdis -d -p 7379:7379 --link redis:redis --net app-net anapsix/webdis