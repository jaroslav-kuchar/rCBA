#!/bin/bash
# mvn package
mvn clean package -DskipTests=true

rm -rf ../inst/java/classes/
cp -fR ./target/classes ../inst/java/

# rm -rf ../inst/java/lib/
# cp -fR ./target/lib ../inst/java/
