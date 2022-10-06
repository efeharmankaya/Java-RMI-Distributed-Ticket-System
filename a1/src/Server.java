import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

public class Server extends UnicastRemoteObject implements IServer {
    // Logging
    static Logger logger;
    static FileHandler fh;
    UDP udp;

    HashMap<EventType, HashMap<String, EventData>> serverData = new HashMap<>();
    public String name;
    int UDPPort;

    public Server(String name) throws RemoteException {
        super();
        this.name = name;
        // TODO setup function to combine both server constructors
        this.logger = Logger.getLogger(name);
        try {
            this.fh = new FileHandler(String.format("logs/server/%s.log", name));
        } catch (IOException e) {
            System.out.println("Exception in Server() : " + e.getMessage());
            return;
        }
        this.logger.addHandler(this.fh);
        SimpleFormatter formatter = new SimpleFormatter();
        this.fh.setFormatter(formatter);
        this.logger.setUseParentHandlers(false);
        // this.UDPPort = getUDPPort();
        this.udp = new UDP(getUDPPort());
        this.udp.start();
    }

    public Server(String name, HashMap<EventType, HashMap<String, EventData>> serverData) throws RemoteException {
        super();
        this.name = name;
        this.serverData = serverData;
        this.logger = Logger.getLogger(name);
        try {
            this.fh = new FileHandler(String.format("logs/server/%s.log", name));
        } catch (IOException e) {
            System.out.println("Exception in Server() : " + e.getMessage());
            return;
        }
        this.logger.addHandler(this.fh);
        SimpleFormatter formatter = new SimpleFormatter();
        this.fh.setFormatter(formatter);
        this.logger.setUseParentHandlers(false);
        // this.UDPPort = getUDPPort();
        this.udp = new UDP(getUDPPort());
        this.udp.start();
    }

    public class UDP extends Thread {
        boolean running;
        DatagramSocket socket;
        // byte[] buffer = new byte[256];
        int port;

        public UDP(int port) {
            super();
            this.running = true;
            this.port = port;
            try {
                this.socket = new DatagramSocket(this.port);
            } catch (Exception e) {
                System.out.println("Exception in UDPServer creating DatagramSocket: " + e.getMessage());
                return;
            }
            // try {
            // this.socket = new DatagramSocket(this.port);
            // } catch (Exception e) {
            // System.out.println("Exception in UDPServer creating DatagramSocket: " +
            // e.getMessage());
            // return;
            // }
        }

        // public Response receiveRequest(ServerRequest request) {

        // }

        public void run() {
            // DatagramSocket socket = new DatagramSocket(this.port);
            while (running) {
                try {

                    byte[] in = new byte[1042];
                    DatagramPacket packet = new DatagramPacket(in, in.length);

                    System.out.println("Waiting to receive...");
                    socket.receive(packet);
                    System.out.println("Received packet");
                    ByteArrayInputStream bais = new ByteArrayInputStream(in);
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    ServerRequest request;
                    try {
                        request = (ServerRequest) ois.readObject();
                    } catch (Exception e) {

                        System.out.println("exception in readObject: " + e.getMessage());
                        System.out.println(ois.available());
                        System.out.println(this.port);
                        continue;
                    }

                    System.out.println("Received request: " + request.user.clientId);
                    System.out.println("Received request: " + request.eventType);

                    // System.out.println("Received request: " + request.message);

                    // parse return address
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    Response response = new Response("coming back from server");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(response);
                    byte[] out = baos.toByteArray();

                    packet = new DatagramPacket(out, out.length, address, port);
                    socket.send(packet);

                    // DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
                    // System.out.println("before packet receive");
                    // this.socket.receive(packet);
                    // System.out.println("after packet receive");

                    // InetAddress address = packet.getAddress();
                    // int port = packet.getPort();
                    // System.out.println("Before object retrieval");
                    // // Received Request
                    // ObjectInputStream iStream = new ObjectInputStream(new
                    // ByteArrayInputStream(this.buffer));
                    // System.out.println("after object input stream");
                    // ServerRequest request = (ServerRequest) iStream.readObject();
                    // System.out.println("request eventType: " + request.eventType);
                    // // System.out.println(String.format("""
                    // // Request:
                    // // UserId: %s
                    // // EventType: %s
                    // // """, request.user.clientId, request.eventType.toString()));

                    // // System.out.println("received from UDPclient: "
                    // // + new String(packet.getData(), 0, packet.getLength()));
                    // // Response res = new Response("from server: " + class.name);
                    // Response response = new Response("from server: ");
                    // ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    // ObjectOutputStream oos = new ObjectOutputStream(baos);
                    // oos.writeObject(response);
                    // byte[] buffer = baos.toByteArray();

                    // // this.buffer = res.getBytes();
                    // packet = new DatagramPacket(buffer, buffer.length, address, port);
                    // // String received = new String(packet.getData(), 0, packet.getLength());

                    // // if (received.equals("end")) {
                    // // this.running = false;
                    // // continue;
                    // // }
                    // socket.send(packet);
                } catch (Exception e) {
                    System.out.println("Exception in UDPServer: " + e.getMessage());
                }
            }
            // this.socket.close();
        }
    }

