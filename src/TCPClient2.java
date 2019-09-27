/*
 * A simple TCP client that sends messages to a server and display the message
   from the server. 
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */


import java.io.*; 
import java.net.*; 

class TCPClient2 {
	
	private static int BUFFERSIZE = 8 * 1024;
	
	private int portNumber;
	private Socket clientSocket;
	private PrintWriter outBuffer; 
	private InputStream inStream;
	private BufferedReader inBuffer;
	private BufferedReader inFromUser;
	
	public TCPClient2(String add, int port) throws IOException
	{
		// Initialize a client socket connection to the server
		portNumber = port;
        clientSocket = new Socket(add, port); 
        
        // Initialize input and an output stream for the connection(s)
        outBuffer = new PrintWriter(clientSocket.getOutputStream(), true);
        
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
            System.out.println("Usage: TCPClient <Server IP> <Server Port>");
            System.exit(1);
        }

        // Make instance of TCP client
        TCPClient2 tcpClient = new TCPClient2(args[0], Integer.parseInt(args[1]));

        // Initialize user input stream
        String line; 
        

        // Get user input and send to the server
        // Display the echo meesage from the server
        System.out.print("Please enter a message to be sent to the server ('logout' to terminate): ");
        
        line = tcpClient.inFromUser.readLine(); 
        
        // Request-response loop
        while (!line.equals("logout"))
        {
            // Send to the server
			tcpClient.outBuffer.println(line);
			
			// What we do now depends on the command we sent, if any
			if (line.trim().equals("list"))
			{
				String fileList = tcpClient.receiveMsg();
				System.out.print(fileList);
			}
			else if (line.trim().startsWith("get"))
			{
				String fileContents = tcpClient.receiveMsg();
				
				//System.out.println("File contents:");
				//System.out.println(fileContents);
				
				// Save file contents
				String fileName = line.substring(3).trim() + "-" + tcpClient.portNumber;
				try 
				{
					//System.out.println("Will save in " + fileName);
					BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
				    writer.write(fileContents); 
				    writer.close();
				    System.out.println("File saved in " + fileName + " (" + 
							fileContents.getBytes().length + " bytes)");
				}
				catch (IOException e)
				{
					System.out.println("Could not write to " + fileName);
					e.printStackTrace();
					System.exit(1);
				}			
			}
			else
			{
				if (!line.equals("terminate"))
				{
					// Getting response from the server
		            line = tcpClient.inBuffer.readLine();
		            System.out.println("Server: " + line);
				}
			}
              
            System.out.print("Please enter a message to be sent to the server ('logout' to terminate): ");
            line = tcpClient.inFromUser.readLine(); 
        }

        // Close the socket
        tcpClient.clientSocket.close();           
    } 
    
    private String receiveMsg()
    {
    		int bytesRead = 0;
		char[] charBuffer = null;
		String msg = "";
		
		try 
		{
			charBuffer = new char[BUFFERSIZE/2]; // 2 bytes per character
			boolean done = false;
			while (!done)
			{
				bytesRead = inBuffer.read(charBuffer, 0, charBuffer.length);
				System.out.println("Bytes read: " + bytesRead);
				inBuffer.read(charBuffer, 0, charBuffer.length);
				msg += new String(charBuffer);
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
} 
