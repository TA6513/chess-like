package game;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final Board board;

    private final List<Piece> playerOnePieces;
    private final List<Piece> playerTwoPieces;
    private final List<Piece> neutralPieces;
    private boolean gameOver;
    private Player winningPlayer;

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

    public Game() {

        board = new Board();
        gameOver = false;
        winningPlayer = null;

        playerOnePieces = new ArrayList<>();
        playerTwoPieces = new ArrayList<>();
        neutralPieces = new ArrayList<>();

        /*
         * Player 1 starts the game.
         */
        currentPlayer = Player.PLAYER_ONE;

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
     * Tells the UI that the game state has changed.
     */
    private void notifyGameStateChanged() {

        if (gameStateChanged != null) {

            gameStateChanged.run();
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

    public boolean movePiece(
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
         *
         * The neutral piece stays on its square,
         * changes allegiance, and its cell becomes
         * permanently claimed by the capturing player.
         */
        if (targetPiece != null
                && targetPiece.wasOriginallyNeutral()) {

            /*
             * Change the neutral piece's allegiance.
             */
            targetPiece.capture(currentPlayer);

            /*
             * Claim the neutral piece's cell.
             *
             * claim() will only work if the cell has not
             * already been claimed.
             */
            destination.claim(currentPlayer);

            /*
             * The attacking piece has used its move even
             * though it did not physically move.
             */
            movingPiece.setMovedThisTurn(true);

            /*
             * Check victory.
             */
            checkGameOver();

            /*
             * Only advance the turn if the game isn't over.
             */
            if (!gameOver) {
                useMove();
            }

            refreshBoard();

            notifyGameStateChanged();

            return true;
        }

        /*
         * Capture an enemy piece.
         *
         * Enemy pieces are removed from the board.
         */
        if (targetPiece != null) {

            removePiece(targetPiece);
        }

        /*
         * Move attacking piece.
         */
        destination.setPiece(
                movingPiece);

        /*
         * If the destination was previously unclaimed,
         * permanently claim it for the moving piece's player.
         *
         * If it was already claimed, claim() does nothing.
         */
        destination.claim(
                movingPiece.getOwner());

        source.setPiece(null);

        movingPiece.setMovedThisTurn(true);

        /*
         * Check whether the enemy has been completely
         * eliminated.
         */
        checkGameOver();

        /*
         * Only advance the turn if the game isn't over.
         */
        if (!gameOver) {
            useMove();
        }

        refreshBoard();

        notifyGameStateChanged();

        return true;
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

        if (piece == null) {
            return;
        }

        /*
         * Remove the piece from whichever player's
         * piece list contains it.
         */
        playerOnePieces.remove(piece);
        playerTwoPieces.remove(piece);

        /*
         * Neutral pieces are never removed.
         */
        neutralPieces.remove(piece);
    }

    private void checkGameOver() {

        boolean playerOneHasPieces = false;
        boolean playerTwoHasPieces = false;

        for (Piece piece : getAllPieces()) {

            if (piece.belongsTo(Player.PLAYER_ONE)) {
                playerOneHasPieces = true;
            }

            if (piece.belongsTo(Player.PLAYER_TWO)) {
                playerTwoHasPieces = true;
            }
        }

        /*
         * Player 1 has no remaining pieces.
         */
        if (!playerOneHasPieces) {

            gameOver = true;
            winningPlayer = Player.PLAYER_TWO;

            return;
        }

        /*
         * Player 2 has no remaining pieces.
         */
        if (!playerTwoHasPieces) {

            gameOver = true;
            winningPlayer = Player.PLAYER_ONE;
        }
    }

    public boolean isGameOver() {

        return gameOver;
    }

    public Player getWinningPlayer() {

        return winningPlayer;
    }
}