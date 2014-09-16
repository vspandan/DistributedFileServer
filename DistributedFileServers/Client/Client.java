import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Client {
	static HashSet<Integer> missedChunks = new HashSet<Integer>();
	static ArrayList<String> disConnectedServers = new ArrayList<String>();
	static ArrayList<Integer> aL = new ArrayList<Integer>();
	static ArrayList<String> servers = new ArrayList<String>();
	static int CHUNK_SIZE = 64 * 1024;
	static String[] serverList = null;
	static RegistryServerIntf regServ = null;
	static ReadWriteInterface masterServ = null;
	static RandomAccessFile r = null;
	static String host = null;
	static Registry registry = null;
	static int port = -1;
	static File f = null;
	static long offSet = 0;

	public static void main(String[] args) {
		byte[] data = new byte[CHUNK_SIZE];

		if (args.length == 3) {

			host = args[1];
			try {

				port = Integer.parseInt(args[2]);
				registry = LocateRegistry.getRegistry(host, port);
				regServ = (RegistryServerIntf) registry
						.lookup("RegistryServerIntf");

				serverList = regServ.GetFileServers();
				masterServ = (ReadWriteInterface) registry
						.lookup(serverList[0]);

				for (String serv : serverList) {
					servers.add(serv);
				}

				System.out.println("Master Server:" + serverList[0]);

				/* ++++++++++++++++++ write file ++++++++++++++++++ */

				if (args[0] != null) {

					f = new File(args[0]);
					if (f.exists()) {
						r = new RandomAccessFile(f, "r");
						byte b = 0;
						int i = 1;
						int status = 0;
						for (i = 1; true; i++) {
							try {
								b = r.readByte();
								data[i - 1] = b;
								if (i == CHUNK_SIZE) {
									offSet += i;
									i = 0;
									status = masterServ.FileWrite64K(
											f.getName(), offSet, data);
									if (status != 0) {
										System.out
												.println("Error While Transfering chunk");
									}
								}

							} catch (EOFException e) {
								if (i < CHUNK_SIZE && i > 1) {
									offSet += i;
									status = masterServ.FileWrite64K(
											f.getName(), offSet, data);
									if (status == -1) {
										System.out
												.println("Server Ignored Last Chunk which is Less Than 64K.. ");
									}
								}
								break;
							} catch (RemoteException e) {
								System.out
										.println("Unable to connect Master Server/Disconnected: Exiting application");
								;
								System.exit(1);
							}
						}
						r.close();
					} else {
						System.out.println("File Not Found.");
					}
					/* ++++++++++++++++++ write file ++++++++++++++++++ */

					ReadWriteInterface fileServ = (ReadWriteInterface) registry
							.lookup(serverList[0]);
					long noOfChunks2Read = fileServ.NumFileChunks(f.getName());

					File dir = new File("output");
					if (dir.exists())
						delete(dir);
					dir.mkdir();
					String ouputFile = f.getName();
					String ouputFilePath = dir.getName() + File.separator;
					readChunks(noOfChunks2Read, ouputFilePath, ouputFile, -1);

					// Retrieving missed chunks
					while (missedChunks.size() > 0)
					{	System.out.println();
						for(String discServ:disConnectedServers){
							System.out.println("Server "+discServ+" is not reachable");
						}
						servers.removeAll(disConnectedServers);
						disConnectedServers.clear();
						noOfChunks2Read = missedChunks.size();
						aL.clear();
						aL.addAll(missedChunks);
						System.out.println("\nDownloading missed chunks :"+missedChunks+"\n");
						missedChunks.clear();
						readChunks(noOfChunks2Read, ouputFilePath, ouputFile, 1);
					}

					// Appending Chunks
					File outputFile = new File(ouputFilePath + f.getName());
					File[] outputChunks = dir.listFiles();
					FileOutputStream fos = new FileOutputStream(outputFile);
					FileInputStream fis = null;
					System.out.println("******Appending all chunks******");
					for (int i = 0; i < noOfChunks2Read; i++) {
						fis = new FileInputStream(outputChunks[i]);
						int ch = 0;
						while ((ch = fis.read()) != -1) {
							fos.write(ch);
						}
						fis.close();
						outputChunks[i].delete();
					}
					fos.close();
				}
			} catch (NumberFormatException e) {
				System.out.println("Invalid Arguments: " + e.getMessage());
			} catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
			} catch (RemoteException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (NotBoundException e) {
				System.out.println(e.getMessage());
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println("Invalid Number of Arguments");
		}
	}

	public static void delete(File file) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				if (file.list().length == 0) {
					file.delete();
				} else {
					String files[] = file.list();
					for (String temp : files) {
						File fileDelete = new File(file, temp);
						delete(fileDelete);
					}
					if (file.list().length == 0) {
						file.delete();
					}
				}
			} else {
				file.delete();
			}
		}
	}

	static class FileServerThread extends Thread {
		int CHUNK_SIZE = 64 * 1024;
		ArrayList<Integer> fileList = null;
		FileOutputStream fos = null;
		String outputFile = null;
		String outputFilePath = null;
		ReadWriteInterface fileServ = null;
		Registry registry = null;
		String fileServName = null;

		public FileServerThread(String fileServName, String ouputFile,
				String ouputFile2, ArrayList<Integer> arrayList, String host,
				int port) throws RemoteException, NotBoundException {
			this.fileList = arrayList;
			this.fileServName = fileServName;
			this.outputFile = ouputFile2;
			this.outputFilePath = ouputFile;
			this.registry = LocateRegistry.getRegistry(host, port);
			this.fileServ = (ReadWriteInterface) this.registry
					.lookup(fileServName);
		}

		@Override
		public void run() {
			try {
				for (Integer chunkNO : fileList) {
					fos = new FileOutputStream(outputFilePath + outputFile
							+ chunkNO);
					fos.write(fileServ.FileRead64K(outputFile, (chunkNO + 1)
							* CHUNK_SIZE));
					System.out.println("Chunk " + chunkNO
							+ "read from FileServer:" + fileServName);
					fos.close();
				}
			} catch (FileNotFoundException e) {
				System.out.println(e.getLocalizedMessage());
			} catch (RemoteException e) {
				missedChunks.addAll(fileList);
				disConnectedServers.add(fileServName);
			} catch (IOException e) {
				System.out.println(e.getLocalizedMessage());
			}
		}
	}

	public static void readChunks(long noOfChunks2Read, String ouputFilePath,
			String ouputFile, int ind) throws NotBoundException, IOException,
			InterruptedException {
		int noOfThreads = 0;
		if (noOfChunks2Read < servers.size())
			noOfThreads = (int) noOfChunks2Read;
		else
			noOfThreads = servers.size();
		HashMap<Integer, ArrayList<Integer>> serverChunkMapping = new HashMap<Integer, ArrayList<Integer>>();
		ArrayList<Integer> chunkList = null;
		for (int i = 0; i < noOfChunks2Read; i++) {
			chunkList = serverChunkMapping.get(i % (noOfThreads));
			if (chunkList == null)
				chunkList = new ArrayList<Integer>();
			if (ind == -1)
				chunkList.add(i);
			else
				chunkList.add(aL.get(i));
			serverChunkMapping.put(i % (noOfThreads), chunkList);
		}

		FileServerThread[] fileReadThreads = new FileServerThread[noOfThreads];
		for (int i = 0; i < noOfThreads; i++) {
			fileReadThreads[i] = new FileServerThread(servers.get(i),
					ouputFilePath, ouputFile, serverChunkMapping.get(i), host,
					port);
			fileReadThreads[i].start();
			fileReadThreads[i].join();
		}
	}
}
