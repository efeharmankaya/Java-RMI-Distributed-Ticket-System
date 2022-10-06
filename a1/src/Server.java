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
        this.udp = new UDP(this, getUDPPort());
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
        this.udp = new UDP(this, getUDPPort());
        this.udp.start();
    }

    public class UDP extends Thread {
        boolean running;
        DatagramSocket socket;
        Server server;
        int port;

        public UDP(Server server, int port) {
            super();
            this.server = server;
            this.running = true;
            this.port = port;
            try {
                this.socket = new DatagramSocket(this.port);
            } catch (Exception e) {
                System.out.println("Exception in UDPServer creating DatagramSocket: " + e.getMessage());
                return;
            }
        }

        public void run() {
            while (running) {
                try {

                    byte[] in = new byte[1042];
                    DatagramPacket packet = new DatagramPacket(in, in.length);

                    System.out.println("Waiting to receive...");
                    socket.receive(packet);
                    System.out.println("Received packet");
                    ByteArrayInputStream bais = new ByteArrayInputStream(in);
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    ServerRequest request = (ServerRequest) ois.readObject();

                    // parse return address
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    // fetch response
                    Response response;
                    if (request.type.equals(ServerAction.list)) {
                        response = this.server.list(request.user, EventType.valueOf(request.eventType));
                    } else if (request.type.equals(ServerAction.reserve)) {
                        response = this.server.reserve(request.user, request.id, request.eventId,
                                EventType.valueOf(request.eventType));
                    } else if (request.type.equals(ServerAction.add)) {
                        response = this.server.add(request.user, request.eventId, EventType.valueOf(request.eventType),
                                request.capacity);
                    } else if (request.type.equals(ServerAction.remove)) {
                        response = this.server.remove(request.user, request.eventId,
                                EventType.valueOf(request.eventType));
                    } else if (request.type.equals(ServerAction.get)) {
                        response = this.server.get(request.user, request.id);
                    } else if (request.type.equals(ServerAction.cancel)) {
                        response = this.server.cancel(request.user, request.id, request.eventId);
                    } else {
                        response = new Response("Invalid server request.");
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(response);
                    byte[] out = baos.toByteArray();

                    packet = new DatagramPacket(out, out.length, address, port);

                    System.out.println("Sending response: " + response.message);
                    socket.send(packet);
                } catch (Exception e) {
                    System.out.println("Exception in UDPServer: " + e.getMessage());
                }
            }
        }
    }

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

        // admin operation on remote server
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;
            if (eventLocationId.equalsIgnoreCase(server.name())) {
                AddRequest request = new AddRequest(user, eventType.toString(), eventId, capacity);
                Response response = sendServerRequest(request, server);
                return response;
            }
        }
        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server %s.", eventId,
                eventLocationId));
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

        // admin operation on remote server
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;
            if (eventLocationId.equalsIgnoreCase(server.name())) {
                RemoveRequest request = new RemoveRequest(user, eventType.toString(), eventId);
                Response response = sendServerRequest(request, server);
                return response;
            }
        }
        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server %s.", eventId,
                eventLocationId));
    }

    public Response list(UserInfo user, EventType eventType) {
        if (!user.hasPermission(Permission.list)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.list.label.toUpperCase());
        }
        // current server events
        StringBuilder events = new StringBuilder(printEvents(eventType));

        // called from remote - return w/o further inter-server communication
        if (!this.name.equalsIgnoreCase(user.server.name()))
            return new Response(events.toString(), true);

        // fetch remote server events
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;

            ListRequest request = new ListRequest(user, eventType.toString());
            Response response = sendServerRequest(request, server);
            if (response.status)
                events.append(response.message);
        }

        return new Response(events.toString(), true);
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

            byte[] in = new byte[1024];
            packet = new DatagramPacket(in, in.length);
            socket.receive(packet);
            System.out.println("Received response back from server");
            ByteArrayInputStream bais = new ByteArrayInputStream(in);
            ObjectInputStream ois = new ObjectInputStream(bais);

            System.out.println("before reading object");
            Response response;
            try {
                response = (Response) ois.readObject();
            } catch (Exception e) {
                System.out.println("Exception in readObject sendServerRequest: " + e.getMessage());
                return new Response(e.getMessage());
            }

            System.out.println("Received: " + response.message);

            return response;
        } catch (Exception e) {
            System.out.println("Exception in sendRequest: " + e.getMessage());
            return new Response("Exception in sendRequest: " + e.getMessage());
        }
    }

    // Regular Operations
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

            eventData.get(eventId).addGuest(new String(id));
            return new Response(String.format("Successfully reserved eventType: %s eventId: %s for clientId: %s",
                    eventType.name(), eventId, id), true);
        }

        // operation on remote server
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;
            if (eventLocationId.equalsIgnoreCase(server.name())) {
                ReserveRequest request = new ReserveRequest(user, eventType.toString(), id, eventId);
                Response response = sendServerRequest(request, server);
                return response;
            }
        }
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

    // TODO inter-server
    public Response get(UserInfo user, String id) {
        if (!user.hasPermission(Permission.get)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.get.label.toUpperCase());
        }

        StringBuilder events = new StringBuilder(getEventsById(id));

        // operation on remote server - return events w/o fetching
        if (!user.server.name().equalsIgnoreCase(this.name)) {
            return new Response(events.toString(), true);
        }
        // operation on current (home) server
        // fetch remote server events
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;

            GetRequest request = new GetRequest(user, id);
            Response response = sendServerRequest(request, server);
            if (response.status && response.message.length() > 0)
                events.append(response.message);
        }

        return new Response(events.toString(), true);
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
                                    "Unable to cancel the ticket - Tickets must be cancelled by the original ticket holder or an admin.");

                        this.serverData.get(event.getKey()).get(eventData.getKey()).removeGuest(id);
                        return new Response("Successfully canceled the ticket for eventId: " + eventId, true);
                    }
                }
            }
            return new Response("Unable to cancel ticket - eventId does not exist.");
        }

        // operation on remote server
        for (ServerPort server : ServerPort.values()) {
            if (server.PORT == -1 || server.PORT == user.server.PORT)
                continue;
            if (server.name().equalsIgnoreCase(eventLocationId)) {
                CancelRequest request = new CancelRequest(user, id, eventId);
                Response response = sendServerRequest(request, server);
                if (response.status && response.message.length() > 0)
                    return response;
            }
        }

        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server.", eventId));

    }
}