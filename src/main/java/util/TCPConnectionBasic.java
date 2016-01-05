/**
 * 
 */
package util;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPConnectionBasic implements TCPConnection {
	protected TCPConnectionDecorator decorator;
	private Socket tcpSocket = null;
	private PrintWriter outputWriter;
	private OutputStream outputStream;
	private BufferedReader inputStream;
	
	public TCPConnectionBasic(String serverHost, int port) throws IOException {
		this(new Socket(serverHost, port));
	}
	
	public TCPConnectionBasic(Socket socket) throws UnknownHostException {
		try {
			tcpSocket = socket;
			inputStream = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
			outputStream=tcpSocket.getOutputStream();
			outputWriter = new PrintWriter(outputStream, true);

		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	@Override
	public void send(String msg) throws Exception{
		if(tcpSocket == null || tcpSocket.isClosed()){
			throw new Exception("Lost tcp connection to server.");
		}
		if(decorator!=null)
			msg=new String(decorator.prepare(msg.getBytes()));
		outputWriter.println(msg);
		outputWriter.flush();
	}

	@Override
	public void setDecorator(TCPConnectionDecorator decorator) {
		this.decorator=decorator;
	}

	@Override
	public TCPConnectionDecorator getDecorator() {
		return this.decorator;
	}

	@Override
	public void send(byte[] msg) throws Exception{
		if(tcpSocket == null || tcpSocket.isClosed()){
			throw new Exception("Lost tcp connection to server.");
		}
		if(decorator!=null)
			msg=decorator.prepare(msg);
		outputStream.write(msg);
		outputStream.flush();
	}
	
	@Override
	public String receive() throws IOException {
		String response = "";
        try{
        	response = inputStream.readLine();
        		
        } catch (SocketException ex){
        	// Client is shutting down
        	response = null;
            Thread.currentThread().interrupt();
        }
		if(decorator!=null && response!=null)
			response=new String(decorator.receive(response.getBytes()));
        return response;
	}

	@Override
	public byte[] receiveBytes() throws IOException {
		String response=receive();
		if(response==null)
			return null;
		if(decorator!=null)
			return decorator.receive(response.getBytes());
		return response.getBytes();
	}
	
	@Override
	public void close() {
		try {
			if(tcpSocket != null || !tcpSocket.isClosed()) {
				tcpSocket.close();
			}
			outputWriter.close();
			inputStream.close();
		} catch (IOException ex) {
			// Ignore it, 'cause we cannot handle it
		}
	}
	
	@Override
	public boolean isClosed() {
		if(tcpSocket == null) {
			return true;
		}
		return tcpSocket.isClosed();
	}
	
}
