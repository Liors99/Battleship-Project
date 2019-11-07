package serverSide;

public class ShipGamePiece 
{
    boolean isSunk;  
    int size; 
    int xPOS;
    int yPOS;
    boolean vertical; 

    public ShipGamePiece(int shipSize)
    {
        size = shipSize; 
        isSunk = false; 
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
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