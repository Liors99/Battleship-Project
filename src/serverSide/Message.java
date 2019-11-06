package serverSide;
public class Message {
	
	private int protocolId;
	private int flag;
	private Client client;
	private String data;
	
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
