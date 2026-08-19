package game;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

	private Game game;

	private Board board;

	private Label turnLabel;

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
		 * Bottom controls.
		 */
		VBox controls = new VBox(
				5,
				turnLabel,
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

		if (game.isGameOver()) {

			Player winner = game.getWinningPlayer();

			String winnerText;

			String backgroundColor;

			if (winner == Player.PLAYER_ONE) {

				winnerText = "PLAYER 1 WINS!";

				backgroundColor = "#1C32FF";

			} else {

				winnerText = "PLAYER 2 WINS!";

				backgroundColor = "#40A056";
			}

			turnLabel.setText(
					winnerText);

			turnLabel.setTextFill(
					javafx.scene.paint.Color.WHITE);

			turnLabel.setStyle(
					"-fx-font-size: 22px;" +
							"-fx-font-weight: bold;" +
							"-fx-padding: 8px 40px;" +
							"-fx-background-color: "
							+ backgroundColor
							+ ";");

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

			playerText = "PLAYER 1'S TURN\n"
					+ "Moves remaining: "
					+ game.getMovesRemaining();

			backgroundColor = "#1C32FF";

		} else {

			playerText = "PLAYER 2'S TURN\n"
					+ "Moves remaining: "
					+ game.getMovesRemaining();

			backgroundColor = "#40A056";
		}

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

		passButton.setDisable(
				game.getMovesRemaining() <= 0);
	}

	public static void main(String[] args) {

		launch(args);
	}
}