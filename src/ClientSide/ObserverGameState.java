package ClientSide;
public class ObserverGameState extends GameState{
    ObserverGameState(){super();}
    ObserverGameState(int[][] board1, int[][] board2){
        super(board1, board2);
    }

    /**
     * does not do anything as this is an observer
     */
    @Override
    public boolean isGameOver() {
        return false;
    }

}
