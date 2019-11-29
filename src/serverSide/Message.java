package serverSide;

/**
 * Class for abstracting the messages that are exchanged between the game server
 * and clients connected to it. Each message has a protocol ID, flag, and data section
 * that can be interpreted for appropriate handling. 
 * @author Mariella
 *
 */
public class Message {
	
	private int protocolId;
	private int flag;
	private Client client; // The client associated with this message
	private String data;
	
	public Message(int protocol, int newFlag, Client newClient, String newData)
	{
		this.protocolId = protocol;
		this.flag = newFlag; 
		this.client = newClient; 
		this.data = newData; 
	}

	public Message() 
	{
		this.protocolId = -1;
		this.flag = -1;
		this.client = null;
		this.data = "";
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

	public void setData(String data) {
		this.data = data;
	}

	public void setClient(Client client) {
		this.client = client;
	}
	
	public Client getClient() {
		return this.client;
	}

	public int getFlag() {
		return flag;
	}

	public String getData() {
		return data;
	}
	

	public int getGameRoomId() {
		return client.getGameRoomId();
	}

}
