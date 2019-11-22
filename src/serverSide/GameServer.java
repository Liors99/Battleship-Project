package serverSide;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

/**
 * Game server for hosting online instances of Battleship, a classic
 * turn-based strategy game. 
 * Notes:
 * A SocketChannel is NIO (non-blocking I/O) device whereas a Socket is
 * a blocking I/O device. A SocketChannel allows communication between
 * a server and multiple clients on a single thread. 
 * @author Mariella
 *
 */
public class GameServer {
	
	private static final int SERVER_ID = 0;
	private static final int WAITING_ROOM_ID = 1;
	private static final int GAME_ROOM_ID = 2;
	private static final int MESSAGING_ID = 8;
	private static final int SIGN_UP_FLAG = 0;
	private static final int LOGIN_FLAG = 1;
	private static final int LOGOUT_FLAG = 2;
	private static final int LOGIN_SIGNUP_SUCCESS = 0;
	private static final int LOGIN_SIGNUP_FAILURE = 1;
	private static final int LOGOUT_SUCCESS = 2;
	private static final int LOGOUT_FAILURE = 3;
	
	//Encoding and decoding
	private Charset charset; 
	private CharsetDecoder decoder;
	
	// Messaging
	private static int BUFFERSIZE = 32;
    
    // Selector and channels
    private Selector selector;
    private ServerSocketChannel tcpChannel;
    private SelectableChannel keyChannel; // for when we iterate through the selector ready key set
    private SelectionKey key; 
    
    //Client-SocketChannel mapping
    HashMap<SocketChannel,Client> clientsByChannel; 
    HashMap<Client,SocketChannel> socketsByClient;
    
    //Storage of registered/signed-up clients
    HashMap<String,Client> signedUpClientsByUsername;
    
    //Waiting room
    WaitingRoom waitingRoom; 
    
    /**
     * Constructor for game server. Creates server socket channel to accept connections
     * to multiple clients and initiates structures to store client - socket channel mappings.
     * @param port : Port over which we will communicate
     * @throws IOException
     */
    public GameServer(int port) throws IOException
    {
    		//Encoding/decoding
    		charset = Charset.forName("us-ascii");  
	    decoder = charset.newDecoder();  
    	
    		//Create the selector to which we will register our TCP and UDP channels
        selector = Selector.open();
        
        //Open socket and make it non-blocking
        tcpChannel = ServerSocketChannel.open();	// open but not yet bound
        tcpChannel.configureBlocking(false);
        
        //Create socket address with input port number and wildcard address 
        InetSocketAddress isa = new InetSocketAddress(port);
        //Bind the socket to the address
        tcpChannel.socket().bind(isa);

        //Register the channel with the selector 
        tcpChannel.register(selector, SelectionKey.OP_ACCEPT); //server is interested in connection requests 
        
        //Initialize the socketchannel-client hashmap
        clientsByChannel = new HashMap<SocketChannel,Client>();
        socketsByClient = new HashMap<Client,SocketChannel>();
        
        //Create waiting room
        waitingRoom = new WaitingRoom(this);
    }
    
    /**
     * Main method wherein we will create an instance of our GameServer class
     * and listen for incoming TCP connection requests via a ServerSocket
     * Channel and send/receive messages.
     * Channels allow non-blocking I/0. 
     * @param args[0] : Port over which we will communicate
     * @throws Exception
     */
    public static void main(String args[]) throws Exception 
    {
    		// Ensure proper command line usage
        if (args.length != 1)
        {
            System.out.println("Usage: TDPServer <Listening Port>");
            System.exit(1);
        }
        
        GameServer selectServer = new GameServer(Integer.parseInt(args[0]));
        
        // Monitor selector-registered sockets for activity
        selectServer.monitorSockets();

        // Close all connections
        selectServer.close();
    }
    
