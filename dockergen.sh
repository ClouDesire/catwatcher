#!/bin/bash -exu
if [ -z $BUILD_NUMBER ]; then
    echo This should run under jenkins only
    exit 1
fi

docker login -e "jenkins@cloudesire.com" -u $PUBLIC_REGISTRY_USERNAME -p $PUBLIC_REGISTRY_PASSWORD

IMAGES=(
  7-jre7
  7-jre8
  8-jre8
)

BASE_NAME="cloudesire/catwatcher"

for IMAGE in "${IMAGES[@]}"
do
  # cloudesire/catwatcher:8-jre8-123
  BUILD_VERSION=$BASE_NAME:$IMAGE-$BUILD_NUMBER
  BUILD_LATEST=$BASE_NAME:$IMAGE-latest

  # Generate proper FROM image
  BASE_IMAGE="cloudesire\/tomcat:${IMAGE}"
  sed s/%BASE_IMAGE%/$BASE_IMAGE/ Dockerfile.gen > Dockerfile-$IMAGE

  # Pull upstream
  docker pull $BASE_IMAGE

  # Build and push docker image
  docker build --no-cache --force-rm -t $BUILD_VERSION -f Dockerfile-$IMAGE .
  docker push $BUILD_VERSION
  docker tag -f $BUILD_VERSION $BUILD_LATEST
  docker push $BUILD_LATEST
  docker rmi $BUILD_VERSION $BUILD_LATEST
done
