package serverSide;

public class ShipGamePiece 
{
    boolean isSunk;  
    int size; 
    int xPOS;
    int yPOS;
    boolean vertical; 
    int[] hitspots; 

    public ShipGamePiece(int shipSize, int x, int y, boolean vert)
    {
        size = shipSize; 
        hitspots = new int[shipSize];
        for(int i : hitspots)
        {
            hitspots[i] = 0;
        }
        isSunk = false; 
        xPOS = x; 
        yPOS = y; 
        vertical = vert;
    }


    public void shipWasHit()
    {
        //Add someting to update the internal array hitspots - which is supposed to mimick the board 
        //except it's just an array of the ship 
    }


    /**
     * IS THIS NEEDED 
     * @return the size
     */
    public int getSize() {
        return size;
    }

  
    /**
     * @return the xPosition
     */
    public int getX() {
        return xPOS;
    }

    public int getY()
    {
        return yPOS;
    }

    /**
     * @return the isSunk
     */
    public boolean isSunk() {
        return isSunk;
    }

    /**
     * @param isSunk the isSunk to set
     */
    public void setSunk(boolean isSunk) {
        this.isSunk = isSunk;
    }

    /**
     * @return the orientation
     */
    public boolean getOrientation() {
        return vertical;
    }

}