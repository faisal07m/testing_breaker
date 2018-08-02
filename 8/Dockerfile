FROM alpine:3.8

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8
ENV JAVA_VERSION 8u171
ENV JAVA_ALPINE_VERSION 8.171.11-r0

# add a simple script that can auto-detect the appropriate JAVA_HOME value
# based on whether the JDK or only the JRE is installed
RUN { \
		echo '#!/bin/sh'; \
		echo 'set -e'; \
		echo; \
		echo 'dirname "$(dirname "$(readlink -f "$(which javac || which java)")")"'; \
	} > /usr/local/bin/docker-java-home \
	&& chmod +x /usr/local/bin/docker-java-home

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk/jre
ENV PATH $PATH:/usr/lib/jvm/java-1.8-openjdk/jre/bin:/usr/lib/jvm/java-1.8-openjdk/bin

RUN set -x \
	&& apk add --no-cache \
		openjdk8-jre="$JAVA_ALPINE_VERSION" \
	&& [ "$JAVA_HOME" = "$(docker-java-home)" ]

RUN apk add --no-cache make

# Create directories for executables and add bififi user
RUN mkdir -p /opt/atm-protocol \
    && adduser -s /bin/bash -D -h /home/workdir bififi

COPY . /opt/atm-protocol/

RUN cd /opt/atm-protocol/build \
	&& make \
	&& chown -R bififi /opt/atm-protocol \
    && chmod -R u+rX /opt/atm-protocol

USER bififi

WORKDIR /home/workdir
EXPOSE 1024-65535

ENTRYPOINT ["sh", "/opt/atm-protocol/build/bankdir/bank"]
CMD []
