FROM openjdk:8
COPY ./read_csv/target/ /app
COPY ./sample /app/sample
WORKDIR /app
CMD ["java", "-jar","read_csv-0.1-jar-with-dependencies.jar","-f","./sample/50.csv"]
