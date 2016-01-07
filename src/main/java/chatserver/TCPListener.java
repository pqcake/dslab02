/**
 * 
 */
package chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nameserver.INameserver;
import nameserver.INameserverForChatserver;
import util.Config;

/**
 * @author Philipp
 *
 */
public class TCPListener implements Runnable{

	private ServerSocket serverSocket;
	private ExecutorService pool;
	private UserHolder users;
	private Config config;
	private INameserverForChatserver root_stub=null;

	/**
	 * @param config 
	 * @throws IOException 
	 * 
	 */
	public TCPListener(Config config,UserHolder users) throws IOException, NotBoundException {
		this.config=config;
		this.users=users;
		this.pool=Executors.newCachedThreadPool();
		this.serverSocket = new ServerSocket(config.getInt("tcp.port"));
		Registry registry=LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port"));
		root_stub=(INameserver) registry.lookup(config.getString("root_id"));
	}

	@Override
	public void run() {		
		Socket socket = null;

		try {
			while(true){
				//waiting for connection
				socket=serverSocket.accept();
				// handle incoming connections from client in a separate thread
				pool.execute(new TCPHandler(socket,users,root_stub,config));
			}
		} catch (SocketException se){

		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (socket != null && !socket.isClosed()){
				users.logout();
			}
		}
	}

	public void close() {
		if (serverSocket != null){
			try {
				//calling this throws a SocketException in threads blocked by ServerSocket.accept()
				serverSocket.close();
				shutdownAndAwaitTermination(pool);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
	 * @param pool
	 */
	void shutdownAndAwaitTermination(ExecutorService pool) {
		   pool.shutdown(); // Disable new tasks from being submitted
		   try {
		     // Wait a while for existing tasks to terminate
		     if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
		       pool.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		       if (!pool.awaitTermination(10, TimeUnit.SECONDS))
		           System.err.println("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
		     pool.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
		 }

}
