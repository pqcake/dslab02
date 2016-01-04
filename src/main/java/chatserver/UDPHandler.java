package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPHandler implements Runnable {

	private DatagramPacket packet;
	private DatagramSocket datagramSocket;
	private UserHolder users;

	public UDPHandler(DatagramSocket datagramSocket,DatagramPacket packet, UserHolder users) {
		this.packet=packet;
		this.users=users;
		this.datagramSocket=datagramSocket;
	}

	@Override
	public void run() {
		// get the data from the packet (only received data not full buffer)
		String request = new String(packet.getData(),0,packet.getLength());
		
		// check if request has the correct format:
		// !list
		String response="Unsupported Command.";
		if("!list".equals(request)){
			response=users.onlineUsers();
		}
		// get the address of the sender (client) from the received
		// packet
		InetAddress address = packet.getAddress();
		// get the port of the sender from the received packet
		int port = packet.getPort();
		byte[] buffer=response.getBytes();
		/*
		 * create a new datagram packet, and write the response bytes,
		 * at specified address and port. the packet contains all the
		 * needed information for routing.
		 */
		packet = new DatagramPacket(buffer, buffer.length, address,	port);
		// finally send the packet
		try {
			datagramSocket.send(packet);
		} catch (IOException e) {
			System.err.println("Error occurred while waiting for/handling packets: "
					+ e.getMessage());
		}
	}

}
