package game;

import game.network.GameClient;
import game.network.GameServer;
import game.network.OnlineGameClient;

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

public class Main extends Application {

	private boolean leavingNetworkGame;

	private boolean gameStarted;

	/*
	 * -----------------------------------------
	 * NETWORK MODE
	 * -----------------------------------------
	 */

	private enum NetworkMode {

		OFFLINE,
		LAN_HOST,
		LAN_CLIENT,
		ONLINE
	}

	/*
	 * -----------------------------------------
	 * GAME
	 * -----------------------------------------
	 */

	private Game game;

	private Board board;

	/*
	 * -----------------------------------------
	 * GAME UI
	 * -----------------------------------------
	 */

	private Label turnLabel;

	private Label playerOneTerritoryLabel;

	private Label playerTwoTerritoryLabel;

	private Label networkStatusLabel;

	private Label roomCodeLabel;

	private Button passButton;

	private Button resetButton;

	private Button leaveGameButton;

	private Button startOfflineButton;

	private VBox gameControls;

	private VBox setupControls;

	/*
	 * -----------------------------------------
	 * NETWORK UI
	 * -----------------------------------------
	 */

	private Button hostLanButton;

	private Button joinLanButton;

	private Button createOnlineRoomButton;

	private Button joinOnlineRoomButton;

	private TextField hostField;

	private TextField portField;

	private TextField roomCodeField;

	/*
	 * -----------------------------------------
	 * NETWORK CONNECTIONS
	 * -----------------------------------------
	 */

	private GameServer server;

	private GameClient client;

	private OnlineGameClient onlineClient;

	private NetworkMode networkMode = NetworkMode.OFFLINE;

	/*
	 * -----------------------------------------
	 * START
	 * -----------------------------------------
	 */

	@Override
	public void start(
			Stage stage) {

		game = new Game();

		leavingNetworkGame = false;

		gameStarted = false;

		/*
		 * Labels.
		 */
		turnLabel = new Label();

		playerOneTerritoryLabel = new Label();

		playerTwoTerritoryLabel = new Label();

		networkStatusLabel = new Label(
				"Status: Offline");

		roomCodeLabel = new Label(
				"Room: -");

		/*
		 * Game buttons.
		 */
		startOfflineButton = new Button(
				"Start Offline Game");

		passButton = new Button(
				"Pass Move");

		resetButton = new Button(
				"Reset Game");

		leaveGameButton = new Button(
				"Leave Game");

		/*
		 * LAN buttons.
		 */
		hostLanButton = new Button(
				"Host LAN Game");

		joinLanButton = new Button(
				"Join LAN Game");

		/*
		 * Online buttons.
		 */
		createOnlineRoomButton = new Button(
				"Create Online Room");

		joinOnlineRoomButton = new Button(
				"Join Online Room");

		/*
		 * Server/host address.
		 */
		hostField = new TextField(
				"127.0.0.1");

		hostField.setPrefWidth(
				160);

		hostField.setPromptText(
				"Server / LAN Host");

		/*
		 * Port.
		 */
		portField = new TextField(
				"5000");

		portField.setPrefWidth(
				70);

		/*
		 * Online room code.
		 */
		roomCodeField = new TextField();

		roomCodeField.setPrefWidth(
				100);

		roomCodeField.setPromptText(
				"Room Code");

		/*
		 * Board.
		 */
		board = game.getBoard();

		board.setAlignment(
				Pos.CENTER);

		/*
		 * No match has started yet.
		 */
		setBoardActive(false);

		registerGameCallback();

		/*
		 * -------------------------------------
		 * BUTTON ACTIONS
		 * -------------------------------------
		 */

		passButton.setOnAction(
				event -> {

					game.requestPass();
				});

		resetButton.setOnAction(
				event -> {

					resetGame();
				});

		leaveGameButton.setOnAction(event -> {

			leaveNetworkGame();
		});

		startOfflineButton.setOnAction(
				event -> {

					startOfflineGame();
				});

		hostLanButton.setOnAction(
				event -> {

					hostLanGame();
				});

		joinLanButton.setOnAction(
				event -> {

					joinLanGame();
				});

		createOnlineRoomButton.setOnAction(
				event -> {

					createOnlineRoom();
				});

		joinOnlineRoomButton.setOnAction(
				event -> {

					joinOnlineRoom();
				});

		/*
		 * -------------------------------------
		 * LAYOUT
		 * -------------------------------------
		 */

		HBox territoryDisplay = new HBox(
				25,
				playerOneTerritoryLabel,
				playerTwoTerritoryLabel);

		territoryDisplay.setAlignment(
				Pos.CENTER);

		HBox serverFields = new HBox(
				5,
				new Label("Host:"),
				hostField,
				new Label("Port:"),
				portField);

		serverFields.setAlignment(
				Pos.CENTER);

		HBox modeControls = new HBox(
				8,
				startOfflineButton,
				hostLanButton,
				joinLanButton);

		modeControls.setAlignment(
				Pos.CENTER);

		HBox lanControls = new HBox(
				8,
				startOfflineButton,
				hostLanButton,
				joinLanButton);

		lanControls.setAlignment(
				Pos.CENTER);

		HBox onlineControls = new HBox(
				8,
				createOnlineRoomButton,
				roomCodeField,
				joinOnlineRoomButton);

		onlineControls.setAlignment(
				Pos.CENTER);

		setupControls = new VBox(
				7,
				serverFields,
				lanControls,
				onlineControls);

		setupControls.setAlignment(
				Pos.CENTER);

		gameControls = new VBox(
				7,
				territoryDisplay,
				turnLabel,
				passButton,
				leaveGameButton,
				resetButton);

		gameControls.setAlignment(
				Pos.CENTER);

		setGameControlsVisible(false);

		setSetupControlsVisible(true);

		VBox controls = new VBox(
				7,
				networkStatusLabel,
				roomCodeLabel,
				setupControls,
				gameControls);

		controls.setAlignment(
				Pos.CENTER);

		controls.setAlignment(
				Pos.CENTER);

		VBox root = new VBox(
				8,
				board,
				controls);

		root.setAlignment(
				Pos.CENTER);

		root.setFillWidth(
				true);

		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		/*
		 * Window.
		 */
		Scene scene = new Scene(
				root,
				740,
				800);

		stage.setTitle(
				"Chess-Like Game");

		stage.setScene(
				scene);

		stage.setResizable(
				true);

		stage.setOnCloseRequest(
				event -> {

					stopNetworking();
				});

		updateDisplay();

		stage.show();
	}

