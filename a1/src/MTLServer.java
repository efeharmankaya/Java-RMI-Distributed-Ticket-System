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
            startRegistry(PORT_NUM);
            // STARTING MTL SERVER DATA
            // TODO add guests
            HashMap<IServer.EventType, HashMap<String, IServer.EventData>> initialServerData = new HashMap<>() {
                {
                    put(IServer.EventType.Arts, new HashMap<>() {
                        {
                            put("MTLM010122", new IServer.EventData(5, new String[] { "MTLP5555", "TORP5555" }));
                            put("MTLA010122", new IServer.EventData(0,
                                    new String[] { "MTLP1111", "MTLP2222", "MTLP3333", "MTLP4444", "MTLP5555" }));
                            put("MTLE010122", new IServer.EventData(15));
                        }
                    });
                    put(IServer.EventType.Concert, new HashMap<>() {
                        {
                            put("MTLM020122", new IServer.EventData(5, new String[] { "MTLP5555", "TORP5555" }));
                            put("MTLA020122", new IServer.EventData(3, new String[] { "MTLP2222" }));
                            put("MTLE020122", new IServer.EventData(15, new String[] { "TORP5555" }));
                        }
                    });
                    put(IServer.EventType.Theatre, new HashMap<>() {
                        {
                            put("MTLM030122", new IServer.EventData(5, new String[] { "MTLP5555" }));
                            put("MTLA030122", new IServer.EventData(3, new String[] { "VANP5555", "VANP2222" }));
                            put("MTLE030122", new IServer.EventData(15, new String[] { "MTLP2222" }));
                        }
                    });
                }
            };

            Server serverObj = new Server(initialServerData);
            registryURL = "rmi://localhost:" + PORT_NUM + "/mtl";
            Naming.rebind(registryURL, serverObj);
            System.out.println("Server registered.  Registry currently contains:");
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
