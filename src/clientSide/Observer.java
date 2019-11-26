package clientSide;

import java.util.List;

public class Observer {
    private GameState gameBoards;
    private Client C;
    private int moveCounter;

    private static void getChatMSG(byte[] msg) {

        String s = new String(msg);
        System.out.println("------------ Your opponent has messaged: " + s);
    }


    private int[] get2Ints( byte[] barr){
        byte[] b1 = new byte[4];
        byte[] b2 = new byte[4];
        for(int k =0;k<4;k++){
            b1[k] = barr[k];
            b2[k] = barr[k*2];
        }
        int[] out = new int[2];
        out[0] = C.fromByteArray(b1);
        out[1] = C.fromByteArray(b2);
       return out;
    }


    /**
     * get a particular board from server dump
     * @param id which board to get
     * @throws InterruptedException
     */
    private void getBoard(int id) throws InterruptedException {
        ClientMessage CM = C.getServerMsg(); //expects dump
        String sBoard = new String(CM.getData());
        System.out.println(sBoard);

        //CM.viewData();

        int [][] board = new int[10][10];
        for(int i=0;i<10;i++){
            for(int j=0;j<10;j++){
                char c = sBoard.charAt(i*10+j);
                if(c == '.'){ //in case we got char
                    board[i][j] = 0;
                }else if(c=='+'){
                    board[i][j] = 1;

                }else if(c=='X'){
                    board[i][j] = 2;
                    moveCounter++;
                }else if(c=='0'){
                    board[i][j] = 3;
                    moveCounter++;
                }
            }
        }
        if(id == 0) {
            gameBoards.setPlayer1Board(board);
        }else if(id == 1){
            gameBoards.setPlayer2Board(board);
        }
        
        //Print to make sure they are as expected
        gameBoards.displayBoards();
        
    }

    /**
     * handles observer
     * currently supports chat and viewing the gameplay
     * @param C
     */
    public void main (Client C){
        this.C = C;
        moveCounter = 0;
        gameBoards = C.getPlayerState();
        try {
            getBoard(0);
            getBoard(1);

            boolean gameOver = false;
            //return;   //for testing
            while(!gameOver){
                ClientMessage CM = C.getServerMsg();
                int protId = CM.getProtocolId();
                if(protId == 3){ //chat
                    getChatMSG(CM.getData());
                }else if(protId == 2){ //hit
                    if(moveCounter%2==0){ //p1
                      int[] ar = get2Ints(CM.getData());
                      Move mv = new Move();
                      mv.setCol(ar[0]);
                      mv.setRow(ar[1]);
                      if(gameBoards.isShipAtPlayer1Board(mv)) mv.setValue(2);
                      else mv.setValue(3);
                      gameBoards.updatePlayer1Board(mv);
                    }else{ //p2
                        int[] ar = get2Ints(CM.getData());
                        Move mv = new Move();
                        mv.setCol(ar[0]);
                        mv.setRow(ar[1]);
                        if(gameBoards.isShipAtPlayer2Board(mv)) {mv.setValue(2);}
                        else{ mv.setValue(3);}
                        gameBoards.updatePlayer2Board(mv);
                    }
                    gameBoards.observe();
                    if(gameBoards.isGameOver() || gameBoards.isGameOver2()) {
                        gameOver = true;
                    }
                    moveCounter++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
