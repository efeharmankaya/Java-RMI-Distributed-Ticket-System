import java.io.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class VANServer {
    static final int PORT_NUM = 1113;

    public static void main(String[] args) throws Exception {
        String registryURL;
        try {
            startRegistry(PORT_NUM);
            // STARTING VAN SERVER DATA
            HashMap<IServer.EventType, HashMap<String, IServer.EventData>> initialServerData = new HashMap<>() {
                {
                    put(IServer.EventType.Arts, new HashMap<>() {
                        {
                            put("VANM010122", new IServer.EventData(5));
                            put("VANA010122", new IServer.EventData(3));
                            put("VANE010122", new IServer.EventData(15));
                        }
                    });
                    put(IServer.EventType.Concert, new HashMap<>() {
                        {
                            put("VANM020122", new IServer.EventData(5));
                            put("VANA020122", new IServer.EventData(3));
                            put("VANE020122", new IServer.EventData(15));
                        }
                    });
                    put(IServer.EventType.Theatre, new HashMap<>() {
                        {
                            put("VANM030122", new IServer.EventData(5));
                            put("VANA030122", new IServer.EventData(3));
                            put("VANE030122", new IServer.EventData(15));
                        }
                    });
                }
            };

            Server serverObj = new Server(initialServerData);
            registryURL = "rmi://localhost:" + PORT_NUM + "/van";
            Naming.rebind(registryURL, serverObj);
            System.out.println("Server registered.  Registry currently contains:");
            listRegistry(registryURL);
            System.out.println("VAN Server ready.");
        } catch (Exception e) {
            System.out.println("Error in VANServer: " + e.getMessage());
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
