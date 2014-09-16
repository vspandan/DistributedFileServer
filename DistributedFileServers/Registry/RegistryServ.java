
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class RegistryServ extends UnicastRemoteObject
  implements RegistryServerIntf {
	
	public RegistryServ() throws RemoteException
	{
	}
	List<String> serversList =new ArrayList<String>();
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length == 2) {
			try {
				String IPAddr = args[0];
				int port = Integer.parseInt(args[1]);
				java.rmi.registry.LocateRegistry.createRegistry(port); 
				RegistryServerIntf s1 = new RegistryServ();
				RegistryServerIntf r = null;
//				Registry reg = null;
				Naming.rebind("//"+IPAddr+":"+port+"/RegistryServerIntf",s1);


			} catch (NumberFormatException nFE) {
				System.out.println("Invalid Port Number");
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		} else {
			System.out.println("Invalid Number of Arguements");
		}

	}

	@Override
	public boolean RegisterServer(String name) {
		serversList.add(name);
		if(serversList.size()==1)
		{
			return true;
		}
		return false;
	}

	@Override
	public String[] GetFileServers() {
		String[] servList=new String[serversList.size()];
		for(int i=0;i<serversList.size();i++)
		{
			servList[i]=serversList.get(i);
		}
		return servList;
	}

}
