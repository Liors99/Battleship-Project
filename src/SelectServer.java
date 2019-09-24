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
import java.util.*;

public class SelectServer {
    public static int BUFFERSIZE = 32;
    public static void main(String args[]) throws Exception 
    {
        if (args.length != 1)
        {
            System.out.println("Usage: UDPServer <Listening Port>");
            System.exit(1);
        }

        // Initialize buffers and coders for receive and send on channels
        String line = "";	
        Charset charset = Charset.forName( "us-ascii" );  
        CharsetDecoder decoder = charset.newDecoder();  
        CharsetEncoder encoder = charset.newEncoder();
        ByteBuffer inBuffer = null;
        CharBuffer cBuffer = null;
        int bytesSent, bytesRecv;     // number of bytes sent or received
        // Declare a datagram packet for DatagramChannel
        DatagramPacket packet = null;
        
        
        // Initialize the selector
        Selector selector = Selector.open();

        /****************************************************************
         * FOR TCP: Create channel to listen for incoming TCP connections
         ****************************************************************/
        
        // Create a server socket channel and make it non-blocking
        ServerSocketChannel tcpChannel = ServerSocketChannel.open();	// open but not yet bound
        tcpChannel.configureBlocking(false);
        
        // Create socket address with specified port number and wildcard address 
        InetSocketAddress isa = new InetSocketAddress(Integer.parseInt(args[0]));
        // Bind the socket to the address
        tcpChannel.socket().bind(isa);
        
        /****************************************************************
         * FOR UDP: Create channel that can send and receive UDP packets
         ****************************************************************/

        // Create a datagram (socket) channel and make it non-blocking
        DatagramChannel udpChannel = DatagramChannel.open();
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
                    SelectionKey key = (SelectionKey)readyItor.next();

                    // Remove current entry
                    readyItor.remove();
                    
                    // Get the channel of the ready key
                    SelectableChannel channel = key.channel();
                    //System.out.println("What kind of channel am I?");
                    //System.out.println(channel.getClass().getName());

                    // Accept new connection requests to TCP socket, if any
                    // If this key's channel is ready to accept a new socket connection
                    if (key.isAcceptable()) 	
                    {
                        // Accept a connection made to this channel's socket
                        SocketChannel clientChannel = ((ServerSocketChannel)channel).accept();
                        clientChannel.configureBlocking(false);
                        System.out.println("Accept conncection from " + clientChannel.socket().toString());
                        
                        // Register the new connection for read operation
                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } 
                    // If the key's channel is ready for reading
                    else if (key.isReadable()) 
                    {
                    		// Open input and output streams
                        inBuffer = ByteBuffer.allocateDirect(BUFFERSIZE);
                        cBuffer = CharBuffer.allocate(BUFFERSIZE);
                        
                    		// If there is a datagram waiting at the UDP socket
                    		if (channel instanceof DatagramChannel)
						{
                    			DatagramChannel udpChan = (DatagramChannel) channel;
                    			// Receive datagram available on this channel 
                    			// Returns the datagram's source address
                    			SocketAddress clientAdd = udpChan.receive(inBuffer);
                    			
                    			// How to do error checking on receive?
                    			
                    			if (clientAdd != null)
                    			{
                    				// make buffer ready for a new sequence of channel-write
                    				// or relative get operations
                    				inBuffer.flip();	// make buffer available    	
                    				decoder.decode(inBuffer, cBuffer, false);
                    				cBuffer.flip();		
                    				line = cBuffer.toString();
                    				System.out.println("Client: " + line);
                    				
                    				// echo the message back
                    				inBuffer.flip();
                    				udpChan.send(inBuffer,clientAdd);
                    			}
						}
                    		
                    		// Else if a TCP client socket channel is ready for reading
						else 
						{
							SocketChannel clientChannel = (SocketChannel) channel;
                  
                            // Read from client socket channel
                            bytesRecv = clientChannel.read(inBuffer);
                            if (bytesRecv <= 0)
                            {
                                System.out.println("read() error, or connection closed");
                                key.cancel();  // deregister the socket
                                continue;
                            }
                            
                            // make buffer ready for a new sequence of channel-write
                            // or relative get operations
                            inBuffer.flip();	// make buffer available    	
                            decoder.decode(inBuffer, cBuffer, false);
                            cBuffer.flip();		
                            line = cBuffer.toString();
                            System.out.println("Client: " + line);
                            
                            // Echo the message back
                            inBuffer.flip();
                            
                            bytesSent = clientChannel.write(inBuffer); 
                            if (bytesSent != bytesRecv)
                            {
                                System.out.println("write() error, or connection closed");
                                key.cancel();  // deregister the socket
                                continue;
                            }
						}
                        
                        if (line.equals("terminate\n"))
                            terminated = true;
                        
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
}
