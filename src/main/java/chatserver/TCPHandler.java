package chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import model.Status;
import model.User;

public class TCPHandler implements Runnable {

	private Socket socket;
	private UserHolder users;
	private User user;
	private BufferedReader reader;
	private PrintWriter writer;
	private static final String ERROR="!error ";

	public TCPHandler(Socket socket, UserHolder users) {
		this.socket=socket;
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

			while ((request = reader.readLine()) != null && !socket.isClosed()) {
				String[] parts = request.split("\\s",2);

				String response;
				if(parts[0].equals("!login")){
					response=login(parts);
				}
				else
					if(user!=null && user.getStatus()==Status.ONLINE){
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
					else{
						response=ERROR+"You are not logged in.";
					}
				writer.println(response);
			}

		} catch (UserLoginException e){
			writer.println(e.getMessage());
		} catch (IOException e) {
			
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

	public String lookup(String [] params){
		if(params.length!=2){
			return ERROR+"Wrong number of arguments.";
		}
		String username=params[1];
		User u=users.getUser(username);
		if(u!=null && u.getAddress()!=null){
			return u.getAddress();
		}
		return ERROR+"Wrong username or user not reachable.";
	}

	public String register(String [] params) {
		if(params.length!=2){
			return ERROR+"Wrong number of arguments.";
		}
		String privateAddress=params[1];
		user.setAddress(privateAddress);
		return "Successfully registered address.";
	}

	public void writeLine(String msg) {
		writer.println(msg);
	}
}
