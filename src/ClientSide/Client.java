package ClientSide;
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

class Client {
	
	private static int BUFFERSIZE = 8 * 1024;
	
	private int portNumber;
	private Socket clientSocket;
	private DataOutputStream outBuffer; 
	private static InputStream inStream;
	private static DataInputStream inBuffer;
	private static BufferedReader inFromUser;
	private static Client tcpClient;
	
	
	//Flags and IDs
	private static final int JOIN_ID = 1; 
	private static final int JOIN_FLAG = 0; 
	private static final int OBSERVE_ID = 1;
	private static final int OBSERVE_FLAG = 1;
	private static final int LOGIN_ID = 0;
	private static final int LOGIN_FLAG = 1; 
	private static final int PLACE_ID = 2;
	private static final int PLACE_FLAG = 1; 
	private static final int HIT_ID = 2; 
	private static final int HIT_FLAG = 2; 
	
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

    public static void main(String args[]) throws Exception 
    { 
        if (args.length != 2)
        {
            System.out.println("Usage: Client <Server IP> <Server Port>");
            System.exit(1);
        }

        // Make instance of TCP client
        tcpClient = new Client(args[0], Integer.parseInt(args[1]));
        
        
        int res= getUserLoginSignup();
        
        /*
        if(res == 0) {
        	// Do signup protocol stuff
        }
        
        */
        
        if(res == 1 || res == 0) {
        	Login();
        	res = getJoinObserve();
        	if(res == 0) {
        		// Observe stuff
        		observeRequest();
        		
        		//Wait for server reply
        	}
        	else if(res == 1) {
        		//Wait to connect to game
        		joinRequest();
        		System.out.println("Joined a game!");
        		
        		
        		//Pre-game phase (placing ships)
        		//while(receiveMsg() == "Pre") {
        		PlaceShip();
        		//}
        		
        		//Mid-game phase 
        		HitShip();
        	}
        }
        
        

        // Close the socket
        tcpClient.clientSocket.close();           
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
    }
    
    private static void Login() throws IOException {
    	String[] splitted_input = getUserMsg("Enter Login information: <usnername> <password>", 2);
    	
    	byte[] username = splitted_input[0].getBytes();
    	byte[] password = splitted_input[1].getBytes();
    	
    	int id = LOGIN_ID;
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
    	
    	
    	
    }
    private static void PlaceShip() throws NumberFormatException, IOException {
    	
    	boolean valid = false;
    	int ship_n;
    	int x;
    	int y;
    	while(!valid) {
    		String[] splitted_input = getUserMsg("Enter a ship to place: <Number> <X,Y> <X,Y>", 3);
        	try {
	        	ship_n =  Integer.parseInt(splitted_input[0]);
	        	x =  Integer.parseInt(splitted_input[1]);
	        	y =  Integer.parseInt(splitted_input[2]);
	        	
	        	Move userMove = new Move();
	        	if(userMove.setCol(x) && userMove.setRow(y)) {
		    		valid = true;
		    	}
	        	else {
	        		continue;
	        	}
	        	
	        	//Use vlad's code here to check position and send to server
	        	
	        	int id = PLACE_ID;
	        	int flag = PLACE_FLAG;
	        	byte protocolByte = (byte) id;
	        	byte flagByte = (byte) flag;
	        	
	        	
	        	byte[] data = new byte[2+3];
	        	data[0] = protocolByte;
	        	data[1] = flagByte;
	        	data[2] = (byte)ship_n;
	        	data[3] = (byte)x;
	        	data[4] = (byte)y;
	        	
	        	SendMessage(data);
	        	
	        	
	        	
        	}
        	catch(Exception e) {
        		continue;
        	}
        	
        	
        	
        	
    	}
    	
    	
    }
    
    
    private static void HitShip() throws IOException {
    	
    	boolean valid = false;
    	int x;
    	int y;
    	
    	while(!valid) {
	    	String[] splitted_input = getUserMsg("Enter a position to hit: <X> <Y>", 2);
	    	
	    	try {
		    	x =  Integer.parseInt(splitted_input[0]);
		    	y =  Integer.parseInt(splitted_input[1]);
	    	
	    	
		    	Move userMove= new Move();
		    	userMove.setCol(x);
		    	userMove.setRow(y);
		    	
		    	//Vlad's function here.
		    	
		    	if(userMove.setCol(x) && userMove.setRow(y)) {
		    		valid = true;
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
	    		continue;
	        	
    	   }
       }
       
        
    }
    
    private static int getJoinObserve() throws IOException {
    	while(true) {
     	   System.out.println("Enter one of the following number: ");
            
           System.out.println("0: Observe a game ");
           System.out.println("1: Join a game");
           System.out.println("2: Logout");
     	   int res= getUserResponse(0 , 1);
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
    private static void receiveMsg()
    {

		
    	
    	try {
    		byte[] inBytes = new byte[BUFFERSIZE];
    		StringBuilder strBuilder = new StringBuilder();
    		int bytesRead = 0;
    		
	    	int protocolId = Character.getNumericValue(inStream.read());
	    	int flag = inStream.read();
	    	
	    	boolean doneReading = false;
    		while(!doneReading)
    		{
    			bytesRead = inStream.read(inBytes);
    			strBuilder.append(new String(inBytes));
    			doneReading = (bytesRead < BUFFERSIZE);
    		}
    	
    	
		} 
    		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    private static int getUserResponse(int i , int j) throws IOException {
    	try {
	    	int res = Integer.parseInt(inFromUser.readLine());
	    	
	    	if(res < i || res > j) {
	    		return -1;
	    	}
	    	
	    	return res;
    	}
    	catch(Exception e) {
    		return -1;
    	}
    }
    
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
} 
