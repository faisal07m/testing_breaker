
bank:
	./maven/bin/mvn --version
	./maven/bin/mvn -Dmaven.repo.local=./lib -o -P!online package
	sed -e 's/__MAINCLASS__/de.upb.bibifi2018.kaffeeklatsch.atm.AtmMain/' < shellwrapper_prelude.sh > atm
	sed -e 's/__MAINCLASS__/de.upb.bibifi2018.kaffeeklatsch.bank.BankMain/' < shellwrapper_prelude.sh > bank
	cat target/kaffeeklatsch-*.jar >> atm
	cat target/kaffeeklatsch-*.jar >> bank
	chmod 755 atm bank

clean:
	rm -r target || true
	rm atm bank || true

offline:
	./maven/bin/mvn -Dmaven.repo.local=./lib -P!online package
	find ./lib -name '_remote.repositories' -delete
	find ./lib -name '*.lastUpdated' -delete

test: bank
	rm -r testdir_oracle || true
	mkdir testdir_oracle
	cp atm bank testdir_oracle/
	cp -r testcases testdir_oracle/testcases
	cd testdir_oracle && python3 ../run_oracle.py
