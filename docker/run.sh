#!/bin/bash -e

if [ ! -f /.tomcat_admin_created ]; then
    source /create_tomcat_admin_user.sh
    mkdir -p /etc/catwatcher
    echo "username=admin
password=$PASS
endpoint=http://localhost:8080
sleepTime=30" > /etc/catwatcher/catwatcher.properties
fi

exec ${CATALINA_HOME}/bin/catalina.sh run

