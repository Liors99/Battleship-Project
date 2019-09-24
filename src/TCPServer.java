/*
 * A simple TCP server that echos messages back to the client.
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */

import java.io.*;
import java.net.*;

public class TCPServer {
    public static void main(String args[]) {
        // Initialize a server socket and a client socket for the server
        ServerSocket echoServer = null;
        Socket clientSocket = null;

        // Initialize an input and an output stream
        String line = "";
        BufferedReader inBuffer;
        DataOutputStream outBuffer;

        if (args.length != 1)
        {
            System.out.println("Usage: TCPServer <Listening Port>");
            System.exit(1);
        }
        
        // Try to open a server socket on the given port
        // Note that we can't choose a port less than 1023 if we are not
        // privileged users (root)
        try {
            echoServer = new ServerSocket(Integer.parseInt(args[0]));
        }
        catch (IOException e) {
            System.out.println(e);
        }
   
        try {
            // Create a socket object from the ServerSocket to listen and accept 
            // connections.
            clientSocket = echoServer.accept();
            System.out.println("Accept connection from " + clientSocket.toString());

            // Open input and output streams
            inBuffer = new BufferedReader(new
                 InputStreamReader(clientSocket.getInputStream()));
            outBuffer = new DataOutputStream(clientSocket.getOutputStream());

            // As long as we receive data, echo that data back to the client.
            while (!line.equals("terminate")) {
                line = inBuffer.readLine();
                System.out.println("Client: " + line);
                outBuffer.writeBytes(line + "\n"); 
            }
            
            // Close the connections
            inBuffer.close();
            outBuffer.close();
            clientSocket.close();
            echoServer.close();
        }   
        catch (IOException e) {
            System.out.println(e);
        }
    }
}
