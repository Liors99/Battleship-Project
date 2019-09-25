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
	
	// Buffers and coders
    private String command;	
    private Charset charset;  
    private CharsetDecoder decoder;  
    private CharsetEncoder encoder;
    private ByteBuffer inByteBuffer;
    private CharBuffer inCharBuffer;
    private ByteBuffer outByteBuffer;
    private CharBuffer outCharBuffer;
    private int bytesSent, bytesRecv;     // number of bytes sent or received
    
    // Selector and channels
    private Selector selector;
    private ServerSocketChannel tcpChannel;
    private DatagramChannel udpChannel;
    private SelectableChannel keyChannel; // for when we iterate through the selector ready key set
    private SelectionKey key;
    private DatagramPacket packet;		 //datagram packet for DatagramChannel
    private SocketChannel udpClientChannel;
    private SocketAddress udpClientAdd;
    
    public SelectServer(int port) throws IOException
    {
    		// Initialize buffers and coders 
    	 	command = "";	
    	    charset = Charset.forName( "us-ascii" );  
    	    decoder = charset.newDecoder();  
    	    encoder = charset.newEncoder();
    	
    		// Create the selector to which we will register our TCP and UDP channels
        selector = Selector.open();
        
        // Open sockets and make it non-blocking
        tcpChannel = ServerSocketChannel.open();	// open but not yet bound
        udpChannel = DatagramChannel.open();
        tcpChannel.configureBlocking(false);
        udpChannel.configureBlocking(false);
        
        // Create socket address with input port number and wildcard address 
        InetSocketAddress isa = new InetSocketAddress(port);
        // Bind the socket to the address
        tcpChannel.socket().bind(isa);
        udpChannel.socket().bind(isa);

        // Register the channels with the selector 
        tcpChannel.register(selector, SelectionKey.OP_ACCEPT); // the TCP server is interested in connection requests
        udpChannel.register(selector, SelectionKey.OP_READ);   // the UDP server is interested in read requests 
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
            System.out.println("Usage: UDPServer <Listening Port>");
            System.exit(1);
        }
        
        SelectServer selectServer = new SelectServer(Integer.parseInt(args[0]));
        
        // Monitor selector-registered sockets for activity
        selectServer.monitorSockets();
        
        // Close all connections
        selectServer.close();
    }
    
    private void monitorSockets() 
    {
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
                    		acceptClientConnection();
                    
                    // If the key's channel is ready for reading,
                    // check whether the channel is a DatagramChannel (UDP)
                    // or a ServerSocketChannel (TCP)
                    else if (key.isReadable()) 
                    {
                    		// First open input and output streams
                        inByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                        inCharBuffer = CharBuffer.allocate(BUFFERSIZE);
                        outByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                        outCharBuffer = CharBuffer.allocate(BUFFERSIZE);	
                        
                    		// If there is a datagram waiting at the UDP socket, receive it.
                    		if (keyChannel == udpChannel)
                    			receiveDatagram();
                    		
                    		// Else if a TCP client socket channel is ready for reading
						else if (keyChannel instanceof SocketChannel)
						{
							// If read is not successful (?), continue
							if (!readFromClientChannel())
								continue;						
						}
                        
                        if (command.equals("terminate\n"))
                            terminated = true;
                        
                        
                        else if (command.equals("list\n"))
                        {
                                String dirName = System.getProperty("user.dir");
                                File[] files = new File(dirName).listFiles();
                                
                                for (File file : files)
                                {
                                		// Put filename in CharBuffer
                                		outCharBuffer.put(file.getName());
                                		outCharBuffer.flip();	// flip buffer: limit is set to current position and position to zero
                                		// Encode to ByteBuffer for transfer
                                		encoder.encode(outCharBuffer, outByteBuffer, false);
                                		
                                		if (keyChannel == udpChannel)
                                			sendToUDPClient(outByteBuffer);
                                		
                                		else if (keyChannel instanceof SocketChannel)
                                		{
                                			if(!sendToTCPClient(outByteBuffer))
                                				continue;
                                		}
                                }
                        }
                        /*
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
		
	}
    
    private void close() throws IOException
    {
    		// close all connections
        Set<SelectionKey> keys = selector.keys();
        Iterator<SelectionKey> itr = keys.iterator();
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
	
    private void acceptClientConnection() throws IOException
    {
    		// Accept a connection made to this channel's socket
        udpClientChannel = ((ServerSocketChannel)keyChannel).accept();
        udpClientChannel.configureBlocking(false);
        System.out.println("Accept connection from " + udpClientChannel.socket().toString());
        
        // Register the new connection for read operation
        udpClientChannel.register(selector, SelectionKey.OP_READ);
    }
    
    private void receiveDatagram() throws IOException
    {
		// Receive datagram available on this channel 
		// Returns the datagram's source address
		udpClientAdd = udpChannel.receive(inByteBuffer);
		
		// How to do error checking on receive?
	
		if (udpClientAdd != null)
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
			udpChannel.send(inByteBuffer,udpClientAdd);
		}
    }
    
    private boolean readFromClientChannel() throws IOException
    {
    		SocketChannel clientChannel = (SocketChannel) keyChannel;
        
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
        System.out.println("TCP Client: " + inCharBuffer.toString());
        
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
   
    private void sendToUDPClient(ByteBuffer outByteBuffer) throws IOException
    {
    		udpChannel.send(outByteBuffer, udpClientAdd);	
    }
    
    private boolean sendToTCPClient(ByteBuffer outByteBuffer) throws IOException
    {
    		outCharBuffer.flip();	// flip buffer: limit is set to current position and position to zero
    		
    		bytesSent = udpClientChannel.write(outByteBuffer); 
    		if (bytesSent != bytesRecv)
    		{
    			System.out.println("write() error, or connection closed");
    			key.cancel();  // deregister the socket
    			//continue;
    			return false;
    		}
    	        
    	        return true;  		
    }
    
    
}
