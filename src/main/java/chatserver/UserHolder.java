package chatserver;

import java.util.concurrent.ConcurrentSkipListMap;

import model.Status;
import model.User;
import util.Config;

public class UserHolder {

	private ConcurrentSkipListMap<String,User> users;

	public UserHolder(Config user_conf) {
		this.users=new ConcurrentSkipListMap<>();
		for(String key:user_conf.listKeys()){
			String name=key.substring(0, key.lastIndexOf(".password"));
			String pw=user_conf.getString(key);
			users.put(name,new User(name,pw));
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder();
		
		for(User u:users.values()){
			sb.append(u.getName()+" ");
			if(u.getStatus()==Status.ONLINE)
				sb.append(Status.ONLINE);
			if(u.getStatus()==Status.OFFLINE)
				sb.append(Status.OFFLINE);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String onlineUsers(){
		StringBuilder sb=new StringBuilder();
		sb.append("Online users:\n");
		for(User u:users.values()){
			if(u.getStatus()==Status.ONLINE)
			sb.append("* "+u.getName());
			sb.append("\n");
		}
		return sb.toString();
	}

	public void logout() {
		for(User u:users.values()){
			if(u.getConn()!=null)
				u.getConn().logout();
		}
	}
	
	public void send(String from,String msg){
		for(User u : users.values()){
			if(!u.getName().equals(from) && u.getStatus()==Status.ONLINE){
				u.getConn().writeLine(msg);
			}
		}
	}
	
	public User getUser(String name){
		return users.get(name);
	}

}
