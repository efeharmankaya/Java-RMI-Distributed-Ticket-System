import java.io.Serializable;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

public interface IServer extends Remote {
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
            return "capacity: " + String.valueOf(this.capacity) + " guests: " + this.guests.toString();
        }
    }

    public static enum Permission {
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

    public enum EventType {
        Arts,
        Theatre,
        Concert,
        None;
    };

    public static enum ServerPort {
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

    // TODO hasPermission - in user or server?
    public class ClientPermissions implements Serializable {
        ClientType type;
        EnumSet<Permission> permissions;

        public ClientPermissions(ClientType type) {
            this.type = type;
            this.permissions = this.type.equals(ClientType.A) ? EnumSet.allOf(Permission.class) : BasicPermissions;
        }
    }

    public static enum ClientType {
        A('A'),
        P('P');

        public final char label;

        private ClientType(char label) {
            this.label = label;
        }
    }

    // TODO Reponse<T> for additional message args?
    public class Response implements Serializable {
        String message;

        public Response(String message) {
            this.message = message;
        }
    }

    public String getIntroMessage(UserInfo user) throws java.rmi.RemoteException;

    public String getUserOptions(UserInfo user) throws java.rmi.RemoteException;

    // !!!!!!! TESTING ONLY
    public String show() throws java.rmi.RemoteException;
    // !!!!!!! TESTING ONLY

    // Server Operations
    public HashMap<String, EventData> getServerData(UserInfo user, EventType eventType)
            throws java.rmi.RemoteException, Exception;

    public HashMap<EventType, HashMap<String, EventData>> getAllServerData(UserInfo user)
            throws java.rmi.RemoteException, Exception;

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