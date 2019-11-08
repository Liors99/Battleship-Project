package serverSide;

public class PlayerFleetBoard
{
    Client user; 
    GameRoom gameRoom;                      //Do I need this? 
    char[][] board = new char[10][10]; 
    int numOfShipsLeftToPlace;    

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
    /** Constructors */
    public PlayerFleetBoard(Client player)
    {
        user = player; 
        numOfShipsLeftToPlace = TOTAL_SHIPS;    
        //Initialize board
        for (int i = 0; i < board.length; i++)
        		for (int j = 0; j < board[0].length; j++)
        			board[i][j] = '0';
    }

    /** Indicates if there are ships that still need to be placed */
    public boolean shipsRemaining()
    {
    		System.out.println("Number of ships remaining: " + numOfShipsLeftToPlace);
        return numOfShipsLeftToPlace > 0;
    }


    public void placeShip(int shipID, int x, int y, boolean vertical)
    {	
	    	int shipSize = shipSizes[shipID];
	    	if(vertical)
	    	{
	    		for(int i = y; i < (y + shipSize); i++)
	    			board[x][i] = '1'; 
	    	}
	    	else
	    	{
	    		for(int i = x; i < (x + shipSize); i++)
	    			board[i][y] = '1';
	    	}
	    	numOfShipsLeftToPlace--;
    }

    public boolean checkBoard(int X, int Y)
    {
        if(board[X][Y] == '1')
        {
            board[X][Y] = 'H'; 
            //Check if the ship has sunk 
            /**if()
            {

            }*/
            return true; 
        }
        else
        {
            return false; 
        }
    }
    
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