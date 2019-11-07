package serverSide;

public class ShipGamePiece 
{
    boolean isSunk;  
    int size; 
    int xPOS;
    int yPOS;
    boolean vertical; 
    int[] hitspots; 

    public ShipGamePiece(int shipSize)
    {
        size = shipSize; 
        hitspots = new int[shipSize];
        for(int i : hitspots)
        {
            hitspots[i] = 0;
        }
        isSunk = false; 
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
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
     * @param x the position to set horizontally 
     * @param y the position to set vertically 
     */
    public void setPosition(int x, int y) {
        this.xPOS = x;
        this.yPOS = y; 
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
     * @param orientation the orientation to set
     */
    public void setOrientation(boolean vert)
    {
        vertical = vert; 
    }

    /**
     * @return the orientation
     */
    public boolean getOrientation() {
        return vertical;
    }

}