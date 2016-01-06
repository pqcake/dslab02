package client;

import cli.Shell;
import java.io.IOException;
import java.net.UnknownHostException;

public class IncomingPeerTCPHandler extends AbstractTCPHandler{
	
	private Shell shell;

	public IncomingPeerTCPHandler(int port,Shell shell) throws IllegalArgumentException, UnknownHostException, IOException {
		super(port);
		this.shell=shell;
	}

	@Override
	protected void hookInReadingLoop(String incoming) throws IOException {
		shell.writeLine(incoming);
		try {
			tcpChannel.send("!ack");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	@Override
//	public void run() {
//		try {
//			while(true){
//				socket = serverSocket.accept();
//				BufferedReader reader = new BufferedReader(
//						new InputStreamReader(socket.getInputStream()));
//				PrintWriter writer = new PrintWriter(
//						socket.getOutputStream(), true);
//				String msg=reader.readLine();
//				shell.writeLine(msg);
//				writer.println("!ack");
//				socket.close();
//			}
//		} catch (IOException e) {
//			try {
//				shell.writeLine("Closing private message socket");
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
//	}
	
}
