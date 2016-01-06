package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import model.Status;
import model.User;
import nameserver.INameserverForChatserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.TCPConnection;
import util.TCPConnectionBasic;

public class TCPHandler implements Runnable {

	private Socket socket;
	private UserHolder users;
	private User user;
	private BufferedReader reader;
	private TCPConnection tcpChannel;
	private PrintWriter writer;
	private static final String ERROR="!error ";
	private INameserverForChatserver root_stub;

	public TCPHandler(Socket socket, UserHolder users, INameserverForChatserver root_stub) {
		this.root_stub=root_stub;
		this.socket=socket;
		this.tcpChannel=new TCPConnectionBasic(socket);
		this.users=users;
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

			while ((request = tcpChannel.receive()) != null && !tcpChannel.isClosed()) {
				String[] parts = request.split("\\s",2);

				String response=ERROR+"You are not logged in.";
				if(parts[0].equals("!login")){
					response=login(parts);
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

	public String logout(){

		if(user!=null){
			user.setStatus(Status.OFFLINE);
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		
		array=getDomainSubstrings(username);
		while(array.length==2){
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
			array=getDomainSubstrings(username);
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
