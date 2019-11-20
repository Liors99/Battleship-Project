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
	private static InputStream inStream;
	private static DataInputStream inBuffer;
	private static BufferedReader inFromUser;
	private static Client tcpClient;
	private static PlayerGameState playerState;
	
	
	//Flags and IDs
	private static final int JOIN_ID = 1; 
	private static final int JOIN_FLAG = 0; 
	private static final int OBSERVE_ID = 1;
	private static final int OBSERVE_FLAG = 1;
	private static final int LOG_ID = 0;
	private static final int LOGIN_FLAG = 1;
	private static final int LOGOUT_FLAG = 2;
	private static final int PLACE_ID = 2;
	private static final int PLACE_FLAG = 1; 
	private static final int HIT_ID = 2; 
	private static final int HIT_FLAG = 2; 
	
	//Flag and IDs for Replies
	private static final int REPLY_LOGIN_ID = 0;
	
	private static final int REPLY_LOGIN_ACK_FLAG = 0;
	private static final int REPLY_LOGIN_NACK_FLAG = 1;
	private static final int REPLY_LOGOUT_ACK_FLAG = 2;
	private static final int REPLY_LOGOUT_NACK_FLAG = 3;
	
	
	private static final int REPLY_JOIN_ID = 1;
	
	private static final int REPLY_JOIN_ACK_FLAG = 0;
	private static final int REPLY_JOIN_NAK_FLAG = 1;
	private static final int REPLY_OBS_ACK_FLAG = 2;
	private static final int REPLY_OBS_NACK_FLAG = 3;
	private static final int REPLY_LIST_ACK = 4;
	private static final int REPLY_LIST_NACK = 5;
	
	private static final int REPLY_SHIP_ID = 2;
	
	private static final int REPLY_SHIP_PLACE_ACK_FLAG = 0;
	private static final int REPLY_SHIP_PLACE_NACK_FLAG = 2;
	private static final int REPLY_SHIP_HIT_FLAG = 1;
	
	private static final int FINISHED_PLACING_ID=3;
	
	private static final int FINISHED_PLACING_ACK_FLAG = 0;
	private static final int FINISHED_PLACING_NACK_FLAG = 1;
	
	private static final int REPLY_TURN_ID = 2;
	
	private static final int REPLY_END_QUIT = 3;
	private static final int REPLY_END_WON = 4;
	private static final int REPLY_END_LOST = 5;
	private static final int REPLY_TURN_FLAG = 6;
	
	private static final int REPLY_HIT_ID = 4;
	private static final int REPLY_HIT_THIS_FLAG = 0;
	private static final int REPLY_HIT_ENEMY_FLAG = 1;
	private static final int REPLY_HIT_NACK = 0; //a miss
	private static final int REPLY_HIT_ACK = 1; //a hit
	
	
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
    	
        
        inFromUser = new BufferedReader(
				new InputStreamReader(System.in));
        
        preLoginPhase();
        
        //tcpClient.clientSocket.close();           
    }
    
    
    private static void preLoginPhase() throws IOException, InterruptedException {
    	int res= getUserLoginSignup();
    	// Make instance of TCP client
        tcpClient = new Client(add , port);
        playerState = new PlayerGameState();
    	
        if(res == 1 || res == 0) {

        	Login();
        	res = getJoinObserve();
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
        		
        		
        		tcpClient.clientSocket.close();   
        	}
        	else {
        		Logout();
        	}
        }
        
    }
    
    private static void preGamePhase() throws NumberFormatException, IOException {
    	//PlayerState.isGameOver();
    	while(playerState.shipsAvaliable()) {
			PlaceShip();
    	}
    	
    	playerState.displayBoards();
    	System.out.println("Waiting for a the other player to finish placing ships....");
    	waitForACK(FINISHED_PLACING_ID, FINISHED_PLACING_ACK_FLAG);
    }
    
    private static void gamePhase() throws IOException, InterruptedException 
    {
    		boolean isWon= false;
    		
	    	while(!playerState.isGameOver()) {
	    		int[] data = receiveMsg();
	    		int protocolId = data[0];
	    		int flag = data[1];
	    		if (data[0] == REPLY_TURN_ID && data[1] == REPLY_TURN_FLAG) //it's my turn
	    		{
	    			HitShip();
	    		}
	    		else if (data[0] == REPLY_HIT_ID && data[1] == REPLY_HIT_THIS_FLAG)
	    		{
	    			getHitShip(data);
	    		}
	    		else if(data[0]==REPLY_TURN_ID && data[1] == REPLY_END_WON) {
	    			isWon=true;
	    			break;
	    		}
	    		else if(data[0]==REPLY_TURN_ID && data[1] == REPLY_END_LOST) {
	    			break;
	    		}
	    		
	    	}
	    	
	    	if(isWon) {
	    		System.out.println("YOU WON THE GAME");
	    	}
	    	else {
	    		System.out.println("YOU LOST THE GAME");
	    	}
	    	
	    	getJoinObserve();
    }
    
    private static void joinRequest() throws IOException {
    	int id = JOIN_ID;
    	int flag = JOIN_FLAG;
    	byte protocolByte = (byte) id;
    	byte flagByte = (byte) flag;
    	
    	byte[] data = new byte[2];
    	data[0] = protocolByte;
    	data[1] = flagByte;
    	
    	SendMessage(data);
    	
    	System.out.println("Trying to find a game....");
    	waitForACK(REPLY_JOIN_ID,REPLY_JOIN_ACK_FLAG);
    }
    
    private static void observeRequest() throws IOException {
    	int id = OBSERVE_ID;
    	int flag = OBSERVE_FLAG;
    	byte protocolByte = (byte) id;
    	byte flagByte = (byte) flag;
    	
    	byte[] data = new byte[2];
    	data[0] = protocolByte;
    	data[1] = flagByte;
    	
    	SendMessage(data);
    	
    	waitForACK(REPLY_JOIN_ID,REPLY_OBS_ACK_FLAG);
    }
    
    private static void Login() throws IOException {
    	boolean valid = false;
    	while(!valid) {
	    	String[] splitted_input = getUserMsg("Enter Login information: <username> <password>", 2);
	    	
	    	byte[] username = splitted_input[0].getBytes();
	    	byte[] password = splitted_input[1].getBytes();
	    	
	    	int id = LOG_ID;
	    	int flag = LOGIN_FLAG;
	    	byte protocolByte = (byte) id;
	    	byte flagByte = (byte) flag;
	    	
	    	
	    	byte[] data = new byte[2 + 4 + username.length + password.length];
	    	data[0] = protocolByte;
	    	data[1] = flagByte;
	    	int pos = 2;
	    	byte[] len = ByteBuffer.allocate(4).putInt(username.length).array();
	    	System.arraycopy(len, 0, data,  pos, len.length);
	    	pos+=4;
	    	
	    	System.arraycopy(username, 0, data,  pos, username.length);
	    	pos += username.length;
	    	System.arraycopy(password, 0, data,  pos, password.length);
	    	
	    	SendMessage(data);
	    	valid = waitForACK(REPLY_LOGIN_ID, REPLY_LOGIN_ACK_FLAG);
    	}
    	
    	
    	
    }
    
    
    private static void Logout() throws IOException, InterruptedException {
    	byte[] data = new byte[2];
    	data[0]=(byte)LOG_ID;
    	data[1] = (byte)LOGOUT_FLAG;
    	
    	SendMessage(data);
    	
    	waitForACK(REPLY_LOGIN_ID,REPLY_LOGOUT_ACK_FLAG);
    	tcpClient.clientSocket.close();
    	preLoginPhase();
    	
    	
    }
    private static void PlaceShip() throws NumberFormatException, IOException {
    	
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
				
	        	int id = PLACE_ID;
	        	int flag = PLACE_FLAG;
	        	byte protocolByte = (byte) id;
	        	byte flagByte = (byte) flag;
	        	
	        	//Send move as string
	        	String dataString = "";
	        	for (String input : splitted_input)
	        		dataString = dataString + input;
	        	
	        	//byte[] data = new byte[2+5];
	        	byte outBytes[] = new byte[2+dataString.getBytes().length];
	        	outBytes[0] = protocolByte;
	        	outBytes[1] = flagByte;
	        	
	        	//Convert data string to byte array
	        	byte[] data = dataString.getBytes();
	        for (int i = 0; i < data.length; i++)
	        		outBytes[2+i] = data[i];
	        	
	        	//data[2] = (byte)ship_n;
	        	//data[3] = (byte)x1;
	        	//data[4] = (byte)y1;
	        	//data[5] = (byte)x2;
	        	//data[6] = (byte)y2;
	        	
	        	SendMessage(outBytes);
	        	waitForACK(REPLY_SHIP_ID, REPLY_SHIP_PLACE_ACK_FLAG);
	        	
        	}
        	catch(Exception e) {
        		continue;
        	}   	
    	}
    	
    	
    }
    
    private static void getHitShip(int[] data) throws InterruptedException 
    {
    		Move hitMove = new Move();
    		hitMove.setCol(data[2]);
    		hitMove.setRow(data[3]);
    		
    		if (data[4] == REPLY_HIT_ACK)
    		{
    			System.out.println("You have been hit at " + data[2] + "," + data[3]);
    			hitMove.setValue(2);
    		}
    		else if (data[3] == REPLY_HIT_NACK)
    		{
    			System.out.println("You have survived an attempted hit at " + data[2] + "," + data[3]);
    			hitMove.setValue(3);
    		}  	
    		playerState.updatePlayer1Board(hitMove);
    		playerState.displayBoards();
    	}
    	   
    private static void HitShip() throws IOException {
    	
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
	        	
	        	int[] ship_cor = getHit();
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

    
    private static int getUserLoginSignup() throws IOException {
       while(true) {
    	   System.out.println("Enter one of the following number: ");
           System.out.println("0: Signup ");
           System.out.println("1: Login");
           
    	   int res= getUserResponse(0 , 1);
    	   
    	   switch (res) {
	        case 0:
	        	return 0;
	        case 1:
	        	return 1;
	        case -1:
	    		System.out.println("Not a valid option");
	    		//continue;
	        	
    	   }
       }
       
        
    }
    
    private static int getJoinObserve() throws IOException {
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
    
    private static void SendMessage(byte[] msg) throws IOException {
        

    	// Send to the server
    	tcpClient.outBuffer.write(msg);
    	tcpClient.outBuffer.flush();
    }
    
    
    //Needs to be worked on
    private static int[] receiveMsg() throws InterruptedException
    {    	
    	int[] result = new int[5];
    	
    	try {
    		byte[] inBytes = new byte[BUFFERSIZE];
    		StringBuilder strBuilder = new StringBuilder();
    		int bytesRead = 0;
    		
    		int protocolId = -1;
    		int flag = -1;
    		
    		if(inStream.available()>0) {
    			protocolId = inStream.read();
    	    	flag = inStream.read();
    	    	
    	    	System.out.println("Received PROTOCOL ID: " + protocolId);
    	    	System.out.println("Received PROTOCOL FLAG: "+flag);
    		}
    		
    		result[0]=protocolId;
    		result[1]=flag;
    		
	    	if(protocolId == REPLY_HIT_ID) {
		    	result[2] = Character.getNumericValue(((char) inStream.read()));
		    	result[3] = Character.getNumericValue(((char) inStream.read()));
		    	result[4] = Character.getNumericValue(((char) inStream.read()));
		    	
		    	//System.out.println("Received data[2]: " + result[2]);
		    	//System.out.println("Received data[3]: " + result[3]);
		    	//System.out.println("Received data[4]: " + result[4]);
	    	}
	    	else {
	    		result[2]=-1;
	    		result[3]=-1;
	    		result[4]=-1;
	    	}
	    	

	    	
	    	
	    	TimeUnit.MICROSECONDS.sleep(1);
	    	//System.out.println("Recieved PROTOCOL ID: " + protocolId);
	    	//System.out.println("Recieved PROTOCL FLAG: "+flag);
	    	
    	
    	
		} 
    		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return result;
    }
    
    
    /**
     * Checks that the user's response when choosing is valid
     * @param i
     * @param j
     * @return
     * @throws IOException
     */
    private static int getUserResponse(int i , int j) throws IOException {
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
    private static String[] getUserMsg(String msg, int args) throws IOException {
    	
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
    private static boolean waitForACK(int ID, int FLAG) {
    	
    	int[] data = new int[5];
    	do {
    		try {
				data= receiveMsg();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	} while( data[0]!= ID);
    	return data[1] == FLAG;
    }
    
    
    
    /**
     * Gets the hit coordiantes and the type 
     * @return
     * @throws InterruptedException
     */
    private static int[] getHit() throws InterruptedException {
    	int[] data = new int[5];
    	do {
    		data = receiveMsg();
    	}while(data[0]!=REPLY_HIT_ID);
    	
    	return data;
    }
} 
