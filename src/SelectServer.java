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
                		// First open input and output streams
                    inByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                    inCharBuffer = CharBuffer.allocate(BUFFERSIZE);
                    outByteBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                    outCharBuffer = CharBuffer.allocate(BUFFERSIZE);	
                    
                		// If there is a datagram waiting at the UDP socket, receive it.
                		if (keyChannel == udpChannel)
                		{
                			command = receiveDatagram();
                			
                			if (command.equals("terminate"))
                				terminated = true;
                		}
                		
                		// Else if a TCP client socket channel is ready for reading
					else if (keyChannel instanceof SocketChannel)
					{
						// If read is not successful (?), continue
						if (!readFromClientChannel())
							continue;		
						
						if (command.equals("terminate\n"))
	                        terminated = true;
	                    
	                    else if (command.equals("list\n"))
	                    {
	                    		boolean success = sendFileList();
	                    		if (!success)
	                    			continue;
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
            
            if (key.isAcceptable())
				try {
					((ServerSocketChannel)key.channel()).socket().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else if (key.isValid())
				try {
					((SocketChannel)key.channel()).socket().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			System.out.println("UDP Client: " + msg);
			
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
        System.out.println("TCP Client: " + command);
        
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
        		System.out.println(file.getName());
        			
        		msg += file.getName() + "\n";
        }
       
        // Write message to outward CharBuffer
    		outCharBuffer.clear();	// set position to zero and set limit to capacity
    		outCharBuffer.put(msg);
    		outCharBuffer.flip();	
    		// Encode to ByteBuffer for transfer
    		encoder.encode(outCharBuffer, outByteBuffer, false);
    		outByteBuffer.flip(); 
    		
    		// Send list of files to TCP client
    		bytesSent = sendToTCPClient(outByteBuffer);
    		
    		// error checking on bytes
    		if (bytesSent != msg.length())
    		{
    			// Error message
        		System.out.println("Sorry :(");
        		// return false;
        }
        
        return true;
    }
   
    
    private int sendToTCPClient(ByteBuffer outByteBuffer) 
    {
	    	try {
	    		bytesSent = udpClientChannel.write(outByteBuffer);
	    	} catch (IOException e) {
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	} 	
	    	return bytesSent; 		
    }
    
    
}
