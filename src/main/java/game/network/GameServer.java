package game.network;

import game.Game;
import game.Move;
import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.application.Platform;

public class GameServer {

    private final Game game;

    private ServerSocket serverSocket;
    private Socket socket;

    private BufferedReader reader;
    private PrintWriter writer;

    private Thread listenerThread;

    private Runnable connectionChanged;

    public GameServer(Game game) {

        if (game == null) {
            throw new IllegalArgumentException(
                    "Game cannot be null.");
        }

        this.game = game;
    }

    /*
     * Starts the server and waits for one player
     * to connect.
     */
    public void start(int port) throws IOException {

        if (serverSocket != null) {
            throw new IllegalStateException(
                    "Server is already running.");
        }

        serverSocket =
                new ServerSocket(port);

        /*
         * Accept the connection on a background
         * thread so the JavaFX UI doesn't freeze.
         */
        Thread connectionThread =
                new Thread(
                        this::waitForConnection);

        connectionThread.setDaemon(true);

        connectionThread.start();
    }

    /*
     * Waits for the client to connect.
     */
    private void waitForConnection() {

        try {

            socket =
                    serverSocket.accept();

            setupConnection();

            /*
             * Host controls Player 1.
             */
            game.setLocalPlayer(
                    Player.PLAYER_ONE);

            notifyConnectionChanged();

            startListener();

        } catch (IOException e) {

            if (!isClosed()) {

                e.printStackTrace();
            }
        }
    }

    /*
     * Sets up the input and output streams.
     */
    private void setupConnection()
            throws IOException {

        reader =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));

        writer =
                new PrintWriter(
                        socket.getOutputStream(),
                        true);
    }

    /*
     * Starts listening for moves from the client.
     */
    private void startListener() {

        listenerThread =
                new Thread(
                        this::listenForMessages);

        listenerThread.setDaemon(true);

        listenerThread.start();
    }

    /*
     * Continuously listens for messages from
     * the other player.
     */
    private void listenForMessages() {

        try {

            String message;

            while ((message =
                    reader.readLine()) != null) {

                handleMessage(message);
            }

        } catch (IOException e) {

            if (!isClosed()) {

                e.printStackTrace();
            }
        }
    }

    /*
     * Handles a message received from the client.
     */
    private void handleMessage(
            String message) {

        if (message == null) {
            return;
        }

        message = message.trim();

        if (message.isEmpty()) {
            return;
        }

        /*
         * Currently the only network message is
         * a MOVE message.
         */
        if (message.startsWith("MOVE ")) {

            try {

                Move move =
                        Move.deserialize(message);

                /*
                 * JavaFX/Game state should eventually
                 * be handled on the JavaFX application
                 * thread.
                 *
                 * For now Game itself contains the
                 * game logic.
                 */
                javafx.application.Platform.runLater(
                        () -> {

                            boolean accepted =
                                    game.applyRemoteMove(
                                            move);

                            if (accepted) {

                                /*
                                 * Send an acknowledgement
                                 * back to the client.
                                 */
                                sendMessage("OK");
                            }
                        });

            } catch (IllegalArgumentException e) {

                System.err.println(
                        "Invalid move received: "
                                + message);
            }
        }
    }

    /*
     * Sends a Move to the client.
     */
    public void sendMove(Move move) {

        if (move == null) {
            return;
        }

        sendMessage(
                move.serialize());
    }

    /*
     * Sends a raw message.
     */
    private void sendMessage(
            String message) {

        if (writer == null) {
            return;
        }

        writer.println(message);
    }

    /*
     * Returns whether a client is connected.
     */
    public boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    /*
     * Registers a callback for connection changes.
     */
    public void setConnectionChanged(
            Runnable callback) {

        connectionChanged = callback;
    }

    private void notifyConnectionChanged() {

        if (connectionChanged != null) {

            javafx.application.Platform.runLater(
                    connectionChanged);
        }
    }

    /*
     * Stops the server.
     */
    public void stop() {

        try {

            if (socket != null) {
                socket.close();
            }

            if (serverSocket != null) {
                serverSocket.close();
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            socket = null;
            serverSocket = null;
            reader = null;
            writer = null;
        }
    }

    private boolean isClosed() {

        return serverSocket == null
                || serverSocket.isClosed();
    }
}