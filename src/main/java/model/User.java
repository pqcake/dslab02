package model;

import chatserver.TCPHandler;

public class User {
	
	private String name;
	private String pw;
	private Status status;
	private TCPHandler conn;
	private String address;

	public User(){
		this.status=Status.OFFLINE;
	}
	public User(String name,String pw) {
		this();
		this.name=name;
		this.pw=pw;
	}
	
	public User(String name) {
		this();
		this.name=name;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}
	
	public TCPHandler getConn() {
		return conn;
	}
	
	public void setConn(TCPHandler conn) {
		this.conn = conn;
	}
	
	public boolean isCorrectPw(String pw){
		return this.pw.equals(pw);
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
}
