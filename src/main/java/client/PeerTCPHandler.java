package client;

import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.UnknownHostException;
import java.io.File;
import java.security.Key;

import cli.Shell;

public class PeerTCPHandler extends AbstractTCPHandler {

	private Shell shell;
	private String msg;
	private String toUser;
	private String fromUser;
    private Key secretKey;
	
	public PeerTCPHandler(String host, int port,Shell shell,String fromUser,String toUser, String msg, Key secretKey) throws UnknownHostException, IOException {
		super(host, port);
		this.shell=shell;
		this.msg=msg;
		this.fromUser=fromUser;
		this.toUser=toUser;
        this.secretKey=secretKey;
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

		int hmacDelimiter = incoming.indexOf(" ");

		if(hmacDelimiter > 0) {
			String hmac = incoming.substring(0, hmacDelimiter);
			String message = incoming.substring(hmacDelimiter+1);

			if(!HashMACService.verifyHMAC(secretKey, hmac, message)) {
				shell.writeLine("Response from " + toUser + " was tampered!");
			}
			else {
				shell.writeLine("Response from " + toUser + " was " + message);
			}

		} else {
			shell.writeLine("Received invalid replay to private message (no HMAC).");
		}
		this.close();
	}

	@Override
	protected void hookBeforeReading() {
		try {
			String sendMsg = "!msg " + fromUser + ": " + msg;
            String hmac = HashMACService.createHMAC(secretKey, sendMsg);
			tcpChannel.send(hmac + " " + sendMsg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void hookCatchIOException() {
		runflag=false;
	}

	@Override
	protected void hookIncomingNull() {
		runflag=false;
	}

}
