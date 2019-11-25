package clientSide;
/*
 * A simple TCP client that sends messages to a server and display the message
   from the server. 
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */


import java.io.*; 
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit; 

class Client {
	
	private static int BUFFERSIZE = 8 * 1024;
	
	private int portNumber;
	private Socket clientSocket;
	private DataOutputStream outBuffer; 
	private InputStream inStream;
	private DataInputStream inBuffer;
	private BufferedReader inFromUser;
	private PlayerGameState playerState;
	
	
	//Flags and IDs
	public static final int JOIN_ID = 1; 
	public static final int JOIN_FLAG = 0; 
	public static final int OBSERVE_ID = 1;
	public static final int OBSERVE_FLAG = 1;
	public static final int LOG_ID = 0;
	public static final int SIGNUP_FLAG = 0;
	public static final int LOGIN_FLAG = 1;
	public static final int LOGOUT_FLAG = 2;
	public static final int PLACE_ID = 2;
	public static final int PLACE_FLAG = 1; 
	public static final int HIT_ID = 2; 
	public static final int HIT_FLAG = 2; 
	
	public static final int CHAT_ID= 8;
	public static final int CHAT_PLAYER_FLAG= 0;
	public static final int CHAT_OBSERVER_FLAG= 1;
	
	//Flag and IDs for Replies
	public static final int REPLY_LOGIN_ID = 0;
	
	public static final int REPLY_LOGIN_ACK_FLAG = 0;
	public static final int REPLY_LOGIN_NACK_FLAG = 1;
	public static final int REPLY_LOGOUT_ACK_FLAG = 2;
	public static final int REPLY_LOGOUT_NACK_FLAG = 3;
	
	
	public static final int REPLY_JOIN_ID = 1;
	
	public static final int REPLY_JOIN_ACK_FLAG = 0;
	public static final int REPLY_JOIN_NAK_FLAG = 1;
	public static final int REPLY_OBS_ACK_FLAG = 2;
	public static final int REPLY_OBS_NACK_FLAG = 3;
	public static final int REPLY_LIST_ACK = 4;
	public static final int REPLY_LIST_NACK = 5;
	
	public static final int REPLY_SHIP_ID = 2;
	
	public static final int REPLY_SHIP_PLACE_ACK_FLAG = 0;
	public static final int REPLY_SHIP_PLACE_NACK_FLAG = 2;
	public static final int REPLY_SHIP_HIT_FLAG = 1;
	
	public static final int FINISHED_PLACING_ID = 3;
	
	public static final int FINISHED_PLACING_ACK_FLAG = 0;
	public static final int FINISHED_PLACING_NACK_FLAG = 1;
	
	public static final int REPLY_TURN_ID = 2;
	
	public static final int REPLY_END_QUIT = 3;
	public static final int REPLY_END_WON = 4;
	public static final int REPLY_END_LOST = 5;
	public static final int REPLY_TURN_FLAG = 6;
	
	public static final int REPLY_HIT_ID = 4;
	public static final int REPLY_HIT_THIS_FLAG = 0;
	public static final int REPLY_HIT_ENEMY_FLAG = 1;
	public static final int REPLY_HIT_NACK = 0; //a miss
	public static final int REPLY_HIT_ACK = 1; //a hit
	
	
	//Reconnect IDs and flags
	public static final int REPLY_RECONNECT_ID = 5;
	
	public static final int REPLY_RECONNECT_DISCONNECT_FLAG = 0;
	public static final int REPLY_RECONNECT_RECONNECT_FLAG = 1;
	public static final int REPLY_RECONNECT_INGAME_FLAG = 2;
	public static final int REPLY_RECONNECT_NOT_INGAME_FLAG = 3;
	
	
	//Dump IDs and flags
	public static final int REPLY_DUMP_ID=6;
	public static final int REPLY_DUMP_THIS_BOARD_FLAG=0;
	public static final int REPLY_DUMP_THIS_HIT_FLAG=1;
	public static final int REPLY_DUMP_OTHER_HIT_FLAG=2;
	
