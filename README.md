#  Welcome to the Springboot instance of XDATA



## Pre-requisites for running the software?

You need to have a Postgres instance installed in your local system or have access to a remote Postgres instance. You need to have a particular database instance, database user and password with access rights on that database and database ip and ports handy with you. These need to be passed into the input json to XDATA.

## How to execute the XDATA FAT JAR?

To run the jar and expose the API, you need to run the fat jar which has the XDATA code embedded in it. The command required from Terminal is *java -jar target/practice-0.0.1-SNAPSHOT.jar* (if you run it from the base folder of the codebase). This will initialize the Spring servlet and expose the APIs.

## How to pass your inputs to the API?

Right now, XDATA exposes two APIs, one where both the instructor and student queries are SQL strings and the second where the student query is a JSON executable.

### 1. Both SQL Strings

This is hosted in /test/getxdataoutput. We will coming to the input JSON format in a bit..
You can use both Postman and Swagger for calling this API.
For Postman, you need to send a post request to http://localhost:8080/test/rest/uploadXDATA. You can have the body format as 'raw' and populate the json in the body. Make sure to make the format as 'JSON'.
![Screenshot from 2022-08-07 17-48-18](https://user-images.githubusercontent.com/104480282/183290206-635f7b88-ad71-4aaf-aee3-02a422917a08.png)


For Swagger, you need to open http://localhost:8080/swagger-ui/index.html#/ in your web browser. In the JSONInput field of the /test/getxdataoutput post request, populate the input JSON. Make sure to change the parameter content type to "application/json".

![Screenshot from 2022-08-07 17-49-40](https://user-images.githubusercontent.com/104480282/183290256-b144e857-8b88-4002-b003-fe053a310854.png)

### 2. Instructor: SQL String and Student: JSON

This currently works through Postman. You need to go send a post request to the URL: http://localhost:8080/test/rest/uploadXDATA. Change the body type to form data. Upload your input SQL jar in the field for "file". Upload your input json in the field called jsonInput. Make sure to change the format of input to text.
![Screenshot from 2022-08-07 17-44-35](https://user-images.githubusercontent.com/104480282/183290125-8631dcb4-85fa-43fc-a54a-6c1efac41696.png)

## What kind of JARs does XDATA accept?

Currently XDATA expects that your JAR will run a SQL query on the Postgres DB and populate the result in a table which is called "student_result" by default

## Hardcodings/Config file in the codebase

As such, I have tried to avoid any hard coding in the codebase as much as possible. However there are a few unsaid constrints in the codebase.

### 1. Maximum size for executable JAR student query

This is set in the application.properties file under src/main/resources. Change the properties spring.servlet.multipart.max-file-size and spring.servlet.multipart.max-request-size if you need to change increase/decrease the limits.

### 2. Table name created by the executable should be the same in the input json executableCommand ( -t parameter) and executableTableName.
If no executableTableName is passed, XDATA assumes name of table created is "Student_result".

### 3. As mentioned above, the database name, user, password, port and ip details should already be present.

## How to compile the fat jar if you make some changes in the codebase?

Since I have used Eclipse exclusively, so this is assuming you are using Eclipse. Also assuming you have already loaded the project as a new Maven project in your eclipse. You will have to install Eclipse m2e from the Eclipse marketplace if you dont have maven support in your eclipse.

1.Right click on the projct name and select Maven. Under Maven, you can select "update project..."

2.Right click on project anme and select "Run As". Under Run As, select "Maven build..." and give "clean package" under goals and run. Your updated fat jar will be ready.

## Input JSON fields and sample Input JSON

![Screenshot from 2022-08-07 18-01-42](https://user-images.githubusercontent.com/104480282/183290681-79c07f30-b147-4a0c-9132-85bcfce1cb14.png)

### Sample Input JSON

[apiinput.txt](https://github.com/pratyoyd/XData-main-springboot/files/9276818/apiinput.txt)


## Output JSON fields and sample Output JSON

![Screenshot from 2022-08-07 18-12-13](https://user-images.githubusercontent.com/104480282/183291064-9d4ae59b-037c-4147-ad03-2a124aac3800.png)

mutantDBType: Purpose of the mutant db which killed the mutant query

mutantDBContent: Data present in the different tables of the mutant db

rsExtracted(Instructor): Result produced by Instructor query on the mutant db.

rsHidden(Student): Result produced by Student query on the mutant db.

### Sample Output JSON


[apioutput.txt](https://github.com/pratyoyd/XData-main-springboot/files/9276815/apioutput.txt)



