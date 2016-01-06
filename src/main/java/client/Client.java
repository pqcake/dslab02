package client;

import java.io.*;
import java.net.*;

import cli.Command;
import cli.Shell;
import org.bouncycastle.util.encoders.Base64;
import util.*;
import util.encrypt.EncryptionUtilAES;
import util.encrypt.EncryptionUtilAuthRSA;
import util.encrypt.EncryptionUtilB64;

public class Client implements IClientCli, Runnable {

	private Config config;
	private Shell shell;
	
	private DatagramSocket udpsocket;
	
	private ServerTCPHandler serverHandler;
	private IncomingPeerTCPHandler incomingpeer;
	
	private Thread pubMsgThread;
	private boolean stop=false;
	
	private String username;
	private String lastMsg="No message received!";
	private static final String ERROR="!error ";

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.config = config;
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		new Thread(shell).start();

		// open a new DatagramSocket
		try {
			udpsocket = new DatagramSocket();
			udpsocket.setSoTimeout(1000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		serverHandler=new ServerTCPHandler();
		try {
			shell.writeLine("Client is up! Enter command.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(!stop){
			try {
				if(serverHandler.isAlive()){ //only because TestFramework calls Client.start() which I only do on login
					lastMsg=serverHandler.getNextMsg();
					shell.writeLine(lastMsg);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				stop=true;
			}
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		String response;
		if(serverHandler.isAlive()){
			response="Already logged in.";
		}else{
			try{
				serverHandler=new ServerTCPHandler(config.getString("chatserver.host"),config.getInt("chatserver.tcp.port"));
				serverHandler.start();
				serverHandler.println("!login "+username+" "+password );

				response=serverHandler.getNextResponse();
				if(response.startsWith(ERROR)){
					serverHandler.close();
				}else{
					this.username=username;
					pubMsgThread=new Thread(this);
					pubMsgThread.start();
				}
			}catch(IOException ioe){
				return "Could not connect to server "+config.getString("chatserver.host")+":"+config.getInt("chatserver.tcp.port");
			}
		}
		return response;
	}

	@Override
	@Command
	public String logout() throws IOException {
		String response="Not logged in.";
		if(serverHandler.isAlive())
		{
			serverHandler.println("!logout");
			this.username="";
			//response=serverHandler.getNextResponse();
			
			serverHandler.close();
			// remove all decorators (akA remove encryption from channel
			serverHandler.getTcpChannel().setDecorator(null);
			pubMsgThread.interrupt();
			incomingpeer.close();
			response="Logged out.";
		}
		return response;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		if(serverHandler.isAlive())
		{
			serverHandler.println("!send "+message);
			return serverHandler.getNextResponse();
		}else{
			return "Not logged in.";
		}
	}
	
	@Override
	@Command
	public String list() throws IOException {
		try{
			sendUDP("!list");
		}catch(IOException ioe){
			return "UDP Packet could not be sent.";
		}
		return readUDP();
	}

	private String readUDP() throws IOException{
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		// wait for response-packet from server (blocking, timeout set to 1000ms in constructor)
		try{
			udpsocket.receive(packet);
		}catch(SocketTimeoutException ste){
			return "Answer to !list was not received in 1000ms - stop waiting";
		}
		return new String(packet.getData(),0,packet.getLength());
	}

	private void sendUDP(String input) throws IOException{
		byte[] buffer = input.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
				InetAddress.getByName(config.getString("chatserver.host")),
				config.getInt("chatserver.udp.port"));

		udpsocket.send(packet);
	}

	@Override
	@Command
	public String msg(String toUser, String message) throws IOException{
		if(serverHandler.isAlive()){
			String address=lookup(toUser);
			if(address.startsWith("!error")){
				return address;
			}
			String [] parts=address.split(":");
			String host=parts[0];
			int port=Integer.parseInt(parts[1]);
			try{
				PeerTCPHandler peer=new PeerTCPHandler(host, port, shell, this.username,toUser, message);
				peer.run();
			}catch(UnknownHostException | ConnectException e){
				return "Could not connect to user "+toUser+"@"+host+":"+port;
			}
			return null; //msg will be printed through PeerTCPHandler
		}
		else{
			return "Not logged in.";
		}
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		if(serverHandler.isAlive()){
			serverHandler.println("!lookup "+username);
			return serverHandler.getNextResponse();
		}else{
			return "Not logged in.";
		}
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		if(serverHandler.isAlive()){
			String [] parts=privateAddress.split(":", 2);
			int port;
			try {
				port = Integer.parseInt(parts[1]);
			}catch(ArrayIndexOutOfBoundsException e){
				return "No port specified.";
			}catch(NumberFormatException nfe){
				return "Problem parsing port \""+parts[1]+"\"";
			}

			if(incomingpeer!=null){
				incomingpeer.close();
			}
			try{
				incomingpeer=new IncomingPeerTCPHandler(port, shell);
			}catch(IOException ioe){
				return "Register unsuccessful: "+ioe.getMessage();
			}catch(IllegalArgumentException ie){
				return ie.getMessage();
			}
			incomingpeer.start();

			serverHandler.println("!register "+privateAddress);
			return serverHandler.getNextResponse();
		}else{
			return "Not logged in.";
		}
	}

	@Override
	@Command
	public String lastMsg() throws IOException {
		return lastMsg;
	}

	@Override
	@Command
	public String exit() throws IOException {

		if(pubMsgThread!=null) {
			pubMsgThread.interrupt();
		}

		if(serverHandler!=null){
			serverHandler.close();
		}

		if(incomingpeer!=null){
			incomingpeer.close();
		}

		udpsocket.close();
		shell.close();

		return "Exiting client.";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		SecurityUtils.registerBouncyCastle();
		new Client(args[0], new Config("client"), System.in,System.out);
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	@Command
	public String authenticate(String username) throws IOException {
		String response;
		String challenge= new String(Base64.encode(SecurityUtils.getSecureRandom()));
		if(serverHandler.isAlive()){
			response="Already logged in.";
		}else{
			try {
				serverHandler = new ServerTCPHandler(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port"));
				//Read key file locations
				String clientKey = config.getString("keys.dir") + "/" + username + ".pem";
				String serverKey = config.getString("chatserver.key");
				//create and init server and client RSA ciphers
				EncryptionUtilAuthRSA rsaUtil = new EncryptionUtilAuthRSA(Keys.readPublicPEM(new File(serverKey)), Keys.readPrivatePEM(new File(clientKey)));
				TCPConnectionDecoratorEncryption rsaDecorator = new TCPConnectionDecoratorEncryption(rsaUtil);
				TCPConnectionDecoratorEncryption b64Decorator = new TCPConnectionDecoratorEncryption(new EncryptionUtilB64());
				rsaDecorator.setDecorator(b64Decorator);
				//ToDo add RSA Decorators to TCPConnection
				serverHandler.getTcpChannel().setDecorator(rsaDecorator);
				serverHandler.println("!authenticate " + username + " " + challenge);
				serverHandler.start();

				response = serverHandler.getNextResponse();
				if (response.startsWith(ERROR)) {
					serverHandler.close();
				} else if (response.startsWith("!ok")) {
					String[] responseArr = response.split(" ");
					if (responseArr[1].equals(challenge)) {
						String serverChallenge = responseArr[2];
						String b64AESKey = responseArr[3];
						String b64AESIv = responseArr[4];
						byte[] aesKEY = Base64.decode(b64AESKey);
						byte[] aesIV = Base64.decode(b64AESIv);
						TCPConnectionDecorator aesAddon = new TCPConnectionDecoratorEncryption(new EncryptionUtilAES(aesKEY, aesIV));
						TCPConnectionDecorator currentDecorator = serverHandler.getTcpChannel().getDecorator();
						if (currentDecorator != null)
							aesAddon.setDecorator(currentDecorator.getDecorator());
						serverHandler.getTcpChannel().setDecorator(aesAddon);
						serverHandler.println(serverChallenge);
						this.username = username;
						pubMsgThread = new Thread(this);
						pubMsgThread.start();
						response = serverHandler.getNextResponse();
					} else {
						response = "The server is fishy!";
						serverHandler.close();
					}
				} else {
					serverHandler.close();
				}
			}catch (FileNotFoundException e){
				return "No key file for this users!";
			}catch(IOException ioe){
				ioe.printStackTrace();
				return "Could not connect to server "+config.getString("chatserver.host")+":"+config.getInt("chatserver.tcp.port");
			}
		}
		return response;
	}
}
