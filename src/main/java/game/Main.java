package game;

import game.network.GameClient;
import game.network.GameServer;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class Main extends Application {

	private Game game;

	private Board board;

	private Label turnLabel;

	private Label playerOneTerritoryLabel;

	private Label playerTwoTerritoryLabel;

	private Label networkStatusLabel;

	private Button passButton;

	private Button resetButton;

	private Button hostButton;

	private Button joinButton;

	private TextField hostField;

	private TextField portField;

	private GameServer server;

	private GameClient client;

	/*
	 * True when this copy of the game is being used
	 * for a network game.
	 */
	private boolean networkGame;

	@Override
	public void start(Stage stage) {

		/*
		 * Create game.
		 */
		game = new Game();

		networkGame = false;

		/*
		 * Create UI labels.
		 */
		turnLabel = new Label();

		playerOneTerritoryLabel = new Label();

		playerTwoTerritoryLabel = new Label();

		networkStatusLabel = new Label("Status: Offline");

		/*
		 * Create buttons.
		 */
		passButton = new Button("Pass Move");

		resetButton = new Button("Reset Game");

		hostButton = new Button("Host Game");

		joinButton = new Button("Join Game");

		/*
		 * Host address field.
		 */
		hostField = new TextField("127.0.0.1");

		hostField.setPrefWidth(130);

		/*
		 * Port field.
		 */
		portField = new TextField("5000");

		portField.setPrefWidth(70);

		/*
		 * Create board.
		 */
		board = game.getBoard();

		board.setAlignment(
				Pos.CENTER);

		/*
		 * Register game callback.
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
		 * Host button.
		 */
		hostButton.setOnAction(event -> {

			hostGame();
		});

		/*
		 * Join button.
		 */
		joinButton.setOnAction(event -> {

			joinGame();
		});

		/*
		 * Initial display.
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
		 * Network controls.
		 */
		HBox networkControls = new HBox(
				5,
				hostButton,
				new Label("Host:"),
				hostField,
				new Label("Port:"),
				portField,
				joinButton);

		networkControls.setAlignment(
				Pos.CENTER);

		/*
		 * Bottom controls.
		 */
		VBox controls = new VBox(
				7,
				networkStatusLabel,
				networkControls,
				territoryDisplay,
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

		/*
		 * Allow board to use available space.
		 */
		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		/*
		 * Window.
		 */
		Scene scene = new Scene(
				root,
				700,
				760);

		stage.setTitle(
				"Chess-Like Game");

		stage.setScene(scene);

		stage.setResizable(true);

		stage.setOnCloseRequest(event -> {

			/*
			 * Stop the server if this computer is hosting.
			 */
			if (server != null) {

				server.stop();

				server = null;
			}

			/*
			 * Disconnect from the host if this computer
			 * is the client.
			 */
			if (client != null) {

				client.disconnect();

				client = null;
			}
		});

		stage.show();
	}

	private void showError(
			String title,
			String message) {

		Alert alert = new Alert(
				Alert.AlertType.ERROR,
				message,
				ButtonType.OK);

		alert.setTitle(title);

		alert.setHeaderText(null);

		alert.showAndWait();
	}

	/*
	 * Starts hosting a network game.
	 */
	private void hostGame() {

		/*
		 * Don't allow multiple network connections.
		 */
		if (server != null
				|| client != null) {

			return;
		}

		int port;

		try {

			port = Integer.parseInt(
					portField.getText().trim());

		} catch (NumberFormatException e) {

			showError(
					"Invalid Port",
					"Please enter a valid port number.");

			return;
		}

		/*
		 * Port must be in the valid TCP range.
		 */
		if (port < 1 || port > 65535) {

			showError(
					"Invalid Port",
					"Port must be between 1 and 65535.");

			return;
		}

		try {

			/*
			 * This copy is now being used for
			 * network play.
			 */
			networkGame = true;

			/*
			 * Create server.
			 */
			server = new GameServer(game);

			/*
			 * The host controls Player 1.
			 *
			 * Do not set this until the server is
			 * actually connected, otherwise Player 1
			 * could move before Player 2 joins.
			 */
			game.setLocalPlayer(
					Player.PLAYER_TWO);

			/*
			 * Send successful local moves to
			 * the connected client.
			 */
			game.setMoveMade(
					move -> {

						if (server != null
								&& server.isConnected()) {

							server.sendMove(move);
						}
					});

			/*
			 * Handle connection changes.
			 */
			server.setConnectionChanged(
					() -> {

						if (server != null
								&& server.isConnected()) {

							/*
							 * Host now controls Player 1.
							 */
							game.setLocalPlayer(
									Player.PLAYER_ONE);

							networkStatusLabel.setText(
									"Status: Player 2 connected");

							hostButton.setDisable(true);
							joinButton.setDisable(true);

							hostField.setDisable(true);
							portField.setDisable(true);

							updateDisplay();

						} else {

							networkStatusLabel.setText(
									"Status: Player 2 disconnected");

							updateDisplay();
						}
					});

			/*
			 * Start server.
			 */
			server.start(port);

			networkStatusLabel.setText(
					"Status: Hosting - waiting for Player 2...");

			/*
			 * Disable network setup controls while
			 * waiting for a connection.
			 */
			hostButton.setDisable(true);
			joinButton.setDisable(true);

			hostField.setDisable(true);
			portField.setDisable(true);

			updateDisplay();

		} catch (IOException e) {

			server = null;

			networkGame = false;

			/*
			 * Restore normal offline control.
			 */
			game.setLocalPlayer(
					Player.PLAYER_ONE);

			showError(
					"Unable to Host Game",
					e.getMessage());
		}
	}

	/*
	 * Joins an existing network game.
	 */
	private void joinGame() {

		String host = hostField.getText().trim();

		if (host.isEmpty()) {

			showError(
					"Invalid Host",
					"Please enter the host IP address.");

			return;
		}

		int port;

		try {

			port = Integer.parseInt(
					portField.getText().trim());

		} catch (NumberFormatException e) {

			showError(
					"Invalid Port",
					"Please enter a valid port number.");

			return;
		}

		if (port < 1 || port > 65535) {

			showError(
					"Invalid Port",
					"Port must be between 1 and 65535.");

			return;
		}

		try {

			/*
			 * This copy is now being used for
			 * network play.
			 */
			networkGame = true;

			/*
			 * Create client.
			 */
			client = new GameClient(game);

			/*
			 * While waiting for the connection,
			 * temporarily make Player 2 the local
			 * player so Player 1 cannot make the
			 * opening move before the connection.
			 */
			game.setLocalPlayer(
					Player.PLAYER_TWO);

			/*
			 * Send successful local moves to
			 * the host.
			 */
			game.setMoveMade(
					move -> {

						if (client != null
								&& client.isConnected()) {

							client.sendMove(move);
						}
					});

			/*
			 * Handle connection changes.
			 */
			client.setConnectionChanged(
					() -> {

						if (client != null
								&& client.isConnected()) {

							/*
							 * Client controls Player 2.
							 */
							game.setLocalPlayer(
									Player.PLAYER_TWO);

							networkStatusLabel.setText(
									"Status: Connected to Player 1");

							hostButton.setDisable(true);
							joinButton.setDisable(true);

							hostField.setDisable(true);
							portField.setDisable(true);

							updateDisplay();

						} else {

							networkStatusLabel.setText(
									"Status: Disconnected");

							updateDisplay();
						}
					});

			/*
			 * Show connection status before
			 * attempting the connection.
			 */
			networkStatusLabel.setText(
					"Status: Connecting...");

			updateDisplay();

			/*
			 * Connect to the host.
			 */
			client.connect(
					host,
					port);

		} catch (IOException e) {

			client = null;

			networkGame = false;

			/*
			 * Restore normal offline control.
			 */
			game.setLocalPlayer(
					Player.PLAYER_ONE);

			networkStatusLabel.setText(
					"Status: Offline");

			showError(
					"Unable to Join Game",
					e.getMessage());

			updateDisplay();
		}
	}

	/*
	 * Stops all network connections.
	 */
	private void stopNetworking() {

		if (server != null) {

			server.stop();

			server = null;
		}

		if (client != null) {

			client.disconnect();

			client = null;
		}

		networkGame = false;

		/*
		 * Return this copy of the game to
		 * normal offline Player 1 control.
		 */
		if (game != null) {

			game.setLocalPlayer(
					Player.PLAYER_ONE);
		}
	}

	/*
	 * Resets the game.
	 */
	private void resetGame() {

		/*
		 * Disconnect from any existing network game.
		 */
		if (server != null) {

			server.stop();

			server = null;
		}

		if (client != null) {

			client.disconnect();

			client = null;
		}

		/*
		 * Create a completely fresh game.
		 */
		game = new Game();

		// ... existing reset code ...

		updateDisplay();

		networkStatusLabel.setText(
				"Status: Not Connected");

		/*
		 * A reset starts a completely new game.
		 *
		 * Stop any existing network connection
		 * first so the old Game object isn't still
		 * connected to another player.
		 */
		stopNetworking();

		/*
		 * Create a completely fresh game.
		 */
		game = new Game();

		networkGame = false;

		/*
		 * Get new board.
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
		 * Update board reference.
		 */
		board = newBoard;

		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		/*
		 * Register callback for new game.
		 */
		registerGameCallback();

		/*
		 * Restore network controls.
		 */
		hostButton.setDisable(false);
		joinButton.setDisable(false);

		hostField.setDisable(false);
		portField.setDisable(false);

		/*
		 * Restore default host address.
		 */
		hostField.setText(
				"127.0.0.1");

		/*
		 * Restore default port.
		 */
		portField.setText(
				"5000");

		networkStatusLabel.setText(
				"Status: Offline");

		/*
		 * Update UI.
		 */
		updateDisplay();
	}

	/*
	 * Registers the callback used by Game to
	 * update the UI.
	 */
	private void registerGameCallback() {

		game.setGameStateChanged(
				this::updateDisplay);
	}

	/*
	 * Determines whether this copy of the game
	 * currently has an active network connection.
	 */
	private boolean isNetworkConnected() {

		return (server != null
				&& server.isConnected())
				|| (client != null
						&& client.isConnected());
	}

	/*
	 * Updates all UI elements.
	 */
	private void updateDisplay() {

		boolean networkGame = server != null
				|| client != null;

		resetButton.setDisable(networkGame);

		/*
		 * Update territory counts.
		 */
		int playerOneCells = game.getBoard()
				.countClaimedCells(
						Player.PLAYER_ONE);

		int playerTwoCells = game.getBoard()
				.countClaimedCells(
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

			int majority = totalCells / 2 + 1;

			if (winner == Player.PLAYER_ONE) {

				winnerText = "PLAYER 1 WINS!";

				backgroundColor = "#1C32FF";

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

				if (playerTwoCells >= majority) {

					winReason = "Territory: "
							+ playerTwoCells
							+ " / "
							+ totalCells;

				} else {

					winReason = "All Player 1 pieces eliminated";
				}
			}

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

		/*
		 * Network waiting state.
		 */
		if (networkGame
				&& !isNetworkConnected()) {

			playerText = "WAITING FOR OPPONENT\n"
					+ "Moves are disabled";

		} else if (game.isLocalPlayersTurn()) {

			playerText = player == Player.PLAYER_ONE
					? "PLAYER 1'S TURN\n"
					: "PLAYER 2'S TURN\n";

		} else {

			playerText = "OPPONENT'S TURN\n";
		}

		playerText += "Moves remaining: "
				+ game.getMovesRemaining();

		turnLabel.setText(
				playerText);

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
		 * The player can only pass if:
		 *
		 * 1. The game isn't over.
		 * 2. It is this computer's turn.
		 * 3. At least one move remains.
		 */
		passButton.setDisable(
				game.isGameOver()
						|| !game.isLocalPlayersTurn()
						|| game.getMovesRemaining() <= 0);
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