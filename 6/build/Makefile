SHELL := /bin/bash


all: atm bank
	@echo ""
	@echo "************"
	@echo "* Creating project DONE"
	@echo "************"

# venv:
# 	@echo ""
# 	@echo "************"
# 	@echo "*"
# 	@echo "* Create venv and install python dependencies"
# 	@echo "*"
# 	@echo "************"
# 	mkdir venv
# 	virtualenv -p python3 venv
# 	$(SHELL) -c "source venv/bin/activate; pip install --upgrade --no-index --find-links='./other_deps' pip; pip install --no-index --find-links='./dependencies' --find-links='./other_deps' -r ./other_deps/requirements.txt -r ./dependencies/requirements.txt"
# 	@echo "************"
# 	@echo "* Done"
# 	@echo "************"

atm:
	@echo ""
	@echo "************"
	@echo "*"
	@echo "* Create atm script"
	@echo "*"
	@echo "************"
	#@echo "#!/bin/bash" >> atm
	#@echo "source "$(CURDIR)"/venv/bin/activate" >> atm
	#@echo "python3 "$(CURDIR)"/atm.py \$$@" >> atm
	cp atm.py atm
	chmod +x atm

bank:
	#@echo "#!/bin/bash" >> bank
	#@echo "source "$(CURDIR)"/venv/bin/activate" >> bank
	#@echo "python3 "$(CURDIR)"/bank.py \$$@" >> bank
	cp bank.py bank
	chmod +x bank


fixdeps:
	@echo ""
	@echo "************"
	@echo "*"
	@echo "* Freezing and downloading python dependencies"
	@echo "*"
	@echo "************"
	rm -r dependencies
	mkdir dependencies
	pip freeze > dependencies/requirements.txt
	cd dependencies; pip download --python-version 34 --no-deps -r requirements.txt
	@echo "************"
	@echo "* Done"
	@echo "************"

test:
	@echo $(CURDIR)
	rm -rf *.exits *.sh
	./createTests


clean:
	@echo ""
	@echo "************"
	@echo "*"
	@echo "* Clean project"
	@echo "*"
	@echo "************"
	rm -rf *.auth *.card *.exits *.sh venv atm bank aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbb ...card ..card
	@echo "************"
	@echo "* Done"
	@echo "************"