	private void setGameControlsVisible(
			boolean visible) {

		gameControls.setVisible(
				visible);

		gameControls.setManaged(
				visible);
	}

	private void setSetupControlsVisible(
			boolean visible) {

		setupControls.setVisible(
				visible);

		setupControls.setManaged(
				visible);
	}

	private void setBoardActive(
			boolean active) {

		if (board == null) {
			return;
		}

		/*
		 * managed=false removes the hidden board from
		 * layout calculations, so it does not leave a
		 * large blank space.
		 */
		board.setManaged(active);

		board.setVisible(active);

		/*
		 * Also explicitly disable interaction.
		 */
		board.setDisable(!active);
	}

	private void startOfflineGame() {

		if (networkMode != NetworkMode.OFFLINE) {
			return;
		}

		gameStarted = true;

		game.setNetworkReady(true);

		game.setRestrictToLocalPlayer(false);

		game.setAuthoritativeOnlineMode(
				false);

		setBoardActive(true);

		setGameControlsVisible(true);

		setSetupControlsVisible(false);

		networkStatusLabel.setText(
				"Status: Offline Game");

		roomCodeLabel.setText(
				"Room: -");

		disableConnectionControls();

		updateDisplay();
	}

	/*
	 * =========================================
	 * LAN HOST
	 * =========================================
	 */

