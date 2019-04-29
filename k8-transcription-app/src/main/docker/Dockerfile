FROM jeanblanchard/java:8
MAINTAINER MHS, mhs@into.software
COPY @project.artifactId@.jar /app/ala-@project.artifactId@.jar
#COPY debug.jar /app/ala-@project.artifactId@.jar
COPY logback.xml /app
WORKDIR /app
ENTRYPOINT ["java","-jar","/app/ala-@project.artifactId@.jar"]
