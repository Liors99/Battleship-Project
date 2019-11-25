package serverSide;

import java.util.*;
import java.util.Map.Entry;

public class PlayerFleetBoard
{
    Client user; 
    GameRoom gameRoom;                      //Do I need this? 
    char[][] board = new char[10][10]; 
    int numOfShipsLeftToPlace;    
    int totalHitsSuffered;

    //This is the list that has all the hits that has been made on the player 
    //<X, Y, H=1/M=0>
    List<Pair<Integer, Integer, Integer>> boardHits = new ArrayList<Pair<Integer, Integer, Integer>>(); 

    //This is the list that has the ships and all their positions 
    //<Ship number, X1, Y1, X2, Y2>
    List<ShipSet<Integer, Integer, Integer, Integer, Integer>> ships = new ArrayList<ShipSet<Integer, Integer, Integer, Integer, Integer>>(); 
    

    private static final int DESTROYER_SHIP_ID = 0; 
    private static final int SUBMARINE_SHIP_ID = 1; 
    private static final int CRUISER_SHIP_ID = 2; 
    private static final int BATTLESHIP_SHIP_ID = 3; 
    private static final int CARRIER_SHIP_ID = 4; 
    
    private static final int TOTAL_SHIPS = 5; 
    private static final int DESTROYER_SHIP_LENGTH = 2;
    private static final int SUBMARINE_SHIP_LENGTH = 3;
    private static final int CRUISER_SHIP_LENGTH = 3;
    private static final int BATTLESHIP_SHIP_LENGTH = 4;
    private static final int CARRIER_SHIP_LENGTH = 5;
    private int[] shipSizes = new int[] 
    		{DESTROYER_SHIP_LENGTH,SUBMARINE_SHIP_LENGTH,CRUISER_SHIP_LENGTH,BATTLESHIP_SHIP_LENGTH,CARRIER_SHIP_LENGTH};
    private static final int MAX_HITS_GAME_OVER = 17; //total of each ship's length
    
    /**
     * Main for testing
     * @param args : None
    public static void main(String[] args)
    {
    		Client client = new Client("mariella", "1234567");
    		PlayerFleetBoard board = new PlayerFleetBoard(client);
    		System.out.println(board.getPrivateBoardString());
    		board.placeShip(4, 0, 4, 4, 4);
    		System.out.println(board.getPrivateBoardString());
    		board.checkBoard(1,4);
    		System.out.println(board.getPrivateBoardString());
    }
    */
    
    /** Constructors */
    public PlayerFleetBoard(Client player)
    {
        user = player; 
        numOfShipsLeftToPlace = TOTAL_SHIPS;   
        totalHitsSuffered = 0;
        //Initialize board
        for (int i = 0; i < board.length; i++)
        		for (int j = 0; j < board[0].length; j++)
        			board[i][j] = '.';
    }

    /** 
     * Indicates if there are ships that still need to be placed 
     * @return TRUE - if there are ships remaining, FALSE - if there aren't any ships left to place
    */
    public boolean shipsRemainingToPlace()
    {
        return numOfShipsLeftToPlace > 0;
    }
    
    /**
     * prints the number of ships that still have to be placed on the servr console
     */
    public void printShipsRemainingToPlace()
    {
    		System.out.println("Number of ships remaining for " + user.getUsername() + ": " + numOfShipsLeftToPlace);
    }
    /**
     * Indicates if all ships have been sunk 
     * @return true if all ships have been sunk - false otherwise 
     */
    public boolean allShipsSunk()
    {
    		if (totalHitsSuffered == MAX_HITS_GAME_OVER)
    		{
    			System.out.println("All of " + user.getUsername() + "'s ships have been sunk");
    			return true;
    		}
    		return false;
    }

    /**
     * Places a 1 where a ship exists in board[][] stored on the server 
     * @param shipID the id of the ship
     * @param x1 x of the starting coordinate 
     * @param y1 y of the starting coordinate 
     * @param x2 x of the ending coordinate 
     * @param y2 y of the ending coordinate 
     */
    public void placeShip(int shipID, int x1, int y1, int x2, int y2)
    {	
        ShipSet<Integer, Integer, Integer, Integer, Integer> ship = new ShipSet<Integer, Integer, Integer, Integer, Integer>(shipID, x1, y1, x2, y2);
        ships.add(ship); 
        System.out.print("Length of Placed Ships Set: "+ ships.size());
        int shipSize = shipSizes[shipID];
        for(int i = y1; i <= y2; i++)
            for (int j = x1; j <= x2; j++)
                board[i][j] = '+'; 
        numOfShipsLeftToPlace--;
        printBoard();
    }

    /**
     * Checks if there a ship placed at point <X,Y> on the board[][] stored on the server 
     * @param X the x coordinate of the point 
     * @param Y the y coordinate of the point 
     * @return true/false if there is a 1 (ship) at that point
     */
    public boolean checkBoard(int X, int Y)
    {	
        if(board[Y][X] == '+')
        {
        		//It's a hit
        		board[Y][X] = 'X';
                totalHitsSuffered++;
                System.out.println(user.getUsername() + " has suffered " + totalHitsSuffered + " hits");
                Pair<Integer, Integer, Integer> hit = new Pair<Integer, Integer, Integer>(X, Y, 1);
                boardHits.add(hit);
                System.out.println("Number of hits in the set: "+boardHits.size());

            return true;
        }
        //It's a miss
        board[Y][X] = '0';
        Pair<Integer, Integer, Integer> miss = new Pair<Integer, Integer, Integer>(X, Y, 0);
        boardHits.add(miss);
        System.out.println("Number of hits in the set: "+boardHits.size());

        return false; 
    }
    
    /**
     * Prints the board on the serverside (Used for testing that shipPlacement on the serverside is
     * working correctly) 
     */
    public void printBoard()
    {
    		for (int x = 0; x < board.length; x++)
    		{
    			for (int y = 0; y < board[0].length; y++)
    				System.out.print(board[x][y] + " ");
    			System.out.println("");
    		}
    }

    /**
     * Method to extract string representation of private game board
     * @return String representing private game board, row-by-row
     */
	public String getPrivateBoardString() {
		
		String boardString = "";
		for (int x = 0; x < board.length; x++)
		{
			for (int y = 0; y < board[0].length; y++)
				boardString += board[x][y];
		}
		return boardString;
	}

}

class Pair<H,L,R> {
    private L l;
    private R r;
    private H h; 
    public Pair(L l, R r, H h){
        this.h = h; 
        this.l = l;
        this.r = r;
    }
    public L getL(){ return l; }
    public R getR(){ return r; }
    public H getH(){ return h; }
    public void setL(L l){ this.l = l; }
    public void setR(R r){ this.r = r; }
    public void setH(H h){ this.h = h; }

}

class ShipSet<S,X,Y,L,R> {
    private L l;
    private R r;
    private X x;
    private Y y;
    private S s; 
    public ShipSet(S s, X x, Y y, L l, R r){
        this.s = s; 
        this.x = x; 
        this.y = y;
        this.l = l;
        this.r = r;
    }
    public L getL(){ return l; }
    public R getR(){ return r; }
    public X getX(){ return x; }
    public Y getY(){ return y; }
    public S getS(){ return s; }
    public void setL(L l){ this.l = l; }
    public void setR(R r){ this.r = r; }
    public void setX(X x){ this.x = x; }
    public void setY(Y y){ this.y = y; }
    public void setS(S s){ this.s = s; }

}