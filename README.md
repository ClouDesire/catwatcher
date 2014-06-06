catwatcher
==========


I am the watcher on the tomcats.

Just put me on a running tomcat and I will take care of stopped webapps.

My greateness arise especially on cloud infrastructures, when instances can reboot everytime and come again up at unexpected times (especially database servers), and you don't want a borked webapps with a bootup Context failed and not responding.

Catwatcher will watch your webapps, and try to (re)start them if they failed.

## Requirements

(Tested on Tomcat 7)

The Tomcat manager webapp should be available and running.

## Configuration

Drop a properties file on */etc/catwatcher/catwatcher.properties*:
```
username=user
password=tomcat
endpoint=http://localhost:8080/manager/text
sleepTime=30000
```

Username and password should be set in tomcat-users.xml:
```
<user name="user" password="tomcat" roles="manager-script" />
```
More info at:
http://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html#Configuring_Manager_Application_Access
