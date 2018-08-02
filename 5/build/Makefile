MVN_FLAGS = -s settings.xml -Dmaven.test.skip=true --offline
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

build: buildJars
	sed -i 's,^ROOT_DIR=.*,'"ROOT_DIR=$(ROOT_DIR)," atm && sed -i 's,^ROOT_DIR=.*,'"ROOT_DIR=$(ROOT_DIR)," bank

buildJars: cleanJars extractDeps
	mvn clean package $(MVN_FLAGS)

extractDeps:
	tar -zxvf "$(ROOT_DIR)/m2-local.tar.gz"

cleanJars:
	$(RM) *.jar
