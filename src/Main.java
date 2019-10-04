import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Таня on 27.05.2017.
 */
public class Main extends Application {

    //запускаємо як сервер = тру, якщо як клієнт = фолс
    private boolean isServer = true;
    public NetworkConnection connection = isServer ? createServer() : createClient();

    static Pane pane = new Pane();
    static Label playerLabel = new Label();
    private Tile[][] board = new Tile[15][15];

    public static boolean playable = true;
    public static List<Combo> combos = new ArrayList<>();

    @Override
    public void init() throws Exception {
        connection.startConnection();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("5 in a row");

        playerLabel.setText("Player " + (isServer ? "X" : "O"));
        pane.setPrefSize(600, 600);
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                Tile tile = new Tile(i, j);
                tile.setTranslateX(j * 40);
                tile.setTranslateY(i * 40);
                pane.getChildren().add(tile);
                board[i][j] = tile;
            }
        }

//////////////////////////////////////////combinations:
        //horizontal
        for (int y = 0; y < 15; y++) {
            for (int z = 0; z < 11; z++) {
                combos.add(new Combo(board[y][z], board[y][z + 1], board[y][z + 2], board[y][z + 3], board[y][z + 4]));
            }
        }

        //vertical
        for (int y = 0; y < 15; y++) {
            for (int z = 0; z < 11; z++) {
                combos.add(new Combo(board[z][y], board[z + 1][y], board[z + 2][y], board[z + 3][y], board[z + 4][y]));
            }
        }

        //diagonal"\"
        for (int y = 0; y < 11; y++) {
            for (int z = 0; z < 11 - y; z++) {
                combos.add(new Combo(board[z + y][z], board[y + z + 1][z + 1], board[y + z + 2][z + 2],
                                     board[y + z + 3][z + 3], board[y + z + 4][z + 4]));
            }
        }
        for (int y = 1; y < 11; y++) {
            for (int z = 0; z < 11 - y; z++) {
                combos.add(new Combo(board[z][z + y], board[z + 1][y + z + 1], board[z + 2][y + z + 2],
                                     board[z + 3][y + z + 3], board[z + 4][y + z + 4]));
            }
        }

        //diagonal "/"
        for (int y = 0; y < 11; y++) {
            for (int z = 0; z < 11 - y; z++) {
                combos.add(new Combo(board[z + y][14 - z], board[y + z + 1][14 - z - 1], board[y + z + 2][14 - z - 2],
                                     board[y + z + 3][14 - z - 3], board[y + z + 4][14 - z - 4]));
            }
        }
        for (int y = 1; y < 11; y++) {
            for (int z = 0; z < 11 - y; z++) {
                combos.add(new Combo(board[z][14 - z - y], board[z + 1][14 - z - 1 - y], board[z + 2][14 - z - 2 - y],
                                     board[z + 3][14 - z - 3 - y], board[z + 4][14 - z - 4 - y]));
            }
        }
///////////////////////////////////////////combinations.

        VBox vBox = new VBox(10, playerLabel, pane);
        vBox.setAlignment(Pos.CENTER);
        primaryStage.setScene(new Scene(vBox));
        primaryStage.show();

    }

    public class Tile extends StackPane {

        public int row, column;
        public Text text = new Text();
        public boolean turnX = isServer ? true : false;

        public Tile(int row, int column) {
            Rectangle border = new Rectangle(40, 40);
            border.setFill(null);
            border.setStroke(Color.BLACK);
            this.row = row;
            this.column = column;

            text.setFont(Font.font(24));
            setAlignment(Pos.CENTER);
            getChildren().addAll(border, text);

            setOnMouseClicked(e -> {
                if (!playable) {
                    return;
                }
                //if ()
                if (e.getButton() == MouseButton.PRIMARY) {
                    try {
                        if (text.getText().isEmpty()) {
                            text.setText(turnX ? "X" : "O");
                        }
                        connection.send(row + " " + column + (turnX ? " X" : " O"));
                        pane.setDisable(true);
                        playerLabel.setText("Waiting for another player...");
                    } catch (Exception e1) {
                        System.out.println("Error on click: " + e1.getMessage());
                    }
                    checkState();
                }
            });
        }

        public double getCenterX() {
            return getTranslateX() + 20;
        }

        public double getCenterY() {
            return getTranslateY() + 20;
        }

        public String getValue() {
            return text.getText();
        }
    }

    public class Combo {

        public Tile[] tiles;

        public Combo(Tile... tiles) {
            this.tiles = tiles;
        }

        public boolean isComplete() {
            for (int i = 0; i < 5; i++) {
                if (tiles[i].getValue().isEmpty()) {
                    return false;
                }
            }
            return tiles[0].getValue().equals(tiles[1].getValue())
                && tiles[0].getValue().equals(tiles[2].getValue())
                && tiles[0].getValue().equals(tiles[3].getValue())
                && tiles[0].getValue().equals(tiles[4].getValue());
        }
    }

    public static void checkState() {
        for (Combo combo : combos) {
            if (combo.isComplete()) {
                playable = false;
                playWinAnimation(combo);
                playerLabel.setText("Player " + combo.tiles[0].getValue() + " won!");
                break;
            }
        }
    }

    public static void playWinAnimation(Combo combo) {
        Line line = new Line();
        line.setStartX(combo.tiles[0].getCenterX());
        line.setStartY(combo.tiles[0].getCenterY());
        line.setEndX(combo.tiles[4].getCenterX());
        line.setEndY(combo.tiles[4].getCenterY());
        pane.getChildren().add(line);

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(2),
                                                 new KeyValue(line.endXProperty(), combo.tiles[4].getCenterX()),
                                                 new KeyValue(line.endYProperty(), combo.tiles[4].getCenterY())));
        timeline.play();
    }

    public void stop() throws Exception {
        connection.closeConnection();
    }

    private Server createServer() {
        return new Server(55555, data -> {
            Platform.runLater(() -> {
                pane.setDisable(false);
                if (data.toString().equals("Connection closed.")) {
                    return;
                }
                board[Integer.parseInt(data.toString().split(" ")[0])][Integer
                    .parseInt(data.toString().split(" ")[1])].text.setText(
                    (data.toString().split(" ")[2] == "X") ? "X" : "O");
                playerLabel.setText("Your turn!");
                checkState();
            });
        });
    }

    private Client createClient() {
        return new Client("127.0.0.1", 55555, data -> {
            Platform.runLater(() -> {
                pane.setDisable(false);
                if (data.toString().equals("Connection closed.")) {
                    return;
                }
                board[Integer.parseInt(data.toString().split(" ")[0])][Integer
                    .parseInt(data.toString().split(" ")[1])].text.setText(
                    (data.toString().split(" ")[2] != "X") ? "X" : "O");
                playerLabel.setText("Your turn!");
                checkState();
            });
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
