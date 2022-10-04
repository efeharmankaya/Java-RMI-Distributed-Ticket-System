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
    public String name;

    public Server(String name) throws RemoteException {
        super();
        this.name = name;
    }

    public Server(String name, HashMap<EventType, HashMap<String, EventData>> serverData) throws RemoteException {
        super();
        this.name = name;
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
        for (ServerPort s : ServerPort.values()) {
            if (s.PORT == -1 || s.PORT == user.server.PORT)
                continue;

            if (s.name().equalsIgnoreCase(eventLocationId)) { // found remote server
                try {
                    String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                    IServer curServer = (IServer) Naming.lookup(registryURL);
                    return curServer.add(user, eventId, eventType, capacity);
                } catch (Exception e) {
                    System.out.println("Exception in admin add on remote server: " + e.getMessage());
                    return new Response("Exception in admin add on remote server: " + e.getMessage());
                }
            }
        }
        // invalid eventId (doesn't match <SERVER-NAME>ID pattern)
        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server.", eventId));
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
        for (ServerPort s : ServerPort.values()) {
            if (s.PORT == -1 || s.PORT == user.server.PORT)
                continue;

            if (s.name().equalsIgnoreCase(eventLocationId)) { // found remote server
                try {
                    String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                    IServer curServer = (IServer) Naming.lookup(registryURL);
                    return curServer.remove(user, eventId, eventType);
                } catch (Exception e) {
                    System.out.println("Exception in admin remove on remote server: " + e.getMessage());
                    return new Response("Exception in admin remove on remote server: " + e.getMessage());
                }
            }
        }
        // invalid eventId (doesn't match <SERVER-NAME>ID pattern)
        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server.", eventId));
    }

    // TODO pretty show
    public Response list(UserInfo user, EventType eventType) {
        if (!user.hasPermission(Permission.list)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.list.label.toUpperCase());
        }

        StringBuilder events = new StringBuilder(printEvents(eventType));
        // admin operation from remote server
        if (!user.server.name().equalsIgnoreCase(this.name)) {
            return new Response(events.toString(), true);
        }

        // admin operation from current server
        for (ServerPort s : ServerPort.values()) {
            if (s.PORT == -1 || s.PORT == user.server.PORT)
                continue;
            try {
                String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                IServer remServer = (IServer) Naming.lookup(registryURL);
                events.append(remServer.list(user, eventType).message);
            } catch (Exception e) {
                System.out.println("Exception in admin list on current server: " + e.getMessage());
                return new Response("Exception in admin list on current server: " + e.getMessage());
            }
        }
        return new Response(events.toString(), true);
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

        StringBuilder clientEvents = new StringBuilder(getEventsById(id));
        if (!user.server.name().equalsIgnoreCase(this.name)) { // operation on remote server
            return new Response(String.format("""
                    %s
                    """, clientEvents.toString()), true);
        }

        // operation on current server
        for (ServerPort s : ServerPort.values()) {
            if (s.PORT == -1 || s.PORT == user.server.PORT)
                continue;

            try {
                String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                IServer remServer = (IServer) Naming.lookup(registryURL);
                clientEvents.append("\n" + remServer.get(user, id).message);
            } catch (Exception e) {
                System.out.println("Exception in get on remote server: " + e.getMessage());
                return new Response("Exception in get on remote server: " + e.getMessage());
            }
        }
        return new Response(clientEvents.toString(), true);
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

        // operation on remote server
        for (ServerPort s : ServerPort.values()) {
            if (s.PORT == -1 || s.PORT == user.server.PORT)
                continue;
            if (eventLocationId.equalsIgnoreCase(s.name())) { // found remote server
                try {
                    String registryURL = "rmi://localhost:" + String.valueOf(s.PORT) + "/" + s.name().toLowerCase();
                    IServer remServer = (IServer) Naming.lookup(registryURL);
                    return remServer.cancel(user, id, eventId);
                } catch (Exception e) {
                    System.out.println("Exception in cancel on remote server: " + e.getMessage());
                    return new Response("Exception in cancel on remote server: " + e.getMessage());
                }
            }
        }

        // invalid eventId (doesn't match <SERVER-NAME>ID pattern)
        return new Response(String.format("Invalid eventId: %s - Unable to connect to remote server.", eventId));

    }
}
