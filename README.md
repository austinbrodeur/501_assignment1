# CPSC 501 Refactoring Assignment

## About
This program is an old networks assignment of mine that simulates a UDP file transfer to a server that has a predetermined amount of packet loss. The goal of the assignment was to ensure the right packets are being sent to the server so it can successfully build the file without packets redundantly being sent.

There are two portions of the assignment that I was not required to write, as the code was provided by the professor. Therefore, I did not refactor any of these portions, as they don't appear to have much need for refactoring, and I don't have a great understanding of how they work. The portions created by the prof are:
1. a3.jar, which contains the dependencies for the TxQueue and Segment classes
2. All of the code in the Server directory

The remaining code in the client directory was written by me, and was refactored by me.

## To Compile
This code requires the a3.jar as a depencency, so use:

`javac -cp <path to a3.jar>;<path to Client directory> *.java`

to compile.

## To Run
Before running the client code, go into the server directory and execute the *server.sh* shell script to open an instance of a local server.

A classpath is also required to run the program. The main method is contained in FastFtp, so to execute, use:

`java -cp <path to a3.jar>;<path to Client directory> FastFtp localhost 2225 <file to transfer to server> <size of packet window> <timeout time>`

For the sake of marking, you can just set the packet window and timeout timer arbitrarily, (100 and 100, for example) unless you'd like to test the performance of the program.