    /**
     * Helper method to check if any channels registered to our selector are
     * ready for operations (i.e., connection establishment, I/O)
     */
    private void monitorSockets() 
    {
    		boolean terminated = false;
    		while(!terminated)
    		{
		    	try {
		    		if (selector.select(500) < 0) // selects a set of keys whose channels are ready for I/O
		    		{
		    			System.out.println("select() failed");
		    			System.exit(1);
		    		}
		    	} catch (IOException e) {
		    		// TODO Auto-generated catch block
		    		e.printStackTrace();
		    	}
		
		    	// Get set of ready keys
		    	// i.e., the keys whose channels have been detected to 
		    	// be ready for at least one operation identified in the
		    	// key's interests upon registration to the selector
		    	Set<SelectionKey> readyKeys = selector.selectedKeys();
		    	Iterator<SelectionKey> readyItor = readyKeys.iterator();
		
		    	// Walk through the ready set
		    	while (readyItor.hasNext()) 
		    	{
		    		// Get the next key
		    		key = (SelectionKey)readyItor.next();
		
		    		// Remove current entry
		    		readyItor.remove();
		
		    		// Get the channel of the key
		    		keyChannel = key.channel();
		
		    		// If this key's channel is ready to accept a new socket connection,
		    		// accept.
		    		if (key.isAcceptable()) 	
		    			acceptClient();
		
		    		// Else if the key's channel is ready for reading
		    		else if (key.isReadable()) 
		    		{          		
		    			// If a TCP client socket channel is ready for reading
		    			if (keyChannel instanceof SocketChannel)
		    			{
		    				readFromClient();
		    			}
		    		} 
		    	} 
    		}
	}
    
