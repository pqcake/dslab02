package client;

import org.bouncycastle.util.encoders.Base64;

import cli.Shell;
import java.io.IOException;
import java.net.UnknownHostException;
import java.io.File;
import java.security.Key;


public class IncomingPeerTCPHandler extends AbstractTCPHandler{
	
	private Shell shell;
    private Key secretKey;

	public IncomingPeerTCPHandler(int port,Shell shell) throws IllegalArgumentException, UnknownHostException, IOException {
		super(port);
		this.shell=shell;
        secretKey = null;
	}

    public void setSecretKey(Key secretKey) {
        this.secretKey = secretKey;
    }

	@Override
	protected void hookInReadingLoop(String incoming) throws IOException {
        String hmac = incoming.substring(0, incoming.indexOf(" "));
        String sender = incoming.substring(incoming.indexOf(" ")+1, incoming.indexOf(":"));
        String message = incoming.substring(incoming.indexOf(":")+2);

        shell.writeLine("(PRIVATE) " + sender + ": " + message);

        String response = "";
        if(HashMACService.verifyHMAC(secretKey, hmac, message)) {
            response = "!ack";
        } else {
            String responseMessage = "!tampered " + message;
            String responseHmac = HashMACService.createHMAC(secretKey, responseMessage);
            response = responseHmac + responseMessage;
            shell.writeLine("Private message from " + sender + " was tampered!");
        }

		try {
			tcpChannel.send(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void hookIncomingNull() {
		//just dont set because normally anyway true, only false when close called and this would overwrite again to true -> thread not ending
		//runflag=true;
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
