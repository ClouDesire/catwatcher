catwatcher
==========

[![Build Status](https://travis-ci.org/ClouDesire/catwatcher.svg?branch=master)](https://travis-ci.org/ClouDesire/catwatcher)

I am the watcher of the tomcats.

Catwatcher is a web application mean to be run on tomcat webservers (local or even remotew), that uses the tomcat-manager API (yeah, a sort of) to monitor che status of every running webapp, and automatically try to restart them if, for some reason, they fails.

A lot of problems may arise especially on highly dynamic cloud infrastructures, when instances can [reboot everytime](http://blogs.msdn.com/b/wats/archive/2013/09/24/windows-azure-virtual-machine-restarted-or-shutdown-with-out-any-notification.aspx) and come again up unpredictably.
A common pitfall is that the database VM takes more time to startup rather than the application server, and then the application server will inevitably fails since it can't connect to the configured database server. In such situation, you need to manually try to re-deploy the web application or setup some complex active monitoring system.

Catwatcher is a set-and-forget solution that once deployed will watch your running webapps, and try to (re-)start them if failed for whatever reason.

## Install

Catwatcher is packaged as war, and you can download it also from maven central or directly from [here](https://github.com/ClouDesire/catwatcher/releases/download/v1.0.0/catwatcher-1.0.0.war).

```
<dependency>
    <groupId>com.cloudesire.catwatcher</groupId>
    <artifactId>catwatcher</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Requirements

This webapp depends on the tomcat-manager webapp, that should be available and deployed on the tomcat server you want to watch. On ubuntu it is sufficent to install the appropiate package:

```
sudo apt-get install tomcat7-admin
```

## Configuration

Create a properties file on */etc/catwatcher/catwatcher.properties* with:
```
username=user
password=tomcat
endpoint=http://localhost:8080
sleepTime=30
```

Username and password should be set on tomcat server in *tomcat-users.xml* (on Ubuntu */etc/tomcat7/tomcat-users.xml*):
```
<user name="user" password="tomcat" roles="manager-script" />
```
More info at:
http://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html#Configuring_Manager_Application_Access
