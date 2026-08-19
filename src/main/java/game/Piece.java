package game;

public class Piece {

    private final int id;

    /*
     * Current owner of the piece.
     */
    private Player owner;

    /*
     * True if this piece started the game as neutral.
     */
    private final boolean originallyNeutral;

    /*
     * Player who currently controls a formerly-neutral piece.
     */
    private Player capturedBy;

    /*
     * Prevents a piece from being used more than once per turn.
     */
    private boolean movedThisTurn;

    public Piece(int id, Player owner) {

        this.id = id;
        this.owner = owner;

        originallyNeutral = owner == Player.NEUTRAL;
        capturedBy = null;

        movedThisTurn = false;
    }

    public int getId() {
        return id;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public boolean isNeutral() {
        return owner == Player.NEUTRAL;
    }

    public boolean belongsTo(Player player) {
        return owner == player;
    }

    public boolean wasOriginallyNeutral() {
        return originallyNeutral;
    }

    public Player getCapturedBy() {
        return capturedBy;
    }

    public boolean wasNeverCaptured() {
        return originallyNeutral && capturedBy == null;
    }

    public boolean hasMovedThisTurn() {
        return movedThisTurn;
    }

    public void setMovedThisTurn(boolean movedThisTurn) {
        this.movedThisTurn = movedThisTurn;
    }

    public void capture(Player player) {

        if (originallyNeutral) {
            capturedBy = player;
        }

        owner = player;

        /*
         * A captured neutral cannot be moved again
         * during the same turn.
         */
        movedThisTurn = true;
    }

    public void resetTurn() {
        movedThisTurn = false;
    }
}