    /**
     * Clean-up method wherein all connections are closed 
     */
    private void close() 
    {
    		// close all connections
        Set<SelectionKey> keys = selector.keys();
        Iterator<SelectionKey> itr = keys.iterator();
        while (itr.hasNext()) 
        {
            SelectionKey key = (SelectionKey)itr.next();
            //itr.remove();
            
            // Close server sockets
            if (key.channel() instanceof ServerSocketChannel)
            {
            	try {
					((ServerSocketChannel)key.channel()).socket().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
			// Close client sockets	
			else if (key.channel() instanceof SocketChannel)
			{
				try {
					((SocketChannel)key.channel()).socket().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}		
        }
    }
	
    /**
     * Method to accept a client, i.e., initialize an associated socket channel
     * and register it to the selector for the server socket
     */
    private void acceptClient() 
    {
    		SocketChannel tcpClientChannel;
    		// Accept a connection made to this channel's socket
        try 
        {
			tcpClientChannel = ((ServerSocketChannel)keyChannel).accept();
			tcpClientChannel.configureBlocking(false);
			 System.out.println("Accepted a connection from " + 
						tcpClientChannel.socket().getInetAddress().toString() +
							":" + tcpClientChannel.socket().getPort());
			 
			 //Register the client to the selector
			 tcpClientChannel.register(selector, SelectionKey.OP_READ);
		} 
        catch (IOException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
    }
    
    /**
     * Method to read incoming packet from client and convert to a message
     * that will be appropriately handled and/or forwarded to the WaitingRoom
     * or GameRoom instance with which the client is associated
     * @return : True is read was success; False is there is a read error
     */
    private boolean readFromClient() 
    {
    		SocketChannel tcpClientChannel = (SocketChannel) keyChannel;
    		ByteBuffer inByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
    		CharBuffer inCharBuffer = CharBuffer.allocate(BUFFERSIZE);
    		int bytesRead = 0;
    		int usernameLength = 0; //only used if login or signup request
    		int chatMsgLength = 0; //only used if chat messaging       
    		boolean doneReading = false;
    		
        // Read from client socket channel
        try 
        {	
        		Message msg = new Message();
        		
        		//Set the client, if applicable
        		if (clientsByChannel.containsKey(tcpClientChannel))
        			msg.setClient(clientsByChannel.get(tcpClientChannel));
        				
        		bytesRead = tcpClientChannel.read(inByteBuffer);
        		if (bytesRead <= 0)
        		{
        			System.out.println("read() error, or connection closed");
        			key.cancel();  // deregister the socket
        			return false;  
        		}
        		else if (bytesRead < BUFFERSIZE)
        		{
        			doneReading = true;
        		}
        		
        		//Flip buffer for reading
        		inByteBuffer.flip();
        		
        		//Get the protocol Id
        		int protocolId = (inByteBuffer.get() & 0xFF);
        		System.out.print("Protocol ID: " + protocolId);
        		msg.setProtocolId(protocolId);
        		
        		//Get the flag
        		int flag = (inByteBuffer.get() & 0xFF);
        		System.out.println("; Flag: " + flag);
        		msg.setFlag(flag);
        		
        		if (msg.getProtocolId() == SERVER_ID && 
                        (msg.getFlag() == SIGN_UP_FLAG || msg.getFlag() == LOGIN_FLAG))
    				usernameLength = inByteBuffer.getInt();
        		else if (msg.getProtocolId() == MESSAGING_ID)
        			chatMsgLength = inByteBuffer.getInt();
        			
        		
        		//Get the data
        		decoder.decode(inByteBuffer, inCharBuffer, false); //advances pointer
        		inCharBuffer.flip();
        		//StringBuilder for data section
        		StringBuilder strBuilder = new StringBuilder();
        		strBuilder.append(inCharBuffer.toString());
        		
        		//If not done reading message
        		while(!doneReading)
        		{
        			inByteBuffer.clear();
        			inCharBuffer.clear();
        			bytesRead = tcpClientChannel.read(inByteBuffer);
        			decoder.decode(inByteBuffer, inCharBuffer, false);
        			inCharBuffer.flip();
        			strBuilder.append(inCharBuffer.toString());
        			doneReading = (bytesRead < BUFFERSIZE);
        		}
        		
        		//Get the data section (payload)
        		msg.setData(strBuilder.toString());
        		System.out.println("Data: " + msg.getData());
        		
        		//Handle message
        		if (msg.getProtocolId() == SERVER_ID)
        		{
        			if (msg.getFlag() == SIGN_UP_FLAG)
        				handleClientSignup(tcpClientChannel, msg, usernameLength);
        			else if(msg.getFlag() == LOGIN_FLAG)
        				handleClientLogin(tcpClientChannel, msg, usernameLength);
        		}
        		else if (msg.getProtocolId() == MESSAGING_ID)
        			handleChatMessage(tcpClientChannel, msg, chatMsgLength);
        		else
        			handleMessage(tcpClientChannel, msg);
        }
        catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Method for handling in-game chat messaging.
     * @param clientChannel : the channel to which the client in question is associated
     * @param msg : the message associated with the login/signup request
     * @param chatMsgLength : the length of the chat message payload
     */
    private void handleChatMessage(SocketChannel tcpClientChannel, Message msg, int chatMsgLength) 
    {
    		byte[] bytes = msg.getData().getBytes();
    		System.out.println("Chat message of length " + chatMsgLength + " from " + clientsByChannel.get(tcpClientChannel).getUsername() + ":");
    		System.out.println(new String(bytes)); //print the chat message
    		handleMessage(tcpClientChannel, msg);
	}

	/**
     * Method for handling client login to game server
     * @param clientChannel : the channel to which the client in question is associated
     * @param msg : the message associated with the login request
     * @param usernameLength : the length of the username provided by the client
     */
    private void handleClientLogin(SocketChannel clientChannel, Message msg, int usernameLength) 
    {
	    	//Get username and password
	    	byte[] bytes = msg.getData().getBytes();
	    	byte[] usernameBytes = Arrays.copyOfRange(bytes, 0, usernameLength);    	
	    	byte[] passwordBytes = Arrays.copyOfRange(bytes, usernameLength, bytes.length);
	    	String providedUsername = new String(usernameBytes);
	    	String providedPassword = new String(passwordBytes);
	    	System.out.println("Provided username: " + providedUsername);
	    	System.out.println("Provided password: " + providedPassword);
	    	
	    	//Initialize message to send back to client
	    	Message reply = new Message();
    		reply.setProtocolId(SERVER_ID);
	    	
	    //Get the register user, if it exists
	    	Client user = signedUpClientsByUsername.get(providedUsername);
	    	
	    	//Check that user exists
	    	if (user != null)
	    	{
	    		//Check that password matches username
	    		if (user.passwordMatches(providedPassword))
	    		{
	    			//Check that client isn't already logged in
	    			if (!waitingRoom.isActiveClient(user))
	    			{
	    				//Add the client to the waiting room
		    			waitingRoom.activateClient(user);	
		    		 	//Set flag of message to success
		    	    		reply.setFlag(LOGIN_SIGNUP_SUCCESS);	
	    			}		
	    		}
	    	}
	    	else
	    	{
	    		//Set flag of message to failure
	    		reply.setFlag(LOGIN_SIGNUP_FAILURE);
	    	}
	    	
	    	//Send message to client
	    	sendToClient(user, reply);	
	}
    
    /**
     * Method for handling client signup to game server
     * @param clientChannel : the channel to which the client in question is associated
     * @param msg : the message associated with the signup request
     * @param usernameLength : the length of the username provided by the client
     */
    private void handleClientSignup(SocketChannel clientChannel, Message msg, int usernameLength) 
    {
	    	//Get username and password
	    	byte[] bytes = msg.getData().getBytes();
	    	byte[] usernameBytes = Arrays.copyOfRange(bytes, 0, usernameLength);    	
	    	byte[] passwordBytes = Arrays.copyOfRange(bytes, usernameLength, bytes.length);
	    	String username = new String(usernameBytes);
	    	String password = new String(passwordBytes);
	    	System.out.println("Username: " + username);
	    	System.out.println("Password: " + password);
	    	
	    	//Create new client and add to hashmap
	    	Client client = new Client(username, password);
	    	clientsByChannel.put(clientChannel, client);
	    	socketsByClient.put(client, clientChannel);
	    	
	    	//Add the client to the waiting room
	    	waitingRoom.activateClient(client);	
	    	//Message to send back if successful
	    	
	    	Message reply = new Message();
	    	reply.setProtocolId(SERVER_ID);
	    	reply.setFlag(LOGIN_SIGNUP_SUCCESS);
	    	sendToClient(client, reply);	
	}

    /**
     * Main method wherein the parsed message, received on the client channel, is
     * handled either directly or via forwarding to the WaitingRoom or appropriate 
     * GameRoom instance
     * @param clientChannel : the channel over which the message arrives
     * @param msg : the message that requires handling
     */
	private void handleMessage(SocketChannel clientChannel, Message msg) 
    {
    		if (msg.getProtocolId() == SERVER_ID) 
    		{
    			if (msg.getFlag() == LOGOUT_FLAG)
    			{	
    				Client client = clientsByChannel.get(clientChannel);
    				waitingRoom.deactiviateClient(client);
    				//Send message to client that has been closed?
    				clientsByChannel.remove(clientChannel);
    				//Message to send back if logout successful
    				Message reply = new Message();
    				reply.setProtocolId(SERVER_ID);
    				reply.setFlag(LOGOUT_SUCCESS);
    				sendToClient(client, reply);
    				
    				try {
    					clientChannel.socket().close();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			}	
    		}
    		else if (msg.getProtocolId() == WAITING_ROOM_ID)
    			waitingRoom.handleMessage(msg);
    		else if (msg.getProtocolId() == GAME_ROOM_ID)
    			waitingRoom.forwardMessageToGameRoom(msg);
    		else if (msg.getProtocolId() == MESSAGING_ID)
    			waitingRoom.forwardMessageToGameRoom(msg);
    		
    		
	}

    
	/**
	 * Main method to send a message to a client via their associated
	 * socket channel.
	 * @param client : the client to which we wish to send a message
	 * @param msg : the message that we wish to send to client
	 * @return : True if the message was successfully sent; False otherwise
	 */
	public boolean sendToClient(Client client, Message msg) 
    {
		//Identify socket channel
		SocketChannel tcpClientChannel = socketsByClient.get(client);
		System.out.println("Sending message to: " + client.getUsername());
		
		//Decompose message
		byte protocolByte = (byte) msg.getProtocolId();
		byte flagByte = (byte) msg.getFlag();
		String data = msg.getData();
		byte[] dataBytes = data.getBytes();
		byte[] outBytes = null;
		int dataStart = 2; //the index at which the data starts
		
		if (msg.getProtocolId() == MESSAGING_ID)
		{
			//We must include the message length, if a chat message
			outBytes = new byte[2 + 4 + dataBytes.length];
			byte[] msgLength = ByteBuffer.allocate(4).putInt(dataBytes.length).array();
			System.arraycopy(msgLength, 0, outBytes, dataStart, msgLength.length);
			dataStart += 4;	// increment by 4 since length takes up 4 bytes
		}
		else
		{
			outBytes = new byte[2 + dataBytes.length];		
		}
		
		outBytes[0] = protocolByte;
		outBytes[1] = flagByte;
		System.arraycopy(dataBytes, 0, outBytes,  dataStart, dataBytes.length);
		System.out.println("Sent PROTOCOL ID: " + outBytes[0]);
		System.out.println("Sent PROTOCOL FLAG: " + outBytes[1]);
		if (dataBytes.length > 0)
			System.out.println("Sent data section: " + new String(outBytes, dataStart, dataBytes.length));
		
    		// Write message to client socket 
    		int bytesSent = 0;
        int msgSize = outBytes.length;
        	
        ByteBuffer outByteBuffer = ByteBuffer.wrap(outBytes);
 	       
    		try {
	    		bytesSent = tcpClientChannel.write(outByteBuffer);
	    	} catch (IOException e) {
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	} 	
    		
    		// error checking on bytes
    		if (bytesSent != msgSize)
    		{
    			System.out.println("write() error, or connection closed");
    			key.cancel();
            return false;    
        }
    		
    		System.out.println("Length of bytes sent: " + msgSize);
        return true;	
    }
	
    
    
}
