package clientSide;
public class GameState {
    private int[][] Player1Board; //the client unless observer
    private int[][] Player2Board; //the enemy
    private boolean gameOver;

    private int Destroyers;
    private int dLength;
    private int Submarines;
    private int sLength;
    private int Cruisers;
    private int cLength;
    private int Battleship;
    private int bLength;
    private int Carrier;
    private int CLength;

    private void initShips(){
        Destroyers = 1;
        Submarines = 1;
        Cruisers = 1;
        Battleship = 1;
        Carrier = 1;

        dLength = 2;
        sLength = 3;
        cLength = 3;
        bLength = 4;
        CLength = 5;
    }

    GameState(){
        initShips();
        Player1Board =new int[10][10];
        Player2Board = new int[10][10];
        gameOver = false;
    }

    GameState(int[][] board1, int[][] board2)
    {
        initShips();
        gameOver = false;
        Player1Board = board1;
        Player2Board = board2;
    }

    /**
     *
     * @param board containts ships, hits, misses
     *              empty space is 0
     *              ship is 1
     *              hit is 2
     *              miss is 3
     */
    private void displayBoard(int[][] board){
        for(int[] row : board){
            for(int i : row){
                switch(i) {
                    case 0:
                        System.out.print('.');
                        break;
                    case 1:
                        System.out.print('+');
                        break;
                    case 2:
                        System.out.print('X');
                        break;
                    case 3:
                        System.out.print('0');
                        break;
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public void printAvaliableShips(){
        if(Destroyers != 0){
            System.out.println("Destroyers Avaliable " + Destroyers);
        }else{
            System.out.println("No Destroyers Left");
        }
        if(Submarines != 0){
            System.out.println("Submarines Avaliable " + Submarines);
        }else{
            System.out.println("No Destroyers Left");
        }
        if(Cruisers != 0){
            System.out.println("Cruisers Avaliable " + Cruisers);
        }else{
            System.out.println("No Destroyers Left");
        }
        if(Battleship != 0){
            System.out.println("Battleship Avaliable " + Battleship);
        }else{
            System.out.println("No Destroyers Left");
        }
        if(Carrier != 0){
            System.out.println("Carrier Avaliable " + Carrier);
        }else{
            System.out.println("No Destroyers Left");
        }
    }

    /**
     * checks if any ships are avaliable
     * @return
     */
    public boolean shipsAvaliable(){
        if(Destroyers != 0 &&
                Submarines != 0 &&
                Cruisers != 0 &&
                Battleship != 0 &&
                Carrier != 0
        ){
            return false;
        }else{
            return true;
        }
    }

    public void displayBoards(){
        displayBoard(getPlayer1Board());
        System.out.println();
        displayBoard(getPlayer2Board());
    }

    private char shipDir(int x1, int y1, int x2, int y2){
        if(x1!=x2 && y1!=y2) return 'F';
        if(x1 == x2){
            return 'v';
        }else{
            return 'h';
        }
    }

    private int shipLength(char dir, int x1, int y1, int x2, int y2){
        //-1 as 0 indexed
        if (dir == 'h'){
            return x2 - x1+1;
        }else{
            return y2 - y1+1;
        }
    }

    private void invalidLen(int id, int length){
        System.out.println("Invalid length "+length +" for id " + id + " OR no ships of this type left");
    }

    private void occupiedSpace(int x, int y){
        System.out.println("Occupied x "+x+" y "+y);
    }

    private void diagShip(){
        System.out.println("Ship is placed diagonally?");
    }

    /**
     * bounds checking
     * @param dir
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private boolean checkValidity(int dir, int x1, int y1, int x2, int y2){
        if(x1<0 || y1<0){
            System.out.println("x1 or y1 below 0");
            return false;
        }
        if(x2>9 || y2>9){
            System.out.println("x2 or y2 above 9");
            return false;
        }
        return true;
    }

    /**
     * places a ship if possible
     * prints useful message to stdout
     * @param x1 of first side
     * @param y1 of first side
     * @param x2 of second side
     * @param y2 of second side
     * @return false if unable to place ship
     */
    public boolean placeShipPlayer1Board(int id, int x1, int y1, int x2, int y2) {
        boolean shipPlaced = true;



        char dir = shipDir(x1, y1, x2, y2);
        if (dir == 'F'){
            diagShip();
        }
        if(!checkValidity(dir, x1, y1, x2, y2)){
            return false;
        }

        int length = shipLength(dir, x1, y1, x2, y2);

        switch(id){
            case 0:
                if(length != dLength || Destroyers==0){
                    invalidLen(id,length);
                    return false;
                }
                break;
            case 1:
                if(length != sLength || Submarines == 0){
                    invalidLen(id,length);
                    return false;
                }

                break;
            case 2:
                if(length != cLength || Cruisers == 0){
                    invalidLen(id,length);
                    return false;
                }
                break;
            case 3:
                if(length != bLength || Battleship==0){
                    invalidLen(id,length);
                    return false;
                }
                break;
            case 4:
                if(length != CLength || Carrier == 0){
                    invalidLen(id,length);
                    return false;
                }
                break;
            default:
                System.out.println("Unknown ship id: " + id);
        }
        //checked if space occupied
        if(dir == 'v'){
            for(int i=y1;i<=y2;i++){
                if(Player1Board[i][x2]!=0){
                   occupiedSpace(x1,i);
                   return shipPlaced;
                }
            }
        }else{
            for(int i=x1;i<=x2;i++){
                if(Player1Board[y1][i]!=0){
                    occupiedSpace(i,y1);
                    return shipPlaced;
                }
            }
        }

        //place ship
        if(dir=='v'){
            for(int i=y1;i<=y2;i++){
                Player1Board[i][x2] = 1;
            }
        }else{
            for(int i=x1;i<=x2;i++){
                Player1Board[y1][i] = 1;
            }
        }

        //dec counter
        switch(id){
            case 0:
                Destroyers--;
                break;
            case 1:
                Submarines--;
                break;
            case 2:
                Cruisers--;
                break;
            case 3:
                Battleship--;
                break;
            case 4:
                Carrier--;
                break;

        }


        return shipPlaced;
    }

    /**
     * updates clients [aka board1, for observer abstraction] board
     * @param mv
     * empty space is 0
     * ship is 1
     * hit is 2
     * miss is 3
     *
     */
    public void updatePlayer1Board(Move mv){
        Player1Board[mv.getRow()][mv.getCol()] = mv.getValue();
    }

    /**
     * updates enemy board [aka board2]
     * @param mv
     * empty space is 0
     * ship is 1
     * hit is 2
     * miss is 3
     *
     */
    public void updatePlayer2Board(Move mv){
        Player2Board[mv.getRow()][mv.getCol()] = mv.getValue();
    }

    public  boolean isGameOver(){return false;}

    public int[][] getPlayer1Board() {
        return Player1Board;
    }

    public void setPlayer1Board(int[][] player1Board) {
        Player1Board = player1Board;
    }

    public boolean isShipAtPlayer1Board(Move Mv) {
    	if(Player1Board[Mv.getRow()][Mv.getCol()]==1) {
    		return true;
    	}else {
    		return false;
    	}
    }
    
    public boolean isShipAtPlayer2Board(Move Mv) {
    	if(Player2Board[Mv.getRow()][Mv.getCol()]==1) {
    		return true;
    	}else {
    		return false;
    	}
    }
    
    public int[][] getPlayer2Board() {
        return Player2Board;
    }

    public void setPlayer2Board(int[][] player2Board) {
        Player2Board = player2Board;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
}
