package game;

import java.util.ArrayList;
import java.util.List;

import java.util.function.Consumer;

public class Game {

    private final Board board;

    private final List<Piece> playerOnePieces;
    private final List<Piece> playerTwoPieces;
    private final List<Piece> neutralPieces;
    private boolean gameOver;
    private Player winner;

    /*
     * The player controlled by this copy of the game.
     *
     * In network play, this will be either PLAYER_ONE
     * or PLAYER_TWO.
     */
    private Player localPlayer;

    /*
     * The player whose turn it currently is.
     */
    private Player currentPlayer;

    /*
     * Number of actions the current player has remaining.
     */
    private int movesRemaining;

    /*
     * True only during Player 1's opening turn.
     */
    private boolean openingTurn;

    /*
     * Callback used to notify the UI that the game state
     * has changed.
     */
    private Runnable gameStateChanged;

    /*
     * Callback used to notify the networking layer that
     * a local move was successfully completed.
     */
    private Consumer<Move> moveMade;

    public Game() {

        board = new Board();
        gameOver = false;
        winner = null;

        playerOnePieces = new ArrayList<>();
        playerTwoPieces = new ArrayList<>();
        neutralPieces = new ArrayList<>();

        /*
         * Player 1 starts the game.
         */
        currentPlayer = Player.PLAYER_ONE;

        /*
         * Until networking is configured, this copy of the
         * game controls Player 1.
         */
        localPlayer = Player.PLAYER_ONE;

        /*
         * Player 1 gets only one action on the opening turn.
         */
        openingTurn = true;
        movesRemaining = 1;

        /*
         * Give the Board access to this Game.
         */
        board.setGame(this);

        /*
         * Create and place all pieces.
         */
        initializePieces();

        /*
         * Prepare Player 1's pieces for their first turn.
         */
        resetPiecesForTurn();
    }

    /*
     * Creates all pieces and places them on the board.
     */
    private void initializePieces() {

        createPlayerOnePieces();
        createPlayerTwoPieces();
        createNeutralPieces();
    }

    /*
     * Creates Player 1's six pieces.
     *
     * Player 1 starts in the southwest corner.
     */
    private void createPlayerOnePieces() {

        int id = 1;

        int[][] positions = {
                { 10, 0 },
                { 10, 1 },
                { 9, 0 },
                { 10, 2 },
                { 9, 1 },
                { 8, 0 }
        };

        for (int[] position : positions) {

            Piece piece = new Piece(id++, Player.PLAYER_ONE);

            playerOnePieces.add(piece);

            placeStartingPiece(
                    piece,
                    position[0],
                    position[1]);
        }
    }

    /*
     * Creates Player 2's six pieces.
     *
     * Player 2 starts in the northeast corner.
     */
    private void createPlayerTwoPieces() {

        int id = 7;

        int[][] positions = {
                { 0, 10 },
                { 0, 9 },
                { 1, 10 },
                { 0, 8 },
                { 1, 9 },
                { 2, 10 }
        };

        for (int[] position : positions) {

            Piece piece = new Piece(id++, Player.PLAYER_TWO);

            playerTwoPieces.add(piece);

            placeStartingPiece(
                    piece,
                    position[0],
                    position[1]);
        }
    }

    /*
     * Creates the three neutral pieces.
     *
     * Positions:
     *
     * Northwest: (0, 0)
     * Center: (7, 7)
     * Southeast: (14, 14)
     */
    private void createNeutralPieces() {

        int id = 13;

        /*
         * Northwest neutral piece.
         */
        Piece northwest = new Piece(id++, Player.NEUTRAL);

        neutralPieces.add(northwest);

        placePiece(
                northwest,
                0,
                0);

        /*
         * Center neutral piece.
         */
        Piece center = new Piece(id++, Player.NEUTRAL);

        neutralPieces.add(center);

        placePiece(
                center,
                5,
                5);

        /*
         * Southeast neutral piece.
         */
        Piece southeast = new Piece(id++, Player.NEUTRAL);

        neutralPieces.add(southeast);

        placePiece(
                southeast,
                10,
                10);
    }

