package serverSide;
/*
 * A simple TCP select server that accepts multiple connections and echo message back to the clients
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 * 
 * Notes:
 * A SocketChannel is NIO (non-blocking I/O) device whereas a Socket is
 * a blocking I/O device. A SocketChannel allows communication between
 * a server and multiple clients on a single thread. 
 * Note that it is not possible to create a channel for an arbitrary
 * pre-existing ServerSocket; a newly created channel is open, but not 
 * yet bound until a bind() method is invoked
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

public class GameServer {
	
	private static final int SERVER_ID = 0;
	private static final int WAITING_ROOM_ID = 1;
	private static final int GAME_ROOM_ID = 2;
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
	private static final int PROTOCOL_ID_SIZE = 1;
	private static final int FLAG_SIZE = 1;
	private static int BUFFERSIZE = 32;
    
    // Selector and channels
    private Selector selector;
    private ServerSocketChannel tcpChannel;
    private SelectableChannel keyChannel; // for when we iterate through the selector ready key set
    private SelectionKey key; 
    
    //Client-SocketChannel mapping
    HashMap<SocketChannel,Client> clients;
    HashMap<Client,SocketChannel> clientSockets;
    
    //Waiting room
    WaitingRoom waitingRoom; 
    
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
        clients = new HashMap<SocketChannel,Client>();
        clientSockets = new HashMap<Client,SocketChannel>();
        
        //Create waiting room
        waitingRoom = new WaitingRoom(this);
    }
    
    /**
     * Main method wherein we will create an instance of our SelectServer class
     * and listen for both incoming TCP connection requests via a ServerSocket
     * Channel and send/receive datagrams via a DatagramChannel.
     * This allows non-blocking I/0. 
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
    
    
    private boolean readFromClient() 
    {
    		SocketChannel tcpClientChannel = (SocketChannel) keyChannel;
    		ByteBuffer inByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
    		CharBuffer inCharBuffer = CharBuffer.allocate(BUFFERSIZE);
    		int bytesRead = 0;
    		int usernameLength = 0; //only used if login or signup request
    		boolean doneReading = false;
    		
        // Read from client socket channel
        try 
        {	
        		Message msg = new Message();
        		
        		//Set the client, if applicable
        		if (clients.containsKey(tcpClientChannel))
        			msg.setClient(clients.get(tcpClientChannel));
        				
        		bytesRead = tcpClientChannel.read(inByteBuffer);
        		if (bytesRead <= 0)
        		{
        			System.out.println("read() error, or connection closed");
        			key.cancel();  // deregister the socket
        			return false;  
        		}
        		else if (bytesRead < BUFFERSIZE)
        		{
        			//Is there more left to the message?
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
        		if (msg.getProtocolId() == SERVER_ID && (msg.getFlag() == SIGN_UP_FLAG || msg.getFlag() == LOGIN_FLAG))
        			handleClientLoginSignup(tcpClientChannel, msg, usernameLength);
        		else
        			handleMessage(tcpClientChannel, msg);
        }
        catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        return true;
    }
    
    private void handleClientLoginSignup(SocketChannel clientChannel, Message msg, int usernameLength) 
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
	    	clients.put(clientChannel, client);
	    	clientSockets.put(client, clientChannel);
	    	//Add the client to the waiting room
	    	waitingRoom.activateClient(client);	
	    	//Message to send back if successful
	    	Message reply = new Message();
	    	reply.setProtocolId(SERVER_ID);
	    	reply.setFlag(LOGIN_SIGNUP_SUCCESS);
	    	sendToClient(client, reply);
		
	}

	private void handleMessage(SocketChannel clientChannel, Message msg) 
    {
    		if (msg.getProtocolId() == SERVER_ID) 
    		{
    			if (msg.getFlag() == LOGOUT_FLAG)
    			{	
    				Client client = clients.get(clientChannel);
    				waitingRoom.deactiviateClient(client);
    				//Send message to client that has been closed?
    				clients.remove(clientChannel);
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
    		
	}

    /*
     * Should this method take client, or fetch from msg?
     */
	public boolean sendToClient(Client client, Message msg) 
    {
		//Identify socket channel
		SocketChannel tcpClientChannel = clientSockets.get(client);
		System.out.println("Sending message to :" + client.getUsername());
		
		//Decompose message
		byte protocolByte = (byte) msg.getProtocolId();
		byte flagByte = (byte) msg.getFlag();
		String data = msg.getData();
		byte[] dataBytes = data.getBytes();
		
		byte[] outBytes = new byte[2 + dataBytes.length];
		outBytes[0] = protocolByte;
		outBytes[1] = flagByte;
		//Copy data bytes over
		//System.out.println(outBytes[0]);
		//System.out.println(outBytes[1]);
		for (int i = 0; i < dataBytes.length; i++)
		{
			//outBytes[2 + i] = dataBytes[i];
			System.out.println(outBytes[2+i]);
		}
		
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
