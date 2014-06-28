catwatcher
==========

[![Build Status](https://travis-ci.org/ClouDesire/catwatcher.svg?branch=master)](https://travis-ci.org/ClouDesire/catwatcher)

I am the watcher on the tomcats.

Just put me on a running tomcat and I will take care of stopped webapps.

My greateness arise especially on cloud infrastructures, when instances can reboot everytime and come again up at unexpected times (especially database servers), and you don't want a borked webapps with a bootup Context failed and not responding.

Catwatcher will watch your webapps, and try to (re)start them if they failed.

## Requirements

(Tested on Tomcat 7)

The Tomcat manager webapp should be available on your tomcat server. On ubuntu it is sufficent to install the appropiates packages:

```
sudo apt-get install tomcat7 tomcat7-admin
```

## Configuration

Create a properties file on */etc/catwatcher/catwatcher.properties* with:
```
username=user
password=tomcat
endpoint=http://localhost:8080
sleepTime=30000
```

Username and password should be set on tomcat server in *tomcat-users.xml* (on Ubuntu */etc/tomcat7/tomcat-users.xml*):
```
<user name="user" password="tomcat" roles="manager-script" />
```
More info at:
http://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html#Configuring_Manager_Application_Access
