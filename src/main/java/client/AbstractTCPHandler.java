package client;

import util.TCPConnection;
import util.TCPConnectionBasic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public abstract class AbstractTCPHandler extends Thread{

	private Socket socket;
	private ServerSocket serverSocket;
	protected BufferedReader reader;
	protected PrintWriter writer;
	protected TCPConnection tcpChannel;
	protected boolean runflag=true;

	public AbstractTCPHandler(){}
	
	public AbstractTCPHandler(String host,int port) throws IllegalArgumentException, IOException {
		this.socket = new Socket(host,port);
		tcpChannel=new TCPConnectionBasic(socket);
		//createReaderWriter();
	}

	public AbstractTCPHandler(int port) throws IllegalArgumentException, IOException {
		serverSocket = new ServerSocket(port);
	}

	private void createReaderWriter() throws IOException{
		reader = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(
				socket.getOutputStream(), true);
	}

	@Override
	public final void run(){
		String incoming = null;
		while(runflag){
			
			if(serverSocket!=null){
				try {
					this.socket=serverSocket.accept();
					tcpChannel=new TCPConnectionBasic(socket);
					//createReaderWriter();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
			if(socket!=null && !socket.isClosed()){
				try{
					hookBeforeReading();
					while ((incoming = tcpChannel.receive()) != null) {
						hookInReadingLoop(incoming);
					}
				}catch(IOException ioe){
					hookCatchIOException();
				}
			}
			if(incoming==null){
				hookIncomingNull();
			}
		}
	}

	protected void hookIncomingNull() {}

	protected void hookCatchIOException(){}

	protected void hookBeforeReading(){}

	protected void hookInReadingLoop(String incoming) throws IOException{}

	public void close(){
		runflag=false;
		try{
			if(serverSocket!=null && !serverSocket.isClosed()){
				serverSocket.close();
			}
			if(socket!=null && !socket.isClosed()){
				socket.close();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public TCPConnection getTcpChannel() {
		return tcpChannel;
	}
}
