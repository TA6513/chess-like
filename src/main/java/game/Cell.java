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

    private Player claimedBy;

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

        claimedBy = null;

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

    public Player getClaimedBy() {
        return claimedBy;
    }

    public boolean isClaimed() {
        return claimedBy != null;
    }

    public boolean claim(Player player) {

        if (claimedBy != null || player == null) {
            return false;
        }

        claimedBy = player;

        updateAppearance();

        return true;
    }

    private void updateAppearance() {

        /*
         * Determine the normal background color.
         *
         * Claimed cells keep their owner's color permanently.
         * Unclaimed cells use the normal checkerboard pattern.
         */
        if (claimedBy == Player.PLAYER_ONE) {

            background.setFill(
                    Color.rgb(28, 50, 255));

        } else if (claimedBy == Player.PLAYER_TWO) {

            background.setFill(
                    Color.rgb(64, 160, 86));

        } else {

            background.setFill(
                    (row + column) % 2 == 0
                            ? Color.WHITE
                            : Color.LIGHTGRAY);
        }

        /*
         * Valid move highlight.
         *
         * The highlight temporarily overrides the cell's
         * normal/claimed background color.
         *
         * Once the highlight is removed, the cell returns
         * to its permanent claimed color.
         */
        if (highlighted) {

            background.setFill(
                    Color.rgb(255, 220, 80));
        }

        /*
         * Display piece sprite.
         *
         * The piece and the cell ownership are independent.
         * Therefore, a Player 2 piece can be sitting on a
         * Player 1 claimed cell without changing the cell color.
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
         * Highlighted = orange
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