	private void hostLanGame() {

		game.setAuthoritativeOnlineMode(
				false);

		if (networkMode != NetworkMode.OFFLINE) {

			return;
		}

		Integer port = readPort();

		if (port == null) {
			return;
		}

		try {

			networkMode = NetworkMode.LAN_HOST;

			game.setNetworkReady(
					false);

			game.setRestrictToLocalPlayer(true);

			game.setLocalPlayer(
					Player.PLAYER_ONE);

			server = new GameServer(game);

			/*
			 * Local move → LAN.
			 */
			game.setMoveMade(
					move -> {

						if (server != null
								&& server.isConnected()) {

							server.sendMove(
									move);
						}
					});

			/*
			 * Local pass → LAN.
			 */
			game.setPassMade(
					() -> {

						if (server != null
								&& server.isConnected()) {

							server.sendPass();
						}
					});

			server.setConnectionChanged(
					() -> {

						if (leavingNetworkGame) {
							return;
						}

						if (server != null
								&& server.isConnected()) {

							game.setNetworkReady(true);

							gameStarted = true;

							setBoardActive(true);

							setGameControlsVisible(true);

							setSetupControlsVisible(false);

							networkStatusLabel.setText(
									"Status: LAN Player 2 connected");
						} else {

							game.setNetworkReady(
									false);

							if (gameStarted) {

								/*
								 * The match had already started,
								 * so this is an opponent disconnect.
								 *
								 * Preserve the final board position.
								 */
								board.setManaged(true);
								board.setVisible(true);
								board.setDisable(true);

								networkStatusLabel.setText(
										"Status: LAN opponent disconnected");

							} else {

								/*
								 * Nobody ever connected.
								 * Keep the board hidden.
								 */
								setBoardActive(false);

								networkStatusLabel.setText(
										"Status: Waiting for LAN player...");
							}
						}

						updateDisplay();
					});

			server.start(
					port);

			gameStarted = false;

			setBoardActive(false);

			networkStatusLabel.setText(
					"Status: Hosting LAN - waiting for Player 2");

			roomCodeLabel.setText(
					"Room: LAN");

			disableConnectionControls();

			updateDisplay();

		} catch (IOException e) {

			server = null;

			networkMode = NetworkMode.OFFLINE;

			game.setNetworkReady(
					true);

			enableConnectionControls();

			showError(
					"Unable to Host LAN Game",
					e.getMessage());
		}
	}

	/*
	 * =========================================
	 * LAN JOIN
	 * =========================================
	 */

	private void joinLanGame() {

		game.setAuthoritativeOnlineMode(
				false);

		if (networkMode != NetworkMode.OFFLINE) {

			return;
		}

		String host = hostField
				.getText()
				.trim();

		if (host.isEmpty()) {

			showError(
					"Invalid Host",
					"Enter the LAN host IP address.");

			return;
		}

		Integer port = readPort();

		if (port == null) {
			return;
		}

		try {

			networkMode = NetworkMode.LAN_CLIENT;

			game.setNetworkReady(
					false);

			client = new GameClient(game);

			game.setMoveMade(
					move -> {

						if (client != null
								&& client.isConnected()) {

							client.sendMove(
									move);
						}
					});

			game.setPassMade(
					() -> {

						if (client != null
								&& client.isConnected()) {

							client.sendPass();
						}
					});

			client.setConnectionChanged(
					() -> {

						if (leavingNetworkGame) {
							return;
						}

						if (client != null
								&& client.isConnected()) {

							game.setNetworkReady(
									true);

							gameStarted = true;

							setBoardActive(true);

							setGameControlsVisible(true);

							setSetupControlsVisible(false);

							networkStatusLabel.setText(
									"Status: Connected to LAN host");

						} else {

							game.setNetworkReady(
									false);

							if (gameStarted) {

								/*
								 * Host disconnected after the
								 * match had already started.
								 */
								board.setManaged(true);
								board.setVisible(true);
								board.setDisable(true);

								networkStatusLabel.setText(
										"Status: LAN host disconnected");

							} else {

								/*
								 * Connection never successfully
								 * started a match.
								 */
								setBoardActive(false);

								networkStatusLabel.setText(
										"Status: LAN connection failed");
							}
						}

						updateDisplay();
					});

			networkStatusLabel.setText(
					"Status: Connecting to LAN host...");

			roomCodeLabel.setText(
					"Room: LAN");

			disableConnectionControls();

			client.connect(
					host,
					port);

			game.setRestrictToLocalPlayer(true);

			game.setLocalPlayer(
					Player.PLAYER_TWO);

			updateDisplay();

		} catch (IOException e) {

			client = null;

			networkMode = NetworkMode.OFFLINE;

			game.setNetworkReady(
					true);

			enableConnectionControls();

			networkStatusLabel.setText(
					"Status: Offline");

			roomCodeLabel.setText(
					"Room: -");

			showError(
					"Unable to Join LAN Game",
					e.getMessage());

			updateDisplay();
		}
	}

