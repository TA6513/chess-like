package game.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.security.SecureRandom;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import game.Player;
import game.Move;

public class DedicatedGameServer {

    private static final int DEFAULT_PORT = 5000;

    /*
     * Characters intentionally exclude easily confused
     * characters such as O, 0, I, and 1.
     */
    private static final String ROOM_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int ROOM_CODE_LENGTH = 6;

    /*
     * Every active room on the dedicated server.
     */
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    /*
     * Number of TCP clients currently connected
     * to this server.
     */
    private final AtomicInteger connectedClients = new AtomicInteger();

    private ServerSocket serverSocket;

    /*
     * -----------------------------------------
     * ROOM STATE
     * -----------------------------------------
     */
    private enum RoomState {

        WAITING,
        PLAYING,
        FINISHED
    }

    /*
     * -----------------------------------------
     * MAIN
     * -----------------------------------------
     */
    public static void main(String[] args) {

        int port = DEFAULT_PORT;

        /*
         * Allow:
         *
         * java -jar server.jar 5000
         */
        if (args.length > 0) {

            try {

                port = Integer.parseInt(
                        args[0]);

            } catch (NumberFormatException e) {

                System.err.println(
                        "Invalid port. Using default port "
                                + DEFAULT_PORT);
            }
        }

        DedicatedGameServer server = new DedicatedGameServer();

        try {

            server.start(
                    port);

        } catch (IOException e) {

            System.err.println(
                    "Dedicated server error: "
                            + e.getMessage());

            e.printStackTrace();
        }
    }

    /*
     * -----------------------------------------
     * SERVER START
     * -----------------------------------------
     */
    public void start(
            int port)
            throws IOException {

        serverSocket = new ServerSocket(
                port);

        System.out.println();

        System.out.println(
                "========================================");

        System.out.println(
                " Chess-Like Game Dedicated Server");

        System.out.println(
                "========================================");

        System.out.println(
                "Listening on port "
                        + port);

        System.out.println();

        logServerStatus();

        /*
         * Accept clients forever.
         *
         * Every client gets its own thread.
         */
        while (!serverSocket.isClosed()) {

            Socket socket = serverSocket.accept();

            System.out.println(
                    "Connection from "
                            + socket.getRemoteSocketAddress());

            try {

                ClientConnection connection = new ClientConnection(
                        socket);

                connectedClients.incrementAndGet();

                connection.start();

                logServerStatus();

            } catch (IOException e) {

                System.err.println(
                        "Could not initialize client: "
                                + e.getMessage());

                socket.close();
            }
        }
    }

    /*
     * -----------------------------------------
     * CREATE ROOM
     * -----------------------------------------
     */
    private void createRoom(
            ClientConnection player) {

        if (player.getRoom() != null) {

            player.send(
                    "ALREADY_IN_ROOM");

            return;
        }

        GameRoom room;

        String roomCode;

        /*
         * putIfAbsent prevents two simultaneous clients
         * from accidentally generating the same code.
         */
        while (true) {

            roomCode = generateRoomCode();

            room = new GameRoom(
                    roomCode);

            if (rooms.putIfAbsent(
                    roomCode,
                    room) == null) {

                break;
            }
        }

        synchronized (room) {

            room.setPlayerOne(
                    player);

            player.setRoom(
                    room);

            /*
             * Tell the creator which room was made
             * and which side they control.
             */
            player.send(
                    "ROOM_CREATED "
                            + roomCode);

            player.send(
                    "PLAYER 1");
        }

        System.out.println(
                "Room "
                        + roomCode
                        + " created. State: WAITING");

        logServerStatus();
    }

    /*
     * -----------------------------------------
     * JOIN ROOM
     * -----------------------------------------
     */
    private void joinRoom(
            ClientConnection player,
            String requestedRoomCode) {

        if (player.getRoom() != null) {

            player.send(
                    "ALREADY_IN_ROOM");

            return;
        }

        String roomCode = requestedRoomCode
                .trim()
                .toUpperCase();

        GameRoom room = rooms.get(
                roomCode);

        if (room == null) {

            player.send(
                    "ROOM_NOT_FOUND");

            return;
        }

        synchronized (room) {

            /*
             * A match that already started or finished
             * cannot accept another player.
             */
            if (room.getState() != RoomState.WAITING) {

                player.send(
                        "ROOM_UNAVAILABLE");

                return;
            }

            if (room.getPlayerTwo() != null) {

                player.send(
                        "ROOM_FULL");

                return;
            }

            room.setPlayerTwo(
                    player);

            player.setRoom(
                    room);

            /*
             * Joining client becomes Player 2.
             */
            player.send(
                    "ROOM_JOINED "
                            + roomCode);

            player.send(
                    "PLAYER 2");

            /*
             * Inform Player 1.
             */
            room.sendToPlayerOne(
                    "OPPONENT_JOINED");

            /*
             * Start the match.
             */
            room.setState(
                    RoomState.PLAYING);

            room.broadcast(
                    "START");

            System.out.println(
                    "Room "
                            + roomCode
                            + " now PLAYING.");
        }

        logServerStatus();
    }

