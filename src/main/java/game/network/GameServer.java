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

        serverSocket = new ServerSocket(port);

        /*
         * Accept the connection on a background
         * thread so the JavaFX UI doesn't freeze.
         */
        Thread connectionThread = new Thread(
                this::waitForConnection);

        connectionThread.setDaemon(true);

        connectionThread.start();
    }

    /*
     * Waits for the client to connect.
     */
    private void waitForConnection() {

        try {

            socket = serverSocket.accept();

            setupConnection();

            /*
             * Host controls Player 1.
             */
            game.setLocalPlayer(
                    Player.PLAYER_ONE);

            notifyConnectionChanged();

            startListener();

        } catch (IOException e) {

            if (!isServerClosed()) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Sets up the input and output streams.
     */
    private void setupConnection()
            throws IOException {

        reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        writer = new PrintWriter(
                socket.getOutputStream(),
                true);
    }

    /*
     * Starts listening for moves from the client.
     */
    private void startListener() {

        listenerThread = new Thread(
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

            while ((message = reader.readLine()) != null) {

                handleMessage(message);
            }

        } catch (IOException e) {

            /*
             * Only report unexpected errors.
             *
             * If stop() intentionally closed the socket,
             * we don't need a stack trace.
             */

            if (!isClientClosed()) {
                e.printStackTrace();
            }

        } finally {

            /*
             * The client disconnected or the socket
             * was closed.
             */
            closeClientConnection();

            /*
             * Tell Main.java that the LAN connection
             * is no longer active.
             */
            notifyConnectionChanged();
        }
    }

    private boolean isClientClosed() {

        return socket == null
                || socket.isClosed();
    }

    private boolean isServerClosed() {

        return serverSocket == null
                || serverSocket.isClosed();
    }

    private void closeClientConnection() {

        try {

            if (socket != null
                    && !socket.isClosed()) {

                socket.close();
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            socket = null;
            reader = null;
            writer = null;
        }
    }

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
         * -----------------------------------------
         * MOVE
         * -----------------------------------------
         */
        if (message.startsWith("MOVE ")) {

            try {

                Move move = Move.deserialize(message);

                Platform.runLater(() -> {

                    boolean accepted = game.applyRemoteMove(move);

                    if (!accepted) {

                        System.err.println(
                                "Remote move was rejected: "
                                        + move.serialize());
                    }
                });

            } catch (IllegalArgumentException e) {

                System.err.println(
                        "Invalid move received: "
                                + message);
            }

            return;
        }

        /*
         * -----------------------------------------
         * PASS
         * -----------------------------------------
         */
        if (message.equals("PASS")) {

            Platform.runLater(() -> {

                boolean accepted = game.applyRemotePass();

                if (!accepted) {

                    System.err.println(
                            "Remote pass was rejected.");
                }
            });

            return;
        }

        System.err.println(
                "Unknown network message: "
                        + message);
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
     * Sends a pass action to the client.
     */
    public void sendPass() {

        sendMessage("PASS");
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

        /*
         * Close the connected player first.
         */
        closeClientConnection();

        /*
         * Then stop listening for new connections.
         */
        try {

            if (serverSocket != null
                    && !serverSocket.isClosed()) {

                serverSocket.close();
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            serverSocket = null;
        }
    }
}