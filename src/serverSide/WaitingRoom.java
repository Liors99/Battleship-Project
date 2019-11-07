package serverSide;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class WaitingRoom {
	
	//Message ids and flags
	private static final int WAITING_ROOM_ID = 1;
	private static final int GAME_JOIN_FLAG = 0;
	private static final int GAME_OBSERVE_ANY_FLAG = 1;
	private static final int GAME_OBSERVE_SPECIFIED_FLAG = 2;
	private static final int LIST_GAMES_FLAG = 3;
	private static final int GAME_JOIN_SUCCESS = 0;
	private static final int GAME_JOIN_FAILURE = 1;
	private static final int GAME_OBSERVE_SUCCESS = 2;
	private static final int GAME_OBSERVE_FAILURE = 3;
	private static final int LIST_GAMES_SUCCESS = 4;
	private static final int LIST_GAMES_FAILURE = 5;
	
	GameServer server;
	HashSet<Client> activeClients; //not sure if we need this list?
	Queue<Client> playerQueue; //clients waiting assignment as player
	HashMap<Integer,GameRoom> games;
	
	public WaitingRoom(GameServer server)
	{
		this.server = server;
		this.activeClients = new HashSet<Client>();
		this.playerQueue = new LinkedList<Client>();
		this.games = new HashMap<Integer,GameRoom>();
	}

	public void activateClient(Client client) {
		activeClients.add(client);
	}
	
	public void deactiviateClient(Client client) {
		activeClients.remove(client);
		int gameRoomId = client.getGameRoomId();
		GameRoom game = games.get(gameRoomId);
		if(client.isPlayer())
		{
			games.remove(gameRoomId); //The game cannot continue without player
			game.communicateToAllPlayerQuit(client);
		}
		else if (client.isObserver())
		{
			game.removeObserver(client);
		}
	}
	
	private void addClientToPlayerQueue(Client client) {
		playerQueue.add(client);
	}

	public void handleMessage(Message msg) {
		
		Client client = msg.getClient();
		
		if (msg.getFlag() == GAME_JOIN_FLAG)
		{
			addClientToPlayerQueue(client);
			createGameIfEnoughPlayers();
			
		}
		else if (msg.getFlag() == GAME_OBSERVE_ANY_FLAG)
		{
			//Assign to a random game 
			int gameRoomId = getRandomGameRoomId();
			addClientAsObserver(client, gameRoomId);
		}
		else if (msg.getFlag() == GAME_OBSERVE_SPECIFIED_FLAG)
		{
			int gameRoomId = Integer.parseInt(msg.getData());
			addClientAsObserver(client, gameRoomId);
		}
		else if (msg.getFlag() == LIST_GAMES_FLAG)
		{
			sendListOfGamesTo(client);
		}
	}
	
	private void addClientAsObserver(Client client, int gameRoomId) 
	{
		GameRoom game = games.get(gameRoomId);
		game.addObserver(client);
		//Send 'observe success' message
		Message observeSuccess = new Message();
		observeSuccess.setClient(client);
		observeSuccess.setProtocolId(WAITING_ROOM_ID);
		observeSuccess.setFlag(GAME_OBSERVE_SUCCESS);
		server.sendToClient(client, observeSuccess);	
	}

	private void sendListOfGamesTo(Client client) 
	{	
		String[] gameIds = new String[games.size()];
		int i = 0;
		for (Integer gameRoomId : games.keySet())
		{
			gameIds[i] = gameRoomId.toString();
			i++;
		}
		String data = String.join(",", gameIds);
		
		//Create message
		Message listOfGames = new Message();
		listOfGames.setClient(client);
		listOfGames.setProtocolId(WAITING_ROOM_ID);
		listOfGames.setFlag(LIST_GAMES_SUCCESS);
		server.sendToClient(client, listOfGames);
	}

	private int getRandomGameRoomId() 
	{
		double randDouble = Math.random() * (games.size() - 1);
		int randInt = (int) Math.round(randDouble);
		return randInt;
	}

	private void createGameIfEnoughPlayers() 
	{	
		//If there are enough players to start a game instance, do so
		if (playerQueue.size() >= 2)
		{
			Client player1 = playerQueue.remove();
			Client player2 = playerQueue.remove();
			GameRoom game = new GameRoom(this.server, this, games.size(), player1, player2);
			//Add game to list of active games
			games.put(game.getGameID(), game);
			//Modify players' statuses
			player1.setPlayer(game.getGameID());
			player2.setPlayer(game.getGameID());			
			//Create 'join success' message to send to clients
			Message joinSuccess = new Message();
			joinSuccess.setProtocolId(WAITING_ROOM_ID);
			joinSuccess.setFlag(GAME_JOIN_SUCCESS);
			server.sendToClient(player1, joinSuccess);
			server.sendToClient(player2, joinSuccess);
		}
	}

	public void forwardMessageToGameRoom(Message msg) {
		
		int gameRoomId = msg.getGameRoomId();
		GameRoom game = games.get(gameRoomId); //need to wrap in Integer?
		game.handleMessage(msg);
	}
	
	public void alertGameHasEnded(int gameRoomId, ArrayList<Client> clients) 
	{
		games.remove(gameRoomId);
		for (Client client : clients)
			client.resetStatus();
	}

	
}
