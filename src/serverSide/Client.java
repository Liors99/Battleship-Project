package serverSide;

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
	
}


