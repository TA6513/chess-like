package game;

public class Move {

    private final int sourceRow;
    private final int sourceColumn;

    private final int destinationRow;
    private final int destinationColumn;

    public Move(
            int sourceRow,
            int sourceColumn,
            int destinationRow,
            int destinationColumn) {

        this.sourceRow = sourceRow;
        this.sourceColumn = sourceColumn;

        this.destinationRow = destinationRow;
        this.destinationColumn = destinationColumn;
    }

    public int getSourceRow() {
        return sourceRow;
    }

    public int getSourceColumn() {
        return sourceColumn;
    }

    public int getDestinationRow() {
        return destinationRow;
    }

    public int getDestinationColumn() {
        return destinationColumn;
    }

    /*
     * Converts this move into a simple text message
     * that can be sent over a network connection.
     *
     * Example:
     *
     * MOVE 14 0 13 0
     */
    public String serialize() {

        return "MOVE "
                + sourceRow + " "
                + sourceColumn + " "
                + destinationRow + " "
                + destinationColumn;
    }

    /*
     * Creates a Move from a network message.
     */
    public static Move deserialize(String message) {

        if (message == null) {
            throw new IllegalArgumentException(
                    "Move message cannot be null.");
        }

        String[] parts =
                message.trim().split("\\s+");

        /*
         * A valid MOVE message contains:
         *
         * MOVE
         * source row
         * source column
         * destination row
         * destination column
         */
        if (parts.length != 5
                || !parts[0].equals("MOVE")) {

            throw new IllegalArgumentException(
                    "Invalid move message: "
                            + message);
        }

        try {

            int sourceRow =
                    Integer.parseInt(parts[1]);

            int sourceColumn =
                    Integer.parseInt(parts[2]);

            int destinationRow =
                    Integer.parseInt(parts[3]);

            int destinationColumn =
                    Integer.parseInt(parts[4]);

            return new Move(
                    sourceRow,
                    sourceColumn,
                    destinationRow,
                    destinationColumn);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid move coordinates: "
                            + message,
                    e);
        }
    }

    @Override
    public String toString() {

        return "Move{"
                + "source=("
                + sourceRow
                + ", "
                + sourceColumn
                + "), destination=("
                + destinationRow
                + ", "
                + destinationColumn
                + ")}";
    }
}