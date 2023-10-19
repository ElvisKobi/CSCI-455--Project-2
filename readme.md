# GoFundMe Simulation

This application simulates a simple GoFundMe platform where users can create fundraising events, donate to existing events, and check the details of events.

## Table of Contents
- [GoFundMe Simulation](#gofundme-simulation)
  - [Table of Contents](#table-of-contents)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
  - [Usage](#usage)
    - [Server](#server)
      - [Starting the Server](#starting-the-server)
      - [Client Connection](#client-connection)
      - [Client Disconnection](#client-disconnection)
    - [Client](#client)
      - [1. Create a new fundraising event](#1-create-a-new-fundraising-event)
      - [2. List fundraising events](#2-list-fundraising-events)
      - [3. Donate to an event](#3-donate-to-an-event)
      - [4. Check event details](#4-check-event-details)
      - [5. Exit](#5-exit)
  - [Communication](#communication)
  - [Features](#features)
  - [Troubleshooting](#troubleshooting)
  - [Contributors](#contributors)

## Getting Started

### Prerequisites

Ensure you have the following installed:
- **Java** (Version 18 or higher)
- An IDE or terminal to run Java applications

### Installation

1. Clone the repository to your local machine.
```bash
git clone https://github.com/Christopher-C-Robinson/CSCI-455--Project-1
```

2. Navigate to the project directory.
```bash
cd path/to/directory
```

3. Compile the Java files.
```bash
javac GoFundMeServer.java GoFundMeClient.java
```

## Usage

### Server

#### Starting the Server
When server starts:
```
Server started. Listening on port [PORT_NUMBER]
```

#### Client Connection
When a new client connects:
```
[TIME]: New client connected: IP = [CLIENT_IP_ADDRESS], Port = [CLIENT_PORT]
```

#### Client Disconnection
When a client disconnects:
```
[TIME]: Client disconnected: IP = [CLIENT_IP_ADDRESS], Port = [CLIENT_PORT]
```

### Client

When the client starts, a main menu displays:
```
Choose an option:
1. Create a new fundraising event
2. List fundraising events
3. Donate to an event
4. Check event details
5. Exit
```

#### 1. Create a new fundraising event
You'll be prompted to input:
- Event name
- Target amount
- Deadline

After entering these, the event is created and you go back to the main menu.

#### 2. List fundraising events
Choosing this shows a list of all ongoing and past events. Each event has an index, and details like the name, target amount, and deadline are shown. You then return to the main menu.

#### 3. Donate to an event
You'll be asked to:
- Enter the index of the event to donate to
- Specify the donation amount

Once the donation is done, a confirmation shows and you go back to the main menu.

#### 4. Check event details
You'll need to:
- Enter the index of the event to check

The client shows detailed info like the name, amount raised, and deadline. You then go back to the main menu.

#### 5. Exit
This option closes the client.

## Communication
The client and server communicate through Java sockets. Data is sent and received using DataInputStream and DataOutputStream.

## Features

- **Create Event**: Users can set up a new fundraising event by giving the event name, target amount, and deadline.
- **List Events**: Shows a list of ongoing and past fundraising events.
- **Donate**: Users can give to an existing event by picking the event index and stating the donation amount.
- **Check Event Details**: Gives detailed info about a chosen event.

## Troubleshooting

- **Server Address Already in Use**: This error means the server port is in use. Make sure no other server instances are active, or think about changing the port number in the source code.
- **Connection Refused on Client**: Make sure the server is running and listening on the right port.

## Contributors

- [Christopher Robinson](<https://github.com/Christopher-C-Robinson>)
- [Elvis Acheampong](<https://github.com/ElvisKobi>)