	/**
	 * Constructor, intitalizes the streams and port number
	 * @param add - address number
	 * @param port - port number
	 * @throws IOException
	 */
	public Client(String add, int port) throws IOException
	{
		// Initialize a client socket connection to the server
		portNumber = port;
        clientSocket = new Socket(add, port); 
        
        // Initialize input and an output stream for the connection(s)
        outBuffer = new DataOutputStream(clientSocket.getOutputStream());
        
        // Break this up into multiple steps so we can see how many bytes
        // are available at the input stream at any given time
        inStream = clientSocket.getInputStream();
        // Make the in buffer capacity 8 KB
        inBuffer = new DataInputStream(
				new BufferedInputStream(inStream));
        
        inFromUser = new BufferedReader(
				new InputStreamReader(System.in));
        
	}
	

	
	private static String add;
	private static int port;
	
    public static void main(String args[]) throws Exception 
    { 
        if (args.length != 2)
        {
            System.out.println("Usage: Client <Server IP> <Server Port>");
            System.exit(1);
        }

        add= args[0];
        port = Integer.parseInt(args[1]);
    	
        preLoginPhase();
        
        //tcpClient.clientSocket.close();           
    }
    
    
    /**
     * Handle the pre-login phase, i.e. asks the client if he wants to signup or login and create a new socket and playerstate
     * @throws IOException
     * @throws InterruptedException
     */
    private static void preLoginPhase() throws IOException, InterruptedException {
    	
    	
    	// Make instance of TCP client
    	Client tcpClient = new Client(add , port);
    	PlayerGameState playerState = new PlayerGameState();
    	
    	tcpClient.setPlayerState(playerState);
    	
    	int res = tcpClient.getUserLoginSignup();
    	
        if(res == 0) {
        	tcpClient.signUp();
        }
        
        else if(res == 1) {

        	tcpClient.Login();
        	
        }
        
        tcpClient.getUserPath();
        
    }
    
    /**
     * Gets the path(play or observe) that the user chooses, and follows that path
     * @throws IOException
     * @throws InterruptedException
     */
    private void getUserPath() throws IOException, InterruptedException {
    	int res = getJoinObserve();
    	if(res == 0) {
    		observeRequest();
    	}
    	else if(res == 1) {
			joinRequest();
			System.out.println("Joined a game!");
			
			
    		//Pre-game phase (placing ships)
			preGamePhase();
    		
    		//Mid-game phase 
			
			gamePhase();
    		
    		
    		this.clientSocket.close();   
    	}
    	else {
    		Logout();
    	}
    }
    
    /**
     * Prompts the user to place ships, as long as there are any ships left to be placed
     * @throws NumberFormatException
     * @throws IOException
     * @throws InterruptedException 
     */
    private void preGamePhase() throws NumberFormatException, IOException, InterruptedException {
    	//PlayerState.isGameOver();
    	while(playerState.shipsAvaliable()) {
    		
    		boolean made_move= false;
    		
    		while(!made_move) {
    			System.out.println("Waiting for user to make a move....");
    			int res = getUserAction_PLACE();
    			switch(res) {
    				case 0:
    					Logout();
    					made_move=true;
    					break;
    				case 1:
    					sendChatMSG();
    					break;
    				case 2:
    					PlaceShip();
    					made_move=true;
    					break;
        					
        			
        		}
    		}
			
    		
    	}
    	
    	playerState.displayBoards();
    	System.out.println("Waiting for a the other player to finish placing ships....");
    	waitForACK(FINISHED_PLACING_ID, FINISHED_PLACING_ACK_FLAG);
    	System.out.println("All ships placed. GAME ON!");
    }
    
