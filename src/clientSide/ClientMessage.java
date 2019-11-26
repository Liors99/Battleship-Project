package clientSide;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientMessage {
	private int protocolId;
	private int flag;
	private List<Byte> data;
	
	public ClientMessage(int protocol, int newFlag, byte[] newData)
	{
		this.protocolId = protocol;
		this.flag = newFlag; 
		this.data = new ArrayList<Byte>();
		setData(newData);
	}

	public ClientMessage() 
	{
		this.protocolId = -1;
		this.flag = -1;
		this.data = new ArrayList<Byte>();;
	}
	
	public int getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(int protocolId) {
		this.protocolId = protocolId;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public void setData(byte[] new_data) {
		for (byte b : new_data) {
		    data.add(b);
		}
	}

	public int getFlag() {
		return flag;
	}

	public byte[] getData() {
		byte[] res = new byte[data.size()];
		for(int i=0; i<res.length;i++) {
			res[i]=data.get(i).byteValue();
		}
		return res;
	}


	public void viewData() {
		System.out.println("Printning contents of ClientMessage");
		for(int i=0; i<data.size();i++) {
			System.out.println(data.get(i));
		}

	}
	
	/**
	 * Special class for converting the raw data into ship hit coordinates 
	 * @return
	 */
	public int[] data1ByteToIntArray() {
		byte[] org = getData();
		int[] res = new int[org.length];
		System.out.println("--------- SIZE OF BYTES TO CONVERT: "+org.length);
		
		for(int i = 0; i < org.length; i++) {
			res[i]=org[i];
		}
		
		System.out.println("res = " + Arrays.toString(res));
		
		return res;
	}
	
	
	/**
	 * Gets the entire packet as a byte[]
	 * @return
	 */
	public byte[] getEntirePacket() {
		byte[] res = new byte[2 + data.size()];
		res[0] = (byte) protocolId;
		res[1] = (byte) flag;
		
		byte[] d = getData();
		System.arraycopy(d, 0, res, 2, d.length);
		
		return res;
	}
	
	
	public int[] data4BytesToIntArray() {
		byte[] org = getData();
		int[] res = new int[org.length/4];
		
		int j = 0;
		for(int i =0 ; i<org.length ; i+=4) {
			res[j]= fromByteArray(Arrays.copyOfRange(org, i, i+4));
			j++;
		}
		
		System.out.println("res = " + Arrays.toString(res));
		return res;
	}
	
	private int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
   }
}
