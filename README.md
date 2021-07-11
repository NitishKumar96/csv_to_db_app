# CSV to DB app

An application to store the csv file into database

To run the application

## Create data base cluster

In main directory run
```sh
docker-compose up -d 
```
this will create one mysql database container on port 3306 and a phpmyadmin container on port 8080.
You can change root passwords in docker-compose file.

## case 1: only java application

All the code and application is present in /java dir, including a already prepared app.jar, hence you can run the following commands.

```sh
$ java -jar ./node/read_app.jar --help
Usage: csv_reader [-hV] [-ai=<analysis_index>] [--batch_size=<batch_size>]
                  [-f=<file_name>] [-t=<table_name>]
                  [--thread_count=<thread_count>]
Reads csv file and store it into mysql table.
      -ai, --analysis_index=<analysis_index>
                  Index of column in csv to be analysed.
      --batch_size=<batch_size>
                  batch size of DB insert
  -f, --file_name=<file_name>
                  .csv file name we have read.
  -h, --help      Show this help message and exit.
  -t, --table_name=<table_name>
                  table name, if not provided will make new table
      --thread_count=<thread_count>
                  number of threads to write data into db.
  -V, --version   Print version information and exit.
```

There are sample files present in java/samples dir which can be used to test the app.

```sh
$ java -jar ./java/app.jar -f ./java/sample/50.csv --thread_count 2 -ai 14 --batch_size 20 
```


## case 2: node app with java app

This contains a basic implementation of node app which will host a HTML form to upload a csv file, and on submit will call the same jar app to process it.

to run the node app 
```sh
node ./node/main.js
```
then visit 
```url
localhost:3000/csv
```
Files uploaded will be placed in ./node/uploads dir with updated names.