/**
 * 
 */
package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

/**
 * @author Philipp
 *
 */
public class UDPListener implements Runnable {

	private Config config;
	private DatagramSocket datagramSocket;
	private ExecutorService pool;
	private UserHolder users;

	public UDPListener(Config config,UserHolder users) throws SocketException {
		this.users=users;
		this.config=config;
		this.pool=Executors.newCachedThreadPool();
		this.datagramSocket = new DatagramSocket(config.getInt("udp.port"));
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
//		try {
//			// constructs a datagram socket and binds it to the specified port
//			datagramSocket = new DatagramSocket(config.getInt("udp.port"));
//		} catch (IOException e) {
//			throw new RuntimeException("Cannot listen on UDP port.", e);
//		}
		byte[] buffer;
		DatagramPacket packet;
		try {
			while(!datagramSocket.isClosed()){
				buffer = new byte[1024];
				packet = new DatagramPacket(buffer, buffer.length);
				// wait for incoming packets from client
				datagramSocket.receive(packet);
				//start new Handler for packet
				pool.execute(new UDPHandler(datagramSocket,packet,users));
			}
		} catch (SocketException se){
		} catch (IOException e) {
			System.err
			.println("Error occurred while waiting for/handling packets: "
					+ e.getMessage());
		} finally {
			if (datagramSocket != null && !datagramSocket.isClosed())
				datagramSocket.close();
		}
	}

	public void close() {
		/*
		 * Note that closing the socket also triggers an exception in the
		 * listening thread
		 */
		if (datagramSocket != null){
			datagramSocket.close();
			pool.shutdown();
		}
	}

}