	/*
	 * =========================================
	 * CREATE ONLINE ROOM
	 * =========================================
	 */

	private void createOnlineRoom() {

		if (networkMode != NetworkMode.OFFLINE) {

			return;
		}

		String host = readHost();

		if (host == null) {
			return;
		}

		Integer port = readPort();

		if (port == null) {
			return;
		}

		try {

			prepareOnlineClient();

			networkStatusLabel.setText(
					"Status: Connecting to dedicated server...");

			disableConnectionControls();

			onlineClient.connect(
					host,
					port);

			/*
			 * Request a brand-new room after
			 * TCP connection succeeds.
			 */
			onlineClient.createRoom();

		} catch (IOException e) {

			onlineConnectionFailed(
					e);
		}
	}

	/*
	 * =========================================
	 * JOIN ONLINE ROOM
	 * =========================================
	 */

	private void joinOnlineRoom() {

		if (networkMode != NetworkMode.OFFLINE) {

			return;
		}

		String host = readHost();

		if (host == null) {
			return;
		}

		Integer port = readPort();

		if (port == null) {
			return;
		}

		String roomCode = roomCodeField
				.getText()
				.trim()
				.toUpperCase();

		if (roomCode.isEmpty()) {

			showError(
					"Room Code Required",
					"Enter the room code created by Player 1.");

			return;
		}

		try {

			prepareOnlineClient();

			networkStatusLabel.setText(
					"Status: Connecting to dedicated server...");

			disableConnectionControls();

			onlineClient.connect(
					host,
					port);

			onlineClient.joinRoom(
					roomCode);

		} catch (IOException e) {

			onlineConnectionFailed(
					e);
		}
	}

	private void leaveNetworkGame() {

		if (!gameStarted
				&& networkMode == NetworkMode.OFFLINE) {

			return;
		}

		/*
		 * Prevent disconnect callbacks from treating this
		 * intentional leave as a network failure.
		 */
		leavingNetworkGame = true;

		/*
		 * Close whichever connection is currently active.
		 */
		stopNetworking();

		/*
		 * Create a fresh offline game.
		 */
		game = new Game();

		Board newBoard = game.getBoard();

		gameStarted = false;

		setBoardActive(false);

		setGameControlsVisible(false);

		setSetupControlsVisible(true);

		enableConnectionControls();

		newBoard.setAlignment(
				Pos.CENTER);

		VBox root = (VBox) board.getParent();

		int boardIndex = root.getChildren()
				.indexOf(board);

		if (boardIndex < 0) {

			leavingNetworkGame = false;

			throw new IllegalStateException(
					"Could not find the board in the main layout.");
		}

		root.getChildren()
				.set(
						boardIndex,
						newBoard);

		board = newBoard;

		gameStarted = false;

		setBoardActive(false);

		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		registerGameCallback();

		enableConnectionControls();

		networkStatusLabel.setText(
				"Status: Offline");

		roomCodeLabel.setText(
				"Room: -");

		roomCodeField.clear();

		leavingNetworkGame = false;

		updateDisplay();
	}

	/*
	 * =========================================
	 * PREPARE ONLINE CLIENT
	 * =========================================
	 */

