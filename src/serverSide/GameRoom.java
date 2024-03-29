package serverSide;

import java.sql.Time;
import java.util.*;

import serverSide.Client;

public class GameRoom
{

    private int gameID; 
    private boolean gameOver; 
    private boolean gameStarted = false; 
    private HashMap<Client, PlayerFleetBoard> playerBoards = new HashMap<Client, PlayerFleetBoard>();
    private Client player1;	//keep track of player1 and 2 for game observers
    private Client player2;
    private Client sourcePlayer; //the current player
    private Client targetPlayer;
    private List<Client> observers = new ArrayList<Client>();  
    private Time duration; 
    private GameServer server;
    WaitingRoom waitingRoom; 
    
    private static final int DESTROYER_SHIP_ID = 0; 
    private static final int SUBMARINE_SHIP_ID = 1; 
    private static final int CRUISER_SHIP_ID = 2; 
    private static final int BATTLESHIP_SHIP_ID = 3; 
    private static final int CARRIER_SHIP_ID = 4;
    
    private static final int PROTOCOL_ID_GAME_LOGOUT = 0; 
    private static final int FLAG_LOGOUT_SUCCESSFUL = 2; 

    private static final int GAMEROOM_ID = 2; 
    private static final int SHIP_PLACEMENT_REQUEST_FLAG = 1; 
    private static final int SUCCESS_SHIP_PLACEMENT_FLAG = 0;
    private static final int FAILURE_SHIP_PLACEMENT_FLAG = 2; 
    private static final int HIT_SHIP_REQUEST_FLAG = 2; 
    private static final int PROTOCOL_SHIP_PLACEMENT_COMPLETE = 3; 
    //private int FLAG_SHIPS_PLACED_ONE_PLAYER = 0; 
    private static final int FLAG_SHIPS_PLACED_BOTH_PLAYERS = 0;
    
    private static final int HIT_REPLY_PROTOCOL_ID = 4;
    private static final int HIT_REPLY_TARGET_FLAG = 0; 
    private static final int HIT_REPLY_SOURCE_FLAG = 1; 
    
    private static final int PLAYER_TURN_FLAG = 6;
    
    private static final int PLAYER_WON_FLAG = 4;
    private static final int PLAYER_LOST_FLAG = 5;
    
    private static final int MESSAGING_ID = 8;
    private static final int MSG_FROM_PLAYER_FLAG = 0;
    private static final int MSG_FROM_OBSERVER_FLAG = 1;
    
    private static final int PLAYER_BOARD_DUMP_ID = 6;
    private static final int DUMP_PLAYER_SHIPS_FLAG = 0; 
    private static final int DUMP_HITS_ON_PLAYER = 1;
    private static final int DUMP_HITS_MADE_FLAG = 2; 
       
    private static final int OBSERVER_BOARD_DUMP_ID = 7;
    
    //For observer dumps
	private static final int PLAYER_1_FLAG = 0;
	private static final int PLAYER_2_FLAG = 1;

    /** Constructors */
    public GameRoom(GameServer newServer, WaitingRoom newWaitingRoom, int gameRoomID, Client player1, Client player2)
    {
        this.server = newServer;
        this.waitingRoom = newWaitingRoom; 
        this.gameID = gameRoomID;
        System.out.println("GameRoom ID: "+ gameID);
        this.gameOver = false; 
        this.player1 = player1;
        this.player2 = player2;
        PlayerFleetBoard board1 = new PlayerFleetBoard(player1); 
        PlayerFleetBoard board2 = new PlayerFleetBoard(player2); 
        playerBoards.put(player1, board1);
        playerBoards.put(player2, board2);  
        createStartingPlayer(player1, player2);

    }


    /**  Methods */

    //Notify a player of turn- returns a messge that indicates its the players turn/or not  

    /**
     * Checks if the game is over 
     * @return true/false if the game is over 
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Adds a client to the observers list
     * @param client the Client being added to the Observers list 
     */
    public void addObserver(Client client)
    {
        observers.add(client); 
    }

