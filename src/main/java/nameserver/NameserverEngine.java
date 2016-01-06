package nameserver;

import java.rmi.RemoteException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;

public class NameserverEngine implements INameserver {

	private Log log = LogFactory.getLog(NameserverEngine.class);
	private ConcurrentSkipListMap<String,INameserver> servers=new ConcurrentSkipListMap<>(); 
	private ConcurrentSkipListMap<String, String> users=new ConcurrentSkipListMap<>();

	/**
	 * TODO make generic register for both User and Nameserver
	 */
	@Override
	public void registerUser(String username, String address)
			throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		
		String [] array=getDomainSubstrings(username);
		if(array.length==1){
//			if(users.containsKey(username)){
//				throw new AlreadyRegisteredException(username+" already registered!");
//			}
			log.info("Registered user: "+username);
			users.put(username, address);
		}else{
			String remaining_zones=array[0];
			String next_zone=array[1];
			INameserver next_ns=servers.get(next_zone);
			if(next_ns==null){
				throw new InvalidDomainException("Domain \""+next_zone+"\" is not registered. Therefore \""+remaining_zones+"\" couldn't be registered.");
			}
			log.info("Calling register of "+remaining_zones+" on ns "+next_zone);
			next_ns.registerUser(remaining_zones, address);
		}
	}

	@Override
	public INameserverForChatserver getNameserver(String zone) throws RemoteException {
		log.info("Get nameserver: "+zone);
		return servers.get(zone);
	}

	@Override
	public String lookup(String username) throws RemoteException {
		log.info("Get user: "+username);
		return users.get(username);
	}

	@Override
	public void registerNameserver(String domain, INameserver nameserver,INameserverForChatserver nameserverForChatserver)
					throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		
		String [] array=getDomainSubstrings(domain);
		if(array.length==1){
			if(servers.containsKey(domain)){
				throw new AlreadyRegisteredException(domain+" already registered!");
			}
			log.info("Registered nameserver: "+domain);
			servers.put(domain, nameserver);
		}else{
			String remaining_zones=array[0];
			String next_zone=array[1];
			INameserver next_ns=servers.get(next_zone);
			if(next_ns==null){
				throw new InvalidDomainException("Domain \""+next_zone+"\" is not registered. Therefore \""+remaining_zones+"\" couldn't be registered.");
			}
			log.info("Calling register of "+remaining_zones+" on ns "+next_zone);
			next_ns.registerNameserver(remaining_zones, nameserver, nameserverForChatserver);
		}
	}
	public String[] getDomainSubstrings(String domain){
		String [] array;
		int pos=domain.lastIndexOf(".");
		if(pos==-1){
			array=new String[1];
			array[0]=domain;
		}else{
			array=new String[2];
			array[0]=domain.substring(0, pos);
			array[1]=domain.substring(pos+1,domain.length());
		}
		return array;
	}
	
	public String toStringZones(){
		StringBuilder sb=new StringBuilder();
		for(String zone:servers.keySet()){
			sb.append(zone+"\n");
		}
		return sb.toString();
	}
	
	public String toStringAddresses(){
		StringBuilder sb=new StringBuilder();
		for(Entry<String,String> e:users.entrySet()){
			sb.append(e.getKey()+" "+e.getValue()+"\n");
		}
		return sb.toString();
	}
}