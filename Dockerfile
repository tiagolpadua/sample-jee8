# Java EE 8 (javax.*) WAR compiled for JDK 21.
#
# No official "javax + JDK 21" application-server image exists: WildFly stops at
# 26.1.3.Final-jdk17, TomEE at 8.0.16-jre17, and even the Open Liberty images
# only ship Java 8/11/17. So this installs the Open Liberty runtime on a Java 21
# base - Open Liberty's runtime still offers the Java EE 8 feature set on JDK 21.
FROM eclipse-temurin:21-jre-jammy

ARG LIBERTY_VERSION=26.0.0.9
ENV WLP_HOME=/opt/ol/wlp
ENV PATH="${WLP_HOME}/bin:${PATH}"

RUN set -eux; \
	apt-get update; \
	apt-get install -y --no-install-recommends curl unzip; \
	rm -rf /var/lib/apt/lists/*; \
	curl -fSL -o /tmp/ol.zip \
	  "https://repo1.maven.org/maven2/io/openliberty/openliberty-runtime/${LIBERTY_VERSION}/openliberty-runtime-${LIBERTY_VERSION}.zip"; \
	unzip -q /tmp/ol.zip -d /opt/ol; \
	rm /tmp/ol.zip; \
	server create defaultServer; \
	rm -f "${WLP_HOME}/usr/servers/defaultServer/server.xml"

COPY src/main/liberty/config/server.xml ${WLP_HOME}/usr/servers/defaultServer/
COPY target/Sample*.war ${WLP_HOME}/usr/servers/defaultServer/dropins/

EXPOSE 8080
CMD ["server", "run", "defaultServer"]
