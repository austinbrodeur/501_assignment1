# CPSC 501 Refactoring Assignment

## About
This program is an old networks assignment of mine that simulates a UDP file transfer to a server that has a predetermined amount of packet loss. The goal of the assignment was to ensure the right packets are being sent to the server so it can successfully build the file without packets redundantly being sent.

There are two portions of the assignment that I was not required to write, as the code was provided by the professor. Therefore, I did not refactor any of these portions, as they don't appear to have much need for refactoring, and I don't have a great understanding of how they work. The portions created by the prof are:
1. a3.jar, which contains the dependencies for the TxQueue and Segment classes
2. All of the code in the Server directory

The remaining code in the client directory was written by me, and was refactored by me.

I tried to not make any significant changes to any of the code dealing with threads, as I know it can get messy trying to understand how they work. Most of my refactorings had to do with simpler, easy to follow functionality.

## To Compile and Run
This code requires the a3.jar file in the Client directory as a depencency, as well as all of the files in the lib directory, so these must be added to the classpath to run and compile. I created this project in Intellij, so if Intellij is used, you can simply open the project file and everything should be configured for you.

Before running the client code, go into the server directory and execute the *server.sh* shell script to open an instance of a local server. This file can be edited to change the probability of packet loss.

For the sake of marking, you can just set the packet window and timeout timer arbitrarily, (100 and 100, for example).

