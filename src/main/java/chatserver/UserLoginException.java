package chatserver;

public class UserLoginException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7829840718475404078L;

	public UserLoginException(String reason){
		super(reason);
	}
}
