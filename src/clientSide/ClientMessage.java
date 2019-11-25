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
	
	/**
	 * Special class for converting the raw data into ship hit coordinates 
	 * @return
	 */
	public int[] getDataShipInts() {
		byte[] placements = getData();
		int[] res = new int[placements.length/4];
		System.out.println("--------- SIZE OF HITS: "+placements.length);
		
		res[0]=fromByteArray(Arrays.copyOfRange(placements, 0, 4));
		res[1]=fromByteArray(Arrays.copyOfRange(placements, 4, 8));
		res[2]=fromByteArray(Arrays.copyOfRange(placements, 8, 12));
		
		return res;
	}
	
	
	public byte[] getEntirePacket() {
		byte[] res = new byte[2 + data.size()];
		res[0] = (byte) protocolId;
		res[1] = (byte) flag;
		
		byte[] d = getData();
		System.arraycopy(d, 0, res, 2, d.length);
		
		return res;
	}
	
	
	public int[] dataToIntArray() {
		byte[] org = getData();
		int[] res = new int[org.length/4];
		
		int j = 0;
		for(int i =0 ; i<org.length ; i+=4) {
			res[j]= fromByteArray(Arrays.copyOfRange(org, i, i+4));
			j++;
		}
		
		return res;
	}
	
	private int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
   }
}