    // TODO fix UDPport logic + w/ input ServerPort
    int getUDPPort() {
        switch (this.name.toLowerCase()) {
            case "mtl":
                return ServerPort.MTL.PORT + 1000;
            case "tor":
                return ServerPort.TOR.PORT + 1000;
            case "van":
                return ServerPort.VAN.PORT + 1000;
            default:
                return 4444;
        }
    }

    int getUDPPort(ServerPort s) {
        return s.PORT + 1000;
    }

    // TODO log events per server
    public Response getIntroMessage(UserInfo user) {
        String options = getUserOptions(user).message;
        return new Response(String.format("""
                Welcome %s
                Connected Server: %s
                ====================
                %s
                """, user.clientId, user.server.name(), options), true);
    }

    public Response getUserOptions(UserInfo user) {
        StringBuffer options = new StringBuffer("\n");
        for (Permission permission : user.permissions.permissions) {
            options.append(permission.message);
        }
        return new Response(options.toString(), true);
    }

    // !!!!!!! TESTING ONLY
    public String show() {
        StringBuffer s = new StringBuffer();
        for (Map.Entry<EventType, HashMap<String, EventData>> x : this.serverData.entrySet()) {
            EventType e = x.getKey();
            HashMap<String, EventData> event = x.getValue();
            s.append(e.name() + "\n");
            for (Map.Entry<String, EventData> c : event.entrySet()) {
                s.append(String.format("\t%s : %s\n", c.getKey(), c.getValue().toString()));
            }
        }
        return s.toString();
    }
    // !!!!!!! TESTING ONLY

    private String printEvents(EventType eventType) {
        HashMap<String, EventData> events = this.serverData.get(eventType);
        StringBuilder out = new StringBuilder();
        // (eventId, EventData)
        for (Map.Entry<String, EventData> e : events.entrySet()) {
            out.append(String.format("%s\n%s\n", e.getKey(), e.getValue().toString()));
        }
        return out.toString();
    }

    // Server Operations
    // Admin Operations
    public Response add(UserInfo user, String eventId, EventType eventType, int capacity) {
        if (!user.hasPermission(Permission.add)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.add.label.toUpperCase());
        }
        String eventLocationId = eventId.substring(0, 3);
        // admin operation on current server
        if (eventLocationId.equalsIgnoreCase(this.name)) {
            HashMap<String, EventData> events = this.serverData.get(eventType);
            if (events.containsKey(eventId))
                return new Response(String.format("Unable to add eventId: %s as it already exists.", eventId));

            events.put(eventId, new EventData(capacity));
            this.serverData.put(eventType, events);
            return new Response(String.format("Successfully added eventId: %s", eventId), true);
        }