    /*
     * -----------------------------------------
     * ROOM CODE
     * -----------------------------------------
     */
    private String generateRoomCode() {

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {

            int index = random.nextInt(
                    ROOM_CHARACTERS.length());

            code.append(
                    ROOM_CHARACTERS.charAt(
                            index));
        }

        return code.toString();
    }

    /*
     * -----------------------------------------
     * FINISH ROOM
     * -----------------------------------------
     */
    private void finishRoom(
            GameRoom room) {

        if (room == null) {
            return;
        }

        synchronized (room) {

            if (room.getState() == RoomState.FINISHED) {

                return;
            }

            room.setState(
                    RoomState.FINISHED);

            /*
             * Remove it from the public room map.
             *
             * Existing players still have a reference
             * to the room object, but nobody new can join.
             */
            rooms.remove(
                    room.getRoomCode(),
                    room);

            System.out.println(
                    "Room "
                            + room.getRoomCode()
                            + " now FINISHED.");
        }

        logServerStatus();
    }

    /*
     * -----------------------------------------
     * AUTHORITATIVE GAME OVER
     * -----------------------------------------
     */
    private void checkAuthoritativeGameOver(
            GameRoom room) {

        AuthoritativeGame game = room.getGame();

        if (!game.isGameOver()) {
            return;
        }

        String winner;

        if (game.getWinner() == Player.PLAYER_ONE) {

            winner = "PLAYER_ONE";

        } else {

            winner = "PLAYER_TWO";
        }

        room.broadcast(
                "GAME_OVER "
                        + winner
                        + " "
                        + game.getWinReason());

        finishRoom(
                room);
    }

    /*
     * -----------------------------------------
     * SERVER STATUS
     * -----------------------------------------
     */
    private synchronized void logServerStatus() {

        int waitingRooms = 0;
        int playingRooms = 0;

        for (GameRoom room : rooms.values()) {

            if (room.getState() == RoomState.WAITING) {

                waitingRooms++;

            } else if (room.getState() == RoomState.PLAYING) {

                playingRooms++;
            }
        }

        int activeRooms = waitingRooms
                + playingRooms;

        System.out.println(
                "SERVER STATUS"
                        + " | Active rooms: "
                        + activeRooms
                        + " | Waiting: "
                        + waitingRooms
                        + " | Playing: "
                        + playingRooms
                        + " | Connected clients: "
                        + connectedClients.get());
    }

    /*
     * =========================================
     * GAME ROOM
     * =========================================
     */
    private class GameRoom {

        private final String roomCode;

        private RoomState state;

        private ClientConnection playerOne;

        private ClientConnection playerTwo;

        private final AuthoritativeGame game;

        GameRoom(
                String roomCode) {

            this.roomCode = roomCode;

            state = RoomState.WAITING;

            game = new AuthoritativeGame();
        }

        AuthoritativeGame getGame() {

            return game;
        }

        Player getPlayerForConnection(
                ClientConnection connection) {

            if (connection == playerOne) {

                return Player.PLAYER_ONE;
            }

            if (connection == playerTwo) {

                return Player.PLAYER_TWO;
            }

            return null;
        }

        String getRoomCode() {

            return roomCode;
        }

        RoomState getState() {

            return state;
        }

        void setState(
                RoomState state) {

            this.state = state;
        }

        ClientConnection getPlayerTwo() {

            return playerTwo;
        }

        void setPlayerOne(
                ClientConnection playerOne) {

            this.playerOne = playerOne;
        }

        void setPlayerTwo(
                ClientConnection playerTwo) {

            this.playerTwo = playerTwo;
        }

        /*
         * Send a message to both players.
         */
        void broadcast(
                String message) {

            if (playerOne != null) {

                playerOne.send(
                        message);
            }

            if (playerTwo != null) {

                playerTwo.send(
                        message);
            }
        }

        void sendToPlayerOne(
                String message) {

            if (playerOne != null) {

                playerOne.send(
                        message);
            }
        }

        /*
         * Called when somebody disconnects.
         */
        void removePlayer(
                ClientConnection player) {

            synchronized (this) {

                boolean wasPlaying = state == RoomState.PLAYING;

                if (player == playerOne) {

                    playerOne = null;

                } else if (player == playerTwo) {

                    playerTwo = null;
                }

                /*
                 * Any disconnect permanently ends
                 * this particular room.
                 */
                finishRoom(
                        this);

                /*
                 * Notify whichever player remains.
                 */
                if (wasPlaying) {

                    broadcast(
                            "OPPONENT_DISCONNECTED");
                }
            }
        }
    }

