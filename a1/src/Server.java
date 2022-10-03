import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server extends UnicastRemoteObject implements IServer {
    // possible string[] for child hashmap value? (currently only used by capacity)
    // possible ServerData type to clean up typing
    // TODO add guest list to eventData ????
    HashMap<EventType, HashMap<String, EventData>> serverData = new HashMap<>();

    public Server() throws RemoteException {
        super();
    }

    public Server(HashMap<EventType, HashMap<String, EventData>> serverData) throws RemoteException {
        super();
        this.serverData = serverData;
    }

    public String getIntroMessage(UserInfo user) {
        String options = getUserOptions(user);
        return String.format("""
                Welcome %s
                Connected Server: %s
                ====================
                %s
                """, user.clientId, user.server.name(), options);
    }

    public String getUserOptions(UserInfo user) {
        StringBuffer options = new StringBuffer("\n");
        for (Permission permission : user.permissions.permissions) {
            options.append(permission.message);
        }
        return options.toString();
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

    // Server Operations
    // Admin Operations
    public Response add(UserInfo user, String eventId, EventType eventType, int capacity) {
        if (!user.hasPermission(Permission.add)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.add.label.toUpperCase());
        }

        HashMap<String, EventData> events = this.serverData.get(eventType);
        if (events.containsKey(eventId))
            return new Response(String.format("Unable to add eventId: %s as it already exists.", eventId));

        events.put(eventId, new EventData(capacity));
        this.serverData.put(eventType, events);
        return new Response(String.format("Successfully added eventId: %s", eventId));
    }

    public Response remove(UserInfo user, String eventId, EventType eventType) {
        if (!user.hasPermission(Permission.remove)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.remove.label.toUpperCase());
        }

        HashMap<String, EventData> events = this.serverData.get(eventType);
        if (!events.containsKey(eventId))
            return new Response("Unable to remove event: eventId does not exist");

        events.remove(eventId);
        this.serverData.put(eventType, events);
        return new Response(String.format("Successfully removed eventId: %s", eventId));
    }

    // TODO pretty show
    public Response list(UserInfo user, EventType eventType) {
        if (!user.hasPermission(Permission.list)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.list.label.toUpperCase());
        }

        HashMap<ServerPort, HashMap<String, EventData>> data = new HashMap<>() {
            {
                put(user.server, serverData.get(eventType));
            }
        };

        try {
            for (ServerPort s : ServerPort.values()) {
                if (s.PORT == -1)
                    continue;
                if (s.PORT == user.server.PORT)
                    continue;

                String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                IServer curServer = (IServer) Naming.lookup(registryURL);
                try {
                    HashMap<String, EventData> eventData = curServer.getServerData(user, eventType);
                    data.put(s, eventData);
                } catch (Exception e) {
                    System.out.println("Exception in LIST: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in LIST - obtaining rmi lookup: " + e.getMessage());
        }

        return new Response(data.toString());
    }

    // Regular Operations
    public Response reserve(UserInfo user, String id, String eventId, EventType eventType) {
        if (!user.hasPermission(Permission.reserve)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.reserve.label.toUpperCase());
        }

        HashMap<String, EventData> eventData = this.serverData.get(eventType);
        if (!eventData.containsKey(eventId))
            return new Response(String.format("Unable to reserve eventId: %s - Does not exist.", eventId));

        if (eventData.get(eventId).capacity < 1)
            return new Response(String.format("Unable to reserve eventID: %s - No remaining tickets.", eventId));

        if (eventData.get(eventId).guests.contains(id))
            return new Response(String.format(
                    "Unable to reserve eventId: %s for clientId: %s - Client already has a reservation", eventId, id));

        eventData.get(eventId).addGuest(id);
        return new Response(String.format("Successfully reserved eventType: %s eventId: %s for clientId: %s",
                eventType.name(), eventId, id));
    }

    private HashMap<EventType, ArrayList<String>> getEventsById(
            HashMap<EventType, HashMap<String, EventData>> serverData, String id) {
        HashMap<EventType, ArrayList<String>> clientEvents = new HashMap();
        for (EventType eventType : EventType.values()) {
            if (eventType.equals(EventType.None))
                continue;

            ArrayList<String> tempEvents = new ArrayList<>();
            HashMap<String, EventData> events = serverData.get(eventType);
            for (Map.Entry<String, EventData> event : events.entrySet()) {
                if (event.getValue().guests.contains(id))
                    tempEvents.add(event.getKey());
            }
            clientEvents.put(eventType, tempEvents);
        }
        return clientEvents;
    }

    public Response get(UserInfo user, String id) {
        if (!user.hasPermission(Permission.get)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.get.label.toUpperCase());
        }

        HashMap<String, HashMap<EventType, ArrayList<String>>> clientEvents = new HashMap<>();
        try {
            for (ServerPort s : ServerPort.values()) {
                if (s.PORT == -1)
                    continue;

                if (s.PORT == user.server.PORT) { // current server
                    clientEvents.put(s.name(), getEventsById(this.serverData, id));
                } else { // remote server
                    String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                    IServer remServer = (IServer) Naming.lookup(registryURL);
                    try {
                        HashMap<EventType, HashMap<String, EventData>> remData = remServer.getAllServerData(user);
                        clientEvents.put(s.name(), getEventsById(remData, id));
                    } catch (Exception e) {
                        System.out.println("Exception in get - during inter-server connection: " + e.getMessage());
                    }
                }
            }
            System.out.println(clientEvents.toString());
        } catch (Exception e) {
            System.out.println("Exception in get: " + e.getMessage());
        }

        return new Response(clientEvents.toString());
    }

    public Response cancel(UserInfo user, String id, String eventId) {
        if (!user.hasPermission(Permission.cancel)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.cancel.label.toUpperCase());
        }

        // TODO cannot cancel unless (admin or user === ticket holder)
        // (key, value) => (eventType, eventData HashMap)
        for (Map.Entry<EventType, HashMap<String, EventData>> event : this.serverData.entrySet()) {
            // (key, value) => (eventId, eventData)
            for (Map.Entry<String, EventData> eventData : event.getValue().entrySet()) {
                if (eventData.getKey().equalsIgnoreCase(eventId)) {
                    if (!eventData.getValue().guests.contains(id))
                        return new Response(String.format(
                                "Unable to cancel ticket - %s does not have a reservation for %s", id, eventId));
                    this.serverData.get(event.getKey()).get(eventData.getKey()).removeGuest(id);
                    return new Response("Successfully canceled the ticket");
                }
            }
        }

        return new Response("Unable to cancel ticket - eventId does not exist.");
    }

    // possible
    // remove these functions
    // replace with regular calling of the function in remote w/ additional param
    // (ie. set state) to stop recursion
    // and just return the called servers info
    public HashMap<EventType, HashMap<String, EventData>> getAllServerData(UserInfo user) throws Exception {
        if (!user.isAdmin())
            throw new Exception("Invalid permissions to access private server data");
        return this.serverData;
    }

    public HashMap<String, EventData> getServerData(UserInfo user, EventType eventType) throws Exception {
        if (!user.isAdmin()) {
            throw new Exception("Invalid permissions to access private server data");
        }
        return this.serverData.get(eventType);
    }

}