	private void prepareOnlineClient() {

		gameStarted = false;

		setBoardActive(false);

		game.setAuthoritativeOnlineMode(
				true);

		game.setRestrictToLocalPlayer(true);

		networkMode = NetworkMode.ONLINE;

		/*
		 * Nobody can play until START arrives.
		 */
		game.setNetworkReady(
				false);

		onlineClient = new OnlineGameClient(
				game);

		/*
		 * Online moves are requests.
		 *
		 * The board does not change until the server
		 * broadcasts APPLY_MOVE.
		 */
		game.setMoveRequested(
				move -> {

					if (onlineClient != null
							&& onlineClient.isConnected()) {

						onlineClient.sendMoveRequest(
								move);
					}
				});

		/*
		 * Same behavior for Pass.
		 */
		game.setPassRequested(
				() -> {

					if (onlineClient != null
							&& onlineClient.isConnected()) {

						onlineClient.sendPassRequest();
					}
				});

		/*
		 * -------------------------------------
		 * TCP CONNECTION
		 * -------------------------------------
		 */

		onlineClient.setConnectionChanged(
				() -> {

					/*
					 * Ignore disconnect notifications caused by
					 * the user intentionally leaving the match.
					 */
					if (leavingNetworkGame) {
						return;
					}

					if (onlineClient != null
							&& onlineClient.isConnected()) {

						networkStatusLabel.setText(
								"Status: Connected to dedicated server");

					} else {

						game.setNetworkReady(
								false);

						networkStatusLabel.setText(
								"Status: Dedicated server disconnected");

						if (networkMode == NetworkMode.ONLINE) {

							showError(
									"Connection Lost",
									"The connection to the dedicated server was lost.\n\n"
											+ "Use Leave Game to return to offline mode.");
						}
					}

					updateDisplay();
				});

		/*
		 * -------------------------------------
		 * ROOM CREATED
		 * -------------------------------------
		 */

		onlineClient.setRoomCreated(
				roomCode -> {

					roomCodeLabel.setText(
							"Room: " + roomCode);

					roomCodeField.setText(
							roomCode);

					networkStatusLabel.setText(
							"Status: Room created - waiting for Player 2");

					/*
					 * Still waiting for START.
					 */
					setBoardActive(false);

					updateDisplay();
				});

		/*
		 * -------------------------------------
		 * ROOM JOINED
		 * -------------------------------------
		 */

		onlineClient.setRoomJoined(
				roomCode -> {

					roomCodeLabel.setText(
							"Room: " + roomCode);

					roomCodeField.setText(
							roomCode);

					networkStatusLabel.setText(
							"Status: Joined room - starting game...");

					setBoardActive(false);

					updateDisplay();
				});

		/*
		 * -------------------------------------
		 * START
		 * -------------------------------------
		 */

		onlineClient.setGameStarted(
				() -> {

					game.setNetworkReady(true);

					gameStarted = true;

					setBoardActive(true);

					setGameControlsVisible(true);

					setSetupControlsVisible(false);

					networkStatusLabel.setText(
							"Status: Online game started");

					updateDisplay();
				});

		onlineClient.setActionRejected(
				reason -> {

					networkStatusLabel.setText(
							"Status: Action rejected");

					showError(
							"Move Rejected",
							"The server rejected that action: "
									+ reason);

					updateDisplay();
				});

		/*
		 * -------------------------------------
		 * ROOM ERRORS
		 * -------------------------------------
		 */

		onlineClient.setRoomNotFound(
				() -> {

					showError(
							"Room Not Found",
							"No room exists with that code.");

					returnToOffline();
				});

		onlineClient.setRoomFull(
				() -> {

					showError(
							"Room Full",
							"That room already has two players.");

					returnToOffline();
				});

		onlineClient.setRoomUnavailable(
				() -> {

					showError(
							"Room Unavailable",
							"That room has already started or finished.");

					returnToOffline();
				});

		/*
		 * -------------------------------------
		 * OPPONENT DISCONNECTED
		 * -------------------------------------
		 */

		onlineClient.setOpponentDisconnected(
				() -> {

					if (leavingNetworkGame) {
						return;
					}

					game.setNetworkReady(
							false);

					gameStarted = true;

					setGameControlsVisible(true);

					board.setManaged(true);
					board.setVisible(true);
					board.setDisable(true);

					networkStatusLabel.setText(
							"Status: Opponent left the game");

					updateDisplay();

					showError(
							"Opponent Left",
							"The other player has left the game.\n\n"
									+ "Use Leave Game to return to offline mode.");
				});

		/*
		 * -------------------------------------
		 * ROOM FINISHED
		 * -------------------------------------
		 */

		onlineClient.setRoomFinished(
				() -> {

					networkStatusLabel.setText(
							"Status: Match finished");

					updateDisplay();
				});
	}

	/*
	 * =========================================
	 * ONLINE FAILURE
	 * =========================================
	 */

