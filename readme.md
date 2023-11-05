# GoFundMe Simulator: UDP Client-Server Application

A simple simulation of a GoFundMe-like platform utilizing a UDP client-server model. The server manages fundraising events while multiple clients can create, view, and donate to these events concurrently.

## Features

- UDP socket communication between clients and server
- Multi-threading on the server-side to handle multiple clients concurrently
- Thread synchronization to manage shared resources
- User-friendly client interface to create, view, and donate to fundraising events
- Exception handling to ensure robustness

## How to Run

1. Compile the Java files:
   ```
   javac GoFundMeServer.java GoFundMeClient.java
   ```
2. Start the server:
   ```
   java GoFundMeServer
   ```
3. In a new terminal, start a client:
   ```
   java GoFundMeClient
   ```

## Client Operations

- **Create a new fundraising event**: Specify the name, target amount, and deadline of the event.
- **List fundraising events**: View a list of ongoing and past fundraising events.
- **Donate to an event**: Specify the event and the amount to donate.
- **Check event details**: View the details of a specific fundraising event.
- **Exit**: Exit the client application.

## Server Operations

The server will automatically handle incoming client requests for the above operations and maintain the state of all fundraising events. It also logs client connections and disconnections.

## Exception Handling

Exception handling is implemented using try-catch blocks to ensure robustness against erroneous input and to manage I/O exceptions.

## Contributors 

- [Christopher Robinson](<https://github.com/Christopher-C-Robinson>)
- [Elvis Acheampong](<https://github.com/ElvisKobi>)
