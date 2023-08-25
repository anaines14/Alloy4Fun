# alloy4fun-api

## How to run

### Prerequisites
- Docker
- Npm 18
- Node
- Meteor

### Setup
- Create `.env` file in the root directory of the project based on `.env.example` file.
- Download the datasets using the `downloadData.py` script in the `data/import` directory.
- Download the ".als" files for each challenge using the Alloy4Fun website.
- Run the `dataPreparation.py` script in the `data/import` directory to prepare the datasets for import.
- Build the API docker image by running the `build_docker_image.bat` script. 
- Start the docker containers using `docker-compose up` in the root directory of the project. This will start the API, the MongoDB database and the Neo4j database.
- Run `npm install` in the `meteor` directory.
- Run `meteor --settings settings.json` in the `meteor` directory or `npm run start`. This will start the Meteor frontend.
- Now, we must import the prepared data into the MongoDB and Neo4j databases. To do this, run the `importDataMongo.py` and `importDataNeo4j.py` scripts in the `data/import` directory. You can also use the API to import the data by sending requests.
- Create the graphs for SpecAssistant by sending a GET request to 
`localhost:8080/hint/setup-graphs` with an array of challenge IDs as the body.

### Usage

Example of a hint generation request for HiGenA:
```
POST localhost:8080/hint/higena-hint
body:
    {
	"challenge": "dkZH6HJNQNLLDX6Aj",
	"predicate": "inv5",
	"model": "sig User {follows : set User,sees : set Photo,posts : set Photo,suggested : set User} sig Influencer extends User {} sig Photo {date : one Day} sig Ad extends Photo {} sig Day {} pred inv5 {all inf : Influencer | all u: User | inf in u.follows}"
    }
```

Example of a hint generation request for SpecAssistant (same body as HiGenA):
```
GET localhost:8080/hint/get
```

Example of a hint generation request for SpecAssistant using HiGenA message generate (same body as HiGenA):
```
POST localhost:8080/hint/spec-hint
```