    /**
     * Removes a client from the observers list
     * @param client the client to be removed from the observers list 
     */
    public void removeObserver(Client client)
    {
        int index = observers.indexOf(client); 
        observers.remove(index); 
    }
    
    /**
     * Handles messages received by the server 
     * - Places ships 
     * - Forward hit requests 
     * @param msg the message from the Client (that the Server has created)
     */
    public void handleMessage(Message msg)
    {
        Client assocClient = msg.getClient(); 
        int protocolID = msg.getProtocolId(); 
        System.out.println("GameRoom Protocol ID: "+ protocolID);
        int flag = msg.getFlag();
        System.out.println("GameRoom Flag: "+ flag);
        String data = msg.getData(); 
        System.out.println("GameRoom Data: "+ data);
        PlayerFleetBoard board = playerBoards.get(assocClient);
        
        /** Ship placement request */
        if(protocolID == GAMEROOM_ID && flag == SHIP_PLACEMENT_REQUEST_FLAG)
        {
            try
            {
                placeShip(board, data);
                Message response = new Message(GAMEROOM_ID, SUCCESS_SHIP_PLACEMENT_FLAG, assocClient, ""); 
                server.sendToClient(assocClient, response);
                
                if(allPlayersFinishedPlacingShips()) //Both players have finished placing ships 
                {   
                		waitingRoom.addToSetOfGamesAllShipsPlaced(this); //Let waiting room know this game has all ships placed
                		communicateToAllPlayersStart();     //Tell all players that they're done 
                }
                
            }
            catch(Exception e)
            {
                Message response = new Message(GAMEROOM_ID, FAILURE_SHIP_PLACEMENT_FLAG, assocClient, "");
                server.sendToClient(assocClient, response);
            }
        }
        /** Hit ship request */
        else if(protocolID == GAMEROOM_ID && flag == HIT_SHIP_REQUEST_FLAG)
        {
            if(assocClient == sourcePlayer)
            {
                char charX = data.charAt(0); 
                char charY = data.charAt(1);
                int hit = checkHit(targetPlayer, Character.getNumericValue(charX), Character.getNumericValue(charY));
                communicateToAllPlayersHit(sourcePlayer, Character.getNumericValue(charX), Character.getNumericValue(charY), hit);
                //check if you have sunk the last ship 
                PlayerFleetBoard targetBoard = playerBoards.get(targetPlayer);
                if (targetBoard.allShipsSunk())
                {
                		this.gameOver = true;
                		communicateToAllPlayersGameOver(sourcePlayer, targetPlayer);
                }
                else
                {
                		//Set turn to next player
            			Client temp = targetPlayer;
            			targetPlayer = sourcePlayer;
            			sourcePlayer = temp;
                    //And tell the next player it is their turn
                    communicateToPlayerTurn();
                }
            }
        }
        
        /** Messaging */
        else if(protocolID == MESSAGING_ID)
        {
        		//determine if message is from a player or an observer
        		int msgFlag = msg.getFlag();
        		if (msgFlag == MSG_FROM_PLAYER_FLAG)
        			shareMessageWithAll(msg);
        		else if (msgFlag == MSG_FROM_OBSERVER_FLAG)
        			shareMessageWithObservers(msg);
        }
    }

    private void shareMessageWithObservers(Message oldMsg) 
    {
		for (Client observer : observers)
		{
			Message newMsg = new Message(MESSAGING_ID, MSG_FROM_OBSERVER_FLAG, observer, oldMsg.getData());
			server.sendToClient(observer, newMsg);
		}	
	}


	private void shareMessageWithAll(Message oldMsg) 
	{
		Client src = oldMsg.getClient();
		//Send message first to the other player
		for (Client player : players())
		{
			if (player != src)
			{
				Message newMsg = new Message(MESSAGING_ID, MSG_FROM_PLAYER_FLAG, player, oldMsg.getData());
				server.sendToClient(player, newMsg);
			}
		}
		//Then share with observers
		shareMessageWithObservers(oldMsg);
	}

