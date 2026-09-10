FROM jboss/wildfly:18.0.0.Final
ADD target/Sample*.war /opt/jboss/wildfly/standalone/deployments/