    private void placeStartingPiece(
            Piece piece,
            int row,
            int column) {

        Cell cell = board.getCell(row, column);

        if (cell == null) {
            throw new IllegalArgumentException(
                    "Invalid board position: ("
                            + row + ", "
                            + column + ")");
        }

        if (cell.isOccupied()) {
            throw new IllegalStateException(
                    "Cell is already occupied: ("
                            + row + ", "
                            + column + ")");
        }

        cell.claim(piece.getOwner());
        cell.setPiece(piece);
    }

    /*
     * Places a piece on a particular board cell.
     */
    private void placePiece(
            Piece piece,
            int row,
            int column) {

        Cell cell = board.getCell(row, column);

        if (cell == null) {

            throw new IllegalArgumentException(
                    "Invalid board position: ("
                            + row + ", "
                            + column + ")");
        }

        if (cell.isOccupied()) {

            throw new IllegalStateException(
                    "Cell is already occupied: ("
                            + row + ", "
                            + column + ")");
        }

        cell.setPiece(piece);
    }

    public void setLocalPlayer(Player player) {

        if (player != Player.PLAYER_ONE
                && player != Player.PLAYER_TWO) {

            throw new IllegalArgumentException(
                    "Local player must be PLAYER_ONE or PLAYER_TWO.");
        }

        localPlayer = player;
    }

    public Player getLocalPlayer() {

        return localPlayer;
    }

    public boolean isLocalPlayersTurn() {

        return currentPlayer == localPlayer;
    }

    /*
     * Determines whether a piece can currently be selected.
     *
     * A piece must:
     *
     * 1. Exist
     * 2. Belong to the current player
     * 3. Not have already moved this turn
     */
    public boolean canMovePiece(Piece piece) {

        return piece != null
                && piece.belongsTo(currentPlayer)
                && !piece.hasMovedThisTurn();
    }

    /*
     * Called after a player successfully performs an action.
     *
     * This includes:
     *
     * - Moving to an empty cell
     * - Capturing an opposing piece
     * - Capturing a neutral piece
     */
    public void useMove() {

        if (gameOver) {
            return;
        }

        movesRemaining--;

        /*
         * The player still has another action.
         *
         * Do NOT end the turn yet.
         */
        if (movesRemaining > 0) {

            notifyGameStateChanged();

            return;
        }

        /*
         * No actions remain.
         * Switch to the other player.
         */
        endTurn();
    }

    /*
     * Allows the player to voluntarily give up one action.
     */
    public void passMove() {

        if (gameOver) {
            return;
        }

        if (movesRemaining <= 0) {
            return;
        }

        /*
         * Nothing to pass if there are no actions remaining.
         */
        if (movesRemaining <= 0) {
            return;
        }

        movesRemaining--;

        /*
         * If this was the final action,
         * switch players.
         */
        if (movesRemaining == 0) {

            endTurn();

            return;
        }

        /*
         * The player still has another action.
         */
        notifyGameStateChanged();
    }

    /*
     * Ends the current player's turn and starts
     * the other player's turn.
     */
    private void endTurn() {

        /*
         * Switch players.
         */
        if (currentPlayer == Player.PLAYER_ONE) {

            currentPlayer = Player.PLAYER_TWO;

        } else {

            currentPlayer = Player.PLAYER_ONE;
        }

        /*
         * Player 1's special one-action opening turn
         * is now over.
         */
        openingTurn = false;

        /*
         * Every normal turn has two actions.
         */
        movesRemaining = 2;

        /*
         * Allow the new player's pieces to move.
         */
        resetPiecesForTurn();

        /*
         * Update the UI.
         */
        notifyGameStateChanged();
    }

    /*
     * Resets the moved-this-turn status of all pieces
     * belonging to the player whose turn is starting.
     */
    private void resetPiecesForTurn() {

        for (Piece piece : getAllPieces()) {

            if (piece.belongsTo(currentPlayer)) {

                piece.resetTurn();
            }
        }
    }

    /*
     * Returns a list containing every piece in the game.
     */
    private List<Piece> getAllPieces() {

        List<Piece> allPieces = new ArrayList<>();

        allPieces.addAll(playerOnePieces);
        allPieces.addAll(playerTwoPieces);
        allPieces.addAll(neutralPieces);

        return allPieces;
    }

    /*
     * Registers the callback used by Main.java to update
     * the turn/move display.
     */
    public void setGameStateChanged(
            Runnable callback) {

        this.gameStateChanged = callback;
    }

