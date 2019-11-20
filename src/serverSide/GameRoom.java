package serverSide;

import java.sql.Time;
import java.util.*;

import serverSide.Client;

public class GameRoom
{

    int gameID; 
    boolean gameOver; 
    HashMap<Client, PlayerFleetBoard> playerBoards = new HashMap<Client, PlayerFleetBoard>();
    Client sourcePlayer; 
    Client targetPlayer;
    static List<Client> observers = new ArrayList<Client>();  
    static Time duration; 
    GameServer server;
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
    


    public static void main(String[] args)
    {
    		Client player1 = new Client("user1","pswd1");
    		Client player2 = new Client("user2","pswd2");
    		GameRoom game = new GameRoom(null,null,1,player1,player2);
    		//Place ship message
    		Message msg = new Message();
    		msg.setProtocolId(GAMEROOM_ID);
    		msg.setFlag(SHIP_PLACEMENT_REQUEST_FLAG);
    		msg.setData("40444");
    		msg.setClient(player1);
    		game.handleMessage(msg);
    		PlayerFleetBoard playerBoard = game.playerBoards.get(player1);
    		playerBoard.printBoard();
    		msg.setData("30333");
    		game.handleMessage(msg);
    		playerBoard.printBoard();
    		msg.setData("20222");
    		game.handleMessage(msg);
    		playerBoard.printBoard();
    		msg.setData("10121");
    		game.handleMessage(msg);
    		playerBoard.printBoard();
    		
    }


    /** Constructors */
    public GameRoom(GameServer newServer, WaitingRoom newWaitingRoom, int gameRoomID, Client player1, Client player2)
    {
        server = newServer;
        waitingRoom = newWaitingRoom; 
        gameID = gameRoomID;
        gameOver = false; 
        PlayerFleetBoard board1 = new PlayerFleetBoard(player1); 
        PlayerFleetBoard board2 = new PlayerFleetBoard(player2); 
        playerBoards.put(player1, board1);
        playerBoards.put(player2, board2);  
        createStartingPlayer(player1, player2);

    }


    /**  Methods */

    //Notify a player of turn- returns a messge that indicates its the players turn/or not  


    public boolean isGameOver() {
        return gameOver;
    }

    /** Adds a client to the observers list */
    public void addObserver(Client client)
    {
        observers.add(client); 
    }

    /** Removes a client from the observers list */
    public void removeObserver(Client client)
    {
        int index = observers.indexOf(client); 
        observers.remove(index); 
    }
    
    /** Still W.I.P */
    public void handleMessage(Message msg)
    {
        Client assocClient = msg.getClient(); 
        int protocolID = msg.getProtocolId(); 
        int flag = msg.getFlag();
        String data = msg.getData(); 
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
    }

	private boolean allPlayersFinishedPlacingShips()
    {
        PlayerFleetBoard board;   
        for (Client player : playerBoards.keySet())
        {
        		board = playerBoards.get(player); 
            if (board.shipsRemainingToPlace())
            {
            		board.printShipsRemainingToPlace();
            		return false;
            }
        }
        return true; 
    }

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

    private void sendChatMessage(Message Msg){}
    
    public void communicateToAllPlayerQuit(Client quitClient){

        gameOver = true; 
        Message msg;
        for (Client player : playerBoards.keySet())
        {
            if (player != quitClient){
                msg = new Message(PROTOCOL_ID_GAME_LOGOUT,FLAG_LOGOUT_SUCCESSFUL, player, "");
                server.sendToClient(player, msg);
            }
        }    
        playerBoards = null;     
        /** Have to implement the closing for observers */

    } 

    private void communicateToAllPlayersHit(Client source, int x, int y, int hit)
    {
        Message msg;
        String data = Integer.toString(x) + Integer.toString(y) + Integer.toString(hit);
        System.out.println("Data that should be sent for Hit: "+ data);
        for (Client player : playerBoards.keySet())
        {
            if (player == source)
                msg = new Message(HIT_REPLY_PROTOCOL_ID, HIT_REPLY_SOURCE_FLAG, player, data);
            else 
                msg = new Message(HIT_REPLY_PROTOCOL_ID, HIT_REPLY_TARGET_FLAG, player, data);
            server.sendToClient(player, msg);
        }
        /** Have to implement for observers */
    }

    private void communicateToAllPlayersStart()
    {
    		System.out.println("All players have placed ships");
        Message msg; 
        for (Client player : playerBoards.keySet())
        {
            msg = new Message(PROTOCOL_SHIP_PLACEMENT_COMPLETE,FLAG_SHIPS_PLACED_BOTH_PLAYERS, player, "");
            server.sendToClient(player, msg);
        }
        //Send to starting player that it is their turn
        communicateToPlayerTurn();
    }

    private void communicateToPlayerTurn() 
    {
    		System.out.println("=========================================================");
    		System.out.println("It is " + sourcePlayer.getUsername() + "'s turn");
    		System.out.println("=========================================================");
    		Message msg = new Message(GAMEROOM_ID, PLAYER_TURN_FLAG, sourcePlayer, "");
    		server.sendToClient(sourcePlayer, msg);
	}
    

    public void communicateToAllPlayersGameOver(Client winner, Client loser) 
    {
    		System.out.println("=========================================================");
    		System.out.println("GAME OVER");
    		System.out.println("=========================================================");
		Message msgWinner = new Message(GAMEROOM_ID, PLAYER_WON_FLAG, winner, "");	
		server.sendToClient(winner, msgWinner);
		Message msgLoser = new Message(GAMEROOM_ID, PLAYER_LOST_FLAG, loser, "");	
		server.sendToClient(loser, msgLoser);
		waitingRoom.alertGameHasEnded(this.gameID, playerBoards.keySet());
	}

    /** WHAT IS THIS SUPPOSED TO DO */
    public Time getRemainingTime()
    {
        return duration; 
    }

    /** Randomly chooses a client to start the game - "50/50" odds */
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


    public int getGameID() {
        return gameID;
    }



}