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
    - [Client](#client)
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

To start the server, execute:
```bash
java GoFundMeServer
```
The server will initiate and await client connections.

### Client

To kick off a client instance:
```bash
java GoFundMeClient
```
Follow the on-screen instructions to engage with the application.

## Features

- **Create Event**: Users can establish a new fundraising event by detailing the event name, target amount, and deadline.
- **List Events**: Presents a list of ongoing and previous fundraising events.
- **Donate**: Users can contribute to an existing event by selecting the event index and indicating the donation sum.
- **Check Event Details**: Showcases in-depth info about a chosen event.

## Troubleshooting

- **Server Address Already in Use**: This error signifies the server port is currently occupied. Ensure no other instances of the server are active, or consider altering the port number in the source code.
  
- **Connection Refused on Client**: Confirm the server is operational and tuning in on the appropriate port.

## Contributors

- [Christopher Robinson](<https://github.com/Christopher-C-Robinson>)
- [Elvis Acheampong](<https://github.com/ElvisKobi>)

