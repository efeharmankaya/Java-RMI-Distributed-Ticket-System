import java.io.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class MTLServer {
    static final int PORT_NUM = 1111;

    public static void main(String[] args) throws Exception {
        String registryURL;
        try {
            // ? TODO: possible user input for server port num?
            startRegistry(PORT_NUM);

            // STARTING MTL SERVER DATA
            HashMap<IServer.EventType, HashMap<String, Integer>> initialServerData = new HashMap<>() {
                {
                    put(IServer.EventType.Arts, new HashMap<>() {
                        {
                            put("MTLM010122", 5);
                            put("MTLA010122", 3);
                            put("MTLE010122", 15);
                        }
                    });
                    put(IServer.EventType.Concert, new HashMap<>() {
                        {
                            put("MTLM010122", 5);
                            put("MTLA010122", 3);
                            put("MTLE010122", 15);
                        }
                    });
                    put(IServer.EventType.Theatre, new HashMap<>() {
                        {
                            put("MTLM030122", 5);
                            put("MTLA030122", 3);
                            put("MTLE030122", 15);
                        }
                    });
                }
            };

            Server serverObj = new Server(initialServerData);
            registryURL = "rmi://localhost:" + PORT_NUM + "/mtl";
            Naming.rebind(registryURL, serverObj);
            System.out.println("Server registered.  Registry currently contains:");
            // list names currently in the registry
            listRegistry(registryURL);
            System.out.println("MTL Server ready.");
        } catch (Exception e) {
            System.out.println("Error in MTLServer: " + e.getMessage());
        }
    }

    private static void startRegistry(int RMIPortNum) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(RMIPortNum);
            registry.list(); // This call will throw an exception
                             // if the registry does not already exist
        } catch (RemoteException e) {
            // No valid registry at that port.
            System.out.println(
                    "Existing RMI registry couldn't be located at port " + RMIPortNum + ". Attempting to create...");
            Registry registry = LocateRegistry.createRegistry(RMIPortNum);
            System.out.println("RMI registry created at port " + RMIPortNum);
        }
    }

    // This method lists the names registered with a Registry object
    private static void listRegistry(String registryURL) throws RemoteException, MalformedURLException {
        System.out.println("Registry " + registryURL + " contains: ");
        String[] names = Naming.list(registryURL);
        for (int i = 0; i < names.length; i++)
            System.out.println(names[i]);
    }
}