    /**
     * The game-phase itself, gets protocols IDs and handles them as the game progresses (i.e. when you can hit ships)
     * @throws IOException
     * @throws InterruptedException
     */
    private void gamePhase() throws IOException, InterruptedException 
    {
    		boolean isWon= false;
    		
	    	while(!playerState.isGameOver()) {
	    		ClientMessage rec_msg = receiveMsg();
	    		int protocolId = rec_msg.getProtocolId();
	    		int flag = rec_msg.getFlag();
	    		if (protocolId == REPLY_TURN_ID && flag == REPLY_TURN_FLAG) //it's my turn
	    		{
	    			
	    			boolean made_move = false;
	    			
	    			while(!made_move) {
	    				int res = getUserAction_HIT();
		    			switch(res) {
		    				case 0:
		    					Logout();
		    					made_move=true;
		    					break;
		    				case 1:
		    					sendChatMSG();
		    					break;
		    				case 2:
		    					HitShip();
		    					made_move=true;
		    					break;
		    					
		    			}
	    			}
	    			
	    			
	    			
	    		}
	    		else if (protocolId == REPLY_HIT_ID && flag == REPLY_HIT_THIS_FLAG)
	    		{
	    			getHitShip(rec_msg.getDataShipInts());
	    		}
	    		else if(protocolId==CHAT_ID && flag == CHAT_PLAYER_FLAG) {
	    			getChatMSG(rec_msg.getData());
	    		}
	    		else if(protocolId==REPLY_TURN_ID && (flag == REPLY_END_WON || flag == REPLY_END_QUIT)) {
	    			isWon=true;
	    			break;
	    		}
	    		else if(protocolId==REPLY_TURN_ID && flag == REPLY_END_LOST) {
	    			break;
	    		}
	    		
	    	}
	    	
	    	if(isWon) {
	    		System.out.println("YOU WON THE GAME");
	    	}
	    	else {
	    		System.out.println("YOU LOST THE GAME");
	    	}
	    	
	    	playerState.resetGame();
	    	getUserPath();
    }
    
    /**
     * Makes a game join request to the server
     * @throws IOException
     */
    private void joinRequest() throws IOException {
    	int id = JOIN_ID;
    	int flag = JOIN_FLAG;
    	
    	ClientMessage send_msg = new ClientMessage();
    	send_msg.setProtocolId(id);
    	send_msg.setFlag(flag);
    	send_msg.setData(intToByteArray(0));
    	
    	SendMessage(send_msg.getEntirePacket());
    	
    	System.out.println("Trying to find a game....");
    	waitForACK(REPLY_JOIN_ID,REPLY_JOIN_ACK_FLAG);
    }
    
