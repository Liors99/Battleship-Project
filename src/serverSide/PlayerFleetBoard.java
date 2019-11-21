package serverSide;

public class PlayerFleetBoard
{
    Client user; 
    GameRoom gameRoom;                      //Do I need this? 
    char[][] board = new char[10][10]; 
    int numOfShipsLeftToPlace;    
    int totalHitsSuffered;

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
    
    
    /** Constructors */
    public PlayerFleetBoard(Client player)
    {
        user = player; 
        numOfShipsLeftToPlace = TOTAL_SHIPS;   
        totalHitsSuffered = 0;
        //Initialize board
        for (int i = 0; i < board.length; i++)
        		for (int j = 0; j < board[0].length; j++)
        			board[i][j] = '0';
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
	    	int shipSize = shipSizes[shipID];
	    	for(int i = y1; i <= y2; i++)
	    		for (int j = x1; j <= x2; j++)
	    			board[i][j] = '1'; 
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
        if(board[Y][X] == '1')
        {
        		//It's a hit
        		totalHitsSuffered++;
        		System.out.println(user.getUsername() + " has suffered " + totalHitsSuffered + " hits");
            return true;
        }
        //It's a miss
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

}