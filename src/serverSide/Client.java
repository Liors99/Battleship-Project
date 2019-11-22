package serverSide;

/**
 * Class for representation of the clients that connect to the server to
 * either play or observer game instances. A client has a username and password,
 * as well as status variables to denote their status within the system, e.g., 
 * player or observer. If the client is associated with a game room, that is 
 * recorded in a field as well.
 * @author Mariella
 *
 */
public class Client {
	
	private static final int NO_GAMEROOM = -1;
	
	private String username;
	private String password;
	//private boolean isOnline;
	private boolean isPlayer;
	private boolean isObserver;
	private int gameRoomId;
	
	public Client(String username, String password)
	{
		this.username = username;
		this.password = password;
		//this.isOnline = true;
		this.isPlayer = false;
		this.isObserver = false;
		this.gameRoomId = NO_GAMEROOM;
	}

	/*public boolean isOnline() {
		return isOnline;
	}*/
	
	public boolean isPlayer() {
		return isPlayer;
	}
	
	public boolean isObserver() {
		return isObserver;
	}
	
	public void setPlayer(int gameId) 
	{
		this.isPlayer = true;
		this.gameRoomId = gameId;
	}
	
	public void setObserver(int gameId) 
	{
		this.isObserver = true;
		this.gameRoomId = gameId;
	}
	
	public int getGameRoomId() {
		return this.gameRoomId;
	}

	public void resetStatus() 
	{
		this.gameRoomId = NO_GAMEROOM;
		this.isPlayer = false;
		this.isObserver = false;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public boolean passwordMatches(String providedPassword) {
		return this.password.equals(providedPassword);
	}
	
}