    /**
     * Makes a game observe request to the server
     * @throws IOException
     */
    private void observeRequest() throws IOException {
    	int id = OBSERVE_ID;
    	int flag = OBSERVE_FLAG;
    	
    	ClientMessage send_msg = new ClientMessage();
    	send_msg.setProtocolId(id);
    	send_msg.setFlag(flag);
    	send_msg.setData(intToByteArray(0));
    	
    	SendMessage(send_msg.getEntirePacket());
    	
    	waitForACK(REPLY_JOIN_ID,REPLY_OBS_ACK_FLAG);
    }
    
    
    /**
     * Gets the username and password, and sends them to the server
     * @throws IOException
     * @throws InterruptedException 
     */
    private void Login() throws IOException, InterruptedException {
    	boolean valid = false;
    	String[] splitted_input = getUserMsg("Enter Login information: <username> <password>", 2);
    	
    	byte[] username = splitted_input[0].getBytes();
    	byte[] password = splitted_input[1].getBytes();
    	
    	
    	ClientMessage send_msg = new ClientMessage();
    	
    	int id = LOG_ID;
    	int flag = LOGIN_FLAG;
    	send_msg.setProtocolId(id);
    	send_msg.setFlag(flag);
    	
    	
    	
    	byte[] data = new byte[4 + username.length + password.length];
    	int pos = 0;
    	byte[] len = ByteBuffer.allocate(4).putInt(username.length).array();
    	System.arraycopy(len, 0, data,  pos, len.length);
    	pos+=4;
    	
    	System.arraycopy(username, 0, data,  pos, username.length);
    	pos += username.length;
    	System.arraycopy(password, 0, data,  pos, password.length);
    	send_msg.setData(data);
    	
    	SendMessage(send_msg.getEntirePacket());
    	valid = waitForACK(REPLY_LOGIN_ID, REPLY_LOGIN_ACK_FLAG);
    	
    	if(!valid) {
    		System.out.println("Username or password incorrect");
    		preLoginPhase();
    	}
    	
    	
    	
    	//Figure out if we are in game
    	ClientMessage rec_msg = receiveMsg();
    	if(rec_msg.getProtocolId() == REPLY_RECONNECT_ID) {
    		
    		//If we are in game
    		if(rec_msg.getFlag() == REPLY_RECONNECT_INGAME_FLAG) {
    			
    			
    			//Get our current board
    			rec_msg = receiveMsg();
    			
    			//Get hits on our board
    			
    			
    			//Gets hits on the enemy board
    			
    			PlaceShip();
    		}
    		
    		//We are NOT in game
    		else {
    			getUserPath();
    		}
    	}
    }
    	
    	
	/**
     * Gets the username and password, and sends them to the server
     * @throws IOException
     * @throws InterruptedException 
     */
    private void signUp() throws IOException, InterruptedException {
    	boolean valid = false;
    	String[] splitted_input = getUserMsg("Enter signup information: <username> <password>", 2);
    	
    	byte[] username = splitted_input[0].getBytes();
    	byte[] password = splitted_input[1].getBytes();
    	
    	
    	ClientMessage send_msg = new ClientMessage();
    	
    	int id = LOG_ID;
    	int flag = SIGNUP_FLAG;
    	send_msg.setProtocolId(id);
    	send_msg.setFlag(flag);
    	
    	
    	byte[] data = new byte[4 + username.length + password.length];
    	int pos = 0;
    	byte[] len = ByteBuffer.allocate(4).putInt(username.length).array();
    	System.arraycopy(len, 0, data,  pos, len.length);
    	pos+=4;
    	
    	System.arraycopy(username, 0, data,  pos, username.length);
    	pos += username.length;
    	System.arraycopy(password, 0, data,  pos, password.length);
    	send_msg.setData(data);
    	
    	SendMessage(send_msg.getEntirePacket());
    	valid = waitForACK(REPLY_LOGIN_ID, REPLY_LOGIN_ACK_FLAG);
    	
    	if(!valid) {
    		System.out.println("Username already exists or already logged in");
    		preLoginPhase();
    	}
    }
    
    /**
     * Sends a logout request to the server and then goes back to the pre-login phase
     * @throws IOException
     * @throws InterruptedException
     */
    private void Logout() throws IOException, InterruptedException {
    	/*
    	byte[] data = new byte[2];
    	data[0]=(byte)LOG_ID;
    	data[1] = (byte)LOGOUT_FLAG;
    	*/
    	
    	ClientMessage send_msg = new ClientMessage();
    	send_msg.setProtocolId(LOG_ID);
    	send_msg.setFlag(LOGOUT_FLAG);
    	send_msg.setData(intToByteArray(0));
    	SendMessage(send_msg.getEntirePacket());
    	
    	waitForACK(REPLY_LOGIN_ID,REPLY_LOGOUT_ACK_FLAG);
    	this.clientSocket.close();
    	preLoginPhase();
    	
    	
    }
    
