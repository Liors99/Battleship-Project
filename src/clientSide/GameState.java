package clientSide;
public class GameState {
    private int[][] Player1Board; //the client unless observer
    private int[][] Player2Board; //the enemy
    private boolean gameOver;

    GameState(){
        gameOver = false;
    }

    GameState(int[][] board1, int[][] board2)
    {
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
                        System.out.print(' ');
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
    


    public void displayBoards(){
        displayBoard(getPlayer1Board());
        displayBoard(getPlayer2Board());
    }
    
    //TODO
    public boolean placeShipPlayer1Board(int x1, int y1, int x2, int y2) {return false;}

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
