.PHONY: all build removecredentials initpython clean filter

TODAY := $(shell date '+%d%m%y')

all:
	gradle -x test run
	cd ./src/main/python && source env/bin/activate && python ./filter.py ../../../linkedInAlerts-$(TODAY).csv

build:
	gradle -x test build

removetoken:
	@fd StoredCredential -x rm

initpython:
	@cd ./src/main/python && python -m venv env
	@cd ./src/main/python && source env/bin/activate && pip install -r ./requirements.txt

filter:
	@rm -f ../../../linkedInAlerts-$(TODAY)-filtered.csv
	@cd ./src/main/python && source env/bin/activate && python ./filter.py ../../../linkedInAlerts-$(TODAY).csv

clean:
	@rm -rf *.log