    /*
     * =========================================
     * CLIENT CONNECTION
     * =========================================
     */
    private class ClientConnection
            extends Thread {

        private final Socket socket;

        private final BufferedReader reader;

        private final PrintWriter writer;

        private GameRoom room;

        private boolean disconnected;

        ClientConnection(
                Socket socket)
                throws IOException {

            this.socket = socket;

            reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            writer = new PrintWriter(
                    socket.getOutputStream(),
                    true);

            disconnected = false;

            setDaemon(
                    true);
        }

        GameRoom getRoom() {

            return room;
        }

        void setRoom(
                GameRoom room) {

            this.room = room;
        }

        @Override
        public void run() {

            try {

                String message;

                while ((message = reader.readLine()) != null) {

                    handleMessage(
                            message.trim());
                }

            } catch (IOException e) {

                if (!disconnected) {

                    System.out.println(
                            "Client connection lost: "
                                    + socket.getRemoteSocketAddress());
                }

            } finally {

                disconnect();
            }
        }

        /*
         * -------------------------------------
         * MESSAGE HANDLING
         * -------------------------------------
         */
        private void handleMessage(
                String message) {

            if (message == null
                    || message.isBlank()) {

                return;
            }

            /*
             * ---------------------------------
             * CREATE ROOM
             * ---------------------------------
             */
            if (message.equals(
                    "CREATE_ROOM")) {

                createRoom(
                        this);

                return;
            }

            /*
             * ---------------------------------
             * JOIN ROOM
             *
             * JOIN_ROOM ABC123
             * ---------------------------------
             */
            if (message.startsWith(
                    "JOIN_ROOM ")) {

                String[] parts = message.split(
                        "\\s+");

                if (parts.length != 2) {

                    send(
                            "INVALID_ROOM");

                    return;
                }

                joinRoom(
                        this,
                        parts[1]);

                return;
            }

            /*
             * Everything below requires a room.
             */
            if (room == null) {

                send(
                        "NOT_IN_ROOM");

                return;
            }

            /*
             * Game actions require a PLAYING room.
             */
            if (room.getState() != RoomState.PLAYING) {

                send(
                        "GAME_NOT_ACTIVE");

                return;
            }

            /*
             * ---------------------------------
             * MOVE
             * ---------------------------------
             */
            if (message.startsWith(
                    "REQUEST_MOVE ")) {

                String moveText = "MOVE "
                        + message.substring(
                                "REQUEST_MOVE ".length());

                try {

                    Move move = Move.deserialize(
                            moveText);

                    Player player = room.getPlayerForConnection(
                            this);

                    AuthoritativeGame.Result result = room.getGame()
                            .applyMove(
                                    player,
                                    move);

                    if (result == AuthoritativeGame.Result.ACCEPTED) {

                        /*
                         * Tell BOTH clients to apply exactly
                         * the validated move.
                         */
                        room.broadcast(
                                "APPLY_"
                                        + move.serialize());

                        /*
                         * Server independently determines
                         * whether this ended the game.
                         */
                        checkAuthoritativeGameOver(
                                room);

                    } else {

                        send(
                                "ACTION_REJECTED "
                                        + result.name());
                    }

                } catch (IllegalArgumentException e) {

                    send(
                            "ACTION_REJECTED INVALID_MOVE");
                }

                return;
            }

            /*
             * ---------------------------------
             * PASS
             * ---------------------------------
             */
            if (message.equals(
                    "REQUEST_PASS")) {

                Player player = room.getPlayerForConnection(
                        this);

                AuthoritativeGame.Result result = room.getGame()
                        .applyPass(
                                player);

                if (result == AuthoritativeGame.Result.ACCEPTED) {

                    room.broadcast(
                            "APPLY_PASS");

                } else {

                    send(
                            "ACTION_REJECTED "
                                    + result.name());
                }

                return;
            }

            /*
             * ---------------------------------
             * LEGACY GAME OVER
             * ---------------------------------
             *
             * The authoritative game now normally
             * determines game-over state itself.
             *
             * This remains for compatibility with
             * older clients.
             */
            if (message.equals(
                    "GAME_OVER")) {

                finishRoom(
                        room);

                room.broadcast(
                        "ROOM_FINISHED");

                return;
            }

            System.err.println(
                    "Unknown message from "
                            + socket.getRemoteSocketAddress()
                            + ": "
                            + message);
        }

        /*
         * Send a line to this client.
         */
        synchronized void send(
                String message) {

            if (!socket.isClosed()) {

                writer.println(
                        message);
            }
        }

        /*
         * -------------------------------------
         * DISCONNECT
         * -------------------------------------
         */
        private void disconnect() {

            if (disconnected) {
                return;
            }

            disconnected = true;

            GameRoom oldRoom = room;

            room = null;

            if (oldRoom != null) {

                oldRoom.removePlayer(
                        this);
            }

            try {

                socket.close();

            } catch (IOException ignored) {
            }

            connectedClients.decrementAndGet();

            System.out.println(
                    "Client disconnected.");

            logServerStatus();
        }
    }
}