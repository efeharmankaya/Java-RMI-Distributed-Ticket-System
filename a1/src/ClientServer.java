import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.*;

// TODO concurrency within city servers
// TODO UDP/IP inter-server communication instead of rmi
//https://www.baeldung.com/udp-in-java

public class ClientServer {
    static InputStreamReader is = new InputStreamReader(System.in);
    static BufferedReader br = new BufferedReader(is);

    // Logging
    static Logger logger = Logger.getLogger(ClientServer.class.getSimpleName());
    static FileHandler fh;

    // TODO delete udp in client
    UDPClient udp = new UDPClient();

    public static void main(String[] args) {
        try {
            new ClientServer().start();
        } catch (Exception e) {
            System.out.println("Exception in main: " + e.getMessage());
        }
    }

    public void start() {
        try {

            String registryURL;
            UserInfo userInfo = getUserInfo();
            // Logging setup
            fh = new FileHandler(String.format("logs/client/%s.log", userInfo.clientId));
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setUseParentHandlers(false);
            logger.info(String.format("""
                        %s logged in
                    """, userInfo.clientId));
            registryURL = "rmi://localhost:" + String.valueOf(userInfo.server.PORT) + "/"
                    + userInfo.server.name().toLowerCase();
            IServer server = (IServer) Naming.lookup(registryURL);

            logger.info(String.format("Connected to %s at Port: %s\n", userInfo.server.name().toUpperCase(),
                    userInfo.server.PORT));

            IServer.Response message = new IServer.Response();
            String input = "";
            IServer.Response response = null;

            while (!input.equalsIgnoreCase("exit")) {
                if (message.isEmpty() && input.isEmpty()) {
                    message = server.getIntroMessage(userInfo);
                    logger.info(String.format("""
                            Requested intro message from the server
                            Parameters: UserInfo
                            Completed: %s
                            Response: %s
                            """, message.status, message.message));
                } else {
                    message = server.getUserOptions(userInfo);
                    logger.info(String.format("""
                                Requested options message from the server
                                Parameters: UserInfo
                                Completed: %s
                                Response: %s
                            """, message.status, message.message));
                }
                input = getUserInput(message.message);

                // !!!!!!! TESTING ONLY
                if (input.equalsIgnoreCase("x")) {
                    try {
                        System.out.println("=========SHOW=========");
                        System.out.println(server.show());
                        System.out.println("=========SHOW=========");
                    } catch (RemoteException e) {
                        System.out.println("Show remote exception: " + e.getMessage());
                    }
                    continue;
                }

                if (input.equalsIgnoreCase("send")) {
                    IServer.Response res = udp.sendMessage("Testing");
                    System.out.println("RESPONSE FROM UDP: " + res);
                    continue;
                }
                // !!!!!!! TESTING ONLY

                if (input.length() > 0 && input.contains(" ")) {
                    String[] inputCommands = input.split(" ");
                    String action = inputCommands[0];
                    if (action.equalsIgnoreCase(IServer.Permission.add.label)) {
                        response = add(server, userInfo, inputCommands);
                    } else if (action.equalsIgnoreCase(IServer.Permission.remove.label)) {
                        response = remove(server, userInfo, inputCommands);
                    } else if (action.equalsIgnoreCase(IServer.Permission.list.label)) {
                        response = list(server, userInfo, inputCommands);
                    } // regular operations
                    else if (action.equalsIgnoreCase(IServer.Permission.reserve.label)) {
                        response = reserve(server, userInfo, inputCommands);
                    } else if (action.equalsIgnoreCase(IServer.Permission.get.label)) {
                        response = get(server, userInfo, inputCommands);
                    } else if (action.equalsIgnoreCase(IServer.Permission.cancel.label)) {
                        response = cancel(server, userInfo, inputCommands);
                    }

                    if (response != null && !response.isEmpty()) {
                        System.out.println(response.message);
                        logEvent(userInfo, inputCommands, response);
                    } else {
                        throw new Exception("Response == null, no response received from the server.");
                    }

                } else if (!input.equalsIgnoreCase("exit")) {
                    System.out.println("Error: Invalid Input");
                }
            }
            fh.close();
        } catch (Exception e) {
            System.out.println("Error in ClientServer: " + e.getMessage());
        }
    }

    public class UDPClient {
        DatagramSocket socket;
        InetAddress address;
        byte[] buffer;

        public UDPClient() {
            try {
                this.socket = new DatagramSocket();
                this.address = InetAddress.getByName("localhost");
            } catch (Exception e) {
                System.out.println("Exception in UDPClient constructor: " + e.getMessage());
                return;
            }
        }

