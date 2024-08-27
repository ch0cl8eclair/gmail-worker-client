# Introduction
This project was conceived to connect to gmail, fetch mails and parse their contents to excel. Although initially indended to parse alert and notification emails other use cases could also be fullfilled via utils.

# Getting started

## Obtain Gmail permissions

Inorder for the program to access your gmail messages you will need to give permissions as described in the [Documentation](https://developers.google.com/gmail/api/quickstart/java).
1. Create a google cloud account at https://console.cloud.google.com
2. Create a new project (I called mine Python-Gmail-Worker-24), see [here](https://developers.google.com/workspace/guides/create-project)
3. Enable the Gmail API for the project see [link](https://console.cloud.google.com/flows/enableapi?apiid=gmail.googleapis.com)
4. Configure the OAuth consent screen, goto Menu > APIs & Services > OAuth consent screen. Complete the app registration and save.
5. Generate a credentials file, goto Menu > APIs & services > Credentials
    - Click Create Credentials > OAuth client ID
    - Click Application type > Desktop app
    - enter a name and click Create
    - download the credentials file to ./src/main/resources/credentials.json

## Update project configuration

The project is driven mainly by the properties file found at: `./src/main/resources/configuration.properties`. 
The file contents look as follows:
```txt
writeMessagesToFile=false
fromEmailFilter=LinkedIn Job Alerts <jobalerts-noreply@linkedin.com>
subjectEmailFilter=
messageSearchQuery=from:jobalerts-noreply@linkedin.com newer_than:1d
messageSearchQueryLimit=50
```
- writeMessagesToFile - flag to control whether to output the message body to a file called: messages.txt
- fromEmailFilter - secondary msg filter to filter mails by sender string
- subjectEmailFilter - secondary msg filter to filter mails by subject string
- messageSearchQuery - primary query string, allows you to control the messages to fetch. Uses the gmail search query syntax
- messageSearchQueryLimit - max msg fetch limit. Msg fetch limit to help prevent the breach of the api daily quota.

## Build the project

In order to build the project you will need Java 21 & Gradle 8 on your system.
```bash
gradle clean build
```

# Run the project

To run the main LinkedIn alerts parser run:
```bash
gradle -x test run
```

The program can also be run from the jar file as follows:
```bash
java -jar ./build/libs/gmail-worker-client-1.0.jar
```

To list the labels defined in Gmail run (this is just a basic command to confirm successful setup):
```bash
gradle -x test run --args="labels"
```

To simply list the emails run:
```bash
gradle -x test run --args="list"
```

When the program is first run a url will be output to the console. Click on this url to open it within a browser. Then select you google account and select continue on the following dialog windows. Once done an access token will be returned to the running process allowing it to continue (press return in the console if it does not move on).

The token will expire after around a week, when this happens the program will exit with an error. In this scenario it is best to remvoe the expired token and then re-run the program which will output the url for you to go to.
```bash
rm tokens/StoredCredential
```

## Using Docker
The project can be build and run with docker, to build it run the following command from the project folder:
```bash
docker build -t klairtech/gmail-parser .
```

To run the program:
```bash
docker run -it --rm -P klairtech/gmail-parser
```

# Makefile
The project contains a makefile that has all major targets to setup the project to running the program.

| Target | Description |
| ------ | ----------- |
| all | runs the program to fetch and export emails to csv |
| build | builds the java project and runs the tests |
| removetoken | clears the expired gmail access token |
| initpython | setups the python environment and installs dependencies |
| clean | remove the log files |

# Filtering results
The project contains a python script to remove duplicate entries from generated csv files. I chose Python to implement this functionality over Java because I wanted to make use of the excellent library I was familiar with: Pandas.
To use this script first install and setup the environment:
```bash
cd ./src/main/python
python -m venv env
source env/bin/activate
pip install -r ./requirements.txt
```
To run the script, you must give it the source csv file and the script will output the results to a file of the same name but with a `-filtered` suffix.
```bash
cd ./src/main/python
source env/bin/activate
python ./filter.py ../../../linkedInAlerts-`date '+%d%m%y'`.csv
```

# Extending the functionality
You can easily extend the functionality by adding your own parser and targetting whatever subset of emails you wish.
Simply extend the following interfaces:
```bash
src/main/java/parser/MessageParser.java
src/main/java/parser/CSVRecord.java
```
The parser should have a no args contructor and you can add your own functionality with the parse method.
The parser returns a list of objects from a single message that hold the data. This design was to handle a linkedIn job alert email that has many jobs, which is then returned as a list of parsed jobs.
The CSVRecord interface allows the parsed record to be output to a csv file. The parser has a method to determine the name of the output file.