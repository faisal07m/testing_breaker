# ATM Protocol

## About

## Team
|Name         	|Email                          |
|---------------|-------------------------------|
|Sid			|`'Isn't this fun?'`            |
|Arkadi         |`"Isn't this fun?"`            |
|Jerry          |`'Isn't this fun?'` 			|
|Thomas         |`'Isn't this fun?'` 			|
|Fisherr        |`'Isn't this fun?'` 			|
|Maq          	|`'Isn't this fun?'` 			|


## Running the project

 1. Download the source code from [Git Repository](https://git.cs.upb.de/bionic-bibifi/atm-protocol) `git clone irb-git@git.cs.upb.de:bionic-bibifi/atm-protocol.git`
 2. Navigate to the base folder and run `mvn clean install`
 3. Running the Bank server (ensure to run this from base folder):  `java -jar bank/target/bank-0.0.1-SNAPSHOT.jar`
 4. Running the ATM client (ensure to run this from base folder): `java -jar atm/target/atm-0.0.1-SNAPSHOT.jar`

> **Note:** This is just a PING-PONG client server communication. Client sends *ping* , server responds *pong*.
