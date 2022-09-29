import java.io.Serializable;
import java.rmi.*;
import java.util.EnumSet;

public interface IServer extends Remote {
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