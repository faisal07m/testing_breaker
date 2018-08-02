default : build

build: make-dirs copy-jars copy-executables give-permissions

#mvn-build:
#	cd ../ && mvn -o -Dmaven.repo.local=dependencies clean install -nsu && cd build/

make-dirs:
	mkdir -p bankdir atmdir

copy-jars:
	cp -rf ../atm/jars/atm.jar atmdir/ && cp -rf ../bank/jars/bank.jar bankdir/ && cp -rf ../atm/jars/atm.jar . && cp -rf ../bank/jars/bank.jar .

copy-executables:
	cp -rf atm atmdir/ && cp -rf bank bankdir/

give-permissions:
	chmod +x atmdir/atm && chmod +x bankdir/bank && chmod +x atm && chmod +x bank