        return new Response("Unable to add event - Event is based on a remote server.");
    }

    public Response remove(UserInfo user, String eventId, EventType eventType) {
        if (!user.hasPermission(Permission.remove)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.remove.label.toUpperCase());
        }

        String eventLocationId = eventId.substring(0, 3);
        // admin operation on current server
        if (eventLocationId.equalsIgnoreCase(this.name)) {
            HashMap<String, EventData> events = this.serverData.get(eventType);
            if (!events.containsKey(eventId))
                return new Response("Unable to remove event: eventId does not exist");

            events.remove(eventId);
            this.serverData.put(eventType, events);
            return new Response(String.format("Successfully removed eventId: %s", eventId), true);
        }

        return new Response("Unable to remote event - Event is based on a remote server.");
    }

    // TODO fix with UDP
    public Response list(UserInfo user, EventType eventType) {
        if (!user.hasPermission(Permission.list)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.list.label.toUpperCase());
        }
        // current server events
        StringBuilder events = new StringBuilder(printEvents(eventType));

        // fetch remote server events

        // TODO refactor into separate function?
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;

            ServerRequest request = new ServerRequest(user, eventType.toString());
            // Response request = new Response("test");
            Response response = sendServerRequest(request, server);
        }

        return new Response(events.toString(), true);

        // StringBuilder events = new StringBuilder(printEvents(eventType));
        // // admin operation from remote server
        // if (!user.server.name().equalsIgnoreCase(this.name)) {
        // return new Response(events.toString(), true);
        // }

        // // admin operation from current server
        // for (ServerPort s : ServerPort.values()) {
        // if (s.PORT == -1 || s.PORT == user.server.PORT)
        // continue;
        // try {
        // String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" +
        // s.name().toLowerCase();
        // IServer remServer = (IServer) Naming.lookup(registryURL);
        // events.append(remServer.list(user, eventType).message);
        // } catch (Exception e) {
        // System.out.println("Exception in admin list on current server: " +
        // e.getMessage());
        // return new Response("Exception in admin list on current server: " +
        // e.getMessage());
        // }
        // }
        // return new Response(events.toString(), true);
    }

    public Response sendServerRequest(ServerRequest request, ServerPort server) {
        try {
            DatagramSocket socket = new DatagramSocket();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(request);
            byte[] out = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(out, out.length, InetAddress.getByName("localhost"),
                    getUDPPort(server));
            System.out.println("Sending request to: " + server.name().toUpperCase() + " port: "
                    + String.valueOf(getUDPPort(server)));
            socket.send(packet);

            byte[] in = new byte[256];
            packet = new DatagramPacket(in, in.length);
            socket.receive(packet);

            ByteArrayInputStream bais = new ByteArrayInputStream(in);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Response response = (Response) ois.readObject();

            System.out.println("Received: " + response.message);
            return response;
        } catch (Exception e) {
            System.out.println("Exception in sendRequest: " + e.getMessage());
            return new Response("Exception in sendRequest: " + e.getMessage());
        }
    }

    // Regular Operations
    // TODO fix with UDP
    public Response reserve(UserInfo user, String id, String eventId, EventType eventType) {
        if (!user.hasPermission(Permission.reserve)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.reserve.label.toUpperCase());
        }

        String eventLocationId = eventId.substring(0, 3);
        if (eventLocationId.equalsIgnoreCase(this.name)) { // operation on current server
            HashMap<String, EventData> eventData = this.serverData.get(eventType);
            if (!eventData.containsKey(eventId))
                return new Response(String.format("Unable to reserve eventId: %s - Does not exist.", eventId));

            if (eventData.get(eventId).capacity < 1)
                return new Response(String.format("Unable to reserve eventID: %s - No remaining tickets.", eventId));

            if (eventData.get(eventId).guests.contains(id))
                return new Response(String.format(
                        "Unable to reserve eventId: %s for clientId: %s - Client already has a reservation", eventId,
                        id));

            // check if called through remote server - to ensure max 3 remote reservations
            if (!eventLocationId.equalsIgnoreCase(user.server.name())) {
                int count = 0;
                // (eventId, EventData)
                for (Map.Entry<String, EventData> e : this.serverData.get(eventType).entrySet()) {
                    if (e.getValue().guests.contains(id))
                        count++;
                }
                if (++count > 3)
                    return new Response("Unable to reserve event - Maximum of 3 remote reservations per city.");
            }

            eventData.get(eventId).addGuest(id);
            return new Response(String.format("Successfully reserved eventType: %s eventId: %s for clientId: %s",
                    eventType.name(), eventId, id), true);
        }

        // operation on remote server
        for (ServerPort s : ServerPort.values()) {
            if (s.PORT == -1 || s.PORT == user.server.PORT)
                continue;
            if (eventLocationId.equalsIgnoreCase(s.name())) { // found remote server
                try {
                    String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                    IServer remServer = (IServer) Naming.lookup(registryURL);
                    return remServer.reserve(user, id, eventId, eventType);
                } catch (Exception e) {
                    System.out.println("Exception in reserve on remote server: " + e.getMessage());
                    return new Response("Exception in reserve on remote server: " + e.getMessage());
                }
            }
        }

        // invalid eventId (doesn't match <SERVER-NAME>ID pattern)
        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server.", eventId));
    }

    private String getEventsById(String id) {
        StringBuilder events = new StringBuilder(this.name);
        // (EventType, EventId Hashmap)
        for (Map.Entry<EventType, HashMap<String, EventData>> eType : this.serverData.entrySet()) {
            if (eType.getKey().equals(EventType.None))
                continue;

            ArrayList<String> tempEvents = new ArrayList<>();
            // (eventId, EventData)
            for (Map.Entry<String, EventData> eData : eType.getValue().entrySet()) {
                if (eData.getValue().guests.contains(id))
                    tempEvents.add(eData.getKey());
            }
            events.append("\t" + eType.getKey().name() + " : " + tempEvents.toString() + "\n");
        }
        return events.toString();
    }

    public Response get(UserInfo user, String id) {
        if (!user.hasPermission(Permission.get)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.get.label.toUpperCase());
        }

        // TODO fix formatting
        return new Response(getEventsById(id));
        // StringBuilder clientEvents = new StringBuilder(getEventsById(id));
        // if (user.server.name().equalsIgnoreCase(this.name)) { // operation on current
        // server
        // return new Response(String.format("""
        // %s
        // """, clientEvents.toString()), true);
        // }

        // operation on current server
        // for (ServerPort s : ServerPort.values()) {
        // if (s.PORT == -1 || s.PORT == user.server.PORT)
        // continue;

        // try {
        // String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" +
        // s.name().toLowerCase();
        // IServer remServer = (IServer) Naming.lookup(registryURL);
        // clientEvents.append("\n" + remServer.get(user, id).message);
        // } catch (Exception e) {
        // System.out.println("Exception in get on remote server: " + e.getMessage());
        // return new Response("Exception in get on remote server: " + e.getMessage());
        // }
        // }
        // return new Response(clientEvents.toString(), true);
    }

    public Response cancel(UserInfo user, String id, String eventId) {
        if (!user.hasPermission(Permission.cancel)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.cancel.label.toUpperCase());
        }
        String eventLocationId = eventId.substring(0, 3);
        // operation on current server
        if (eventLocationId.equalsIgnoreCase(this.name)) {
            // (key, value) => (eventType, eventData HashMap)
            for (Map.Entry<EventType, HashMap<String, EventData>> event : this.serverData.entrySet()) {
                // (key, value) => (eventId, eventData)
                for (Map.Entry<String, EventData> eventData : event.getValue().entrySet()) {
                    if (eventData.getKey().equalsIgnoreCase(eventId)) {
                        if (!eventData.getValue().guests.contains(id))
                            return new Response(String.format(
                                    "Unable to cancel ticket - %s does not have a reservation for %s", id, eventId));

                        if (!user.clientId.equalsIgnoreCase(id) && !user.isAdmin()) // user != ticket holder && !admin
                            return new Response(
                                    "Unable to cancel the ticket - Tickets must be cancelled by the original ticket holder.");

                        this.serverData.get(event.getKey()).get(eventData.getKey()).removeGuest(id);
                        return new Response("Successfully canceled the ticket for eventId: " + eventId, true);
                    }
                }
            }
            return new Response("Unable to cancel ticket - eventId does not exist.");
        }

        return new Response("Unable to remote event - Event is based on a remote server.");
        // operation on remote server
        // for (ServerPort s : ServerPort.values()) {
        // if (s.PORT == -1 || s.PORT == user.server.PORT)
        // continue;
        // if (eventLocationId.equalsIgnoreCase(s.name())) { // found remote server
        // try {
        // String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" +
        // s.name().toLowerCase();
        // IServer remServer = (IServer) Naming.lookup(registryURL);
        // return remServer.cancel(user, id, eventId);
        // } catch (Exception e) {
        // System.out.println("Exception in cancel on remote server: " +
        // e.getMessage());
        // return new Response("Exception in cancel on remote server: " +
        // e.getMessage());
        // }
        // }
        // }

        // // invalid eventId (doesn't match <SERVER-NAME>ID pattern)
        // return new Response(String.format("Invalid eventId: %s - Unable to connect to
        // remote server.", eventId));

    }
}