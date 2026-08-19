package game;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;

public class Cell extends StackPane {

    private final int row;
    private final int column;

    private final Rectangle background;
    private final ImageView pieceImage;

    private Piece piece;

    private boolean selected;
    private boolean highlighted;

    public Cell(int row, int column) {

        this.row = row;
        this.column = column;

        background = new Rectangle();

        background.setMouseTransparent(true);

        pieceImage = new ImageView();

        pieceImage.setPreserveRatio(false);
        pieceImage.setSmooth(false);
        pieceImage.setVisible(false);

        pieceImage.setMouseTransparent(true);

        getChildren().addAll(
                background,
                pieceImage);

        setAlignment(Pos.CENTER);

        updateAppearance();
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public Piece getPiece() {
        return piece;
    }

    public boolean isOccupied() {
        return piece != null;
    }

    public void setPiece(Piece piece) {

        this.piece = piece;

        updateAppearance();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {

        this.selected = selected;

        updateAppearance();
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {

        this.highlighted = highlighted;

        updateAppearance();
    }

    public void resize(double size) {

        setPrefSize(size, size);

        /*
         * Allow the cell to shrink when the window shrinks.
         */
        setMinSize(0, 0);

        setMaxSize(size, size);

        background.setWidth(size);
        background.setHeight(size);

        double spriteSize = Math.max(0, size - 2);

        pieceImage.setFitWidth(spriteSize);
        pieceImage.setFitHeight(spriteSize);
    }

    private void updateAppearance() {

        /*
         * Normal board color.
         */
        background.setFill(
                (row + column) % 2 == 0
                        ? Color.WHITE
                        : Color.LIGHTGRAY);

        /*
         * Valid move highlight.
         *
         * This gets its own distinct color so it is
         * clearly different from the normal board.
         */
        if (highlighted) {

            background.setFill(
                    Color.rgb(255, 220, 80));
        }

        /*
         * Display piece sprite.
         */
        if (piece != null) {

            Image image = getPieceImage(piece);

            pieceImage.setImage(image);

            pieceImage.setVisible(
                    image != null);

        } else {

            pieceImage.setImage(null);

            pieceImage.setVisible(false);
        }

        /*
         * Cell border.
         *
         * Selected = yellow
         * Highlighted = gold/orange
         * Normal = black
         */
        if (selected) {

            setStyle(
                    "-fx-border-color: #FFFF00;" +
                            "-fx-border-width: 3px;");

        } else if (highlighted) {

            setStyle(
                    "-fx-border-color: #FF9900;" +
                            "-fx-border-width: 3px;");

        } else {

            setStyle(
                    "-fx-border-color: black;" +
                            "-fx-border-width: 1px;");
        }
    }

    private Image getPieceImage(Piece piece) {

        String path;

        /*
         * Neutral-origin pieces.
         */
        if (piece.wasOriginallyNeutral()) {

            if (piece.getCapturedBy() == Player.PLAYER_ONE) {

                path = "/sprites/robot_blue.png";

            } else if (piece.getCapturedBy() == Player.PLAYER_TWO) {

                path = "/sprites/robot_green.png";

            } else {

                path = "/sprites/robot.png";
            }

        }

        /*
         * Player 1.
         */
        else if (piece.getOwner() == Player.PLAYER_ONE) {

            path = "/sprites/spaceman.png";
        }

        /*
         * Player 2.
         */
        else {

            path = "/sprites/alien.png";
        }

        InputStream stream = getClass()
                .getResourceAsStream(path);

        if (stream == null) {

            System.err.println(
                    "Could not find sprite: "
                            + path);

            return null;
        }

        return new Image(stream);
    }
}