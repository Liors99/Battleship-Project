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
	private static PlayerGameState PlayerState;
	
	
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
	private static final int FINISHED_PLACING_ACK_FLAG=0;
	private static final int FINISHED_PLACING_NACK_FLAG=1;
	
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
    
    
    private static void preLoginPhase() throws IOException {
    	int res= getUserLoginSignup();
    	// Make instance of TCP client
        tcpClient = new Client(add , port);
        PlayerState = new PlayerGameState();
    	
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
    			
        		HitShip();
        		
        		tcpClient.clientSocket.close();   
        	}
        	else {
        		Logout();
        	}
        }
        
    }
    
    private static void preGamePhase() throws NumberFormatException, IOException {
    	while(PlayerState.shipsAvaliable()) {
			PlaceShip();
    	}
    	
    	waitForACK(FINISHED_PLACING_ID, FINISHED_PLACING_ACK_FLAG);
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
    	
    	System.out.println("Trying to connect to server....");
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
	    	String[] splitted_input = getUserMsg("Enter Login information: <usnername> <password>", 2);
	    	
	    	byte[] username = splitted_input[0].getBytes();
	    	byte[] password = splitted_input[1].getBytes();
	    	
	    	int id = LOG_ID;
	    	int flag = LOGIN_FLAG;
	    	byte protocolByte = (byte) id;
	    	byte flagByte = (byte) flag;
	    	
	    	
	    	byte[] data = new byte[2+4+username.length+password.length];
	    	data[0] = protocolByte;
	    	data[1] = flagByte;
	    	int pos =2;
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
    
    
    private static void Logout() throws IOException {
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
    		PlayerState.displayBoards();
    		PlayerState.printAvaliableShips(); //prints avaliable ships
    		
    		String[] splitted_input = getUserMsg("Enter a ship to place: <Number> <X> <Y> <X> <Y>", 5);
        	try {
	        	ship_n =  Integer.parseInt(splitted_input[0]);
	        	x1 =  Integer.parseInt(splitted_input[1]);
	        	y1 =  Integer.parseInt(splitted_input[2]);
	        	x2 =  Integer.parseInt(splitted_input[3]);
	        	y2 =  Integer.parseInt(splitted_input[4]);
	        	



	        	valid = PlayerState.placeShipPlayer1Board(ship_n, x1, y1, x2, y2); // return if successfully placed. ship_n assumed to be 0 for destroyer, coords from 0-9 inclusive
				if(!valid){
					continue; // the ship placement failed.
				}
				
				//PlayerState.displayBoards();
				
	        	int id = PLACE_ID;
	        	int flag = PLACE_FLAG;
	        	byte protocolByte = (byte) id;
	        	byte flagByte = (byte) flag;
	        	
	        	
	        	byte[] data = new byte[2+5];
	        	data[0] = protocolByte;
	        	data[1] = flagByte;
	        	data[2] = (byte)ship_n;
	        	data[3] = (byte)x1;
	        	data[4] = (byte)y1;
	        	data[5] = (byte)x2;
	        	data[7] = (byte)y2;
	        	
	        	SendMessage(data);
	        
	        	
	        	
	        	
	        	//valid = waitForACK(REPLY_SHIP_ID, REPLY_SHIP_PLACE_ACK_FLAG);
	        	
        	}
        	catch(Exception e) {
        		continue;
        	}
        	
        	
        	
        	
    	}
    	
    	
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
		    	valid = PlayerState.isValidMove(userMove);
		    	if(!valid) {
		    		continue;
		    	}
		    	else {
		    		PlayerState.updatePlayer2Board(userMove);
		    	}
		    	
		    	
		    	
		    	int id = HIT_ID;
	        	int flag = HIT_FLAG;
	        	byte protocolByte = (byte) id;
	        	byte flagByte = (byte) flag;
	        	
	        	
	        	byte[] data = new byte[2+2];
	        	data[0] = protocolByte;
	        	data[1] = flagByte;
	        	data[2] = (byte)x;
	        	data[3] = (byte)y;
	        	
	        	SendMessage(data);
	        	
	        	server_ACK= true;
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
    	int[] result = new int[4];
    	
    	try {
    		byte[] inBytes = new byte[BUFFERSIZE];
    		StringBuilder strBuilder = new StringBuilder();
    		int bytesRead = 0;
    		
    		int protocolId = -1;
    		int flag = -1;
    		
    		if(inStream.available()>0) {
    			protocolId = inStream.read();
    	    	flag = inStream.read();
    	    	
    	    	System.out.println("Recieved PROTOCOL ID: " + protocolId);
    	    	System.out.println("Recieved PROTOCL FLAG: "+flag);
    		}
    		
    		result[0]=protocolId;
    		result[1]=flag;
	    	
	    	
	    	if(inStream.available()>0) {
		    	result[2]=inStream.read();
		    	result[3]=inStream.read();
	    	}
	    	else {
	    		result[2]=-1;
	    		result[3]=-1;
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
    	
    	int[] data = new int[4];
    	do {
    		try {
				data= receiveMsg();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	} while( data[0]!= ID);
    	return data[1] == FLAG;
    }
} 
