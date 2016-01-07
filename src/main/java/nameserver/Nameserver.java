package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory; 

import cli.Command;
import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;

	/**
	 * Config properties
	 */
	private String domain;
	private String registry_host;
	private int registry_port;
	private String root_id;

	/**
	 * Remote object
	 */
	private NameserverEngine ns=null;

	/**
	 * Registry
	 */
	private Registry registry = null;

	private Log log = LogFactory.getLog(Nameserver.class);

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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		config=new Config(componentName);
		/* domain: the domain that is managed by this nameserver. 
		 * The root nameserver is the only nameserver that does not have this property. 
		 * Therefore you can easily check if the nameserver you are currently starting 
		 * is either an ordinary nameserver or a root name server.
		 */
		try{
			domain=config.getString("domain");
		}catch(MissingResourceException mre){}

		registry_host=config.getString("registry.host");
		registry_port=config.getInt("registry.port");
		root_id=config.getString("root_id");

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {
		INameserver root_stub=null;
		ns=new NameserverEngine();

		try {
			if(isRoot()){
				log.info("Server is root-ns");
				registry = LocateRegistry.createRegistry(registry_port);
				root_stub=(INameserver) UnicastRemoteObject.exportObject(ns,0);
				registry.bind(root_id, root_stub);
			}else{
				registry=LocateRegistry.getRegistry(registry_host, registry_port);
				root_stub=(INameserver) registry.lookup(root_id);
				INameserver ns_stub=(INameserver) UnicastRemoteObject.exportObject(ns, 0);
				root_stub.registerNameserver(domain, ns_stub, ns_stub);
			}
			new Thread(shell).start();
			shell.writeLine(componentName+" up and waiting for commands!");

		}catch(java.rmi.ConnectException ce){
			System.err.println(ce.getMessage()+"\nroot-ns not running?\nShutdown now.");
			try {
				exit();
			} catch (IOException e1) {}
		}
		catch (IOException | AlreadyRegisteredException | InvalidDomainException | NotBoundException | AlreadyBoundException e) {
			System.err.println(e.getMessage()+"\nShutdown now.");
			try {
				exit();
			} catch (IOException e1) {}
		}
	}

	private boolean isRoot(){
		return domain==null;
	}

	@Override
	@Command
	public String nameservers() throws IOException {
		return ns.toStringZones();
	}

	@Override
	@Command
	public String addresses() throws IOException {
		return ns.toStringAddresses();
	}

	@Override
	@Command
	public String exit() throws IOException {
		/*
		 * Shutdown the nameserver. Do not forget to unexport its remote object using the static method
		 * UnicastRemoteObject.unexportObject(Remote obj, boolean force) and in the case of the
		 * root nameserver also unregister the remote object and close the registry by invoking the before
		 * mentioned static unexportObject method and registry reference as parameter. Otherwise the
		 * application may not stop.
		 */
		try{
			if(isRoot() && registry!=null){
				registry.unbind(root_id);
				UnicastRemoteObject.unexportObject(registry, true);
			}
			UnicastRemoteObject.unexportObject(ns, false);
		}catch(NoSuchObjectException | NotBoundException e){}

		if(shell!=null){
			shell.close();
		}

		return "Nameserver "+componentName+" shutdown.";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		new Thread(nameserver).start();
	}

	/**
	 * http://codereview.stackexchange.com/a/113035
	 * @param s
	 * @return
	 */
	public static String reverseDomain(String s) {
		if (s == null || s.isEmpty()) return s;
		String[] components = s.split("\\.");
		StringBuilder result = new StringBuilder(s.length());
		for (int i = components.length - 1; i > 0; i--) {
			result.append(components[i]).append(".");
		}
		return result.append(components[0]).toString();
	}
}
