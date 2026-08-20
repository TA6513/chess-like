package game.network;

import game.Game;
import game.Move;
import game.Player;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.function.Consumer;

public class OnlineGameClient {

	private final Game game;

	private Socket socket;

	private BufferedReader reader;

	private PrintWriter writer;

	private Thread listenerThread;

	/*
	 * General connection callback.
	 */
	private Runnable connectionChanged;

	/*
	 * Both players are connected and gameplay
	 * may begin.
	 */
	private Runnable gameStarted;

	/*
	 * Opponent disconnected during a game.
	 */
	private Runnable opponentDisconnected;

	/*
	 * Room callbacks.
	 */
	private Consumer<String> roomCreated;

	private Consumer<String> roomJoined;

	private Consumer<String> actionRejected;

	private Consumer<String> gameOverReceived;

	private Runnable roomNotFound;

	private Runnable roomFull;

	private Runnable roomUnavailable;

	private Runnable roomFinished;

	private boolean disconnecting;

	public OnlineGameClient(
			Game game) {

		if (game == null) {

			throw new IllegalArgumentException(
					"Game cannot be null.");
		}

		this.game = game;

		disconnecting = false;
	}

	/*
	 * -----------------------------------------
	 * CONNECT
	 * -----------------------------------------
	 */

	public void connect(
			String host,
			int port)
			throws IOException {

		if (socket != null
				&& !socket.isClosed()) {

			throw new IllegalStateException(
					"Online client is already connected.");
		}

		disconnecting = false;

		socket = new Socket(
				host,
				port);

		reader = new BufferedReader(
				new InputStreamReader(
						socket.getInputStream()));

		writer = new PrintWriter(
				socket.getOutputStream(),
				true);

		startListener();

		notifyConnectionChanged();
	}

	/*
	 * -----------------------------------------
	 * CREATE / JOIN ROOM
	 * -----------------------------------------
	 */

	public void createRoom() {

		sendMessage(
				"CREATE_ROOM");
	}

	public void joinRoom(
			String roomCode) {

		if (roomCode == null
				|| roomCode.isBlank()) {

			throw new IllegalArgumentException(
					"Room code cannot be empty.");
		}

		sendMessage(
				"JOIN_ROOM "
						+ roomCode
								.trim()
								.toUpperCase());
	}

	/*
	 * -----------------------------------------
	 * GAME MESSAGES
	 * -----------------------------------------
	 */

	public void sendMoveRequest(
			Move move) {

		if (move == null) {
			return;
		}

		sendMessage(
				"REQUEST_MOVE "
						+ move.getSourceRow()
						+ " "
						+ move.getSourceColumn()
						+ " "
						+ move.getDestinationRow()
						+ " "
						+ move.getDestinationColumn());
	}

	public void sendPassRequest() {

		sendMessage(
				"REQUEST_PASS");
	}

	public void sendGameOver() {

		sendMessage(
				"GAME_OVER");
	}

	/*
	 * -----------------------------------------
	 * LISTENER
	 * -----------------------------------------
	 */

	private void startListener() {

		listenerThread = new Thread(
				this::listenForMessages);

		listenerThread.setDaemon(
				true);

		listenerThread.start();
	}

	private void listenForMessages() {

		try {

			String message;

			while ((message = reader.readLine()) != null) {

				handleMessage(
						message.trim());
			}

		} catch (IOException e) {

			if (!disconnecting) {

				System.err.println(
						"Dedicated server connection lost: "
								+ e.getMessage());
			}

		} finally {

			/*
			 * If the socket died unexpectedly,
			 * update the UI.
			 */
			if (!disconnecting) {

				Platform.runLater(
						this::notifyConnectionChanged);
			}
		}
	}

	/*
	 * -----------------------------------------
	 * MESSAGE HANDLING
	 * -----------------------------------------
	 */

