package client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerTCPHandler extends AbstractTCPHandler{

	//LinkedBlockingQueue or SynchronousQueue
	private LinkedBlockingQueue<String> msgs;
	private LinkedBlockingQueue<String> responses;
	
	public ServerTCPHandler(String host,int port) throws UnknownHostException, IOException {
		super(host, port);
		this.msgs=new LinkedBlockingQueue<>();
		this.responses=new LinkedBlockingQueue<>();
	}
	
	public ServerTCPHandler() {}
	
	@Override
	protected void hookInReadingLoop(String incoming) throws IOException {
		try {
			if(incoming.startsWith("!msg ")){
				msgs.put(incoming.substring(5)); //"!msg ".length
			}else{
				responses.put(incoming);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new IOException();
		}		
	}
	
//	@Override
//	public void run() {
//		if(socket!=null && !socket.isClosed()){
//			try{
//				String incoming;
//				while ((incoming = reader.readLine()) != null) {
//					try {
//						if(incoming.startsWith("!msg ")){
//							msgs.put(incoming.substring(5)); //"!msg ".length
//						}else{
//							responses.put(incoming);
//						}
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//						break;
//					}
//				}
//			} catch (UnknownHostException e) {
//				System.out.println("Cannot connect to host: " + e.getMessage());
//			} catch (IOException e) {
//				//System.out.println(e.getClass().getSimpleName() + ": "+ e.getMessage());
//			}finally {
//				System.out.println("Server closed socket.");
//			}
//		}
//	}

	public String getNextMsg() throws InterruptedException {
		return msgs.take();
	}

	public String getNextResponse() {
		try {
			return responses.take();
		} catch (InterruptedException e) {
			return e.getMessage();
		}
	}

	public void println(String string) {
		if(writer!=null){
			writer.println(string);
		}
	}
}
