package serverSide;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TestClass {

	public static void main(String[] args) {
	
		byte[] recvBytes = new byte[32];
		
		//Simulating writing
		int protocolId = 1;
		int flag = 1;
		byte protocolByte = (byte) protocolId;
		byte flagByte = (byte) flag;
		String data = "some payload";
		byte[] dataBytes = data.getBytes();
		
		byte[] sendBytes = new byte[2 + dataBytes.length];
		sendBytes[0] = protocolByte;
		sendBytes[1] = flagByte;
		System.out.println(sendBytes[0]);
		System.out.println(sendBytes[1]);
		for (int i = 0; i < dataBytes.length; i++)
		{
			sendBytes[2 + i] = dataBytes[i];
			System.out.println(sendBytes[2+i]);
		}
		System.out.println("Length of bytes sent: " + sendBytes.length);
	
		ByteArrayOutputStream output = new ByteArrayOutputStream(sendBytes.length);
		output.write(sendBytes, 0, sendBytes.length);
	
		//Simulating reading
		
		byte[] inBytes = output.toByteArray();
		System.out.println("Length of bytes received: " + sendBytes.length);
		
		ByteArrayInputStream input = new ByteArrayInputStream(inBytes);
		DataInputStream inStream = new DataInputStream(input);
		try {
			System.out.println("Protocol Id:" + inStream.read());
			System.out.println("Flag:" + inStream.read());
			boolean doneReading = false;
			StringBuilder strBuilder = new StringBuilder();
			
    			while(!doneReading)
    			{
    				int bytesRead = inStream.read(recvBytes);
    				System.out.println("Bytes read: " + bytesRead);
    				strBuilder.append(new String(inBytes));
    				System.out.println("String so far: " + strBuilder.toString());
    				doneReading = (bytesRead < 32);
    			}
    		
    			//Get the data section (payload)
    			System.out.println("Data:" + strBuilder.toString());
    			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