    /*
     * Registers the callback used when this game makes
     * a local move.
     */
    public void setMoveMade(
            Consumer<Move> callback) {

        this.moveMade = callback;
    }

    /*
     * Tells the UI that the game state has changed.
     */
    private void notifyGameStateChanged() {

        if (gameStateChanged != null) {

            gameStateChanged.run();
        }
    }

    /*
     * Notifies the networking layer that a local move
     * was successfully completed.
     */
    private void notifyMoveMade(Move move) {

        if (moveMade != null) {

            moveMade.accept(move);
        }
    }

    /*
     * Returns the game board.
     */
    public Board getBoard() {

        return board;
    }

    /*
     * Returns the player whose turn it is.
     */
    public Player getCurrentPlayer() {

        return currentPlayer;
    }

    /*
     * Returns the number of actions remaining.
     */
    public int getMovesRemaining() {

        return movesRemaining;
    }

    /*
     * Returns whether this is the opening turn.
     */
    public boolean isOpeningTurn() {

        return openingTurn;
    }

    /*
     * Returns Player 1's pieces.
     */
    public List<Piece> getPlayerOnePieces() {

        return playerOnePieces;
    }

    /*
     * Returns Player 2's pieces.
     */
    public List<Piece> getPlayerTwoPieces() {

        return playerTwoPieces;
    }

    /*
     * Returns the neutral pieces.
     */
    public List<Piece> getNeutralPieces() {

        return neutralPieces;
    }

    /*
     * Handles selecting a piece and determining its valid moves.
     */
    public void highlightValidMoves(Cell source) {

        clearHighlights();

        if (source == null ||
                source.getPiece() == null) {

            return;
        }

        Piece piece = source.getPiece();

        /*
         * The piece must belong to the current player
         * and must not have moved already this turn.
         */
        if (!canMovePiece(piece)) {

            return;
        }

        int row = source.getRow();
        int column = source.getColumn();

        /*
         * Pieces can currently move to any adjacent
         * square, including diagonals.
         */
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {

            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {

                /*
                 * Don't consider the current square.
                 */
                if (rowOffset == 0 &&
                        columnOffset == 0) {

                    continue;
                }

                int newRow = row + rowOffset;

                int newColumn = column + columnOffset;

                Cell destination = board.getCell(
                        newRow,
                        newColumn);

                if (destination == null) {
                    continue;
                }

                if (isValidMove(
                        source,
                        destination)) {

                    destination.setHighlighted(true);
                }
            }
        }
    }

    public boolean isValidMove(
            Cell source,
            Cell destination) {

        if (source == null ||
                destination == null ||
                source.getPiece() == null) {

            return false;
        }

        Piece piece = source.getPiece();

        if (!canMovePiece(piece)) {
            return false;
        }

        int rowDifference = Math.abs(
                destination.getRow()
                        - source.getRow());

        int columnDifference = Math.abs(
                destination.getColumn()
                        - source.getColumn());

        /*
         * Must move exactly one square.
         */
        if (rowDifference > 1 ||
                columnDifference > 1 ||
                (rowDifference == 0 &&
                        columnDifference == 0)) {

            return false;
        }

        Piece target = destination.getPiece();

        /*
         * Cannot move onto an allied piece.
         */
        if (target != null &&
                target.belongsTo(currentPlayer)) {

            return false;
        }

        return true;
    }

    private boolean applyMove(
            Cell source,
            Cell destination) {

        if (gameOver) {
            return false;
        }

        if (!isValidMove(source, destination)) {
            return false;
        }

        Piece movingPiece = source.getPiece();

        Piece targetPiece = destination.getPiece();

        /*
         * Capture neutral piece.
         */
        if (targetPiece != null
                && targetPiece.wasOriginallyNeutral()) {

            targetPiece.capture(currentPlayer);

            destination.claim(currentPlayer);

            movingPiece.setMovedThisTurn(true);

            checkGameOver();

            if (!gameOver) {
                useMove();
            }

            refreshBoard();

            notifyGameStateChanged();

            return true;
        }

        /*
         * Capture enemy piece.
         */
        if (targetPiece != null) {

            removePiece(targetPiece);
        }

        /*
         * Move attacking piece.
         */
        destination.setPiece(
                movingPiece);

        destination.claim(
                movingPiece.getOwner());

        source.setPiece(null);

        movingPiece.setMovedThisTurn(true);

        checkGameOver();

        if (!gameOver) {
            useMove();
        }

        refreshBoard();

        notifyGameStateChanged();

        return true;
    }

