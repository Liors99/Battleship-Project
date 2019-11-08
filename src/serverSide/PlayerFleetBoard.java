package serverSide;

public class PlayerFleetBoard
{
    Client user; 
    GameRoom gameRoom;                      //Do I need this? 
    char[][] board = new char[8][8]; 
    ShipGamePiece[] pieces = new ShipGamePiece[5];
    int numOfShipsLeft;    
    int shipSunk; 

    private int DESTROYER_SHIP_ID = 0; 
    private int SUBMARINE_SHIP_ID = 1; 
    private int CRUISER_SHIP_ID = 2; 
    private int BATTLESHIP_SHIP_ID = 3; 
    private int CARRIER_SHIP_ID = 4; 
    private int TOTAL_SHIPS = 5; 

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

        if(shipID == DESTROYER_SHIP_ID)
        {
           pieces[DESTROYER_SHIP_ID].setOrientation(vertical); 
           pieces[DESTROYER_SHIP_ID].setSize(2);
           pieces[DESTROYER_SHIP_ID].setPosition(X, Y);

        }
        else if(shipID == SUBMARINE_SHIP_ID)
        {
            pieces[SUBMARINE_SHIP_ID].setOrientation(vertical); 
            pieces[SUBMARINE_SHIP_ID].setSize(3);
            pieces[SUBMARINE_SHIP_ID].setPosition(X, Y);  
        }
        else if(shipID == CRUISER_SHIP_ID)
        {
            pieces[CRUISER_SHIP_ID].setOrientation(vertical); 
            pieces[CRUISER_SHIP_ID].setSize(3);
            pieces[CRUISER_SHIP_ID].setPosition(X, Y);  
        }
        else if(shipID == BATTLESHIP_SHIP_ID)
        {
            pieces[BATTLESHIP_SHIP_ID].setOrientation(vertical); 
            pieces[BATTLESHIP_SHIP_ID].setSize(4);
            pieces[BATTLESHIP_SHIP_ID].setPosition(X, Y);  
        }
        else if(shipID == CARRIER_SHIP_ID)
        {
            pieces[CARRIER_SHIP_ID].setOrientation(vertical); 
            pieces[CARRIER_SHIP_ID].setSize(5);
            pieces[CARRIER_SHIP_ID].setPosition(X, Y); 
            
        }

        updateBoard(X, Y, pieces[shipID].getSize(), vertical); 
        numOfShipsLeft--; 

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