package serverSide;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * The "waiting room" class. The waiting room keeps track of all active clients and
 * GameRoom (game) instances. Clients that are active in the system are "redirected" to
 * the waiting room, where they may choose to enqueue themselves as players, or request
 * to observe an ongoing game instance. 
 * @author Mariella
 *
 */
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
	HashSet<Client> activeClients; 
	Queue<Client> playerQueue; //clients waiting assignment as player
	HashMap<Integer,GameRoom> games;
	
	public WaitingRoom(GameServer server)
	{
		this.server = server;
		this.activeClients = new HashSet<Client>();
		this.playerQueue = new LinkedList<Client>();
		this.games = new HashMap<Integer,GameRoom>();
	}

	/**
	 * Add the client to the list of current active clients.
	 * @param client : the client we wish to activate
	 */
	public void activateClient(Client client) {
		activeClients.add(client);
	}
	
	/**
	 * Method to deactivate client, should they logout or disconnect. Currently,
	 * the default is to end the game instance with which they were involved if they
	 * should drop from the system.
	 * @param client : the client to deactivate
	 */
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
	
	/**
	 * The client has made a request to join a game. Enqueue the client to the
	 * player queue. When two players are available, they will be popped from the queue
	 * and assigned to a newly-created game instance.
	 * @param client : the client that has requested to join a game
	 */
	private void addClientToPlayerQueue(Client client) {
		playerQueue.add(client);
	}

	/**
	 * Central method for handling of client messages which have been forwarded to 
	 * the waiting room.
	 * @param msg : the client message that requires handling
	 */
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
	
	/**
	 * A client has made a request to observe a game room. Add them as an
	 * observer to the game room. 
	 * @param client : the client that has made an observer request
	 * @param gameRoomId : the game room id of the GameRoom instance that
	 * the client would like to observe
	 */
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

	/**
	 * For potential observers. A method to send all currently active games
	 * to a would-be observer, so they can make their selection of the game they
	 * wish to observe from the list. 
	 * !!! Not yet fully implemented/tested.
	 * @param client : the client that would like to become an observer
	 */
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

	/**
	 * Helper function to retrieve a random game room id should an observer
	 * not wish to specify a particular game to observer.
	 * @return : a random game room id
	 */
	private int getRandomGameRoomId() 
	{
		double randDouble = Math.random() * (games.size() - 1);
		int randInt = (int) Math.round(randDouble);
		return randInt;
	}

	/**
	 * Method wherein we create a new GameRoom instance if two players are 
	 * available to be matched.
	 */
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
			System.out.println("New game started");
			
			//Stuff for debugging to circumvent game play
			//game.communicateToAllPlayersGameOver(player1, player2);
		}
	}

	/**
	 * Method to forward a message from client to the game room to which they
	 * are associated.
	 * @param msg : the message that needs to be forwarded to a GameRoom instance 
	 * for handling
	 */
	public void forwardMessageToGameRoom(Message msg) {
		
		int gameRoomId = msg.getGameRoomId();
		GameRoom game = games.get(gameRoomId); //need to wrap in Integer?
		game.handleMessage(msg);
	}
	
	/**
	 * Method to notify WaitingRoom that a GameRoom instance may be discarded because
	 * the game has concluded.
	 * @param gameRoomId : game room id of the game instance that has concluded
	 * @param clients : the clients associated with the game instance that must be 
	 * notified that it has concluded
	 */
	public void alertGameHasEnded(int gameRoomId, Set<Client> clients) 
	{
		games.remove(gameRoomId);
		for (Client client : clients)
			client.resetStatus(); // client is no longer a player (or observer) at conclusion of game
	}

	/**
	 * Method to check whether a client is currently active/online/logged in
	 * @param user : the Client that we wish to verify is logged in
	 * @return True if the user is logged in; False otherwise
	 */
	public boolean isActiveClient(Client user) {
		
		return activeClients.contains(user);
	}

	
}
