package clientSide;
public class PlayerGameState extends GameState {

    /**
     *  to update fleet boards see game state functions.
     */


    PlayerGameState(){super();}
    PlayerGameState(int[][] board1, int[][] board2){
        super(board1, board2);
    }

    public boolean isValidMove(Move mv){
        if(this.getPlayer2Board()[mv.getRow()][ mv.getCol()] > 1){ //if there is a hit;miss there already
        	System.out.println("You have already tried to hit there, try another position");
            return false;
        }

        return true;
    }

    /**
     *
     * @return if the game is over as clients ships are gone.
     
    @Override
    public boolean isGameOver() {
        boolean shipExists = false;
        l:
        for(int[] row : this.getPlayer1Board()){
            for(int i : row){
                if(i==1){
                    shipExists = true;
                    break l;
                }
            }
        }
       return !shipExists;
    }
    */
}
