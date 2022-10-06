import java.io.Serializable;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

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

        private String getGuests() {
            if (this.guests.size() == 0)
                return "N/A";

            return this.guests.toString();
        }

        @Override
        public String toString() {
            return "\tcapacity: " + String.valueOf(this.capacity) + " guests: " + this.getGuests();
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

    public enum ServerAction implements Serializable {
        // admin
        add,
        remove,
        list,

        // regular
        reserve,
        get,
        cancel;
    }

    public class ServerRequest implements Serializable {
        ServerAction type;
        UserInfo user;
        String eventType;

        String id = "";
        String eventId = "";

        // only ServerAction.add
        int capacity = 0;

        // construct ServerAction.list
        public ServerRequest(ServerAction type, UserInfo user, String eventType) {
            this.type = type;
            this.user = user;
            this.eventType = eventType;
        }

        // construct ServerAction.get
        // note - differentiating bool added from list
        public ServerRequest(ServerAction type, UserInfo user, String id, boolean _unused) {
            this.type = type;
            this.user = user;
            this.id = id;
        }

        // construct ServerAction.remove
        public ServerRequest(ServerAction type, UserInfo user, String eventType, String eventId) {
            this.type = type;
            this.user = user;
            this.eventType = eventType;
            this.eventId = eventId;
        }

        // construct ServerAction.reserve
        // construct ServerAction.cancel
        public ServerRequest(ServerAction type, UserInfo user, String eventType, String id, String eventId) {
            this.type = type;
            this.user = user;
            this.eventType = eventType;
            this.id = id;
            this.eventId = eventId;
        }

        // construct ServerAction.add
        public ServerRequest(ServerAction type, UserInfo user, String eventType, String eventId, int capacity) {
            this.type = type;
            this.user = user;
            this.eventType = eventType;
            this.eventId = eventId;
            this.capacity = capacity;
        }

    }

    // admin
    public class ListRequest extends ServerRequest {
        public ListRequest(UserInfo user, String eventType) {
            super(ServerAction.list, user, eventType);
        }
    }

    public class AddRequest extends ServerRequest {
        public AddRequest(UserInfo user, String eventType, String eventId, int capacity) {
            super(ServerAction.add, user, eventType, eventId, capacity);
        }
    }

    public class RemoveRequest extends ServerRequest {
        public RemoveRequest(UserInfo user, String eventType, String eventId) {
            super(ServerAction.remove, user, eventType, eventId);
        }
    }

    // regular
    public class ReserveRequest extends ServerRequest {
        public ReserveRequest(UserInfo user, String eventType, String id, String eventId) {
            super(ServerAction.reserve, user, eventType, id, eventId);
        }
    }

    public class GetRequest extends ServerRequest {
        public GetRequest(UserInfo user, String id) {
            super(ServerAction.get, user, id, true);
        }
    }

    public class CancelRequest extends ServerRequest {
        public CancelRequest(UserInfo user, String id, String eventId) {
            super(ServerAction.cancel, user, "", id, eventId);
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