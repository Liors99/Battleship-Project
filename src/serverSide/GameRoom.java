package serverSide;

import java.sql.Time;
import java.util.*;

import serverSide.Client;

public class GameRoom
{

    int gameID; 
    boolean gameOver; 
    HashMap<Client, PlayerFleetBoard> players = new HashMap<Client, PlayerFleetBoard>();
    Client playerTurn; 
    static List<Client> observers = new ArrayList<Client>();  
    static Time duration; 
    GameServer server;
    WaitingRoom waitingRoom; 
    private int DESTROYER_SHIP_ID = 0; 
    private int SUBMARINE_SHIP_ID = 1; 
    private int CRUISER_SHIP_ID = 2; 
    private int BATTLESHIP_SHIP_ID = 3; 
    private int CARRIER_SHIP_ID = 4;
    
    private int PROTOCOL_ID_GAME_LOGOUT = 0; 
    private int FLAG_LOGOUT_SUCCESSFUL = 2; 

    private int GAMEROOM_ID = 2; 
    private int SHIP_PLACEMENT_REQUEST_FLAG = 1; 
    private int SUCCESS_SHIP_PLACEMENT_FLAG = 0;
    private int FAILURE_SHIP_PLACEMENT_FLAG = 2; 
    private int HIT_SHIP_REQUEST_FLAG = 2; 

    private int HIT_REPLY_PROTOCOL_ID = 4; 
    private int HIT_REPLY_TARGET_FLAG = 0; 
    private int HIT_REPLY_SOURCE_FLAG = 1; 




    /** Constructors */
    public GameRoom(GameServer newServer, WaitingRoom newWaitingRoom, int gameRoomID, Client player1, Client player2)
    {
        server = newServer;
        waitingRoom = newWaitingRoom; 
        gameID = gameRoomID;
        gameOver = false; 
        PlayerFleetBoard board1 = new PlayerFleetBoard(player1); 
        PlayerFleetBoard board2 = new PlayerFleetBoard(player2); 
        players.put(player1, board1);
        players.put(player2, board2);  
        playerTurn = createStartingPlayer(player1, player2);

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
    
    /** Something has to be changed with the server responses */
    public void handleMessage(Message msg)
    {
        Client assocClient = msg.getClient(); 
        int protocolID = msg.getProtocolId(); 
        int flag = msg.getFlag();
        String data = msg.getData(); 
        PlayerFleetBoard board = players.get(assocClient);
        
        /** Ship placement request */
        if(protocolID == GAMEROOM_ID && flag == SHIP_PLACEMENT_REQUEST_FLAG)
        {
            try
            {
                shipPlacement(board, data);
                Message response = new Message(GAMEROOM_ID, SUCCESS_SHIP_PLACEMENT_FLAG, assocClient, ""); 
                server.sendToClient(assocClient, response);
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
            if(playerTurn.equals(assocClient))
            {
                char charX = data.charAt(0); 
                char charY = data.charAt(1);
                Client target = null; 
                for(Client player : players.keySet())
                {
                    if(player != assocClient) target = player; 
                }
                int hit = checkHit(target, Character.getNumericValue(charX), Character.getNumericValue(charY));
                communicateToAllPlayersHit(assocClient, Character.getNumericValue(charX), Character.getNumericValue(charY), hit);
            }
            else
            {
                //failure
            }
        }

    }

    public int checkHit(Client target, int X, int Y)
    {
        PlayerFleetBoard board = players.get(target);
        if (board.checkBoard(X, Y)) 
            return 1; 
        else 
            return 0;   //it's a miss 
    }

    public void shipPlacement(PlayerFleetBoard board, String data)
    {
        if(board.shipsRemaining()){
            boolean vertical = false; 
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

            if(X1 == X2) vertical = false; 
            else if (Y1 == Y2) vertical = true;

            if(shipNum == DESTROYER_SHIP_ID)
            {
                board.placeShip(DESTROYER_SHIP_ID, X1, Y1, vertical);
            }
            else if(shipNum == SUBMARINE_SHIP_ID)
            {
                board.placeShip(SUBMARINE_SHIP_ID, X1, Y1, vertical);
            }
            else if(shipNum == CRUISER_SHIP_ID)
            {
                board.placeShip(CRUISER_SHIP_ID, X1, Y1, vertical);
            }
            else if(shipNum == BATTLESHIP_SHIP_ID)
            {
                board.placeShip(BATTLESHIP_SHIP_ID, X1, Y1, vertical);
            }
            else if(shipNum == CARRIER_SHIP_ID)
            {
                board.placeShip(CARRIER_SHIP_ID, X1, Y1, vertical);
            }
        
        }
        else{
            //failed 
        }
    }

    public void sendChatMessage(Message Msg){}
    
    public void communicateToAllPlayerQuit(Client quitClient){

        gameOver = true; 
        Message msg;
        for (Client player : players.keySet())
        {
            if (player != quitClient){
                msg = new Message(PROTOCOL_ID_GAME_LOGOUT,FLAG_LOGOUT_SUCCESSFUL, player, "");
                server.sendToClient(player, msg);
            }
        }    
        players = null;     
        /** Have to implement the closing for observers */

    } 

    public void communicateToAllPlayersHit(Client source, int x, int y, int hit)
    {
        Message msg;
        String data = Integer.toString(x)+Integer.toString(y)+Integer.toString(hit);
        System.out.println("Data that should be sent for Hit: "+data);
        for (Client player : players.keySet())
        {
            if (player == source)
                msg = new Message(HIT_REPLY_PROTOCOL_ID, HIT_REPLY_SOURCE_FLAG, source, data);
            else 
                msg = new Message(HIT_REPLY_PROTOCOL_ID, HIT_REPLY_TARGET_FLAG, source, data);
            server.sendToClient(player, msg);
        }

        /** Have to implement for observers */
        
    }

    public PlayerFleetBoard getPlayerFleetBoard(Client client)
    {
        return players.get(client);
    }

    /** WHAT IS THIS SUPPOSED TO DO */
    public Time getRemainingTime()
    {
        return duration; 
    }

    /** Randomly chooses a client to start the game - "50/50" odds */
    public Client createStartingPlayer(Client player1, Client player2)
    {
        Random random = new Random(); 
        int randomnumber = random.nextInt(100); 
        if((randomnumber % 2) == 0) return player1; 
        else return player2;      
    }

    /**
     * @return the gameID
     */
    public int getGameID() {
        return gameID;
    }



}