# Java RMI Distributed Ticket System

Implemented a festival's ticket reservation system as a distributed application using Java RMI. The distributed system allows clients to login and perform operations on the 3 remote servers (MTL, TOR, VAN) using a permission based system. Each city's server hosts their own server data structure corresponding to their respective events.

# Usage

### Launching the Servers

Using 3 separate terminal instances run the following commands to compile and start each server

```
> javac <server-name>.java
> java <server-name>

# ex.   > javac MTLServer.java
        > java MTLServer
```

### Launching the Client

Using another terminal instance run the following commands to compile and start the client server used to login from and perform operations

```
> javac ClientServer.java
> java ClientServer
```

### Client ID usage

The client ID used to login to the client server follows the structure:

`<server-name><client-type><client-id>`

Ex.

```
MTL Admin User: `MTLA1234`
TOR User: `TORP1234`
```

From which the `<server-name>` stems from the server shorthand names [MTL, TOR, VAN]
and the `<client-type>` stems from either 'A' or 'P' (although any letter that is not 'A' will have regular user permissions)

# Server Data Structure

```java
HashMap<EventType, HashMap<String, EventData>> serverData;

// Example Structure
serverData {
    EventType.Arts: {
        "<event-id>": {
            capacity: int,
            guests: String[]
        },
        "MTLM010122": {
            capacity: 9,
            guests: ["MTLP5555"]
        },
        "MTLM101122": {...},
    },
    EventType.Theatre: {...},
    EventType.Concert: {...}
}
```
