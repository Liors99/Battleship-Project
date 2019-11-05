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
	private InputStream inStream;
	private static BufferedReader inBuffer;
	private static BufferedReader inFromUser;
	private static Client tcpClient;
	
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
        inBuffer = new BufferedReader(
        					new	InputStreamReader(inStream), BUFFERSIZE); 
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
        	}
        	else if(res == 1) {
        		//Wait to connect to game
        		joinRequest();
        		System.out.println("Joined a game!");
        		
        		
        		//Pre-game phase (placing ships)
        		while(receiveMsg() == "Pre") {
        			PlaceShip();
        		}
        		
        		//Mid-game phase 
        		HitShip();
        	}
        }
        
        

        // Close the socket
        tcpClient.clientSocket.close();           
    }
    
    
    private static void joinRequest() throws IOException {
    	int id = 1;
    	int flag = 0;
    	byte protocolByte = (byte) id;
    	byte flagByte = (byte) flag;
    	
    	byte[] data = new byte[2];
    	data[0] = protocolByte;
    	data[1] = flagByte;
    	
    	SendMessage(data);
    }
    
    private static void observeRequest() throws IOException {
    	int id = 1;
    	int flag = 1;
    	byte protocolByte = (byte) id;
    	byte flagByte = (byte) flag;
    	
    	byte[] data = new byte[2];
    	data[0] = protocolByte;
    	data[1] = flagByte;
    	
    	SendMessage(data);
    }
    
    private static void Login() throws IOException {
    	System.out.println("Enter Login information: <usnername> <password>");
    	String[] splitted_input = inFromUser.readLine().split("\\s+");
    	
    	byte[] username = splitted_input[0].getBytes();
    	byte[] password = splitted_input[1].getBytes();
    	
    	int id = 0;
    	int flag = 1;
    	byte protocolByte = (byte) id;
    	byte flagByte = (byte) flag;
    	
    	
    	
    	byte[] data = new byte[2+username.length+password.length];
    	data[0] = protocolByte;
    	data[1] = flagByte;
    	System.arraycopy(username, 2, data,  0, username.length);
    	int pos = 2 + username.length;
    	System.arraycopy(password, pos+1, data,  0, username.length);
    	
    	SendMessage(data);
    	
    	
    	
    }
    private static void PlaceShip() throws NumberFormatException, IOException {
    	
    	boolean valid = false;
    	
    	while(!valid) {
    		System.out.println("Enter a ship to place: <Number> <X,Y> <X,Y>");
        	String[] splitted_input = inFromUser.readLine().split("\\s+");
        	
        	
        	int ship_n =  Integer.parseInt(splitted_input[0]);
        	int x =  Integer.parseInt(splitted_input[1]);
        	int y =  Integer.parseInt(splitted_input[2]);
        	
        	
        	Move userMove = new Move();
        	if(userMove.setCol(x) && userMove.setRow(y)) {
	    		valid = true;
	    	}
        	else {
        		continue;
        	}
        	
        	//Use vlad's code here to check position and send to server
        	
        	int id = 2;
        	int flag = 1;
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
    	
    	
    }
    
    
    private static void HitShip() throws IOException {
    	
    	boolean valid = false;
    	
    	while(!valid) {
	    	System.out.println("Enter a position to hit: <X> <Y>");
	    	String[] splitted_input = inFromUser.readLine().split("\\s+");
	    	int x =  Integer.parseInt(splitted_input[0]);
	    	int y =  Integer.parseInt(splitted_input[1]);
	    	
	    	Move userMove= new Move();
	    	userMove.setCol(x);
	    	userMove.setRow(y);
	    	
	    	//Vlad's function here.
	    	
	    	if(userMove.setCol(x) && userMove.setRow(y)) {
	    		valid = true;
	    	}
	    	
	    	
	    	int id = 2;
        	int flag = 2;
        	byte protocolByte = (byte) id;
        	byte flagByte = (byte) flag;
        	
        	
        	byte[] data = new byte[2+2];
        	data[0] = protocolByte;
        	data[1] = flagByte;
        	data[2] = (byte)x;
        	data[3] = (byte)y;
        	
        	SendMessage(data);
    	}
    	
    	
    }

    
    private static int getUserLoginSignup() throws IOException {
       while(true) {
    	   System.out.println("Enter one of the following number: ");
           System.out.print("0: Signup ");
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
            
           System.out.print("0: Join a game ");
           System.out.println("1: Observe a game");
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
    
    private static String receiveMsg()
    {
    	
    	int bytesRead = 0;
		char[] charBuffer = null;
		String msg = "";
		
		try 
		{
			charBuffer = new char[BUFFERSIZE]; 
			boolean done = false;
			while (!done)
			{
				bytesRead = inBuffer.read(charBuffer, 0, charBuffer.length);
				//System.out.println("Bytes read: " + bytesRead);
				msg += (new String(Arrays.copyOfRange(charBuffer, 0, bytesRead)));
				done = (bytesRead < BUFFERSIZE);
			}
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// How do we check that we received the entire message?
		// Use first 2 bytes of message to contain message size
		
		return msg;
    }
    
    
    private static int getUserResponse(int i , int j) throws IOException {
    	int res = Integer.parseInt(inFromUser.readLine());
    	
    	if(res < i || res > j) {
    		return -1;
    	}
    	
    	return res;
    	
    }
} 
