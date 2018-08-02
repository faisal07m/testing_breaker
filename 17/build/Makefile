default: pre-build
	cat stub.sh bankapp/target/bankapp-1.0-SNAPSHOT-jar-with-dependencies.jar > bank && chmod +x bank
	cat stub.sh atmapp/target/atmapp-1.0-SNAPSHOT-jar-with-dependencies.jar > atm && chmod +x atm

pre-build:
	rm -f -- *.out
	rm -f -- bank
	rm -f -- atm
	mvn -U clean install



