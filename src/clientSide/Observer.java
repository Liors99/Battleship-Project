package clientSide;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Observer {
    private GameState gameBoards;
    private Client C;
    private int moveCounter;

    private static void getChatMSG(byte[] msg) {

        String s = new String(msg);
        System.out.println("------------ Player messaged: " + s);
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
        //System.out.println(sBoard);

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


    private  int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Gets the hit coordiantes and the type
     * @return
     * @throws InterruptedException
     */
    private int[] getHitData(ClientMessage rec_msg){
        int[] data = new int[5];
        int id = -1;


        data[0] = id;
        data[1]=rec_msg.getFlag();

        byte[] msg_data = rec_msg.getData();

        System.out.println("-------- Length of data recieved: " + msg_data.length);
        data[2] = fromByteArray(Arrays.copyOfRange(msg_data, 0, 4));
        data[3] = fromByteArray(Arrays.copyOfRange(msg_data, 4, 8));
        data[4] = fromByteArray(Arrays.copyOfRange(msg_data, 8, 12));


        System.out.println("Received data[2]: " + data[2]);
        System.out.println("Received data[3]: " + data[3]);
        System.out.println("Received data[4]: " + data[4]);
        return data;
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
                if(protId == 8){ //chat
                    getChatMSG(CM.getData());
                }else if(protId == 4){ //hit
                    if(moveCounter%2==0){ //p1
                      int[] ar = getHitData(CM);
                      Move mv = new Move();
                      mv.setCol(ar[2]);
                      mv.setRow(ar[3]);
                      if(gameBoards.isShipAtPlayer1Board(mv)) mv.setValue(2);
                      else mv.setValue(3);
                      gameBoards.updatePlayer1Board(mv);
                    }else{ //p2
                        int[] ar = getHitData(CM);
                        Move mv = new Move();
                        mv.setCol(ar[2]);
                        mv.setRow(ar[3]);
                        if(gameBoards.isShipAtPlayer2Board(mv)) {mv.setValue(2);}
                        else{ mv.setValue(3);}
                        gameBoards.updatePlayer2Board(mv);
                    }
                    gameBoards.observe();
                    if(gameBoards.isGameOver() || gameBoards.isGameOver2()) {
                        System.out.println("Game over");
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
