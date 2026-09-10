#!/usr/bin/env bash

export JAVA_HOME=/Users/tiagopadua/dev/openlogic-openjdk-8u352-b08-mac-x64/jdk1.8.0_352.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn clean install
rm /Volumes/LEXAR_1TB/glassfish4_1_2/glassfish/domains/domain1/autodeploy/*.war
cp target/*.war /Volumes/LEXAR_1TB/glassfish4_1_2/glassfish/domains/domain1/autodeploy/