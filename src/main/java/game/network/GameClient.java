package game.network;

import game.Game;
import game.Move;
import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Platform;

public class GameClient {

    private final Game game;

    private Socket socket;

    private BufferedReader reader;
    private PrintWriter writer;

    private Thread listenerThread;

    private Runnable connectionChanged;

    public GameClient(Game game) {

        if (game == null) {
            throw new IllegalArgumentException(
                    "Game cannot be null.");
        }

        this.game = game;
    }

    /*
     * Connects to the host.
     */
    public void connect(
            String host,
            int port) throws IOException {

        if (socket != null
                && !socket.isClosed()) {

            throw new IllegalStateException(
                    "Client is already connected.");
        }

        socket = new Socket(
                host,
                port);

        setupConnection();

        /*
         * Client controls Player 2.
         */
        game.setLocalPlayer(
                Player.PLAYER_TWO);

        notifyConnectionChanged();

        startListener();
    }

    /*
     * Sets up input and output streams.
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
     * Starts listening for moves from the host.
     */
    private void startListener() {

        listenerThread = new Thread(
                this::listenForMessages);

        listenerThread.setDaemon(true);

        listenerThread.start();
    }

    /*
     * Continuously listens for messages.
     */
    private void listenForMessages() {

        try {

            String message;

            while ((message = reader.readLine()) != null) {

                handleMessage(message);
            }

        } catch (IOException e) {

            if (!isClosed()) {

                e.printStackTrace();
            }
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
         * Handle a move received from Player 1.
         */
        if (message.startsWith("MOVE ")) {

            try {

                Move move = Move.deserialize(message);

                /*
                 * Apply the remote move on the
                 * JavaFX application thread.
                 */
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
        }
    }

    /*
     * Sends a Move to the host.
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
     * Returns whether the client is connected.
     */
    public boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    /*
     * Registers a connection callback.
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
     * Disconnects from the host.
     */
    public void disconnect() {

        try {

            if (socket != null) {
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

    private boolean isClosed() {

        return socket == null
                || socket.isClosed();
    }
}