    public void dumpBoardContents(Client client)
    {
        //System.out.println("Dump Board Contents");
        PlayerFleetBoard clientBoard = playerBoards.get(client); 
        System.out.println("Size of client board: "+clientBoard.ships.size());
        //The first position is the number of shops that are currently placed (Max 5)
        String newData = "";
        //Go through all ships currently placed and add them to the byte array (data section)
        for (ShipSet<Integer, Integer, Integer, Integer, Integer> ship : clientBoard.ships) {

            //for the information associated with each ship and add it to a position in the byte array
            newData += ship.getS().toString();   //shipID
            newData += ship.getX().toString();  //x1
            newData += ship.getY().toString();   //y1
            newData += ship.getL().toString();   //x2
            newData += ship.getR().toString();   //y2
        }
        System.out.println("Data Section: "+newData);
        //BOARD_DUMP_ID = 6, DUMP_PLAYER_SHIPS_FLAG = 0
        Message msg = new Message(PLAYER_BOARD_DUMP_ID, DUMP_PLAYER_SHIPS_FLAG, client, newData);
        server.sendToClient(client, msg);


        //Send the hits on each Client 
        System.out.println("Number of Hits on Client: "+ playerBoards.values().size());
        String message61 = "";
        String message62 = ""; 
        for(PlayerFleetBoard board : playerBoards.values())
        {
            System.out.println("Number of Boards to Parse: "+playerBoards.values().size());
            newData = "";
            //For each hit that has been made add the hit to the data section in the message
            if(board.boardHits.size() > 0){
                //get all the hits made on this board
                for (Pair<Integer, Integer, Integer> hit : board.boardHits)
                {
                    newData += hit.getH().toString(); //Hit or Miss (1/0)
                    newData += hit.getL().toString(); //X
                    newData += hit.getR().toString(); //Y
                }
            }
            System.out.println("Data section for Hit: "+newData); 
            //If the board that we are getting the hits for is the the clients board (Flag = hits on)
            if(board == clientBoard){
                System.out.println("BOARD IS THE CLIENT BOARD");
                message61 = newData; 
                System.out.println("Message 6.2: "+message62);
                //board.printBoard();
            }
            //The board is the targets board - so its the hits we have made 
            else {
                message62 = newData; 
                System.out.println("Message 6.1: "+message61);
                //board.printBoard();
            }
            
        }
        //HAVING PROBLEMS 
        msg = new Message(PLAYER_BOARD_DUMP_ID, DUMP_HITS_ON_PLAYER, client, message61);
        server.sendToClient(client, msg);  

        //WORKS
        msg = new Message(PLAYER_BOARD_DUMP_ID, DUMP_HITS_MADE_FLAG, client, message62);
        server.sendToClient(client, msg);
        
        if(gameStarted) //All ships have been placed 
        {
            //ID 3, FLAG 0 (protocol response for finishing placing ships)
            msg = new Message(PROTOCOL_SHIP_PLACEMENT_COMPLETE,FLAG_SHIPS_PLACED_BOTH_PLAYERS, client, "");
            server.sendToClient(client, msg);
            if(client == sourcePlayer)//turn)
            {
                communicateToPlayerTurn(); 
            }
            //Check player turn and send ID = 2 Flag = 6 
        }
        System.out.println("Skipped");
    }

	/**
     * Checks the 2 players boards in to see if either have shipsRemainingToPlace
     * @return true if all players have finished placing ships 
     */
	private boolean allPlayersFinishedPlacingShips()
    {
        PlayerFleetBoard board;   
        for (Client player : players())
        {
        		board = playerBoards.get(player); 
            if (board.shipsRemainingToPlace())
            {
            		board.printShipsRemainingToPlace();
            		return false;
            }
        }
        gameStarted = true; 
        return true; 
    }

