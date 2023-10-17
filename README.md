# CSCI-455-Project-1-Client
This is the client for CSCI-455: Networking and Parallel Computation project 1.

## Authors
Brandon Gasser and Carson Miller

## What does the client do?
The client displays a user interface for the user to interact with. It has options for the user to input what server they would like to connect to, or if none is given, will try to connect to the users self-hosted server. The client will then communicate with the server to allow the user to interact with different fundraisers by creating and donating to them.

Because this works over a network when stated to, sometimes the tcp connection may fail. If this happens, either try to click "connect" again, or restart the client altogether.

## Logging
The client is built with MVVM architecture. With this in mind, each component will print whenever a component has been instructed to talk to the server. You will see multiple log messages for doing something such as connecting to a server to show each component communicating with another to achieve the goal with a final message from a base component stating its success. Messages are also printed out when sent or received so communication is clear between the client and server to the user. 

NOTE: Logs are outputted to the command line the client was launched from **NOT** in the client UI itself! 

## Building and Running from Source

### Requirements

- [JDK 17+](https://www.oracle.com/java/technologies/downloads/#java17)
- [Gradle](https://gradle.org/install/)

Clone the repo and run `gradle build` in the project's base directory.

## Running from Release

### Requirements

- [JDK 17+](https://www.oracle.com/java/technologies/downloads/#java17)

Download the Jar file in the releases tab and run it. Ensure that you download the Jar for your OS. Run `java -jar client-<Your OS>.jar`