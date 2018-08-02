FROM openjdk:8-jre

RUN apt update && apt install -yqq nano make dos2unix

COPY ./build /build
WORKDIR /build

RUN dos2unix * && \
	cd gradle/wrapper && \
	dos2unix *

CMD "/bin/bash"