.PHONY: all clean

all: atm bank

atm: bin/ATM.class
	echo -n "#!/bin/bash\njava -classpath "$(shell pwd)"/bin ATM \"\$$@\"\n" > atm
	chmod +x atm

bank: bin/Bank.class
	echo -n "#!/bin/bash\njava -classpath "$(shell pwd)"/bin Bank \"\$$@\"\n" > bank
	chmod +x bank

bin/Bank.class: bin
	javac -classpath src -d bin src/Bank.java

bin/ATM.class: bin
	javac -classpath src -d bin src/ATM.java

bin:
	mkdir bin

clean:
	rm -f atm bank bin/* *.auth *.card