    public boolean movePiece(
            int sourceRow,
            int sourceColumn,
            int destinationRow,
            int destinationColumn) {

        Cell source = board.getCell(
                sourceRow,
                sourceColumn);

        Cell destination = board.getCell(
                destinationRow,
                destinationColumn);

        if (source == null || destination == null) {
            return false;
        }

        return movePiece(
                source,
                destination);
    }

    public boolean movePiece(Move move) {

        if (move == null) {
            return false;
        }

        boolean success = movePiece(
                move.getSourceRow(),
                move.getSourceColumn(),
                move.getDestinationRow(),
                move.getDestinationColumn());

        /*
         * Only notify the networking layer if the move
         * was actually accepted.
         */
        if (success) {

            notifyMoveMade(move);
        }

        return success;
    }

    public boolean movePiece(
            Cell source,
            Cell destination) {

        if (gameOver) {
            return false;
        }

        /*
         * Local moves must belong to this computer.
         */
        if (!isLocalPlayersTurn()) {
            return false;
        }

        return applyMove(source, destination);
    }

    public boolean applyRemoteMove(Move move) {

        if (move == null) {
            return false;
        }

        /*
         * A remote move should only arrive while the
         * opponent is the current player.
         */
        if (isLocalPlayersTurn()) {
            return false;
        }

        Cell source = board.getCell(
                move.getSourceRow(),
                move.getSourceColumn());

        Cell destination = board.getCell(
                move.getDestinationRow(),
                move.getDestinationColumn());

        if (source == null || destination == null) {
            return false;
        }

        return applyMove(
                source,
                destination);
    }

    /*
     * Removes all valid-move highlights.
     */
    private void clearHighlights() {

        for (int row = 0; row < board.getSize(); row++) {

            for (int column = 0; column < board.getSize(); column++) {

                Cell cell = board.getCell(
                        row,
                        column);

                cell.setHighlighted(false);
            }
        }
    }

    /*
     * Refreshes the visual state of every cell.
     */
    private void refreshBoard() {

        for (int row = 0; row < board.getSize(); row++) {

            for (int column = 0; column < board.getSize(); column++) {

                Cell cell = board.getCell(
                        row,
                        column);

                cell.setPiece(
                        cell.getPiece());
            }
        }
    }

    private void removePiece(Piece piece) {

        if (piece.getOwner() == Player.PLAYER_ONE) {

            playerOnePieces.remove(piece);

        } else if (piece.getOwner() == Player.PLAYER_TWO) {

            playerTwoPieces.remove(piece);
        }
    }

    public int getPlayerOneClaimedCells() {

        return board.countClaimedCells(
                Player.PLAYER_ONE);
    }

    public int getPlayerTwoClaimedCells() {

        return board.countClaimedCells(
                Player.PLAYER_TWO);
    }

    private void checkGameOver() {

        /*
         * Number of cells required for a majority.
         *
         * The board is 11 x 11 = 121 cells.
         * A majority is therefore 61 cells.
         */
        int majority = (board.getSize() * board.getSize()) / 2 + 1;

        /*
         * Count each player's claimed cells.
         */
        int playerOneClaimed = board.countClaimedCells(
                Player.PLAYER_ONE);

        int playerTwoClaimed = board.countClaimedCells(
                Player.PLAYER_TWO);

        /*
         * Check whether either player has eliminated
         * every piece belonging to the opposing player.
         */
        boolean playerOneEliminated = playerTwoPieces.isEmpty();

        boolean playerTwoEliminated = playerOnePieces.isEmpty();

        /*
         * Player 1 wins by elimination or territory.
         */
        if (playerOneEliminated
                || playerOneClaimed >= majority) {

            gameOver = true;
            winner = Player.PLAYER_ONE;

            notifyGameStateChanged();

            return;
        }

        /*
         * Player 2 wins by elimination or territory.
         */
        if (playerTwoEliminated
                || playerTwoClaimed >= majority) {

            gameOver = true;
            winner = Player.PLAYER_TWO;

            notifyGameStateChanged();

            return;
        }
    }

    public boolean isGameOver() {

        return gameOver;
    }

    public Player getWinner() {

        return winner;
    }
}