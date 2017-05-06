# HELIOS Lamp Simulator

This repository contains an implementation of a client of HELIOS system and it simulates the source of continuous data coming from the sensor network where nodes are the city's lamps.
The purpose of this client is testing performance of HELIOS system, offering an high load of input data. 
This application is called CINI_StreetLamp_Client, because it implements the source data simultor of a project for the **[CINI Smart City University Challenge](https://it.eventbu.com/l-aquila/cini-smart-city-university-challenge/2263724)**.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

### Prerequisites
* [Maven](https://maven.apache.org/) - Dependency Management to build the system

### Compile
To compile code with all dependencies and create the executable Java file *.jar*, first change the current directory to the main directory of the project:  
```
~$ cd {$HELIOS_CLIENT_HOME} 
{$HELIOS_CLIENT_HOME}$ mvn clean validate compile test package org.apache.maven.plugins:maven-assembly-plugin:2.2-beta-5:assembly 
```
Now *.jar* file is created in *{$HELIOS_CLIENT_HOME}/target*.

### Execution
To run locally the client, change the current directory in *{$HELIOS_CLIENT_HOME}/target* and execute

```
{$HELIOS_CLIENT_HOME}/target$ java -jar CINI_StreetLamp_Client-1.0-jar-with-dependencies.jar <rabbitHost>
```
where *<rabbitHost>* is the hostname of the input RabbitMQ queue where this application will send messages on and it can be:
* hostname of Docker container where RabbitMQ server is deployed on, if HELIOS system is running *locally* 
* hostname of Load Balancer of the Cloud cluster where HELIOS system is deployed on if it is running in *production*
  
  
## Versioning

We use [Git](https://git-scm.com/) for versioning.

## Authors

* **Ovidiu Daniel Barba** - [OviDanielB](https://github.com/OviDanielB)
* **Laura Trivelloni** - [lauratrive](https://github.com/lauratrive)
* **Emanuele Vannacci** - [Zanna-94](https://github.com/Zanna-94)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details