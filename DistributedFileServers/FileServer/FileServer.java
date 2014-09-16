
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;


public class FileServer extends UnicastRemoteObject implements ReadWriteInterface {

	public FileServer() throws RemoteException
	{
	}
	long readOffset = 0;
	File f = null;
	FileOutputStream fos = null;
	FileInputStream fis = null;

	static int CHUNK_SIZE = 64 * 1024;
	static boolean master = false;

	@Override
	public byte[] FileRead64K(String filename, long offset) throws IOException,
			RemoteException {
		byte[] b = new byte[CHUNK_SIZE];
		long chunk = offset / (CHUNK_SIZE);
		fis = new FileInputStream(filename + "/" + filename + chunk);
		fis.read(b);
		return b;
	}

	@Override
	public int FileWrite64K(String filename, long offset, byte[] data)
			throws IOException, RemoteException {
		if (master) {
			f = new File(filename);
			if (!f.isDirectory() || !f.exists()) {
				f.mkdir();
			}
			if (f.isDirectory()) {
				if (offset % (CHUNK_SIZE) == 0) {
					File temp = new File(filename + "/" + filename + offset
							/ (CHUNK_SIZE));
					fos = new FileOutputStream(temp);
					fos.write(data);
					fos.close();
				} else {
					return -1;
				}
			}
		}
		return 0;
	}

	@Override
	public long NumFileChunks(String filename) throws IOException,
			RemoteException {
		f = new File(filename);
		if (f.isDirectory()) {
			return f.listFiles().length;
		} else {
			return -1;
		}
	}

	public static void main(String[] args) {
		if (args.length == 2) {
			ReadWriteInterface s = null;
			ReadWriteInterface rWI = null;
			String IPAddr = args[0];
			Registry reg = null;
			RegistryServerIntf registryServer = null;
			try {
				s = new FileServer();
				int port = Integer.parseInt(args[1]);
				reg = LocateRegistry.getRegistry(IPAddr, port);
				// Get RegistryServer and get registered.
				registryServer = (RegistryServerIntf) reg
						.lookup("RegistryServerIntf");
				String s_name="Server_"
						+ (new Date().getTime());
				master = registryServer.RegisterServer(s_name);

				// Register with RMI
				Naming.rebind("//"+IPAddr+":"+port+"/"+s_name,s);
				String msg="";
				if(master)
					msg="Master ";
				System.out.println(msg+"Server "+s_name+" started");
				

			} catch (NumberFormatException e) {
				System.out.println("Invalid Port Number");
			} catch (RemoteException e) {
				System.out.println(e.getMessage());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println("Invalid Number of Arguements");
		}
	}

}
