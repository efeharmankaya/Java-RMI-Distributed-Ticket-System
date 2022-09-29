import java.io.Serializable;

public class UserInfo implements Serializable {
    String clientId;
    IServer.ServerPort server;
    IServer.ClientPermissions permissions = new IServer.ClientPermissions(IServer.ClientType.P);

    public UserInfo() {
        this.clientId = "";
        this.server = IServer.ServerPort.NONE;
    }

    public UserInfo(String clientId) {
        this.clientId = clientId;
        this.server = IServer.ServerPort.NONE;
    };

    public UserInfo(String clientId, IServer.ServerPort server) {
        this.clientId = clientId;
        this.server = server;
    }

    public void setClientId(String clientId) {
        if (clientId.length() < 5)
            return;
        this.clientId = clientId;
        String location = clientId.substring(0, 3);
        // ? TODO: check if other occurrences of this can be deleted
        for (IServer.ServerPort server : IServer.ServerPort.values())
            if (location.equalsIgnoreCase(server.name()))
                this.server = server;
    }

    public boolean hasPermission(IServer.Permission permission) {
        return this.permissions.permissions.contains(permission);
    }

    public boolean validate() {
        boolean isValid = false;
        if (this.clientId.length() < 5)
            isValid = false;
        isValid = this.server.validate();
        if (isValid) {
            char type = this.clientId.charAt(3);
            if (type == IServer.ClientType.A.label)
                this.permissions = new IServer.ClientPermissions(IServer.ClientType.A);
        }
        return isValid;
    }

    @Override
    public String toString() {
        return String.format("""
                -----------
                Client ID: %s
                Server:
                    Name: %s
                    Port: %d
                Permissions:
                    Type: %s
                    Permissions: %s
                -----------
                """, this.clientId, this.server.name(), this.server.PORT, this.permissions.type,
                this.permissions.permissions);
    }
}
