import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Map;

public class Server extends UnicastRemoteObject implements IServer {
    // possible string[] for child hashmap value? (currently only used by capacity)
    private HashMap<EventType, HashMap<String, Integer>> serverData = new HashMap<>();

    public Server() throws RemoteException {
        super();
    }

    public Server(HashMap<EventType, HashMap<String, Integer>> serverData) throws RemoteException {
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
        for (Map.Entry<EventType, HashMap<String, Integer>> x : this.serverData.entrySet()) {
            EventType e = x.getKey();
            HashMap<String, Integer> event = x.getValue();
            s.append(e.name() + "\n");
            for (Map.Entry<String, Integer> c : event.entrySet()) {
                s.append(String.format("\t%s : %d\n", c.getKey(), c.getValue()));
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

        HashMap<String, Integer> events = this.serverData.get(eventType);
        if (events.containsKey(eventId)) {
            return new Response(String.format("Unable to add eventId: %s as it already exists.", eventId));
        }

        events.put(eventId, capacity);
        this.serverData.put(eventType, events);
        return new Response(String.format("Successfully added eventId: %s", eventId));
    }

    public Response remove(UserInfo user, String eventId, EventType eventType) {
        if (!user.hasPermission(Permission.remove)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.remove.label.toUpperCase());
        }

        return new Response("REMOVE: Good for now");
    }

    public Response list(UserInfo user, EventType eventType) {
        if (!user.hasPermission(Permission.list)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.list.label.toUpperCase());
        }

        return new Response("LIST: Good for now");
    }

    // Regular Operations
    public Response reserve(UserInfo user, String id, String eventId, EventType eventType) {
        if (!user.hasPermission(Permission.reserve)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.reserve.label.toUpperCase());
        }

        return new Response("RESERVE: Good for now");
    }

    public Response get(UserInfo user, String id) {
        if (!user.hasPermission(Permission.get)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.get.label.toUpperCase());
        }

        return new Response("GET: Good for now");
    }

    public Response cancel(UserInfo user, String id, String eventId) {
        if (!user.hasPermission(Permission.cancel)) {
            return new Response(
                    "User doesn't have valid permissions to access : " + Permission.cancel.label.toUpperCase());
        }

        return new Response("CANCEL: Good for now");
    }
}
