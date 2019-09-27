private String receiveMsg()
    {
    		int bytesRead = 0;
		char[] charBuffer = null;
		String msg = "";
		
		try 
		{
			charBuffer = new char[BUFFERSIZE]; 
			boolean done = false;
			while (!done)
			{
				bytesRead = inBuffer.read(charBuffer, 0, charBuffer.length);
				//System.out.println("Bytes read: " + bytesRead);
				msg += (new String(Arrays.copyOfRange(charBuffer, 0, bytesRead)));
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