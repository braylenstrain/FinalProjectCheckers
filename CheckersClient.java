package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class CheckersClient extends Application implements CheckersConstants {
	private boolean myTurn; //Indicates if player has the turn
	private int thisPlayer; //Player number of this client
	private int otherPlayer; //Player number of opponent
	private int rowSelected; //Row of Cell that selected piece is moved to
	private int columnSelected; //Column of Cell that selected piece is moved to
	private Cell[][] board = new Cell[8][8]; //Create board
	private Text txtStatus = new Text("Text will go here."); //Status report at bottom of window
	private boolean continueToPlay = true; //The game hasn't ended
	private boolean waiting = true; //Waiting for this player to make a turn
	private String host = "localhost"; //Game played on same computer as server
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	private ArrayList<Cell> possibleMoves = new ArrayList<>(4); //List of all possible move locations of selected piece
	private Cell selectedPieceLocation; //Location of selected piece prior to move
	private Cell originalPieceLocation; //Original piece location in case of double jump
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		//Create menu
		MenuBar menuBar = new MenuBar();
		Menu menuExit = new Menu("Exit");
		Menu menuReadThis = new Menu("Read this first!");
		MenuItem menuItemExit = new MenuItem("Exit the game");
		MenuItem menuItemExplanation = new MenuItem("Due to the project already going well over 15 hours, this is a dumbed down version"
				+ " of Checkers.\n No crowned pieces, no double jumping. Once no more moves can be made, the player with the most pieces wins.");
		menuExit.getItems().add(menuItemExit);
		menuReadThis.getItems().add(menuItemExplanation);
		menuBar.getMenus().add(menuExit);
		menuBar.getMenus().add(menuReadThis);
		
		//Set font of status text
		txtStatus.setFont(Font.font("Verdana", 20));
		
		//Stores cells in a GridPane to mimic a checker board
		GridPane boardPane = new GridPane();
		boardPane.setAlignment(Pos.CENTER);
		
		//Add cell to boards, alternating black and white backgrounds
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++) {
				Cell cell = new Cell(i, j);
				boardPane.add(board[i][j] = cell, j, i);
				if (i % 2 == 0) {
					if (j % 2 == 0) {
						cell.getStyleClass().add("redcell");
					} else {
						cell.getStyleClass().add("blackcell");
					}
				} else {
					if (j % 2 == 0) cell.getStyleClass().add("blackcell");
					else cell.getStyleClass().add("redcell");
				}
			}
		
		//If cell background is black, put piece in that cell
		for (int i = 1; i < boardPane.getChildren().size();i++) {
			Cell currentCell = (Cell)boardPane.getChildren().get(i);
			if (i <= 23 && currentCell.getStyleClass().contains("blackcell")) {
				currentCell.getChildren().add(new CheckerPiece(1));
				currentCell.getChildren().get(0).getStyleClass().add("blackpiece");
				currentCell.pieceID = 1;
			} else if (i >= 40 && currentCell.getStyleClass().contains("blackcell")) {
				currentCell.getChildren().add(new CheckerPiece(2));
				currentCell.getChildren().get(0).getStyleClass().add("whitepiece");
				currentCell.pieceID = 2;
			}
		}
		
		//Put board, menu, and text into BorderPane
		BorderPane window = new BorderPane();
		window.setStyle("-fx-background-color: lightgrey;");
		BorderPane.setAlignment(boardPane, Pos.CENTER);
		BorderPane.setAlignment(txtStatus, Pos.BASELINE_CENTER);
		window.setTop(menuBar);
		window.setCenter(boardPane);
		window.setBottom(txtStatus);
		
		Scene scene = new Scene(window, 500, 500);
		scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		primaryStage.setTitle("Checkers");
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
		
		//Exit menu functionality
		menuItemExit.setOnAction(e -> System.exit(0));
		
		//Connect to server
		try {
			@SuppressWarnings("resource")
			Socket socket = new Socket(host, 8000);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//Control game on seperate thread
		new Thread(() -> {
			try {
				//Server notifies client if they are player 1 or 2
				thisPlayer = fromServer.readInt();
				Platform.runLater(() -> primaryStage.setTitle("Checkers: You are Player " + thisPlayer));
				
				//Set otherPlayer and txtStatus based on thisPlayer
				if (thisPlayer == PLAYER1) {
					otherPlayer = 2;
					boardPane.setRotate(180);
					Platform.runLater(() -> txtStatus.setText("Waiting for Player 2 to join."));
					
					//Ignore server notifying player 2
					fromServer.readInt();
					Platform.runLater(() -> txtStatus.setText("Player 2 has joined. Your move."));
					
					myTurn = true;
				} else if (thisPlayer == PLAYER2) {
					otherPlayer = 1;
					Platform.runLater(() -> txtStatus.setText("Waiting for Player 1 to move."));
				}
				
				while (continueToPlay) {
					if (thisPlayer == PLAYER1) {
						waitForPlayerAction();
						sendMove();
						receiveMove();
					} else if (thisPlayer == PLAYER2) {
						receiveMove();
						waitForPlayerAction();
						sendMove();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}).start();
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	//Keeps thread waiting until player sends move
	private void waitForPlayerAction() throws InterruptedException {
		while (waiting) {
			Thread.sleep(100);
		}
		
		waiting = true;
	}
	
	//Receive opponent's move from server
	private void receiveMove() {
		try {
			int status = fromServer.readInt();
			selectedPieceLocation = board[fromServer.readInt()][fromServer.readInt()];
			rowSelected = fromServer.readInt();
			columnSelected = fromServer.readInt();
			updateBoard(otherPlayer);
			
			//Check for game over
			if (status == PLAYER1_WINS) {
				Platform.runLater(() -> txtStatus.setText("Player 1 wins"));
				continueToPlay = false;
			} else if (status == PLAYER2_WINS) {
				Platform.runLater(() -> txtStatus.setText("Player 2 wins"));
				continueToPlay = false;
			} else if (status == DRAW) {
				Platform.runLater(() -> txtStatus.setText("It's a tie"));
			} else {
				Platform.runLater(() -> txtStatus.setText("Your move"));
				myTurn = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Send move to server
	private void sendMove() {
		//Update GUI to reflect move
		updateBoard(thisPlayer);
		
		//Send move
		try {
			toServer.writeInt(selectedPieceLocation.row);
			toServer.writeInt(selectedPieceLocation.column);
			toServer.writeInt(rowSelected);
			toServer.writeInt(columnSelected);
			
			//Notify server if game is won or if play should continue
			int winCheck = checkForWin();
			toServer.writeInt(winCheck);
			if (winCheck != CONTINUE) {
				if (winCheck == PLAYER1_WINS) Platform.runLater(() -> txtStatus.setText("Player 1 wins"));
				else if (winCheck == PLAYER2_WINS) Platform.runLater(() -> txtStatus.setText("Player 2 wins"));
				else Platform.runLater(() -> txtStatus.setText("It's a tie"));
				
				continueToPlay = false;
			}
			
			//Unhighlight cells
			unhighlight();
			selectedPieceLocation.getStyleClass().remove("potentialmover");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Update pieces on the board
	private void updateBoard(int player) {
		Platform.runLater(() -> {
			
			//Move player piece to new location
			board[rowSelected][columnSelected].getChildren().add(new CheckerPiece(player));
			
			//Update new location with appropriate ID
			board[rowSelected][columnSelected].pieceID = selectedPieceLocation.pieceID;
			
			//Take opponent piece if needed
			if (rowSelected - selectedPieceLocation.row  != 1 && rowSelected - selectedPieceLocation.row != -1) {
				int takeRow = (rowSelected + selectedPieceLocation.row) / 2;
				int takeColumn = (columnSelected + selectedPieceLocation.column) / 2;
				board[takeRow][takeColumn].getChildren().clear();
				board[takeRow][takeColumn].pieceID = 0;
			}
			
			//Reset cell of old position of player piece
			selectedPieceLocation.pieceID = 0;
			selectedPieceLocation.getChildren().clear();
		});
	}

	
	//Checks if opposing player has any moves left before their turn starts
	private int checkForWin() {
		int player1Pieces = 0; //Adds up total pieces of player 1
		int player2Pieces = 0; //Adds up total pieces of player 2
		boolean continueGame = false; //Set to true if a possible move can be made
		
		//Go through board.
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Cell cell = board[i][j];
				System.out.print(cell.pieceID);
				//Cell contains a piece
				if (cell.getChildren().size() > 0) {
					//Add to total player count for whoever own this piece
					if (cell.pieceID == PLAYER1) player1Pieces++; else player2Pieces++;
					//Check for possible move if piece belongs to opposing player. Break loop if so.
					if (cell.pieceID == otherPlayer) {
						if (checkForPossibleMoves(cell)) {
							continueGame = true;
							break;
						}
					}
				}
			}
			if (continueGame) {
				return CONTINUE;
			}
		}
		//No possible moves were found for opposing player
		if (player1Pieces > player2Pieces) {
			return PLAYER1_WINS;
		} else if (player2Pieces > player1Pieces) {
			return PLAYER2_WINS;
		} else {
			return DRAW;
		}
	}
	
	//See if current cell contains a piece with a possible move
	private boolean checkForPossibleMoves(Cell cell) {
		if (cell.pieceID == PLAYER2) {
			try {
				if (board[cell.row - 1][cell.column - 1].pieceID == 0 
				|| board[cell.row - 1][cell.column - 1].pieceID == PLAYER1 && board[cell.row - 2][cell.column - 2].pieceID == 0) {
					return true;
				}
			} catch (IndexOutOfBoundsException ex) {}
			try {
				if (board[cell.row - 1][cell.column + 1].pieceID == 0
				|| board[cell.row - 1][cell.column + 1].pieceID == PLAYER1 && board[cell.row - 2][cell.column + 2].pieceID == 0) {
					return true;
				}
			} catch (IndexOutOfBoundsException ex) {}
			
		} else if (cell.pieceID == PLAYER1) {
			try {
				if (board[cell.row + 1][cell.column - 1].pieceID == 0 
				|| board[cell.row + 1][cell.column - 1].pieceID == PLAYER2 && board[cell.row + 2][cell.column - 2].pieceID == 0) {
					return true;
				}
			} catch (IndexOutOfBoundsException ex) {}
			try {
				if (board[cell.row + 1][cell.column + 1].pieceID == 0
				|| board[cell.row + 1][cell.column + 1].pieceID == PLAYER2 && board[cell.row + 2][cell.column + 2].pieceID == 0) {
					return true;
				}
			} catch (IndexOutOfBoundsException ex) {}
		}
	return false;
	}
	
	//Highlight all possible moves of selected piece
	private void highlightPossibleMoves() {
		selectedPieceLocation.getStyleClass().add("potentialmover");
		if (selectedPieceLocation.pieceID == 1) {
			highlightDownwardMoves();
		} else if (selectedPieceLocation.pieceID == 2) {
			highlightUpwardMoves();
		}
		
		for (Cell cell: possibleMoves) {
			cell.getStyleClass().add("highlightedcell");
		}
	}
	
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
	
	public class Cell extends StackPane {
		private int row; //Indicates row position of this Cell
		private int column; //Indicates column position of this Cell
		private int pieceID; //Determines which player the piece in the cell belongs to
		
		public Cell (int row, int column) {
			this.row = row;
			this.column = column;
			this.setPrefSize(50, 50);
			this.setOnMouseClicked(e -> handleMouseClick());
		}
		
		private void handleMouseClick() {
			if (myTurn && pieceID == thisPlayer) {
				if (selectedPieceLocation != null) selectedPieceLocation.getStyleClass().remove("potentialmover");
				selectedPieceLocation = this;
				if (possibleMoves.size() > 0) unhighlight();
				highlightPossibleMoves();
			} else if (myTurn && possibleMoves.contains(this)) {
				myTurn = false;
				rowSelected = row;
				columnSelected = column;
				pieceID = 0;
				waiting = false;
				selectedPieceLocation.getStyleClass().remove("potentialmover");
				unhighlight();
				txtStatus.setText("Other player's turn");
			}
		}
				
	}

}
