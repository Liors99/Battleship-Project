package serverSide;

public class PlayerFleetBoard
{
    Client user; 
    GameRoom gameRoom;                      //Do I need this? 
    int numOfShipsLeft;    
    int shipSunk; 

    private int DESTROYER_SHIP_ID = 0;
    private int DESTROYER_SIZE = 2;  
    private int SUBMARINE_SHIP_ID = 1;
    private int SUBMARINE_SIZE = 3;  
    private int CRUISER_SHIP_ID = 2;
    private int CRUISER_SIZE = 3;  
    private int BATTLESHIP_SHIP_ID = 3;
    private int BATTLESHIP_SIZE = 4;  
    private int CARRIER_SHIP_ID = 4; 
    private int CARRIER_SIZE = 5;
    private int TOTAL_SHIPS = 5; 
    private int BOARD_SIZE = 10;

    char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
    ShipGamePiece[] pieces = new ShipGamePiece[TOTAL_SHIPS];

    /** Constructors */
    public PlayerFleetBoard(Client player)
    {
        user = player; 
        numOfShipsLeft = TOTAL_SHIPS;        
        shipSunk = 0; 
    }

    /** Indicates if there are ships that still need to be placed */
    public boolean shipsRemaining()
    {
        if(numOfShipsLeft > 0) return true; 
        else return false; 
    }


    /** This method puts the ships on the board */
    public void updateBoard(int X, int Y, int size, boolean vert)
    {   
        if(vert)
        {
            for(int i = Y; i < (Y+size); i++)
            {
                board[X][i] = '1'; 
            }
        }
        else
        {
            for(int i = X; i < (X+size); i++)
            {
                board[i][Y] = '1'; 
            }
        }

    }

    public void placeShip(int shipID, int X, int Y, boolean vertical)
    {

        System.out.println("PLACESHIP");

        try{
            if(shipID == DESTROYER_SHIP_ID)
            {
                ShipGamePiece destroyer = new ShipGamePiece(DESTROYER_SIZE, X, Y, vertical);
                pieces[DESTROYER_SHIP_ID] = destroyer; 

            }
            else if(shipID == SUBMARINE_SHIP_ID)
            {
                ShipGamePiece submarine = new ShipGamePiece(SUBMARINE_SIZE, X, Y, vertical);
                pieces[SUBMARINE_SHIP_ID] = submarine; 
            }
            else if(shipID == CRUISER_SHIP_ID)
            {
                ShipGamePiece cruiser = new ShipGamePiece(CRUISER_SIZE, X, Y, vertical);
                pieces[CRUISER_SHIP_ID] = cruiser;
            }
            else if(shipID == BATTLESHIP_SHIP_ID)
            {
                ShipGamePiece battleShip = new ShipGamePiece(BATTLESHIP_SIZE, X, Y, vertical);
                pieces[BATTLESHIP_SHIP_ID] = battleShip;
            }
            else if(shipID == CARRIER_SHIP_ID)
            {
                ShipGamePiece carrier = new ShipGamePiece(CARRIER_SIZE, X, Y, vertical);
                pieces[CARRIER_SHIP_ID] = carrier;
            }
        }
        catch(Exception e)
        {
            System.out.println(e);
            throw e;
        }

        //updateBoard(X, Y, pieces[shipID].getSize(), vertical);
        System.out.println(numOfShipsLeft);         
        numOfShipsLeft--;
        System.out.println("Number of ships left: "+numOfShipsLeft);

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

    public boolean allShipsSunk()
    {
        return TOTAL_SHIPS == shipSunk; 
    }    

}