package client;

import java.io.IOException;
import java.net.UnknownHostException;

import cli.Shell;

public class PeerTCPHandler extends AbstractTCPHandler {

	private Shell shell;
	private String msg;
	private String toUser;
	private String fromUser;
	
	public PeerTCPHandler(String host, int port,Shell shell,String fromUser,String toUser, String msg) throws UnknownHostException, IOException {
		super(host, port);
		this.shell=shell;
		this.msg=msg;
		this.fromUser=fromUser;
		this.toUser=toUser;
	}

//	@Override
//	public void run() {
//		if(socket!=null && !socket.isClosed()){
//			try{
//				writer.println("(PRIVATE) "+fromUser+": "+msg); 
//				String incoming=reader.readLine();
//				shell.writeLine(toUser+" replied with "+incoming);
//				
//			} catch (IOException e) {
//				System.out.println(e.getClass().getSimpleName() + ": "
//						+ e.getMessage());
//			}
//			this.close();
//		}
//
//	}

	@Override
	protected void hookInReadingLoop(String incoming) throws IOException {
		shell.writeLine(toUser+" replied with "+incoming);
		this.close();
	}

	@Override
	protected void hookBeforeReading() {
		writer.println("(PRIVATE) "+fromUser+": "+msg); 
	}

	@Override
	protected void hookCatchIOException() {
		runflag=false;
	}

}
