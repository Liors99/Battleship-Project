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
import java.nio.charset.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SelectServer {
	
	private static int BUFFERSIZE = 32;
	
    // Move these object declarations here so they can be accessed
	// by both main and static helper methods
	// Static since they belong to the class rather than an instance
	// of the class since we are essentially following a procedural
	// design
	// => => => 
	// Initialize buffers and coders for receive and send on channels
    private static String command = "";	
    private static Charset charset = Charset.forName( "us-ascii" );  
    private static CharsetDecoder decoder = charset.newDecoder();  
    private static CharsetEncoder encoder = charset.newEncoder();
    private static ByteBuffer inByteBuffer = null;
    private static CharBuffer inCharBuffer = null;
    private static ByteBuffer outByteBuffer = null;
    private static CharBuffer outCharBuffer = null;
    private static int bytesSent, bytesRecv;     // number of bytes sent or received
    private static DatagramPacket packet = null; //datagram packet for DatagramChannel
    
    // Initialize the selector and channels
    private static Selector selector = null;
    private static ServerSocketChannel tcpChannel;
    private static DatagramChannel udpChannel;
    private static SelectableChannel rdyChannel; // for when we iterate through the selector ready key set
    private static SelectionKey key;
    
    
    public static void main(String args[]) throws Exception 
    {
        if (args.length != 1)
        {
            System.out.println("Usage: UDPServer <Listening Port>");
            System.exit(1);
        }

        /*
         * // Initialize buffers and coders for receive and send on channels
        String command = "";	
        Charset charset = Charset.forName( "us-ascii" );  
        CharsetDecoder decoder = charset.newDecoder();  
        CharsetEncoder encoder = charset.newEncoder();
        ByteBuffer inByteBuffer = null;
        CharBuffer inCharBuffer = null;
        ByteBuffer outByteBuffer = null;
        CharBuffer outCharBuffer = null;
        int bytesSent, bytesRecv;     // number of bytes sent or received
        // Declare a datagram packet for DatagramChannel
        DatagramPacket packet = null;
        */
        
        // Initialize the selector
        Selector selector = Selector.open();
        
        /****************************************************************
         * FOR TCP: Create channel to listen for incoming TCP connections
         ****************************************************************/
        
        // Open server socket channel and make it non-blocking
        tcpChannel = ServerSocketChannel.open();	// open but not yet bound
        tcpChannel.configureBlocking(false);
        
        // Create socket address with specified port number and wildcard address 
        InetSocketAddress isa = new InetSocketAddress(Integer.parseInt(args[0]));
        // Bind the socket to the address
        tcpChannel.socket().bind(isa);
        
        /****************************************************************
         * FOR UDP: Create channel that can send and receive UDP packets
         ****************************************************************/

        // Open datagram socket channel and make it non-blocking
        udpChannel = DatagramChannel.open();
        udpChannel.configureBlocking(false);
        
        // Bind the datagram socket to the same address (port) as the server socket
        udpChannel.socket().bind(isa);

        // Register the channels with the selector 
        // Register that the TCP server is interested in connection requests
        // Register that the UDP server is interested in read requests 
        tcpChannel.register(selector, SelectionKey.OP_ACCEPT);
        udpChannel.register(selector, SelectionKey.OP_READ);

        // Wait for something happen among all registered sockets
        try {
            boolean terminated = false;
            while (!terminated) 
            {
                if (selector.select(500) < 0) // selects a set of keys whose channels are ready for I/O
                {
                    System.out.println("select() failed");
                    System.exit(1);
                }
                
                // Get set of ready sockets
                Set readyKeys = selector.selectedKeys();
                Iterator readyItor = readyKeys.iterator();

                // Walk through the ready set
                while (readyItor.hasNext()) 
                {
                    // Get key from set
                    key = (SelectionKey)readyItor.next();

                    // Remove current entry
                    readyItor.remove();
                    
                    // Get the channel of the ready key
                    rdyChannel = key.channel();
                    //System.out.println("What kind of channel am I?");
                    //System.out.println(channel.getClass().getName());

                    // Accept new connection requests to TCP socket, if any
                    // If this key's channel is ready to accept a new socket connection
                    if (key.isAcceptable()) 	
                    {
                    		acceptClientConnection();
                    		/*
                        // Accept a connection made to this channel's socket
                        SocketChannel clientChannel = ((ServerSocketChannel)channel).accept();
                        clientChannel.configureBlocking(false);
                        System.out.println("Accept conncection from " + clientChannel.socket().toString());
                        
                        // Register the new connection for read operation
                        clientChannel.register(selector, SelectionKey.OP_READ);
                        */
                    } 
                    // If the key's channel is ready for reading
                    else if (key.isReadable()) 
                    {
                    		// Open input and output streams
                        inByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                        inCharBuffer = CharBuffer.allocate(BUFFERSIZE);
                        outByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                        outCharBuffer = CharBuffer.allocate(BUFFERSIZE);	
                        
                    		// If there is a datagram waiting at the UDP socket
                    		if (rdyChannel instanceof DatagramChannel)
						{
                    			receiveDatagram();
                    			/*
                    			DatagramChannel udpChan = (DatagramChannel) rdyChannel;
                    			// Receive datagram available on this channel 
                    			// Returns the datagram's source address
                    			SocketAddress clientAdd = udpChan.receive(inByteBuffer);
                    			
                    			// How to do error checking on receive?
                    			
                    			if (clientAdd != null)
                    			{
                    				// make buffer ready for a new sequence of channel-write
                    				// or relative get operations
                    				inByteBuffer.flip();	// make buffer available    	
                    				decoder.decode(inByteBuffer, inCharBuffer, false);
                    				inCharBuffer.flip();		
                    				command = inCharBuffer.toString();
                    				System.out.println("UDP Client: " + command);
                    				
                    				// echo the message back
                    				inByteBuffer.flip();
                    				udpChan.send(inByteBuffer,clientAdd);
                    			}
                    			*/
						}
                    		
                    		// Else if a TCP client socket channel is ready for reading
						else 
						{
							if (!readFromClientChannel())
								continue;
							/*
							SocketChannel clientChannel = (SocketChannel) rdyChannel;
                  
                            // Read from client socket channel
                            bytesRecv = clientChannel.read(inByteBuffer);
                            if (bytesRecv <= 0)
                            {
                                System.out.println("read() error, or connection closed");
                                key.cancel();  // deregister the socket
                                continue;
                            }
                            
                            // make buffer ready for a new sequence of channel-write
                            // or relative get operations
                            inByteBuffer.flip();	// make buffer available    	
                            decoder.decode(inByteBuffer, inCharBuffer, false);
                            inCharBuffer.flip();		
                            command = inCharBuffer.toString();
                            System.out.println("TCP Client: " + command);
                            
                            // Echo the message back
                            inByteBuffer.flip();
                            
                            bytesSent = clientChannel.write(inByteBuffer); 
                            if (bytesSent != bytesRecv)
                            {
                                System.out.println("write() error, or connection closed");
                                key.cancel();  // deregister the socket
                                continue;
                            }
                            */
						}
                        
                        if (command.equals("terminate\n"))
                            terminated = true;
                        
                        /*
                        else if (command.equals("list\n"))
                        {
                                String dirName = System.getProperty("user.dir");
                                File[] files = new File(dirName).listFiles();
                                
                                for (File file : files)
                                {
                                		// Put filename in Char Buffer
                                		outCharBuffer.put(file.getName());
                                		// Send filename to Client
                                		//if ()
                                		//sendToUDPClient(outCharBuffer);
                                }
                        }
                        else 
                        {
                        		String[] words = command.split("\\s+");
                        		if (words[0].equals("get") && words.length == 2)
                        		{
                        			try
                        			{
                        				String fileName = words[1];
                        				BufferedReader reader = new BufferedReader(new FileReader(fileName));
                        				String fileLine;
                        				
                        				while ((fileLine = reader.readLine()) != null)
                        				{
                        					// Send file contents to client a line at a time
                        				}
                        				
                        				reader.close();
                        			}
                        			catch (Exception e)
                        			{
                        				String errMsg = "Error in opening file <filename>";
                        				// Send error message to client
                        			}
                        			
                        		}
                        }
                        */
                        
                    

                        
                    } // end of else if (key.isReadible())
                } // end of while (readyItor.hasNext()) 
            } // end of while (!terminated)
        }
        catch (IOException e) {
            System.out.println(e);
        }
 
        // close all connections
        Set keys = selector.keys();
        Iterator itr = keys.iterator();
        while (itr.hasNext()) 
        {
            SelectionKey key = (SelectionKey)itr.next();
            //itr.remove();
            if (key.isAcceptable())
                ((ServerSocketChannel)key.channel()).socket().close();
            else if (key.isValid())
                ((SocketChannel)key.channel()).socket().close();
        }
    }
    
    // HELPER METHODS
    public static void acceptClientConnection() throws Exception
    {
    		// Accept a connection made to this channel's socket
        SocketChannel clientChannel = ((ServerSocketChannel)rdyChannel).accept();
        clientChannel.configureBlocking(false);
        System.out.println("Accept conncection from " + clientChannel.socket().toString());
        
        // Register the new connection for read operation
        clientChannel.register(selector, SelectionKey.OP_READ);
    }
    
    public static void receiveDatagram() throws Exception
    {
    		DatagramChannel udpChan = (DatagramChannel) rdyChannel;
		// Receive datagram available on this channel 
		// Returns the datagram's source address
		SocketAddress clientAdd = udpChan.receive(inByteBuffer);
		
		// How to do error checking on receive?
		
		if (clientAdd != null)
		{
			// make buffer ready for a new sequence of channel-write
			// or relative get operations
			inByteBuffer.flip();	// make buffer available    	
			decoder.decode(inByteBuffer, inCharBuffer, false);
			inCharBuffer.flip();		
			command = inCharBuffer.toString();
			System.out.println("UDP Client: " + command);
			
			// echo the message back
			inByteBuffer.flip();
			udpChan.send(inByteBuffer,clientAdd);
		}
    }
    
    public static boolean readFromClientChannel() throws Exception
    {
    		SocketChannel clientChannel = (SocketChannel) rdyChannel;
        
        // Read from client socket channel
        bytesRecv = clientChannel.read(inByteBuffer);
        if (bytesRecv <= 0)
        {
            System.out.println("read() error, or connection closed");
            key.cancel();  // deregister the socket
            //continue;
            return false;	// operation was not successful
        }
        
        // make buffer ready for a new sequence of channel-write
        // or relative get operations
        inByteBuffer.flip();	// make buffer available    	
        decoder.decode(inByteBuffer, inCharBuffer, false);
        inCharBuffer.flip();		
        command = inCharBuffer.toString();
        System.out.println("TCP Client: " + command);
        
        // Echo the message back
        inByteBuffer.flip();
        
        bytesSent = clientChannel.write(inByteBuffer); 
        if (bytesSent != bytesRecv)
        {
            System.out.println("write() error, or connection closed");
            key.cancel();  // deregister the socket
            //continue;
            return false;
        }
        
        return true;
    }
   
    public static void sendToUDPClient(CharBuffer outCharBuffer)
    {
    		outCharBuffer.flip();	// flip buffer: limit is set to current position and position to zero
    		
    }
    
    public static void sendToTCPClient(CharBuffer outCharBuffer)
    {
    		outCharBuffer.flip();	// flip buffer: limit is set to current position and position to zero
    		
    }
    
    
}