    /**
     * Checks if the targets board has a ship at position <X,Y> when a client wants to perform a hit 
     * @param target the player that the hit is being performed on 
     * @param x the x coordinate of the position 
     * @param y the y coordinate of the position 
     * @return int 1 if it's a hit, 0 if it's a miss 
     */
    private int checkHit(Client target, int x, int y)
    {
    		System.out.println("Checking if move " + x + "," + y + " is a hit on " + target.getUsername() + "...");
        PlayerFleetBoard board = playerBoards.get(target);
        if (board.checkBoard(x, y)) 
        {
        		System.out.println("It's a hit on " + target.getUsername() + " at " + x + "," + y);
            return 1;
        }
        System.out.println("It's a miss on " + target.getUsername() + " at " + x + "," + y);
        return 0;   //it's a miss 
    }

    /**
     * Places a ship on a clients board
     * @param board the board that the ship is being placed on 
     * @param data the string of data from the message that is has the shipID/ <X1, Y1> / <X2, Y2>
     * within it (has to be extracted)
     */
    private void placeShip(PlayerFleetBoard board, String data)
    {
        if(board.shipsRemainingToPlace()){
            char charID = data.charAt(0); 
            char charX = data.charAt(1); 
            char charY = data.charAt(2); 
            int shipNum = Character.getNumericValue(charID);
            int X1 = Character.getNumericValue(charX);
            int Y1 = Character.getNumericValue(charY);
            charX = data.charAt(3); 
            charY = data.charAt(4); 
            int X2 = Character.getNumericValue(charX);
            int Y2 = Character.getNumericValue(charY);

            System.out.println("Placing " + charID + " from " + X1 + "," + Y1 + " to " + X2 + "," + Y2);
            
            if(shipNum == DESTROYER_SHIP_ID)
            {
                board.placeShip(DESTROYER_SHIP_ID, X1, Y1, X2, Y2);
            }
            else if(shipNum == SUBMARINE_SHIP_ID)
            {
                board.placeShip(SUBMARINE_SHIP_ID, X1, Y1, X2, Y2);
            }
            else if(shipNum == CRUISER_SHIP_ID)
            {
                board.placeShip(CRUISER_SHIP_ID, X1, Y1, X2, Y2);
            }
            else if(shipNum == BATTLESHIP_SHIP_ID)
            {
                board.placeShip(BATTLESHIP_SHIP_ID, X1, Y1, X2, Y2);
            }
            else if(shipNum == CARRIER_SHIP_ID)
            {
                board.placeShip(CARRIER_SHIP_ID, X1, Y1, X2, Y2);
            }
        
        }
        else{
            //failed 
        }
    }
    
    /**
     * Communicates to all other members of the GameRoom that a player has left 
     * Either we are in an idle state, the timer has started or we are dropping the connection 
     * for the remainder of the players 
     * @param quitClient - the client that disconnected 
     */
    public void communicateToAllPlayerQuit(Client quitClient){

        gameOver = true; 
        Message msg;
        for (Client player : players())
        {
            if (player != quitClient){
                msg = new Message(PROTOCOL_ID_GAME_LOGOUT,FLAG_LOGOUT_SUCCESSFUL, player, ""); // need individual protocol for other player dropped?
                server.sendToClient(player, msg);
            }
        }    
        playerBoards = null;     
        /** Have to implement the closing for observers */

    } 

    /**
     * Communicates to all members of the GameRoom if a "Hit" request from a source client was a Hit 
     * or a miss 
     * @param source - the Client that sent the "hit" request 
     * @param x - the x position of the hit request 
     * @param y - the y position of the hit request 
     * @param hit - 1/0 based on if it was a hit or a miss 
     */
    private void communicateToAllPlayersHit(Client source, int x, int y, int hit)
    {
        Message msg;
        //String data = Integer.toString(x) + Integer.toString(y) + Integer.toString(hit);
        String data = "" + x + y + hit;
        System.out.println("Data that should be sent for Hit: " + data);
        System.out.println("Length of hit data, in bytes: " + data.getBytes().length);
        
        //Send to players
        for (Client player : players())
        {
            if (player == source)
                msg = new Message(HIT_REPLY_PROTOCOL_ID, HIT_REPLY_SOURCE_FLAG, player, data);
            else 
                msg = new Message(HIT_REPLY_PROTOCOL_ID, HIT_REPLY_TARGET_FLAG, player, data);
            server.sendToClient(player, msg);
        }
        
        //And send to observers
        for (Client observer : observers)
        {
        		if (source == player1) //hit by player 1, on player 2
        			msg = new Message(HIT_REPLY_PROTOCOL_ID, PLAYER_2_FLAG, observer, data); //hit on player 2
        		else //hit by player 2, on player 1
        			msg = new Message(HIT_REPLY_PROTOCOL_ID, PLAYER_1_FLAG, observer, data); //hit on player 1
        		server.sendToClient(observer, msg);
        }
    }

