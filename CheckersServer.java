
/*
 * Author:Braylen Strain
 * Date:04/09/2022
 * 
 * This program allows two clients to play a super dumbed down version of checkers due to being short on time.
 */
package application;
	
import java.net.*;
import java.util.*;
import java.io.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;


public class CheckersServer extends Application implements CheckersConstants {
	private int sessionNo = 1; //Numbers a session
	private int gameStatus;
	@Override
	public void start(Stage primaryStage) {
		//Create exit menu item
		MenuBar menuBar = new MenuBar();
		Menu menuExit = new Menu("Exit");
		MenuItem menuItemExit = new MenuItem("Exit program.");
		menuExit.getItems().add(menuItemExit);
		menuBar.getMenus().add(menuExit);
		
		//Keeps a log of the different sessions and their status
		TextArea taLog = new TextArea();
		taLog.setEditable(false);
		
		//Set the scene
		VBox window = new VBox();
		window.setPrefHeight(400);
		window.getChildren().addAll(menuBar, new ScrollPane(taLog));
		Scene scene = new Scene(window,400,200);
		primaryStage.setTitle("Checkers Server");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		//Allow exit that clears the port
		menuItemExit.setOnAction(e -> System.exit(0));
		
		//Start the server
		new Thread( () -> {
			try {
				try (ServerSocket serverSocket = new ServerSocket(8000)) {
					Platform.runLater(() -> taLog.appendText(new Date() + ": Server Started\n"));
					
					//Continuously create new sessions for players
					for (;;) {
						Platform.runLater(() -> taLog.appendText("\tWaiting for Players to join session " + sessionNo + "\n"));
						Socket player1 = serverSocket.accept();
						Platform.runLater(() -> taLog.appendText(new Date() + ": Player 1 joined session " + sessionNo + "\n"));
						new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);
						Socket player2 = serverSocket.accept();
						Platform.runLater(() -> taLog.appendText(new Date() + ": Player 2 joined session " + sessionNo + "\n"));
						new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);
						Platform.runLater(() -> taLog.appendText("\tStart a thread for session " + sessionNo++ + "\n"));
						
						new Thread(new RunASession(player1, player2)).start();
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}).start();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	class RunASession implements Runnable, CheckersConstants {
		private Socket player1;
		private Socket player2;
		
		public RunASession(Socket player1, Socket player2) {
			this.player1 = player1;
			this.player2 = player2;
		}
		
		@Override
		public void run() {
			try {
				//Create input/output streams from/to players
				DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
				DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
				DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
				DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());
				
				toPlayer1.writeInt(1); //Let player one know to go
				
				//Gameplay continues until someone wins
				while (true) {
					//Receive move from player 1
					int oldRow = fromPlayer1.readInt(); //Old position row
					int oldColumn = fromPlayer1.readInt(); //Old position column
					int newRow = fromPlayer1.readInt(); //New position row
					int newColumn = fromPlayer1.readInt(); //New position column
					gameStatus = fromPlayer1.readInt(); //Save game status for after move is sent to player 2
					
					//Send move and status to player 2
					toPlayer2.writeInt(gameStatus);
					toPlayer2.writeInt(oldRow);
					toPlayer2.writeInt(oldColumn);
					toPlayer2.writeInt(newRow);
					toPlayer2.writeInt(newColumn);
					
					//Check for end of game
					if (gameStatus != CONTINUE) break;
					
					//Receive move from player 2
					oldRow = fromPlayer2.readInt(); //Old position row
					oldColumn = fromPlayer2.readInt(); //Old position column
					newRow = fromPlayer2.readInt(); //New position row
					newColumn = fromPlayer2.readInt(); //New position column
					gameStatus = fromPlayer2.readInt();
					
					//Send move and status to player 1
					toPlayer1.writeInt(gameStatus);
					toPlayer1.writeInt(oldRow);
					toPlayer1.writeInt(oldColumn);
					toPlayer1.writeInt(newRow);
					toPlayer1.writeInt(newColumn);
					
					//Check for end of game
					if (gameStatus != CONTINUE) break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}
