#!/bin/bash
mvn package
cp -fR ./target/classes ../inst/java/
cp -fR ./target/lib ../inst/java/
