#!/bin/bash -exu
if [ -z $BUILD_NUMBER ]; then
    echo This should run under jenkins only
    exit 1
fi

BASE_NAME="cloudesire/catwatcher"
BUILD_VERSION=$BASE_NAME:$BUILD_NUMBER
BUILD_LATEST=$BASE_NAME:latest

docker build --no-cache --force-rm -t $BUILD_VERSION .

docker login -e "jenkins@cloudesire.com" -u $PUBLIC_REGISTRY_USERNAME -p $PUBLIC_REGISTRY_PASSWORD
docker push $BUILD_VERSION
docker tag -f $BUILD_VERSION $BUILD_LATEST
docker push $BUILD_LATEST
docker rmi $BUILD_VERSION $BUILD_LATEST
exit 0