        // TODO try singular function to parse through required data from send and
        // return needed data
        public IServer.Response sendMessage(String message) {
            try {
                this.buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length, this.address, 2111); // temp
                                                                                                                 // for
                                                                                                                 // mtl
                this.socket.send(packet);
                packet = new DatagramPacket(this.buffer, this.buffer.length);
                this.socket.receive(packet);
                String response = new String(packet.getData(), 0, packet.getLength());
                return new IServer.Response(response);
            } catch (Exception e) {
                System.out.println("Exception in UDPClient sendMessage: " + e.getMessage());
                return new IServer.Response("Exception in UDPClient sendMessage: " + e.getMessage());
            }
        }
    }

    public static String getUserInput(String message) {
        System.out.println(message);
        System.out.print("> ");
        String input = "";
        try {
            input = br.readLine();
        } catch (IOException e) {
            System.out.println("IOException in getUserInput: " + e.getMessage());
        }
        return input;
    }

    public static UserInfo getUserInfo() {
        UserInfo user = new UserInfo();
        while (!user.validate()) {
            try {
                System.out.print("Enter your client ID: ");
                String clientId = br.readLine();
                user.setClientId(clientId);
            } catch (Exception e) {
                System.out.println("getUserInfo error: " + e.getMessage());
            }
            if (!user.validate())
                System.out.println("Invalid client ID | try again\n");
        }
        return user;
    }

    public static IServer.Response add(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 4) {
            return new IServer.Response("Invalid input parameters for 'add' | requires exactly 4 parameters | Note: "
                    + IServer.Permission.add.message);
        }

        IServer.EventType eventType = eventFromString(inputCommands[2]);
        if (eventType.equals(IServer.EventType.None)) {
            return new IServer.Response("Invalid eventType | Options: " + IServer.EventType.values().toString());
        }

        int capacity;
        try {
            capacity = Integer.parseInt(inputCommands[3]);
            if (capacity < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return new IServer.Response("Invalid capacity | Must be of type int >= 0");
        }

        IServer.Response response;
        try {
            response = server.add(user, inputCommands[1], eventType, capacity);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in ADD: " + e.getMessage());
        }
        return response;
    }

    public static IServer.Response remove(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 3) {
            return new IServer.Response("Invalid input parameters for 'remove' | requires exactly 3 parameters | Note: "
                    + IServer.Permission.remove.message);
        }

        IServer.EventType eventType = eventFromString(inputCommands[2]);
        if (eventType.equals(IServer.EventType.None))
            return new IServer.Response("Invalid eventType | Options: " + Arrays.asList(IServer.EventType.values()));

        IServer.Response response;
        try {
            response = server.remove(user, inputCommands[1], eventType);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in REMOVE: " + e.getMessage());
        }
        return response;
    }

    public static IServer.Response list(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 2) {
            return new IServer.Response("Invalid input parameters for 'list' | requires exactly 2 parameters | Note: "
                    + IServer.Permission.list.message);
        }

        IServer.EventType eventType = eventFromString(inputCommands[1]);
        if (eventType.equals(IServer.EventType.None))
            return new IServer.Response("Invalid eventType | Options: " + Arrays.asList(IServer.EventType.values()));

        IServer.Response response;
        try {
            response = server.list(user, eventType);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in LIST: " + e.getMessage());
        }
        return response;
    }

    public static IServer.Response reserve(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 4) {
            return new IServer.Response(
                    "Invalid input parameters for 'reserve' | requires exactly 4 parameters | Note: "
                            + IServer.Permission.reserve.message);
        }

        IServer.EventType eventType = eventFromString(inputCommands[3]);
        if (eventType.equals(IServer.EventType.None))
            return new IServer.Response("Invalid eventType | Options: " + Arrays.asList(IServer.EventType.values()));

        IServer.Response response;
        try {
            response = server.reserve(user, inputCommands[1], inputCommands[2], eventType);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in RESERVE: " + e.getMessage());
        }
        return response;
    }

    public static IServer.Response get(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 2) {
            return new IServer.Response(
                    "Invalid input parameters for 'get' | requires exactly 2 parameters | Note: "
                            + IServer.Permission.get.message);
        }

        IServer.Response response;
        try {
            response = server.get(user, inputCommands[1]);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in GET: " + e.getMessage());
        }
        return response;
    }

    public static IServer.Response cancel(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 3)
            return new IServer.Response("Invalid input parameters for 'cancel' | requires exactly 3 parameters | Note: "
                    + IServer.Permission.cancel.message);

        IServer.Response response;
        try {
            response = server.cancel(user, inputCommands[1], inputCommands[2]);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in CANCEL: " + e.getMessage());
        }
        return response;
    }

    public static IServer.EventType eventFromString(String eString) {
        IServer.EventType eventType = IServer.EventType.None;
        for (IServer.EventType e : IServer.EventType.values()) {
            if (e.equals(IServer.EventType.None))
                continue;
            if (eString.equalsIgnoreCase(e.name())) {
                eventType = e;
            }
        }
        return eventType;
    }

    public static void logEvent(UserInfo user, String[] inputCommands, IServer.Response response) {
        logger.info(String.format("""

                    Calling user: %s
                    Requested %s from server
                    Parameters: %s
                    Completed: %s
                    Response:
                %s
                """, user.clientId, inputCommands[0].toUpperCase(), inputCommands.toString(), response.status,
                response.message));
    }
}