	private void onlineConnectionFailed(
			IOException e) {

		if (onlineClient != null) {

			onlineClient.disconnect();

			onlineClient = null;
		}

		networkMode = NetworkMode.OFFLINE;

		game.setNetworkReady(
				true);

		game.setLocalPlayer(
				Player.PLAYER_ONE);

		game.setMoveMade(
				null);

		game.setPassMade(
				null);

		enableConnectionControls();

		networkStatusLabel.setText(
				"Status: Offline");

		roomCodeLabel.setText(
				"Room: -");

		showError(
				"Online Connection Failed",
				e.getMessage());

		updateDisplay();
	}

	/*
	 * =========================================
	 * RETURN TO OFFLINE
	 * =========================================
	 */

	private void returnToOffline() {

		if (onlineClient != null) {

			onlineClient.disconnect();

			onlineClient = null;
		}

		networkMode = NetworkMode.OFFLINE;

		game.setNetworkReady(
				true);

		game.setLocalPlayer(
				Player.PLAYER_ONE);

		game.setMoveMade(
				null);

		game.setPassMade(
				null);

		enableConnectionControls();

		networkStatusLabel.setText(
				"Status: Offline");

		roomCodeLabel.setText(
				"Room: -");

		updateDisplay();
	}

	/*
	 * =========================================
	 * NETWORK UTILITIES
	 * =========================================
	 */

	private String readHost() {

		String host = hostField
				.getText()
				.trim();

		if (host.isEmpty()) {

			showError(
					"Invalid Host",
					"Enter the server address.");

			return null;
		}

		return host;
	}

	private Integer readPort() {

		try {

			int port = Integer.parseInt(
					portField
							.getText()
							.trim());

			if (port < 1
					|| port > 65535) {

				showError(
						"Invalid Port",
						"Port must be between 1 and 65535.");

				return null;
			}

			return port;

		} catch (NumberFormatException e) {

			showError(
					"Invalid Port",
					"Enter a valid port number.");

			return null;
		}
	}

	private void disableConnectionControls() {

		startOfflineButton.setDisable(true);

		hostLanButton.setDisable(true);

		joinLanButton.setDisable(true);

		createOnlineRoomButton.setDisable(true);

		joinOnlineRoomButton.setDisable(true);

		hostField.setDisable(true);

		portField.setDisable(true);

		roomCodeField.setDisable(true);
	}

	private void enableConnectionControls() {

		startOfflineButton.setDisable(false);

		hostLanButton.setDisable(false);

		joinLanButton.setDisable(false);

		createOnlineRoomButton.setDisable(false);

		joinOnlineRoomButton.setDisable(false);

		hostField.setDisable(false);

		portField.setDisable(false);

		roomCodeField.setDisable(false);
	}

	/*
	 * =========================================
	 * STOP NETWORKING
	 * =========================================
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

		if (onlineClient != null) {

			onlineClient.disconnect();

			onlineClient = null;
		}

		networkMode = NetworkMode.OFFLINE;

		if (game != null) {

			game.setNetworkReady(
					true);

			game.setRestrictToLocalPlayer(false);

			game.setLocalPlayer(
					Player.PLAYER_ONE);

			game.setMoveMade(
					null);

			game.setPassMade(
					null);

			game.setAuthoritativeOnlineMode(
					false);

			game.setMoveRequested(
					null);

			game.setPassRequested(
					null);
		}
	}

	/*
	 * =========================================
	 * RESET
	 * =========================================
	 */

	private void resetGame() {

		/*
		 * Reset is only allowed during an active
		 * offline game.
		 */
		if (networkMode != NetworkMode.OFFLINE
				|| !gameStarted) {

			showError(
					"Reset Unavailable",
					"Only an active offline game can be reset.");

			return;
		}

		/*
		 * Create a completely fresh offline game.
		 */
		game = new Game();

		/*
		 * Offline mode controls both players.
		 */
		game.setRestrictToLocalPlayer(false);

		game.setNetworkReady(true);

		game.setAuthoritativeOnlineMode(false);

		/*
		 * Get the new board.
		 */
		Board newBoard = game.getBoard();

		newBoard.setAlignment(
				Pos.CENTER);

		/*
		 * Replace the old board in the layout.
		 */
		VBox root = (VBox) board.getParent();

		int boardIndex = root.getChildren()
				.indexOf(board);

		root.getChildren()
				.set(
						boardIndex,
						newBoard);

		/*
		 * Update the board reference.
		 */
		board = newBoard;

		VBox.setVgrow(
				board,
				Priority.ALWAYS);

		/*
		 * Register the callback on the new Game object.
		 */
		registerGameCallback();

		/*
		 * We are still inside an offline game.
		 */
		gameStarted = true;

		setBoardActive(true);

		setGameControlsVisible(true);

		setSetupControlsVisible(false);

		/*
		 * Keep the game-status display appropriate
		 * for offline play.
		 */
		networkStatusLabel.setText(
				"Status: Offline Game");

		roomCodeLabel.setText(
				"Room: -");

		/*
		 * Update the new game's UI.
		 */
		updateDisplay();
	}

