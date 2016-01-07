package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;

import cli.Command;
import cli.Shell;
import util.Config;
import util.SecurityUtils;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private Config user_conf;
	private UserHolder users;
	private Shell shell;
	private TCPListener tcplistener;
	private UDPListener udplistener;
	private Thread shellthread;

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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
				
		user_conf=new Config("user");
		users=new UserHolder(user_conf);

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {
		/*
		 * Finally, make the Shell process the commands read from the
		 * InputStream by invoking Shell.run(). Note that Shell implements the
		 * Runnable interface. Thus, you can run the Shell asynchronously by
		 * starting a new Thread:
		 * 
		 * Thread shellThread = new Thread(shell); shellThread.start();
		 * 
		 * In that case, do not forget to terminate the Thread ordinarily.
		 * Otherwise, the program will not exit.
		 */
		shellthread=new Thread(shell);
		shellthread.start(); //todo kill this thread on jvm bind exception
		try {
			shell.writeLine(componentName
					+ " up and waiting for commands!");
		} catch (IOException e) {
			e.printStackTrace();
			exit();
		}
		
		//Start TCP and UDP Listeners in seperate Threads
		try {
			this.tcplistener=new TCPListener(config,users);
			this.udplistener=new UDPListener(config,users);
			new Thread(tcplistener).start();
			new Thread(udplistener).start();
		} catch (IOException e) {
			try {
				shell.writeLine(e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}finally{
				exit();
			}
		}
	}

	@Override
	@Command
	public String users() throws IOException {
		return users.toString();
	}

	@Override
	@Command
	public String exit() {
		System.out.println("exit called");
		if(users!=null){
			users.logout();
		}
		if(tcplistener!=null){
			tcplistener.close();
		}
		if(udplistener!=null){
			udplistener.close();
		}
		if(shell!=null){
			shell.close();
			//shellthread.interrupt();
		}
		return "Server shut down successful";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		SecurityUtils.registerBouncyCastle();
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		chatserver.run();
	}
}
