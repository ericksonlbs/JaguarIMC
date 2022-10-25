all: prepare build

# Copy Jacoco Core dependency to local maven repository
prepare:
	mvn install:install-file -Dfile=br.usp.each.saeg.jaguar.core/lib/org.jacoco.core-0.7.6.jar \
     -DgroupId=br.usp.each.saeg -DartifactId=org.jacoco.core \
     -Dversion=0.7.6 -Dpackaging=jar

# Build only Jaguar Core and its dependencies
build_core:
	mvn install -pl br.usp.each.saeg.jaguar.core -am
	cp ./br.usp.each.saeg.jaguar.core/target/br.usp.each.saeg.jaguar.core-1.0.0-jar-with-dependencies.jar ./br.usp.each.saeg.jaguar.maven.plugin/src/main/lib/jaguar.core-1.0.0-jar-with-dependencies.jar

# Build all modules
build:
	mvn clean install -Dmaven.test.failure.ignore=true
	
# Run a simple example use of Jaguar Core
run:	
	./br.usp.each.saeg.jaguar.example/run.sh
	
# Build and install Jaguar Maven Plugin
build_maven:	
	mvn clean install -pl br.usp.each.saeg.jaguar.maven.plugin -am
	mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file \
	 -Dfile=./br.usp.each.saeg.jaguar.maven.plugin/target/jaguar-maven-plugin-0.0.1-SNAPSHOT.jar
	
# Verify example
verify_example:
	mvn clean verify -pl br.usp.each.saeg.jaguar.example -am -Dmaven.test.failure.ignore=true

# Build the whole project from a docker image
docker:
	docker build -t myrepo/jaguar-core:latest -f br.usp.each.saeg.jaguar.build/Dockerfile .