	/*
	 * =========================================
	 * GAME CALLBACK
	 * =========================================
	 */

	private void registerGameCallback() {

		game.setGameStateChanged(
				this::updateDisplay);
	}

	/*
	 * =========================================
	 * DISPLAY
	 * =========================================
	 */

	private void updateDisplay() {

		int playerOneCells = game.getBoard()
				.countClaimedCells(
						Player.PLAYER_ONE);

		int playerTwoCells = game.getBoard()
				.countClaimedCells(
						Player.PLAYER_TWO);

		int totalCells = game.getBoard().getSize()
				* game.getBoard().getSize();

		/*
		 * -------------------------------------
		 * TERRITORY COUNTERS
		 * -------------------------------------
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
		 * -------------------------------------
		 * GAME OVER
		 * -------------------------------------
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

			passButton.setDisable(
					true);

			return;
		}

		/*
		 * -------------------------------------
		 * NORMAL TURN DISPLAY
		 * -------------------------------------
		 */

		Player player = game.getCurrentPlayer();

		String backgroundColor;

		if (player == Player.PLAYER_ONE) {

			backgroundColor = "#1C32FF";

		} else {

			backgroundColor = "#40A056";
		}

		String playerText;

		/*
		 * Network game waiting for opponent.
		 */
		if (networkMode != NetworkMode.OFFLINE
				&& !game.isNetworkReady()) {

			/*
			 * A network game may either be waiting to start
			 * or may have stopped because someone disconnected.
			 */
			if (networkStatusLabel.getText()
					.contains("Opponent left")) {

				playerText = "OPPONENT LEFT\n"
						+ "Game paused";

			} else if (networkStatusLabel.getText()
					.contains("disconnected")) {

				playerText = "CONNECTION LOST\n"
						+ "Game paused";

			} else {

				playerText = "WAITING FOR OPPONENT\n";
			}

		} else if (game.isLocalPlayersTurn()) {

			if (player == Player.PLAYER_ONE) {

				playerText = "PLAYER 1'S TURN\n";

			} else {

				playerText = "PLAYER 2'S TURN\n";
			}

		} else {

			playerText = "OPPONENT'S TURN\n";
		}

		if (game.isNetworkReady()
				|| networkMode == NetworkMode.OFFLINE) {

			playerText += "Moves remaining: "
					+ game.getMovesRemaining();
		}

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
		 * No game action buttons should be usable until
		 * a match has actually started.
		 */
		boolean canPass = gameStarted
				&& game.isNetworkReady()
				&& game.isLocalPlayersTurn()
				&& game.getMovesRemaining() > 0;

		passButton.setDisable(
				!canPass);

		/*
		 * Reset is only safe offline.
		 */
		resetButton.setDisable(
				!gameStarted
						|| networkMode != NetworkMode.OFFLINE);

		/*
		 * Leave Game only makes sense during
		 * a network game.
		 */
		leaveGameButton.setDisable(
				!gameStarted
						&& networkMode == NetworkMode.OFFLINE);
	}

	/*
	 * =========================================
	 * ERROR
	 * =========================================
	 */

	private void showError(
			String title,
			String message) {

		if (message == null
				|| message.isBlank()) {

			message = "An unknown error occurred.";
		}

		Alert alert = new Alert(
				Alert.AlertType.ERROR,
				message,
				ButtonType.OK);

		alert.setTitle(
				title);

		alert.setHeaderText(
				null);

		alert.showAndWait();
	}

	/*
	 * =========================================
	 * EXISTING GETTERS
	 * =========================================
	 */

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

	/*
	 * =========================================
	 * MAIN
	 * =========================================
	 */

	public static void main(
			String[] args) {

		launch(args);
	}
}