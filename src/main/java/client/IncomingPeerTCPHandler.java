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
        int hmacDelimiter = incoming.indexOf(" ");

        if(hmacDelimiter > 0) {
            String hmac = incoming.substring(0, hmacDelimiter);
            String message = incoming.substring(hmacDelimiter+1);

            String strippedMessage =  message.substring(message.indexOf(" ")+1); // message without !msg

            shell.writeLine("(PRIVATE) " + strippedMessage);

            String response = "";
            if(HashMACService.verifyHMAC(secretKey, hmac, message)) {
                response = "!ack";
            } else {
                response = "!tampered " + message;
                shell.writeLine("Private message was tampered!");
            }

            String responseHmac = HashMACService.createHMAC(secretKey, response);
            response = responseHmac + " " + response;
            try {
                tcpChannel.send(response);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            shell.writeLine("Received invalid private message (no HMAC).");
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