    /**
     * Handles ship placement from client and sends the placement to the server
     * @throws NumberFormatException
     * @throws IOException
     */
    private void PlaceShip() throws NumberFormatException, IOException {
    	
    	boolean valid = false;
    	int ship_n;
    	int x1,y1,x2,y2;
    	while(!valid) {
    		playerState.displayBoards();
    		playerState.printAvaliableShips(); //prints avaliable ships
    		
    		String[] splitted_input = getUserMsg("Enter a ship to place: <Number> <X> <Y> <X> <Y>", 5);
        	try {
	        	ship_n =  Integer.parseInt(splitted_input[0]);
	        	x1 =  Integer.parseInt(splitted_input[1]);
	        	y1 =  Integer.parseInt(splitted_input[2]);
	        	x2 =  Integer.parseInt(splitted_input[3]);
	        	y2 =  Integer.parseInt(splitted_input[4]);
	        	
	        	valid = playerState.placeShipPlayer1Board(ship_n, x1, y1, x2, y2); // return if successfully placed. ship_n assumed to be 0 for destroyer, coords from 0-9 inclusive
				if(!valid){
					System.out.println("Invalid move!");
					continue; // the ship placement failed.
				}
				
				//PlayerState.displayBoards();
				
				ClientMessage send_msg = new ClientMessage();
				
	        	int id = PLACE_ID;
	        	int flag = PLACE_FLAG;
	        	
	        	send_msg.setProtocolId(id);
	        	send_msg.setFlag(flag);
	        	
	        	byte protocolByte = (byte) id;
	        	byte flagByte = (byte) flag;
	        	
	        	//Send move as string
	        	String dataString = "";
	        	for (String input : splitted_input)
	        		dataString = dataString + input;
	        	
	        	//byte[] data = new byte[2+5];
	        	byte outBytes[] = new byte[dataString.getBytes().length];
	        	outBytes[0] = protocolByte;
	        	outBytes[1] = flagByte;
	        	
	        	//Convert data string to byte array
	        	byte[] data = dataString.getBytes();
	        for (int i = 0; i < data.length; i++)
	        		outBytes[i] = data[i];
	        	
	        	//data[2] = (byte)ship_n;
	        	//data[3] = (byte)x1;
	        	//data[4] = (byte)y1;
	        	//data[5] = (byte)x2;
	        	//data[6] = (byte)y2;
	        
	        	send_msg.setData(intToByteArray(outBytes.length));
	        	send_msg.setData(outBytes);
	        	
	        	SendMessage(send_msg.getEntirePacket());
	        	waitForACK(REPLY_SHIP_ID, REPLY_SHIP_PLACE_ACK_FLAG);
	        	
        	}
        	catch(Exception e) {
        		continue;
        	}   	
    	}
    	
    	
    }
    
