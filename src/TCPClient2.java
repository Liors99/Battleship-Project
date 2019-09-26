/*
 * A simple TCP client that sends messages to a server and display the message
   from the server. 
 * For use in CPSC 441 lectures
 * Instructor: Prof. Mea Wang
 */


import java.io.*; 
import java.net.*; 

class TCPClient2 {
	
	private static int BUFFERSIZE = 300;
	
	private Socket clientSocket;
	private PrintWriter outBuffer; 
	private InputStream inStream;
	private BufferedReader inBuffer;
	private BufferedReader inFromUser;
	
	public TCPClient2(String add, int port) throws IOException
	{
		// Initialize a client socket connection to the server
        clientSocket = new Socket(add, port); 
        
        // Initialize input and an output stream for the connection(s)
        outBuffer = new PrintWriter(clientSocket.getOutputStream(), true);
        
        // Break this up into multiple steps so we can see how many bytes
        // are available at the input stream at any given time
        inStream = clientSocket.getInputStream();
        inBuffer = new BufferedReader(
        					new	InputStreamReader(inStream)); 
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
				tcpClient.receiveFileList();
			}
			else if (line.trim().startsWith("get"))
			{
				tcpClient.receiveFileContents();
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
    
    private void receiveFileList()
    {
    		String fileList = receiveMsg();
    		System.out.print(fileList);
    }
    
    private void receiveFileContents()
    {
    		String fileContents = receiveMsg();
    		System.out.print(fileContents);
    
    }
    
    private String receiveMsg()
    {
    		//int bytesRead = 0;
		char[] charBuffer = null;
		
		try {
			charBuffer = new char[BUFFERSIZE];
			//bytesRead = inBuffer.read(charBuffer, 0, charBuffer.length);
			inBuffer.read(charBuffer, 0, charBuffer.length);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// How do we check that we received the entire message?
		
		return new String(charBuffer);
    }
} 