	private void handleMessage(
			String message) {

		if (message == null
				|| message.isBlank()) {

			return;
		}

		/*
		 * -------------------------------------
		 * PLAYER ASSIGNMENT
		 * -------------------------------------
		 */

		if (message.equals(
				"PLAYER 1")) {

			Platform.runLater(() -> {

				game.setLocalPlayer(
						Player.PLAYER_ONE);
			});

			return;
		}

		if (message.equals(
				"PLAYER 2")) {

			Platform.runLater(() -> {

				game.setLocalPlayer(
						Player.PLAYER_TWO);
			});

			return;
		}

		/*
		 * -------------------------------------
		 * ROOM CREATED
		 * -------------------------------------
		 */

		if (message.startsWith(
				"ROOM_CREATED ")) {

			String roomCode = message.substring(
					"ROOM_CREATED ".length())
					.trim();

			Platform.runLater(() -> {

				if (roomCreated != null) {

					roomCreated.accept(
							roomCode);
				}
			});

			return;
		}

		/*
		 * -------------------------------------
		 * ROOM JOINED
		 * -------------------------------------
		 */

		if (message.startsWith(
				"ROOM_JOINED ")) {

			String roomCode = message.substring(
					"ROOM_JOINED ".length())
					.trim();

			Platform.runLater(() -> {

				if (roomJoined != null) {

					roomJoined.accept(
							roomCode);
				}
			});

			return;
		}

		/*
		 * -------------------------------------
		 * ROOM ERRORS
		 * -------------------------------------
		 */

		if (message.equals(
				"ROOM_NOT_FOUND")) {

			runLater(
					roomNotFound);

			return;
		}

		if (message.equals(
				"ROOM_FULL")) {

			runLater(
					roomFull);

			return;
		}

		if (message.equals(
				"ROOM_UNAVAILABLE")) {

			runLater(
					roomUnavailable);

			return;
		}

		/*
		 * -------------------------------------
		 * START
		 * -------------------------------------
		 */

		if (message.equals(
				"START")) {

			runLater(
					gameStarted);

			return;
		}

		/*
		 * -------------------------------------
		 * REMOTE MOVE
		 * -------------------------------------
		 */

		/*
		 * Server-approved move.
		 *
		 * APPLY_MOVE 10 0 9 0
		 */
		if (message.startsWith(
				"APPLY_MOVE ")) {

			try {

				/*
				 * Move.deserialize expects a message
				 * beginning with MOVE.
				 */
				String moveMessage = "MOVE "
						+ message.substring(
								"APPLY_MOVE ".length());

				Move move = Move.deserialize(
						moveMessage);

				Platform.runLater(() -> {

					boolean accepted = game.applyAuthoritativeMove(
							move);

					if (!accepted) {

						System.err.println(
								"Authoritative move could "
										+ "not be applied locally: "
										+ move);
					}
				});

			} catch (IllegalArgumentException e) {

				System.err.println(
						"Invalid APPLY_MOVE message: "
								+ message);
			}

			return;
		}

		/*
		 * -------------------------------------
		 * REMOTE PASS
		 * -------------------------------------
		 */

		if (message.equals(
				"APPLY_PASS")) {

			Platform.runLater(() -> {

				boolean accepted = game.applyAuthoritativePass();

				if (!accepted) {

					System.err.println(
							"Authoritative PASS could not "
									+ "be applied locally.");
				}
			});

			return;
		}

		/*
		 * -------------------------------------
		 * OPPONENT DISCONNECTED
		 * -------------------------------------
		 */

		if (message.equals(
				"OPPONENT_DISCONNECTED")) {

			runLater(
					opponentDisconnected);

			return;
		}

		/*
		 * -------------------------------------
		 * ROOM FINISHED
		 * -------------------------------------
		 */

		if (message.equals(
				"ROOM_FINISHED")) {

			runLater(
					roomFinished);

			return;
		}

		if (message.startsWith(
				"ACTION_REJECTED ")) {

			String reason = message.substring(
					"ACTION_REJECTED ".length())
					.trim();

			Platform.runLater(() -> {

				if (actionRejected != null) {

					actionRejected.accept(
							reason);
				}
			});

			return;
		}

		if (message.startsWith(
				"GAME_OVER ")) {

			String result = message.substring(
					"GAME_OVER ".length())
					.trim();

			Platform.runLater(() -> {

				if (gameOverReceived != null) {

					gameOverReceived.accept(
							result);
				}
			});

			return;
		}

		/*
		 * These are currently informational
		 * server rejections.
		 */
		if (message.equals(
				"GAME_NOT_ACTIVE")) {

			System.err.println(
					"Server rejected action: "
							+ "game is not active.");

			return;
		}

		if (message.equals(
				"NOT_IN_ROOM")) {

			System.err.println(
					"Server rejected action: "
							+ "client is not in a room.");

			return;
		}

		if (message.equals(
				"ALREADY_IN_ROOM")) {

			System.err.println(
					"Server rejected room request: "
							+ "already in a room.");

			return;
		}

		if (message.equals(
				"INVALID_ROOM")) {

			System.err.println(
					"Server rejected invalid room request.");

			return;
		}

		if (message.equals(
				"OPPONENT_JOINED")) {

			/*
			 * START will follow, so no special
			 * action is currently necessary.
			 */
			return;
		}

		System.err.println(
				"Unknown server message: "
						+ message);
	}

	/*
	 * -----------------------------------------
	 * SEND
	 * -----------------------------------------
	 */

	private synchronized void sendMessage(
			String message) {

		if (writer == null
				|| !isConnected()) {

			return;
		}

		writer.println(
				message);
	}

	/*
	 * -----------------------------------------
	 * CALLBACK HELPERS
	 * -----------------------------------------
	 */

	private void runLater(
			Runnable callback) {

		if (callback != null) {

			Platform.runLater(
					callback);
		}
	}

	private void notifyConnectionChanged() {

		if (connectionChanged != null) {

			connectionChanged.run();
		}
	}

	/*
	 * -----------------------------------------
	 * CALLBACK SETTERS
	 * -----------------------------------------
	 */

	public void setConnectionChanged(
			Runnable callback) {

		connectionChanged = callback;
	}

	public void setActionRejected(
			Consumer<String> callback) {

		actionRejected = callback;
	}

	public void setGameOverReceived(
			Consumer<String> callback) {

		gameOverReceived = callback;
	}

	public void setGameStarted(
			Runnable callback) {

		gameStarted = callback;
	}

	public void setOpponentDisconnected(
			Runnable callback) {

		opponentDisconnected = callback;
	}

	public void setRoomCreated(
			Consumer<String> callback) {

		roomCreated = callback;
	}

	public void setRoomJoined(
			Consumer<String> callback) {

		roomJoined = callback;
	}

	public void setRoomNotFound(
			Runnable callback) {

		roomNotFound = callback;
	}

	public void setRoomFull(
			Runnable callback) {

		roomFull = callback;
	}

	public void setRoomUnavailable(
			Runnable callback) {

		roomUnavailable = callback;
	}

	public void setRoomFinished(
			Runnable callback) {

		roomFinished = callback;
	}

	/*
	 * -----------------------------------------
	 * CONNECTION STATE
	 * -----------------------------------------
	 */

	public boolean isConnected() {

		return socket != null
				&& socket.isConnected()
				&& !socket.isClosed();
	}

	/*
	 * -----------------------------------------
	 * DISCONNECT
	 * -----------------------------------------
	 */

	public void disconnect() {

		disconnecting = true;

		try {

			if (socket != null) {

				socket.close();
			}

		} catch (IOException ignored) {

		} finally {

			socket = null;

			reader = null;

			writer = null;
		}
	}
}