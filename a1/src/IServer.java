import java.io.Serializable;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

public interface IServer extends Remote {
    // TODO Serializable???
    class EventData implements Serializable {
        int capacity;
        ArrayList<String> guests;

        public EventData() {
            this.capacity = 0;
            this.guests = new ArrayList<>();
        }

        public EventData(int capacity) {
            this.capacity = capacity;
            this.guests = new ArrayList<>();
        }

        public EventData(int capacity, String[] guests) {
            this.capacity = capacity;
            this.guests = new ArrayList<String>(Arrays.asList(guests));
        }

        public void addGuest(String id) {
            this.guests.add(id);
            this.capacity--;
        }

        public void removeGuest(String id) {
            this.guests.remove(id);
            this.capacity++;
        }

        @Override
        public String toString() {
            return "\tcapacity: " + String.valueOf(this.capacity) + " guests: " + this.guests.toString();
        }
    }

    public static enum Permission implements Serializable {
        // regular permissions
        reserve("reserve",
                "reserve clientID eventID eventType - Reserves a ticket for a given clientID at the given eventID\n"),
        get("get", "get clientID - Gets all active tickets for a given clientID (all cities)\n"),
        cancel("cancel", "cancel clientID eventID - Cancels the ticket for given clientID at the given eventID\n"),
        // admin permissions
        add("add", "add eventID eventType capacity - Adds a new reservation slot with a given eventID\n"),
        remove("remove", "remove eventID eventType - Removes a reservation slot for a given eventID\n"),
        list("list", "list eventType - Lists all available reservation slot for a given eventType (in all cities)\n"),
        exit("exit", "exit - Exits the client session\n");

        public String message, label;

        Permission(String label, String message) {
            this.label = label;
            this.message = message;
        }

        @Override
        public String toString() {
            return this.message;
        }
    }

    public static final EnumSet<Permission> BasicPermissions = EnumSet.of(Permission.reserve, Permission.get,
            Permission.cancel, Permission.exit);
    public static final EnumSet<Permission> AdminPermissions = EnumSet.allOf(Permission.class);

    public enum EventType implements Serializable {
        Arts,
        Theatre,
        Concert,
        None;
    };

    public static enum ServerPort implements Serializable {
        MTL(1111),
        TOR(1112),
        VAN(1113),
        NONE(-1);

        public final int PORT;

        private ServerPort(int PORT) {
            this.PORT = PORT;
        }

        public boolean validate() {
            if (this.PORT == ServerPort.NONE.PORT)
                return false;
            return true;
        }
    }

    public class ClientPermissions implements Serializable {
        ClientType type;
        EnumSet<Permission> permissions;

        public ClientPermissions(ClientType type) {
            this.type = type;
            this.permissions = this.type.equals(ClientType.A) ? EnumSet.allOf(Permission.class) : BasicPermissions;
        }
    }

    public static enum ClientType implements Serializable {
        A('A'),
        P('P');

        public final char label;

        private ClientType(char label) {
            this.label = label;
        }
    }

    public class Response implements Serializable {
        String message;
        boolean status;

        public Response() {
            this.message = "";
            this.status = false;
        }

        public Response(String message) {
            this.message = message;
            this.status = false;
        }

        public Response(String message, boolean status) {
            this.message = message;
            this.status = status;
        }

        public boolean isEmpty() {
            return this.message.isEmpty();
        }
    }

    public enum ServerActions implements Serializable {
        list,
        reserve;
    }

    public class ServerRequest implements Serializable {
        UserInfo user;
        String eventType;

        public ServerRequest(UserInfo user, String eventType) {
            this.user = user;
            this.eventType = eventType;
        }
    }

    public Response getIntroMessage(UserInfo user) throws java.rmi.RemoteException;

    public Response getUserOptions(UserInfo user) throws java.rmi.RemoteException;

    // !!!!!!! TESTING ONLY
    public String show() throws java.rmi.RemoteException;
    // !!!!!!! TESTING ONLY

    // Server Operations
    // Admin Operations
    public Response add(UserInfo user, String eventId, EventType eventType, int capacity)
            throws java.rmi.RemoteException;

    public Response remove(UserInfo user, String eventId, EventType eventType) throws java.rmi.RemoteException;

    public Response list(UserInfo user, EventType eventType) throws java.rmi.RemoteException;

    // Regular Operations
    public Response reserve(UserInfo user, String id, String eventId, EventType eventType)
            throws java.rmi.RemoteException;

    public Response get(UserInfo user, String id) throws java.rmi.RemoteException;

    public Response cancel(UserInfo user, String id, String eventId) throws java.rmi.RemoteException;
}