    /**
     * Gets hit information from the server and updates the corresponding board on client side
     * @param data
     * @throws InterruptedException
     */
    private void getHitShip(int[] data) throws InterruptedException 
    {
    		Move hitMove = new Move();
    		hitMove.setCol(data[0]);
    		hitMove.setRow(data[1]);
    		
    		if (data[2] == REPLY_HIT_ACK)
    		{
    			System.out.println("You have been hit at " + data[0] + "," + data[1]);
    			hitMove.setValue(2);
    		}
    		else
    		{
    			System.out.println("You have survived an attempted hit at " + data[0] + "," + data[1]);
    			hitMove.setValue(3);
    		}  	
    		playerState.updatePlayer1Board(hitMove);
    		playerState.displayBoards();
    	}
    	   
    
    /**
     * Sends a hit to the server and updates the corresponding board on client side
     * @throws IOException
     */
    private void HitShip() throws IOException {
    	
    	boolean valid = false;
    	boolean server_ACK= false;
    	int x;
    	int y;
    	
    	while(!valid && !server_ACK) {
	    	String[] splitted_input = getUserMsg("Enter a position to hit: <X> <Y>", 2);
	    	
	    	try {
		    	x =  Integer.parseInt(splitted_input[0]);
		    	y =  Integer.parseInt(splitted_input[1]);
	    	
	    	
		    	Move userMove= new Move();
		    	userMove.setCol(x);
		    	userMove.setRow(y);
		    	
		    	
		    	//Vlad's function here.
		    	
		    	valid = playerState.isValidMove(userMove);
		    	if(!valid) {
		    		continue;
		    	}
		    	
		    	byte[] xByte = splitted_input[0].getBytes();
		    	byte[] yByte = splitted_input[1].getBytes();
		    	
		    	
		    	int id = HIT_ID;
	        	int flag = HIT_FLAG;
	        	byte protocolByte = (byte) id;
	        	byte flagByte = (byte) flag;
	        	
	        	
	        	byte[] data = new byte[2 + xByte.length + yByte.length];
	        	data[0] = protocolByte;
	        	data[1] = flagByte;
	        	System.arraycopy(xByte, 0, data,  2, xByte.length);
		    	System.arraycopy(yByte, 0, data,  2 + xByte.length, yByte.length);
	        	
	        	SendMessage(data);
	        	
	        	int[] ship_cor = getHitData();
	        	if(ship_cor[1] == REPLY_HIT_ENEMY_FLAG) {
	        		if(ship_cor[4] == REPLY_HIT_ACK) {
	        			System.out.println("You have successfully hit your opponent at "+x +"," + y);
	        			userMove.setValue(2);
	        		}
	        		else {
	        			System.out.println("You have missed your shot at "+x +"," + y);
	        			userMove.setValue(3);
	        		}
	        		playerState.updatePlayer2Board(userMove);
	        	}
	        	else {
	        		System.out.println("Received wrong confirmation, try again:");
	        		continue;
	        	}
	        	
	        	playerState.displayBoards();
	        	
	        	//server_ACK= true;
	        	//server_ACK = waitForACK(REPLY_SHIP_ID,REPLY_SHIP_HIT_FLAG);
	    	}
	    	catch(Exception e) {
	    		continue;
	    	}
    	}
    	
    	
    }

    
    /**
     * Gets from the user if he wants to login or sign up
     * @return
     * @throws IOException
     */
    private int getUserLoginSignup() throws IOException {
       while(true) {
    	   System.out.println("Enter one of the following number: ");
           System.out.println("0: Signup ");
           System.out.println("1: Login");
           
    	   int res= getUserResponse(0 , 1);
    	   
    	   switch (res) {
	        case 0 :
	        	return 0;
	        case 1 :
	        	return 1;
	        case -1:
	    		System.out.println("Not a valid option");
	    		//continue;
	        	
    	   }
       }
       
        
    }
    
    /**
     * Gets from the user if he wants to join or observe a game
     * @return
     * @throws IOException
     */
    private int getJoinObserve() throws IOException {
    	while(true) {
     	   System.out.println("Enter one of the following number: ");
            
           System.out.println("0: Observe a game ");
           System.out.println("1: Join a game");
           System.out.println("2: Logout");
     	   int res= getUserResponse(0 , 2);
     	   switch (res) {
 	        case 0:
 	        	return 0;
 	        case 1:
 	        	return 1;
 	        case 2:
 	        	return 2;
 	        case -1:
 	    		System.out.println("Not a valid option");
 	    		continue;
 	        	
     	   }
        }
    }
    
    
    
