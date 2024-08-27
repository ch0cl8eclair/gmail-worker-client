.PHONY: all build removecredentials initpython clean

all:
	gradle -x test run
	@cd ./src/main/python && source env/bin/activate && python ./filter.py ../../../linkedInAlerts-`date '+%d%m%y'`.csv

build:
	gradle -x test build

removetoken:
	@fd StoredCredential -x rm

initpython:
	@cd ./src/main/python && python -m venv env
	@cd ./src/main/python && source env/bin/activate && pip install -r ./requirements.txt

clean:
	@rm -rf *.log
