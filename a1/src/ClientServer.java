import java.io.*;
import java.rmi.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientServer {
    static InputStreamReader is = new InputStreamReader(System.in);
    static BufferedReader br = new BufferedReader(is);

    public static void main(String[] args) {
        try {
            String registryURL;
            UserInfo userInfo = getUserInfo();

            registryURL = "rmi://localhost:" + String.valueOf(userInfo.server.PORT) + "/mtl";
            IServer server = (IServer) Naming.lookup(registryURL);

            String message = "";
            String input = "";
            IServer.Response response = null;

            while (!input.equalsIgnoreCase("exit")) {
                // TODO add input param to getMessage
                if (message.isEmpty() && input.isEmpty())
                    message = server.getIntroMessage(userInfo);
                else
                    message = server.getUserOptions(userInfo);
                input = getUserInput(message);

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
                // !!!!!!! TESTING ONLY

                if (input.length() > 0 && input.contains(" ")) {
                    String[] inputCommands = input.split(" ");
                    String action = inputCommands[0];
                    if (action.equalsIgnoreCase(IServer.Permission.add.label)) {
                        response = add(server, userInfo, inputCommands);
                    } else if (action.equalsIgnoreCase(IServer.Permission.remove.label)) {
                        response = remove(server, userInfo, inputCommands);
                    }
                    if (response != null) {
                        System.out.println(response.message);
                    }

                } else {
                    System.out.println("Error: Invalid Input");
                }

            }

        } catch (Exception e) {
            System.out.println("Error in ClientServer: " + e.getMessage());
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
        // ! Testing
        // System.out.println("USER");
        // System.out.println(user.toString());
        return user;
    }

    public static IServer.Response add(IServer server, UserInfo user, String[] inputCommands) {
        if (inputCommands.length != 4) {
            return new IServer.Response("Invalid input parameters for 'add' | requires exactly 4 parameters | Note: "
                    + IServer.Permission.add.message);
        }

        String eString = inputCommands[2];
        IServer.EventType eventType = IServer.EventType.None;
        for (IServer.EventType e : IServer.EventType.values()) {
            if (e.equals(IServer.EventType.None))
                continue;
            if (eString.equalsIgnoreCase(e.name())) {
                eventType = e;
            }
        }
        if (eventType.equals(IServer.EventType.None)) {
            return new IServer.Response("Invalid eventType | Options: " + IServer.EventType.values().toString());
        }

        int capacity;
        try {
            capacity = Integer.parseInt(inputCommands[3]);
        } catch (NumberFormatException e) {
            return new IServer.Response("Invalid capacity | Must be of type int");
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

        // TODO possible eventType from string function (repeated in add function)
        String eString = inputCommands[2];
        IServer.EventType eventType = IServer.EventType.None;
        for (IServer.EventType e : IServer.EventType.values()) {
            if (e.equals(IServer.EventType.None))
                continue;
            if (eString.equalsIgnoreCase(e.name())) {
                eventType = e;
            }
        }
        if (eventType.equals(IServer.EventType.None)) {

            return new IServer.Response("Invalid eventType | Options: " + Arrays.asList(IServer.EventType.values()));
        }

        IServer.Response response;
        try {
            response = server.remove(user, inputCommands[1], eventType);
        } catch (RemoteException e) {
            return new IServer.Response("Remote Exception in REMOVE: " + e.getMessage());
        }
        return response;
    }
}