    /**
     * Gets the action that the user wants to perform given a list of options for THE HIT PHASE
     * @return
     * @throws IOException 
     */
    private int getUserAction_HIT() throws IOException {
    	while(true) {
    		System.out.println("Enter one of the following number: ");
    		
    		System.out.println("0: Logout ");
    		System.out.println("1: Send chat message ");
    		System.out.println("2: Make a hit move ");
    		
    		int res= getUserResponse(0 , 2);
      	   	switch (res) {
	  	        case 0:
	  	        	return 0;
	  	        case 1:
	  	        	return 1;
	  	        case 2:
	  	        	return 2;
	  	        case -1:
	  	    		System.out.println("Not a valid option");
	  	    		continue;
  	        	
      	   }
    	}
    }
    
    
    /**
     * Gets the action that the user wants to perform given a list of options for THE PLACE PHASE
     * @return
     * @throws IOException 
     */
    private int getUserAction_PLACE() throws IOException {
    	while(true) {
    		System.out.println("Enter one of the following number: ");
    		
    		System.out.println("0: Logout ");
    		System.out.println("1: Send chat message ");
    		System.out.println("2: Place a ship ");
    		
    		int res= getUserResponse(0 , 2);
      	   	switch (res) {
	  	        case 0:
	  	        	return 0;
	  	        case 1:
	  	        	return 1;
	  	        case 2:
	  	        	return 2;
	  	        case -1:
	  	    		System.out.println("Not a valid option");
	  	    		continue;
  	        	
      	   }
    	}
    }
    
    /**
     * Sends any message to the server using the stream
     * @param msg
     * @throws IOException
     */
    public void SendMessage(byte[] msg) throws IOException {
    	// Send to the server
    	this.outBuffer.write(msg);
    	this.outBuffer.flush();
    }
    
    
    /**
     * Gets ANY message from the server and decodes it into protocol ID and FLAG, and puts this information into an array along with data.
     * @return
     * @throws InterruptedException
     */
    private ClientMessage receiveMsg() throws InterruptedException
    {    	
    	int[] result = new int[5];
    	
    	ClientMessage in_msg = new ClientMessage();
    	try {
    		
    		
    		
    		byte[] inBytes = new byte[BUFFERSIZE];
    		StringBuilder strBuilder = new StringBuilder();
    		int bytesRead = 0;
    		
    		int protocolId = -1;
    		int flag = -1;
    		int data_length = -1;
    		
    		if(inStream.available()>0) {
    			protocolId = inStream.read();
    	    	flag = inStream.read();
    	    	data_length = fromByteArray(inStream.readNBytes(4));
    	    	
    	    	in_msg.setProtocolId(protocolId);
    	    	in_msg.setFlag(flag);
    	    	in_msg.setData(inStream.readNBytes(data_length));
    	    	
    	    	System.out.println("Received PROTOCOL ID: " + protocolId);
    	    	System.out.println("Received PROTOCOL FLAG: "+flag);
    	    	System.out.println("LENGTH OF DATA: "+data_length);
    		}
    		
    		result[0]=protocolId;
    		result[1]=flag;
    		
    		
    		
	    	if(protocolId == REPLY_HIT_ID) {
	    		in_msg.setData(intToByteArray(Character.getNumericValue(((char) inStream.read()))));
	    		in_msg.setData(intToByteArray(Character.getNumericValue(((char) inStream.read()))));
	    		in_msg.setData(intToByteArray(Character.getNumericValue(((char) inStream.read()))));
	    		

	    	}
	    	else if(protocolId == CHAT_ID) {
	    		//Get the size of the text message, and move populate the data section.
	    		
	    		int size = inStream.read();
	    	}
	    	else {
	    		result[2]=-1;
	    		result[3]=-1;
	    		result[4]=-1;
	    	}

	    	
	    	TimeUnit.MICROSECONDS.sleep(1);
	    	
    	
    	
		} 
    		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return in_msg;
    }
    
    
    /**
     * Checks that the user's response when choosing is valid
     * @param i
     * @param j
     * @return
     * @throws IOException
     */
    private int getUserResponse(int i , int j) throws IOException {
    	try {
    		String user_input = inFromUser.readLine();
    		
	    	int res = Integer.parseInt(user_input);
	    	
	    	if(res < i || res > j) {
	    		return -1;
	    	}
	    	
	    	return res;
    	}
    	catch(Exception e) {
    		return -1;
    	}
    }
    
