package ClientSide;

public class Move {
	
	private static int ROW_NUMBER = 10;
	private static int COL_NUMBER = 10;
	
	private int col;
	private int row;
	private int value = 3;
	
	public int getCol() {
		return col;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getValue() {
		return value;
	}
	
	public int getRowNumber() {
		return ROW_NUMBER;
	}
	
	public int getColNumber() {
		return COL_NUMBER;
	}
	
	
	
	public boolean setCol(int c) {
		if(c>0 && c<=COL_NUMBER) {
			this.col = c;
			return true;
		}
		
		return false;
	}
	
	public boolean setRow(int r) {
		if(r>0 && r<= ROW_NUMBER) {
			this.row = r;
			return true;
		}
		
		return false;
		
	}
	
	public boolean setValue(int v) {
		this.value = v;
		
		return true;
	}
	
	

}
