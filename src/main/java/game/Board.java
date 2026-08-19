package game;

import javafx.scene.layout.GridPane;

public class Board extends GridPane {

    private static final int SIZE = 11;

    private final Cell[][] cells;

    private Cell selectedCell;

    private Game game;

    public Board() {

        cells = new Cell[SIZE][SIZE];

        createBoard();

        /*
         * Allow the board to shrink all the way down.
         */
        setMinSize(0, 0);

        /*
         * Keep the board square.
         */
        widthProperty().addListener(
                (obs, oldValue, newValue) -> resizeBoard());

        heightProperty().addListener(
                (obs, oldValue, newValue) -> resizeBoard());
    }

    public void setGame(Game game) {

        this.game = game;
    }

    private void createBoard() {

        for (int row = 0; row < SIZE; row++) {

            for (int column = 0; column < SIZE; column++) {

                Cell cell = new Cell(row, column);

                cells[row][column] = cell;

                add(
                        cell,
                        column,
                        row);

                cell.setOnMouseClicked(
                        event -> handleCellClick(cell));
            }
        }
    }

    private void handleCellClick(Cell cell) {

        if (game == null) {
            return;
        }

        /*
         * No piece currently selected.
         *
         * Try to select the clicked piece.
         */
        if (selectedCell == null) {

            if (cell.getPiece() == null) {
                return;
            }

            if (!game.canMovePiece(
                    cell.getPiece())) {

                return;
            }

            selectedCell = cell;

            cell.setSelected(true);

            game.highlightValidMoves(cell);

            return;
        }

        /*
         * Clicking the selected piece again
         * cancels the selection.
         */
        if (cell == selectedCell) {

            clearSelection();

            return;
        }

        /*
         * Try to move to the clicked cell.
         */
        if (game.movePiece(
                selectedCell,
                cell)) {

            clearSelection();

            return;
        }

        /*
         * Invalid destination.
         *
         * Keep the selected piece highlighted.
         */
    }

    private void clearSelection() {

        if (selectedCell != null) {

            selectedCell.setSelected(false);
        }

        selectedCell = null;

        clearHighlights();
    }

    private void clearHighlights() {

        for (int row = 0; row < SIZE; row++) {

            for (int column = 0; column < SIZE; column++) {

                cells[row][column]
                        .setHighlighted(false);
            }
        }
    }

    private void resizeBoard() {

        double availableWidth = getWidth();
        double availableHeight = getHeight();

        if (availableWidth <= 0 ||
                availableHeight <= 0) {

            return;
        }

        /*
         * The board must remain square.
         */
        double boardSize = Math.min(
                availableWidth,
                availableHeight);

        /*
         * Use whole pixels so the pixel-art sprites
         * remain as clean as possible.
         */
        double cellSize = Math.floor(
                boardSize / SIZE);

        /*
         * Resize every cell.
         */
        for (int row = 0; row < SIZE; row++) {

            for (int column = 0; column < SIZE; column++) {

                cells[row][column]
                        .resize(cellSize);
            }
        }
    }

    public Cell getCell(
            int row,
            int column) {

        if (row < 0
                || row >= SIZE
                || column < 0
                || column >= SIZE) {

            return null;
        }

        return cells[row][column];
    }

    public int getSize() {

        return SIZE;
    }
}