    /**
     * Verifies that the user's input matches the number of parameters required
     * @param msg
     * @param args
     * @return
     * @throws IOException
     */
    private String[] getUserMsg(String msg, int args) throws IOException {
    	
    	boolean valid = false;
    	String[] splitted_input = null;
    	
    	while(!valid) {
    	
	    	System.out.println(msg);
	    	splitted_input = inFromUser.readLine().split("\\s+");
	    	
	    	if(splitted_input.length == args) {
	    		valid = true;
	    	}
    	}
    	
    	return splitted_input;
    }
    
    /**
     * Gets ACK from server
     * @param ID - protocol ID
     * @param FLAG - protocol flag
     * @return True if ACK recieved, False otherwise
     */
    private boolean waitForACK(int ID, int FLAG) {
    	
    	//int[] data = new int[5];
    	int id=-1;
    	int flag=-1;
    	ClientMessage rec_msg = null;
    	do {
    		try {
    			rec_msg = receiveMsg();
    			id= rec_msg.getProtocolId();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	} while( id!= ID);
    	
    	flag = rec_msg.getFlag();
    	return flag == FLAG;
    }
    
    
    
    public ClientMessage getServerMsg() throws InterruptedException {
    	ClientMessage res;
    	do {
    		res = receiveMsg();
    	}while(res.getProtocolId()==-1 && res.getFlag()==-1);
    	
    	return res;
    	
    	
    }
    
    /**
     * Gets the hit coordiantes and the type 
     * @return
     * @throws InterruptedException
     */
    private int[] getHitData() throws InterruptedException {
    	int[] data = new int[5];
    	int id = -1;
    	ClientMessage rec_msg;
    	do {
    		rec_msg = receiveMsg();
    		id = rec_msg.getProtocolId();
    	}while(id!=REPLY_HIT_ID);
    	
    	data[0] = id;
    	data[1]=rec_msg.getFlag();
    	
    	byte[] msg_data = rec_msg.getData();
    	
    	System.out.println("-------- Length of data recieved: " + msg_data.length);
    	data[2] = fromByteArray(Arrays.copyOfRange(msg_data, 0, 4));
    	data[3] = fromByteArray(Arrays.copyOfRange(msg_data, 4, 8));
    	data[4] = fromByteArray(Arrays.copyOfRange(msg_data, 8, 12));
    	
    	
    	System.out.println("Received data[2]: " + data[2]);
    	System.out.println("Received data[3]: " + data[3]);
    	System.out.println("Received data[4]: " + data[4]);
    	return data;
    }
    
   
    
    
    //"Special command handling"
    
    
    
   
   /**
    * Sends a chat message to the server
    * @param msg
    * @throws IOException 
    */
   private void sendChatMSG() throws IOException {
	   System.out.println("Type in a message to be sent: ");
	   
	   String msg = inFromUser.readLine();
	   ClientMessage packet = new ClientMessage(CHAT_ID, CHAT_PLAYER_FLAG, msg.getBytes());
	   SendMessage(packet.getEntirePacket());
   }
   
   private static void getChatMSG(byte[] msg) {
	   
	   String s = new String(msg);
	   System.out.println("------------ Your opponent has messaged: " + s);
   }
   
   
   //Helper methods (GENERAL)
   
   /**
    * Converts an integer to a byte array
    * @param n
    * @return
    */
   public static byte[] intToByteArray(int n) {
   	return ByteBuffer.allocate(4).putInt(n).array();
   }
   
   
   /**
    * Converts a byte array into an integer
    * @param bytes
    * @return
    */
   public static int fromByteArray(byte[] bytes) {
       return ByteBuffer.wrap(bytes).getInt();
  }
   
   
   public InputStream getInputStream() {
	   return this.inStream;
   }
   
   private void setPlayerState(PlayerGameState g) {
	   this.playerState=g;
   }
   
   public PlayerGameState getPlayerState() {
	   return this.playerState;
   }
   
   
} 
