package game.network;

import game.Move;
import game.Player;

public class AuthoritativeGame {

	public enum Result {
		ACCEPTED,
		WRONG_PLAYER,
		GAME_OVER,
		INVALID_MOVE,
		INVALID_ACTION
	}

	private static final int SIZE = 11;

	private final ServerPiece[][] board = new ServerPiece[SIZE][SIZE];

	private final Player[][] claimedBy = new Player[SIZE][SIZE];

	private Player currentPlayer;

	private int movesRemaining;

	private boolean openingTurn;

	private boolean gameOver;

	private Player winner;

	private String winReason;

	public AuthoritativeGame() {

		currentPlayer = Player.PLAYER_ONE;

		movesRemaining = 1;

		openingTurn = true;

		gameOver = false;

		winner = null;

		initializePieces();
	}

	/*
	 * -----------------------------------------
	 * INITIAL BOARD
	 * -----------------------------------------
	 */

	private void initializePieces() {

		/*
		 * Player 1.
		 */
		int[][] playerOnePositions = {
				{ 10, 0 },
				{ 10, 1 },
				{ 9, 0 },
				{ 10, 2 },
				{ 9, 1 },
				{ 8, 0 }
		};

		for (int[] position : playerOnePositions) {

			ServerPiece piece = new ServerPiece(
					Player.PLAYER_ONE,
					false);

			placePiece(
					piece,
					position[0],
					position[1]);

			claimedBy[position[0]][position[1]] = Player.PLAYER_ONE;
		}

		/*
		 * Player 2.
		 */
		int[][] playerTwoPositions = {
				{ 0, 10 },
				{ 0, 9 },
				{ 1, 10 },
				{ 0, 8 },
				{ 1, 9 },
				{ 2, 10 }
		};

		for (int[] position : playerTwoPositions) {

			ServerPiece piece = new ServerPiece(
					Player.PLAYER_TWO,
					false);

			placePiece(
					piece,
					position[0],
					position[1]);

			claimedBy[position[0]][position[1]] = Player.PLAYER_TWO;
		}

		/*
		 * Neutral pieces.
		 */
		placePiece(
				new ServerPiece(
						Player.NEUTRAL,
						true),
				0,
				0);

		placePiece(
				new ServerPiece(
						Player.NEUTRAL,
						true),
				5,
				5);
		placePiece(
				new ServerPiece(
						Player.NEUTRAL,
						true),
				10,
				10);
	}

	private void placePiece(
			ServerPiece piece,
			int row,
			int column) {

		board[row][column] = piece;
	}

	/*
	 * =========================================
	 * MOVE
	 * =========================================
	 */

	public synchronized Result applyMove(
			Player player,
			Move move) {

		if (gameOver) {
			return Result.GAME_OVER;
		}

		if (player != currentPlayer) {
			return Result.WRONG_PLAYER;
		}

		if (move == null) {
			return Result.INVALID_MOVE;
		}

		int sourceRow = move.getSourceRow();

		int sourceColumn = move.getSourceColumn();

		int destinationRow = move.getDestinationRow();

		int destinationColumn = move.getDestinationColumn();

		if (!isOnBoard(sourceRow, sourceColumn)
				|| !isOnBoard(
						destinationRow,
						destinationColumn)) {

			return Result.INVALID_MOVE;
		}

		ServerPiece movingPiece = board[sourceRow][sourceColumn];

		if (movingPiece == null) {
			return Result.INVALID_MOVE;
		}

		if (movingPiece.owner != currentPlayer) {
			return Result.INVALID_MOVE;
		}

		if (movingPiece.movedThisTurn) {
			return Result.INVALID_MOVE;
		}

		int rowDifference = Math.abs(
				destinationRow
						- sourceRow);

		int columnDifference = Math.abs(
				destinationColumn
						- sourceColumn);

		/*
		 * Adjacent square only.
		 */
		if (rowDifference > 1
				|| columnDifference > 1
				|| (rowDifference == 0
						&& columnDifference == 0)) {

			return Result.INVALID_MOVE;
		}

		ServerPiece target = board[destinationRow][destinationColumn];

		/*
		 * Cannot move onto an allied piece.
		 */
		if (target != null
				&& target.owner == currentPlayer) {

			return Result.INVALID_MOVE;
		}

		/*
		 * -------------------------------------
		 * NEUTRAL CAPTURE
		 * -------------------------------------
		 *
		 * Originally-neutral pieces always remain
		 * in their square and change allegiance.
		 */
		if (target != null
				&& target.originallyNeutral) {

			target.owner = currentPlayer;

			target.movedThisTurn = true;

			movingPiece.movedThisTurn = true;

			claimCell(
					destinationRow,
					destinationColumn,
					currentPlayer);

			finishAction();

			return Result.ACCEPTED;
		}

		/*
		 * -------------------------------------
		 * ENEMY CAPTURE
		 * -------------------------------------
		 */

		if (target != null) {

			board[destinationRow][destinationColumn] = null;
		}

		/*
		 * Move the attacking piece.
		 */
		board[destinationRow][destinationColumn] = movingPiece;

		board[sourceRow][sourceColumn] = null;

		movingPiece.movedThisTurn = true;

		claimCell(
				destinationRow,
				destinationColumn,
				movingPiece.owner);

		finishAction();

		return Result.ACCEPTED;
	}

