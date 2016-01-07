package chatserver;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

import model.Status;
import model.User;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import org.bouncycastle.util.encoders.Base64;
import util.*;
import util.encrypt.EncryptionUtilAES;
import util.encrypt.EncryptionUtilAuthRSA;
import util.encrypt.EncryptionUtilB64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TCPHandler implements Runnable {

	private Socket socket;
	private UserHolder users;
	private User user;
	private BufferedReader reader;
	private TCPConnection tcpChannel;
	private PrintWriter writer;
	private static final String ERROR="!error ";
	private INameserverForChatserver root_stub;
	private Config config;

	public TCPHandler(Socket socket, UserHolder users, INameserverForChatserver root_stub,Config config) {
		this.root_stub=root_stub;
		this.socket=socket;
		this.tcpChannel=new TCPConnectionBasic(socket);
		this.users=users;
		this.config=config;
	}

	@Override
	public void run() {
		try {
			// prepare the input reader for the socket
			reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			// prepare the writer for responding to clients requests
			writer = new PrintWriter(socket.getOutputStream(),
					true);
			String request;
			resetToRSA();

			while ((request = tcpChannel.receive()) != null && !tcpChannel.isClosed()) {
				String[] parts = request.split("\\s",2);

				String response=ERROR+"You are not logged in.";
				if(parts[0].equals("!authenticate")){
					response=authenticate(parts);
				}
				else{
					if(user!=null){
						synchronized (user) {
							if(user.getStatus()==Status.ONLINE){
								switch (parts[0]){
								case "!logout":
									response=logout();
									break;
								case "!send":
									response=send(parts);
									break;
								case "!lookup":
									response=lookup(parts);
									break;
								case "!register":
									response=register(parts);
									break;
								default:
									response = "Command \""+parts[0]+"\" not supported.";
								}
							}
						}
					}
//					else{
//						response=ERROR+"You are not logged in.";
//					}
				}
				System.out.println("sending:"+response);
				tcpChannel.send(response);
			}

		} catch (UserLoginException e){
			try {
				tcpChannel.send(e.getMessage());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (socket != null && !socket.isClosed()){
				logout();
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	private void resetToRSA() throws IOException{
		//Read key file locations
		String serverKey=config.getString("key");
		//create and init server and client RSA ciphers
		EncryptionUtilAuthRSA rsaUtil=new EncryptionUtilAuthRSA(null, Keys.readPrivatePEM(new File(serverKey)));
		TCPConnectionDecoratorEncryption rsaDecorator=new TCPConnectionDecoratorEncryption(rsaUtil);
		TCPConnectionDecoratorEncryption b64Decorator=new TCPConnectionDecoratorEncryption(new EncryptionUtilB64());
		rsaDecorator.setDecorator(b64Decorator);
		tcpChannel.setDecorator(rsaDecorator);
	}

	public String login(String [] params) throws UserLoginException{
		if(params.length==2){
			params=params[1].split("\\s");
			if(params.length!=2)
				return ERROR+"Wrong number of arguments.";
		}
		String username=params[0];
		String password=params[1];
		User u=users.getUser(username);

		if(u!=null){
			synchronized (u) {
				if(u.getStatus()!=Status.ONLINE){
					if(u.isCorrectPw(password)){
						u.setStatus(Status.ONLINE);
						u.setConn(this);
						user=u;
						return "Successfully logged in.";
					}
				}else{
					if(u.getConn()!=this){
						throw new UserLoginException(ERROR+"User already logged in on other client!");					
					}
					throw new UserLoginException(ERROR+"You are already logged in.");
				}
			}
		}
		throw new UserLoginException(ERROR+"Wrong username or password.");
	}

	public String authenticate(String [] params) throws UserLoginException{
		System.out.println("Starting auth!");
		if(params.length==2){
			params=params[1].split("\\s");
			if(params.length!=2) {
				System.out.println("FAIL");
				return ERROR + "Wrong number of arguments.";
			}
		}
		String username=params[0];
		String challenge=params[1];
		String clientKey=config.getString("keys.dir")+"/"+username+".pub.pem";
		String serverKey=config.getString("key");
		User u=users.getUser(username);

		if(u!=null){
			synchronized (u) {
				if(u.getStatus()!=Status.ONLINE){
					EncryptionUtilAuthRSA rsaUtil= null;
					try {
						File clientKeyFile=new File(clientKey);
						File serverKeyFile=new File(serverKey);
						rsaUtil = new EncryptionUtilAuthRSA(Keys.readPublicPEM(clientKeyFile),Keys.readPrivatePEM(serverKeyFile));
						TCPConnectionDecoratorEncryption rsaDecorator=new TCPConnectionDecoratorEncryption(rsaUtil);
						TCPConnectionDecoratorEncryption b64Decorator=new TCPConnectionDecoratorEncryption(new EncryptionUtilB64());
						rsaDecorator.setDecorator(b64Decorator);
						tcpChannel.setDecorator(rsaDecorator);
						String msg="!ok "+challenge;
						// generate server challenge
						byte[] serverChallenge=SecurityUtils.getSecureRandom();
						String serverChallengeB64= new String(Base64.encode(serverChallenge));
						// generate aes key
						KeyGenerator generator = KeyGenerator.getInstance("AES");
						// KEYSIZE is in bits
						generator.init(256);
						SecretKey key = generator.generateKey();
						byte[] aesKey=key.getEncoded();
						String aesKeyB64= new String(Base64.encode(aesKey));
						// generate iv
						byte[] iv=SecurityUtils.getSecureRandomSmall();
						String ivB64= new String(Base64.encode(iv));
						msg+=" "+serverChallengeB64+" "+aesKeyB64+" "+ivB64;
						try {
							tcpChannel.send(msg);
							TCPConnectionDecorator aesAddon=new TCPConnectionDecoratorEncryption(new EncryptionUtilAES(aesKey,iv));
							TCPConnectionDecorator currentDecorator=tcpChannel.getDecorator();
							if(currentDecorator!=null)
								aesAddon.setDecorator(currentDecorator.getDecorator());
							tcpChannel.setDecorator(aesAddon);
							String response=tcpChannel.receive();
							if(response.equals(serverChallengeB64)){
								u.setStatus(Status.ONLINE);
								u.setConn(this);
								user=u;
								return "Successfully logged in.";
							}else{
								resetToRSA();
								throw new UserLoginException(ERROR+"Wrong challenge response u are not user:"+username);
							}

						} catch (Exception e) {
							resetToRSA();
							e.printStackTrace();
							throw new UserLoginException(ERROR+e.getMessage());
						}
					} catch (FileNotFoundException e){
						tcpChannel.setDecorator(new TCPConnectionDecoratorEncryption(new EncryptionUtilB64()));
						throw new UserLoginException(ERROR+"Key file not found on server sry!");
					} catch (IOException | NoSuchAlgorithmException e) {
						e.printStackTrace();
						try {
							resetToRSA();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						throw new UserLoginException(ERROR+e.getMessage());
					}
				}else{
					if(u.getConn()!=this){
						tcpChannel.setDecorator(new TCPConnectionDecoratorEncryption(new EncryptionUtilB64()));
						throw new UserLoginException(ERROR+"User already logged in on other client!");
					}
					throw new UserLoginException(ERROR+"You are already logged in.");
				}
			}
		}
		throw new UserLoginException(ERROR+"Wrong username or password.");
	}

	public String logout(){

		if(user!=null){
			user.setStatus(Status.OFFLINE);
//			try {
//				socket.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return "Successfully logged out.";
		}
		return "!error User was not logged in";
	}

	public String send(String [] params){
		if(params.length!=2){
			return ERROR+"Wrong number of arguments.";
		}
		String message=params[1];
		users.send(user.getName(), "!msg "+user.getName()+": "+message);
		return "Public message successfully sent.";
	}

//	public String lookup(String [] params){
//		if(params.length!=2){
//			return ERROR+"Wrong number of arguments.";
//		}
//		String username=params[1];
//		User u=users.getUser(username);
//		if(u!=null && u.getAddress()!=null){
//			return u.getAddress();
//		}
//		return ERROR+"Wrong username or user not reachable.";
//	}
	
	public String lookup(String [] params){
		if(params.length!=2){
			return ERROR+"Wrong number of arguments.";
		}
		String username=params[1];
		String[] array;
		INameserverForChatserver ns=root_stub;

		while((array=getDomainSubstrings(username)).length==2){
			username=array[0];
			try {
				ns=ns.getNameserver(array[1]);
				if(ns==null){
					return ERROR+"Domain of user not registered";
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		try {
			String address=ns.lookup(username);
			if(address!=null){
				return address;
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ERROR+"Wrong username or user not reachable.";
	}
	
	public String[] getDomainSubstrings(String domain){
		String [] array;
		int pos=domain.lastIndexOf(".");
		if(pos==-1){
			array=new String[1];
			array[0]=domain;
		}else{
			array=new String[2];
			array[0]=domain.substring(0, pos);
			array[1]=domain.substring(pos+1,domain.length());
		}
		return array;
	}

	public String register(String [] params) {
		if(params.length!=2){
			return ERROR+"Wrong number of arguments.";
		}
		String privateAddress=params[1];
		try {
			root_stub.registerUser(user.getName(), privateAddress);
		} catch (RemoteException | AlreadyRegisteredException | InvalidDomainException e) {
			return ERROR+e.getMessage();
		}
		return "Successfully registered address.";
	}

	public void writeLine(String msg) {
		if(tcpChannel!=null&&!tcpChannel.isClosed())
			try {
				tcpChannel.send(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
