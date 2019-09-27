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
    		
        boolean terminated = false;
        while (!terminated) 
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
                		acceptClientConnection();
                
                // If the key's channel is ready for reading,
                // check whether the channel is a DatagramChannel (UDP)
                // or a ServerSocketChannel (TCP)
                else if (key.isReadable()) 
                {
                		// First allocate inbound buffers, used by both UDP and TCP
                    inByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                    inCharBuffer = CharBuffer.allocate(BUFFERSIZE);
                  
                		// If there is a datagram waiting at the UDP socket, receive it.
                		if (keyChannel == udpChannel)
                		{
                			command = receiveDatagram();
                			
                			if (command.equals("terminate"))
                			{
                				terminated = true;
                			}
                		}
                		
                		// Else if a TCP client socket channel is ready for reading
					else if (keyChannel instanceof SocketChannel)
					{
						// If read is not successful (?), continue
						if (!readFromClientChannel())
							continue;		
						
						if (command.equals("terminate\n"))
						{
							System.out.println("TCP Client: terminate");
							terminated = true;
						}
	                    else if (command.equals("list\n"))
	                    {
	                    		System.out.println("TCP Client: list");
	                    		boolean success = sendFileList();
	                    		if (!success)
	                    			continue;
	                    } 
	                    else if (command.trim().startsWith("get"))
	                    {
	                    		// Remove any excess while spaces in the command; print instead of println
	                    		System.out.println("TCP Client: get " + command.substring(3).trim());
	                    		boolean success = sendFileContents();
	                    		if (!success)
	                    		{
	                    			System.out.println("open() failed");
	                    			continue;
	                    		}
	                    }
	                    else
	                    {
	                    		System.out.print("TCP Client : " + command);
	                    		String msg = "Unknown command: " + command;
	                    		sendToTCPClient(msg);
	                    }
					}
          
                } // end of else if (key.isReadible())
            } // end of while (readyItor.hasNext()) 
        } // end of while (!terminated)	
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
	
    private void acceptClientConnection() 
    {
    		// Accept a connection made to this channel's socket
        try {
			udpClientChannel = ((ServerSocketChannel)keyChannel).accept();
			udpClientChannel.configureBlocking(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        System.out.println("Accept connection from " + udpClientChannel.socket().toString());
        
        // Register the new connection for read operation
        try {
			udpClientChannel.register(selector, SelectionKey.OP_READ);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private String receiveDatagram() 
    {
    		String msg = ""; 
		// Receive datagram available on this channel 
		// Returns the datagram's source address
		try {
			udpClientAdd = udpChannel.receive(inByteBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// How to do error checking on receive?
	
		if (udpClientAdd != null)
		{
			// make buffer ready for a new sequence of channel-write
			// or relative get operations
			inByteBuffer.flip();	// make buffer available    	
			decoder.decode(inByteBuffer, inCharBuffer, false);
			inCharBuffer.flip();	 // make buffer ready for get()	
			msg = inCharBuffer.toString();
			System.out.print("UDP Client: " + msg);
			
			// echo the message back
			inByteBuffer.flip(); // make buffer ready for write()/get()
			try {
				udpChannel.send(inByteBuffer,udpClientAdd);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return msg;
    }
    
    private boolean readFromClientChannel() 
    {
    		udpClientChannel = (SocketChannel) keyChannel;
        
        // Read from client socket channel
        try {
			bytesRecv = udpClientChannel.read(inByteBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (bytesRecv <= 0)
        {
            System.out.println("read() error, or connection closed");
            key.cancel();  // deregister the socket
            //continue;
            return false;	// operation was not successful
        }
        
        // make buffer ready for a new sequence of channel-write
        // or relative get operations
        inByteBuffer.flip();	// is this necessary here?  	
        decoder.decode(inByteBuffer, inCharBuffer, false);
        inCharBuffer.flip();		
        command = inCharBuffer.toString();
       
        /*
        // Echo the message back
        inByteBuffer.flip(); //make buffer ready for write()
        
        try {
			bytesSent = udpClientChannel.write(inByteBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        if (bytesSent != bytesRecv)
        {
            System.out.println("write() error, or connection closed");
            key.cancel();  // deregister the socket
            //continue;
            return false;
        }
        */
        return true;
    }
    
    private boolean sendFileList()
    {
    		String dirName = System.getProperty("user.dir");
        File[] files = new File(dirName).listFiles();
        String msg = "";
        
        for (File file : files)
        {
        		if (!file.isFile())	// Only list "normal" files, i.e., not directories
        			continue;
        		
        		// For debugging
        		//System.out.println(file.getName());
        			
        		msg += file.getName() + "\n";
        }
        
        return sendToTCPClient(msg);   
    }
    
    private boolean sendFileContents()
    {
    		String fileName = command.substring(3).trim();
	 	String fileMsg = "";
	 	//System.out.println("Getting file " + fileName);
	 	
		try
		{
			// Read files contents line by line and append to outgoing message
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String fileLine;
			
			while ((fileLine = reader.readLine()) != null)
			{
				// Append file line to file message
				fileMsg += fileLine + "\n";
			}
			
			reader.close();
		}
		catch (FileNotFoundException e)
		{
			sendToTCPClient("Error in opening file " + fileName + "\n");
			return false; // did not succeed 
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// for debugging
		//System.out.println("Files contents:");
		//System.out.println(fileMsg);
		
		// Send file contents to client and return success boolean
		return sendToTCPClient(fileMsg);
    }
   
    
    private boolean sendToTCPClient(String msg) 
    {
    		// Write message to outward CharBuffer
        int msgSize = msg.getBytes().length;
        ByteBuffer outByteBuffer = ByteBuffer.allocateDirect(msgSize);
        CharBuffer outCharBuffer = CharBuffer.allocate(msgSize);	
        /*	
        System.out.println("Our message is " + msgSize + " bytes long");
        	System.out.println("Our buffer capacity is " + outCharBuffer.capacity() + "bytes");
    		*/
    		outCharBuffer.clear();	// set position to zero and set limit to capacity
    		outCharBuffer.put(msg);
    		outCharBuffer.flip();	
    		// Encode to ByteBuffer for transfer
    		encoder.encode(outCharBuffer, outByteBuffer, false);
    		outByteBuffer.flip(); 
    		
    		//System.out.println("bytes sent : " + bytesSent);
    		try {
	    		bytesSent = udpClientChannel.write(outByteBuffer);
	    	} catch (IOException e) {
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	} 	
    		
    		// error checking on bytes
    		if (bytesSent != msg.length())
    		{
    			System.out.println("write() error, or connection closed");
    			key.cancel();
            return false;    
        }
        
        return true;	
    }
    
    
}
