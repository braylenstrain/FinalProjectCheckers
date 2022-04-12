# Industry Projects 1175 Final Project *Checkers*

## Synopsis
This project allows two clients to connect to a server to play a simplified game of checkers.
The differences from normal checkers are:
1. There is no double jumping.
2. There are no crowned pieces.
3. The game ends when no more moves can be made. The player with the most pieces wins.

## Motivation
This is a project to display my understanding of the topics that I have learned from this course.

## How to Run
Two clients are paired through the server. When it is a player's turn, they click on a piece to see it's possible moves highlighted. They then click one of the highlighted squares to make that move. Then it is the other player's turn.

## Code Example
This code snippet shows how the program highlights possible moves for a piece. The cells that meet the criteria are put into an ArrayList called *possibleMoves*. Depending on the player, it looks at either the row above or below the piece. If a cell is out of bounds, an *IndexOutOfBoundsException* is caught. If the player chooses a difference piece or makes a move, *unhighlight()* will clear the ArrayList.
```
	//Highlight moves for Player 1 pieces
	private void highlightDownwardMoves() {
		int row = selectedPieceLocation.row;
		int column = selectedPieceLocation.column;
		
		//Check Cell to bottom left of selected piece
		try {
			Cell potentialMove1 = board[row + 1][column - 1];
			if (potentialMove1.pieceID == 0) possibleMoves.add(potentialMove1);
			
			//Check for possible capture move
			else if (potentialMove1.pieceID == otherPlayer) {
				int captureRow = potentialMove1.row;
				int captureColumn = potentialMove1.column;
				Cell potentialTake = board[captureRow + 1][captureColumn - 1];
				if (potentialTake.pieceID == 0) {
					possibleMoves.add(potentialTake);
				}
			}
		} catch(IndexOutOfBoundsException ex) {}
		
		//Check Cell to bottom right of selected piece
		try {
			Cell potentialMove2 = board[row + 1][column + 1];
			if (potentialMove2.pieceID == 0) possibleMoves.add(potentialMove2);
			
			//Check for possible capture move
			else if (potentialMove2.pieceID == otherPlayer) {
				int captureRow = potentialMove2.row;
				int captureColumn = potentialMove2.column;
				Cell potentialTake = board[captureRow + 1][captureColumn + 1];
				if (potentialTake.pieceID == 0) {
					possibleMoves.add(potentialTake);
				}
			} 
		} catch (IndexOutOfBoundsException ex) {}
	}
	
	//Highlight moves for Player 2 pieces
	private void highlightUpwardMoves() {
		int row = selectedPieceLocation.row;
		int column = selectedPieceLocation.column;
		
		//Check Cell to top left of selected piece
		try {
			Cell potentialMove1 = board[row - 1][column - 1];
			if (potentialMove1.pieceID == 0) possibleMoves.add(potentialMove1);
			
			//Check for possible capture move
			else if (potentialMove1.pieceID == otherPlayer) {
				int captureRow = potentialMove1.row;
				int captureColumn = potentialMove1.column;
				Cell potentialTake = board[captureRow - 1][captureColumn - 1];
				if (potentialTake.pieceID == 0) {
					possibleMoves.add(potentialTake);
				}
			}
		} catch(IndexOutOfBoundsException ex) {}
		
		//Check Cell to top right of selected piece
		try {
			Cell potentialMove2 = board[row - 1][column + 1];
			if (potentialMove2.pieceID == 0) possibleMoves.add(potentialMove2);
			
			//Check for possible capture move
			else if (potentialMove2.pieceID == otherPlayer) {
				int captureRow = potentialMove2.row;
				int captureColumn = potentialMove2.column;
				Cell potentialTake = board[captureRow - 1][captureColumn + 1];
				if (potentialTake.pieceID == 0) {
					possibleMoves.add(potentialTake);
				}
			}
		} catch (IndexOutOfBoundsException ex) {}
	}
	
	private void unhighlight() {
		for (Cell cell: possibleMoves) {
			cell.getStyleClass().remove("highlightedcell");
		}
		
		possibleMoves.clear();
	}
 ```
