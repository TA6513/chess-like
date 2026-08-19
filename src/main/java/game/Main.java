package game;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

	private Game game;

	private Board board;

	private Label turnLabel;

	private Label playerOneTerritoryLabel;

	private Label playerTwoTerritoryLabel;

	private Button passButton;

	@Override
	public void start(Stage stage) {

		/*
		 * Create game.
		 */
		game = new Game();

		/*
		 * Create UI.
		 */
		turnLabel = new Label();

		playerOneTerritoryLabel = new Label();

		playerTwoTerritoryLabel = new Label();

		passButton = new Button("Pass Move");

		Button resetButton = new Button("Reset Game");

		/*
		 * Create board.
		 */
		board = game.getBoard();

		board.setAlignment(
				Pos.CENTER);

		/*
		 * Update the UI whenever Game changes.
		 */
		registerGameCallback();

		/*
		 * Pass button.
		 */
		passButton.setOnAction(event -> {

			game.passMove();
		});

		/*
		 * Reset button.
		 */
		resetButton.setOnAction(event -> {

			resetGame();
		});

		/*
		 * Initial UI update.
		 */
		updateDisplay();

		/*
		 * Territory display.
		 */
		HBox territoryDisplay = new HBox(
				25,
				playerOneTerritoryLabel,
				playerTwoTerritoryLabel);

		territoryDisplay.setAlignment(
				Pos.CENTER);

		/*
		 * Bottom controls.
		 */
		VBox controls = new VBox(
				5,
				turnLabel,
				territoryDisplay,
				passButton,
				resetButton);

		controls.setAlignment(
				Pos.CENTER);

		/*
		 * Main layout.
		 */
		VBox root = new VBox(
				8,
				board,
				controls);

		root.setAlignment(
				Pos.CENTER);

		root.setFillWidth(true);

		/*
		 * Allow the board to use all available
		 * vertical space.
		 */
		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		/*
		 * Window.
		 */
		Scene scene = new Scene(
				root,
				600,
				700);

		stage.setTitle(
				"Chess-Like Game");

		stage.setScene(scene);

		stage.setResizable(true);

		stage.show();
	}

	private void resetGame() {

		/*
		 * Create a completely fresh game.
		 */
		game = new Game();

		/*
		 * Get the new board.
		 */
		Board newBoard = game.getBoard();

		newBoard.setAlignment(
				Pos.CENTER);

		/*
		 * Replace the board in the layout.
		 */
		VBox root = (VBox) board.getParent();

		int boardIndex = root.getChildren()
				.indexOf(board);

		root.getChildren()
				.set(
						boardIndex,
						newBoard);

		/*
		 * Update our board reference.
		 */
		board = newBoard;

		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		/*
		 * Register the callback for the new Game.
		 */
		registerGameCallback();

		/*
		 * Update UI.
		 */
		updateDisplay();
	}

	private void registerGameCallback() {

		game.setGameStateChanged(
				this::updateDisplay);
	}

	private void updateDisplay() {

		/*
		 * Update territory counts first.
		 *
		 * This happens during normal turns as well as
		 * after the game ends.
		 */
		int playerOneCells = game.getBoard().countClaimedCells(
				Player.PLAYER_ONE);

		int playerTwoCells = game.getBoard().countClaimedCells(
				Player.PLAYER_TWO);

		int totalCells = game.getBoard().getSize()
				* game.getBoard().getSize();

		/*
		 * Player 1 territory display.
		 */
		playerOneTerritoryLabel.setText(
				"PLAYER 1 CELLS: "
						+ playerOneCells
						+ " / "
						+ totalCells);

		playerOneTerritoryLabel.setTextFill(
				javafx.scene.paint.Color.rgb(
						28,
						50,
						255));

		playerOneTerritoryLabel.setStyle(
				"-fx-font-size: 16px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 4px;");

		/*
		 * Player 2 territory display.
		 */
		playerTwoTerritoryLabel.setText(
				"PLAYER 2 CELLS: "
						+ playerTwoCells
						+ " / "
						+ totalCells);

		playerTwoTerritoryLabel.setTextFill(
				javafx.scene.paint.Color.rgb(
						64,
						160,
						86));

		playerTwoTerritoryLabel.setStyle(
				"-fx-font-size: 16px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 4px;");

		/*
		 * Game over display.
		 */
		if (game.isGameOver()) {

			Player winner = game.getWinner();

			String winnerText;

			String backgroundColor;

			String winReason;

			/*
			 * Number of cells required for a majority.
			 */
			int majority = totalCells / 2 + 1;

			if (winner == Player.PLAYER_ONE) {

				winnerText = "PLAYER 1 WINS!";

				backgroundColor = "#1C32FF";

				/*
				 * Determine why Player 1 won.
				 */
				if (playerOneCells >= majority) {

					winReason = "Territory: "
							+ playerOneCells
							+ " / "
							+ totalCells;

				} else {

					winReason = "All Player 2 pieces eliminated";
				}

			} else {

				winnerText = "PLAYER 2 WINS!";

				backgroundColor = "#40A056";

				/*
				 * Determine why Player 2 won.
				 */
				if (playerTwoCells >= majority) {

					winReason = "Territory: "
							+ playerTwoCells
							+ " / "
							+ totalCells;

				} else {

					winReason = "All Player 1 pieces eliminated";
				}
			}

			/*
			 * Show winner and reason.
			 */
			turnLabel.setText(
					winnerText
							+ "\n"
							+ winReason);

			turnLabel.setTextFill(
					javafx.scene.paint.Color.WHITE);

			turnLabel.setStyle(
					"-fx-font-size: 20px;" +
							"-fx-font-weight: bold;" +
							"-fx-padding: 8px 40px;" +
							"-fx-background-color: "
							+ backgroundColor
							+ ";");

			/*
			 * Disable passing after the game ends.
			 */
			passButton.setDisable(true);

			return;
		}

		/*
		 * Normal turn display.
		 */
		Player player = game.getCurrentPlayer();

		String playerText;
		String backgroundColor;

		if (player == Player.PLAYER_ONE) {

			backgroundColor = "#1C32FF";

		} else {

			backgroundColor = "#40A056";
		}

		if (game.isLocalPlayersTurn()) {

			playerText = player == Player.PLAYER_ONE
					? "PLAYER 1'S TURN\n"
					: "PLAYER 2'S TURN\n";

		} else {

			playerText = "OPPONENT'S TURN\n";
		}

		playerText += "Moves remaining: "
				+ game.getMovesRemaining();

		turnLabel.setText(playerText);

		turnLabel.setTextFill(
				javafx.scene.paint.Color.WHITE);

		turnLabel.setStyle(
				"-fx-font-size: 18px;" +
						"-fx-font-weight: bold;" +
						"-fx-padding: 8px 40px;" +
						"-fx-background-color: "
						+ backgroundColor
						+ ";");

		/*
		 * Enable/disable pass button.
		 */
		passButton.setDisable(
				game.getMovesRemaining() <= 0);
	}

	public boolean isGameOver() {

		return game.isGameOver();
	}

	public Player getWinner() {

		return game.getWinner();
	}

	public int getPlayerOneClaimedCells() {

		return board.countClaimedCells(
				Player.PLAYER_ONE);
	}

	public int getPlayerTwoClaimedCells() {

		return board.countClaimedCells(
				Player.PLAYER_TWO);
	}

	public static void main(String[] args) {

		launch(args);
	}
}