	/*
	 * =========================================
	 * PASS
	 * =========================================
	 */

	public synchronized Result applyPass(
			Player player) {

		if (gameOver) {
			return Result.GAME_OVER;
		}

		if (player != currentPlayer) {
			return Result.WRONG_PLAYER;
		}

		if (movesRemaining <= 0) {
			return Result.INVALID_ACTION;
		}

		movesRemaining--;

		if (movesRemaining == 0) {

			endTurn();
		}

		return Result.ACCEPTED;
	}

	/*
	 * =========================================
	 * ACTION / TURN
	 * =========================================
	 */

	private void finishAction() {

		checkGameOver();

		if (gameOver) {
			return;
		}

		movesRemaining--;

		if (movesRemaining == 0) {

			endTurn();
		}
	}

	private void endTurn() {

		if (currentPlayer == Player.PLAYER_ONE) {

			currentPlayer = Player.PLAYER_TWO;

		} else {

			currentPlayer = Player.PLAYER_ONE;
		}

		openingTurn = false;

		movesRemaining = 2;

		resetPiecesForTurn();
	}

	private void resetPiecesForTurn() {

		for (int row = 0; row < SIZE; row++) {

			for (int column = 0; column < SIZE; column++) {

				ServerPiece piece = board[row][column];

				if (piece != null
						&& piece.owner == currentPlayer) {

					piece.movedThisTurn = false;
				}
			}
		}
	}

	/*
	 * =========================================
	 * TERRITORY
	 * =========================================
	 */

	private void claimCell(
			int row,
			int column,
			Player player) {

		/*
		 * Claims are permanent.
		 */
		if (claimedBy[row][column] == null) {

			claimedBy[row][column] = player;
		}
	}

	public int countClaimedCells(
			Player player) {

		int count = 0;

		for (int row = 0; row < SIZE; row++) {

			for (int column = 0; column < SIZE; column++) {

				if (claimedBy[row][column] == player) {

					count++;
				}
			}
		}

		return count;
	}

	/*
	 * =========================================
	 * WIN CONDITIONS
	 * =========================================
	 */

	private void checkGameOver() {

		int majority = SIZE * SIZE / 2 + 1;

		if (countClaimedCells(
				Player.PLAYER_ONE) >= majority) {

			gameOver = true;

			winner = Player.PLAYER_ONE;

			winReason = "TERRITORY";

			return;
		}

		if (countClaimedCells(
				Player.PLAYER_TWO) >= majority) {

			gameOver = true;

			winner = Player.PLAYER_TWO;

			winReason = "TERRITORY";

			return;
		}

		/*
		 * This mirrors your current Game.java:
		 * elimination counts the six original
		 * non-neutral pieces.
		 */
		if (countOriginalPieces(
				Player.PLAYER_ONE) == 0) {

			gameOver = true;

			winner = Player.PLAYER_TWO;

			winReason = "ELIMINATION";

			return;
		}

		if (countOriginalPieces(
				Player.PLAYER_TWO) == 0) {

			gameOver = true;

			winner = Player.PLAYER_ONE;

			winReason = "ELIMINATION";
		}
	}

	private int countOriginalPieces(
			Player player) {

		int count = 0;

		for (int row = 0; row < SIZE; row++) {

			for (int column = 0; column < SIZE; column++) {

				ServerPiece piece = board[row][column];

				if (piece != null
						&& !piece.originallyNeutral
						&& piece.owner == player) {

					count++;
				}
			}
		}

		return count;
	}

	/*
	 * =========================================
	 * HELPERS
	 * =========================================
	 */

	private boolean isOnBoard(
			int row,
			int column) {

		return row >= 0
				&& row < SIZE
				&& column >= 0
				&& column < SIZE;
	}

	/*
	 * =========================================
	 * GETTERS
	 * =========================================
	 */

	public Player getCurrentPlayer() {

		return currentPlayer;
	}

	public int getMovesRemaining() {

		return movesRemaining;
	}

	public boolean isOpeningTurn() {

		return openingTurn;
	}

	public boolean isGameOver() {

		return gameOver;
	}

	public Player getWinner() {

		return winner;
	}

	public String getWinReason() {

		return winReason;
	}

	/*
	 * =========================================
	 * INTERNAL PIECE
	 * =========================================
	 */

	private static class ServerPiece {

		private final boolean originallyNeutral;

		private Player owner;

		private boolean movedThisTurn;

		ServerPiece(
				Player owner,
				boolean originallyNeutral) {

			this.owner = owner;

			this.originallyNeutral = originallyNeutral;

			movedThisTurn = false;
		}
	}
}