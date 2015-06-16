FROM cloudesire/tomcat:7-jre8
MAINTAINER ClouDesire <dev@cloudesire.com>

ADD ./target/catwatcher-*.war /tomcat/webapps/catwatcher.war
ADD ./docker/run.sh /run.sh

CMD ["/run.sh"]
