import java.io.IOException;
import java.rmi.*;

public interface RegistryServerIntf extends Remote {

	boolean RegisterServer(String name) throws IOException, RemoteException;;
	String[] GetFileServers() throws IOException, RemoteException;;
}