    /**
     * Sends a message to all clients that all players have finished placing ships and that 
     * the game (Hit phase) can commence 
     */
    private void communicateToAllPlayersStart()
    {
    		System.out.println("All players have placed ships");
        Message msg; 
        for (Client player : players())
        {
            msg = new Message(PROTOCOL_SHIP_PLACEMENT_COMPLETE,FLAG_SHIPS_PLACED_BOTH_PLAYERS, player, "");
            server.sendToClient(player, msg);
        }
        //Send to starting player that it is their turn
        communicateToPlayerTurn();
    }

    /**
     * Sends a message to the clients indicating it's their turn 
     */
    private void communicateToPlayerTurn() 
    {
    		System.out.println("=========================================================");
    		System.out.println("It is " + sourcePlayer.getUsername() + "'s turn");
    		System.out.println("=========================================================");
    		Message msg = new Message(GAMEROOM_ID, PLAYER_TURN_FLAG, sourcePlayer, "");
    		server.sendToClient(sourcePlayer, msg);
	}
    
    /**
     * Once one player has had all their ships sunk, send the message to that client saying that they 
     * have lost the game, and send a message to the other client indicating that they have won
     * @param winner - the player/client that won the game 
     * @param loser - the player/client that lost the game 
     */
    public void communicateToAllPlayersGameOver(Client winner, Client loser) 
    {
    		System.out.println("=========================================================");
    		System.out.println("GAME OVER");
    		System.out.println("=========================================================");
		waitingRoom.alertGameHasEnded(this.gameID, players());
		Message msgWinner = new Message(GAMEROOM_ID, PLAYER_WON_FLAG, winner, "");	
		server.sendToClient(winner, msgWinner);
		Message msgLoser = new Message(GAMEROOM_ID, PLAYER_LOST_FLAG, loser, "");	
		server.sendToClient(loser, msgLoser);
		
	}

    /** 
     * WHAT IS THIS SUPPOSED TO DO 
     * Will be used for handling dropping and reconnecting of players  
     * */
    public Time getRemainingTime()
    {
        return duration; 
    }

    /**
     * Randomly chooses a client to start the game - "50/50" odds
     * @param player1 - the first player in the game 
     * @param player2 - the second player in the game 
     */
    public void createStartingPlayer(Client player1, Client player2)
    {
        Random random = new Random(); 
        int randomnumber = random.nextInt(100); 
        if((randomnumber % 2) == 0) 
        	{
        		sourcePlayer = player1;
        		targetPlayer = player2;
        	}
        else 
        	{
        		sourcePlayer = player2;
        		targetPlayer = player1;
        	}
    }

    /**
     * @return the ID for the game 
     */
    public int getGameID() {
        return gameID;
    }

    public void dumpBoardContentsToObserverUponJoin(Client observer)
    {
    		//Dump player 1's board
    		PlayerFleetBoard board1 = playerBoards.get(player1);
    		String boardString1 = board1.getPrivateBoardString();
    		Message boardDump1 = new Message(OBSERVER_BOARD_DUMP_ID, PLAYER_1_FLAG, observer, boardString1);
    		server.sendToClient(observer, boardDump1);
    		//Dump player 2's board
    		PlayerFleetBoard board2 = playerBoards.get(player2);
    		String boardString2 = board2.getPrivateBoardString();
    		Message boardDump2 = new Message(OBSERVER_BOARD_DUMP_ID, PLAYER_2_FLAG, observer, boardString2);
    		server.sendToClient(observer, boardDump2);
    }
    
    public Set<Client> players()
    {
    		return this.playerBoards.keySet();
    }


}