package application;

import javafx.scene.shape.Circle;

public class CheckerPiece extends Circle {
	private int pieceID; //Player number that owns piece
	
	public CheckerPiece(int playerID) {
		this.pieceID = playerID;
		setRadius(20);
		if (playerID == 1) {
			getStyleClass().add("blackpiece");
		} else if (playerID == 2) {
			getStyleClass().add("whitepiece");
		}
	}

	public int getPieceID() {
		return pieceID;
	}

}
