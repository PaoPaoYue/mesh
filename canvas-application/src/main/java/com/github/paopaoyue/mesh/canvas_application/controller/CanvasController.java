package com.github.paopaoyue.mesh.canvas_application.controller;

import com.github.paopaoyue.mesh.canvas_application.JavaFxLauncher;
import com.github.paopaoyue.mesh.canvas_application.config.Properties;
import com.github.paopaoyue.mesh.canvas_application.controller.draw.*;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import com.github.paopaoyue.mesh.canvas_application.service.ClientCanvasService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class CanvasController {

    private static final Logger logger = LoggerFactory.getLogger(CanvasController.class);

    private final ObservableList<CanvasProto.User> userList;
    private final ObservableList<CanvasProto.TextMessage> messageList;
    private final ClientCanvasService clientCanvasService;
    @Autowired
    private Properties prop;
    @FXML
    private Canvas drawingCanvas;
    @FXML
    private Canvas stageCanvas;
    @FXML
    private Canvas persistentCanvas;
    @FXML
    private Button colorPickerButton;
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private ToggleButton penToggleButton;
    @FXML
    private ToggleButton eraserToggleButton;
    @FXML
    private ToggleButton lineToggleButton;
    @FXML
    private ToggleButton rectangleToggleButton;
    @FXML
    private ToggleButton ovalToggleButton;
    @FXML
    private ToggleButton circleToggleButton;
    @FXML
    private ToggleButton textToggleButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button saveAsButton;
    @FXML
    private Button resetButton;
    @FXML
    private Button importButton;
    @FXML
    private TextField usernameTextField;
    @FXML
    private Label promptLabel;
    @FXML
    private ListView<CanvasProto.User> userListView;
    @FXML
    private ListView<CanvasProto.TextMessage> chatListView;
    @FXML
    private TextField messageTextField;


    private GraphicsContext drawingContext;
    private GraphicsContext stageContext;
    private GraphicsContext persistentContext;
    private ITool currentTool;
    private Color color = Color.RED;
    private double lineWidth = 1.0;

    public CanvasController(@Lazy ClientCanvasService clientCanvasService) {
        this.clientCanvasService = clientCanvasService;
        this.userList = FXCollections.observableArrayList();
        this.messageList = FXCollections.observableArrayList();
    }

    public void initialize() {
        userListView.setCellFactory(userCellFactory(userListView));
        userListView.setItems(userList);
        chatListView.setCellFactory(chatCellFactory(chatListView));
        chatListView.setItems(messageList);

        drawingContext = drawingCanvas.getGraphicsContext2D();
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleCanvasMousePressed);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleCanvasMouseDragged);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleCanvasMouseReleased);

        stageContext = stageCanvas.getGraphicsContext2D();
        persistentContext = persistentCanvas.getGraphicsContext2D();

        if (!clientCanvasService.getCurrentUser().getIsHost()) {
            saveButton.setVisible(false);
            saveAsButton.setVisible(false);
            resetButton.setVisible(false);
            importButton.setVisible(false);
        }

        // load auto save
        try {
            clientCanvasService.resetCanvas(true, prop.getAutoSavePath());
        } catch (IOException e) {
            logger.error("Failed to load auto save", e);
        }

        // Set initial drawing color
        setDrawingColor(color);
        // Set initial drawing line width
        setDrawingLineWidth(lineWidth);

        showLoginDialog();
    }

    public ITool getCurrentTool() {
        return currentTool;
    }

    public boolean runLaterJoinConfirm(CanvasProto.User user) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            showLoginRequestDialog(user, future);
        });
        try {
            return future.get(prop.getLoginTimeout(), TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to confirm join", e);
        }
        return false;
    }

    public void runLaterShowAlert(Alert.AlertType alertType, String message, boolean exit) {
        Platform.runLater(() -> showAlert(alertType, message, exit));
    }

    public void runLaterSyncData(CanvasProto.SyncResponse responseData) {
        Platform.runLater(() -> {
            // merge userList by userId
            mergeInPlace(userList, responseData.getUsersList(), Comparator.comparing(CanvasProto.User::getUserId));
            // merge messageList by timestamp
            mergeInPlace(messageList, responseData.getTextMessagesList(), Comparator.comparing(CanvasProto.TextMessage::getTimestamp));

            // clear persistent canvas if reset
            if (responseData.getReset()) {
                persistentContext.clearRect(0, 0, persistentCanvas.getWidth(), persistentCanvas.getHeight());
            }

            // draw persistent items
            for (var item : responseData.getItemsList()) {
                drawCanvasItem(persistentContext, item);
            }

            // clear stage canvas
            stageContext.clearRect(0, 0, stageCanvas.getWidth(), stageCanvas.getHeight());

            // draw transient item
            List<CanvasProto.CanvasItem> transientItems = getValuesSortedByKeys(responseData.getTransientItemMapMap());
            for (var item : transientItems) {
                drawCanvasItem(stageContext, item);
            }
        });
    }

    @FXML
    private void showLoginDialog() {
        // Create controls for the dialog
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        Label promptLabel = new Label("Please enter your username:");
        TextField usernameTextField = new TextField();
        usernameTextField.setPromptText("Enter Username");
        Label infoLabel = new Label();
        infoLabel.setVisible(false);
        vBox.getChildren().addAll(promptLabel, usernameTextField, infoLabel);

        Dialog<String> loginDialog = createDialog("Welcome to Canvas Application!", vBox, dialog -> {
            String username = usernameTextField.getText();
            if (username.isEmpty() || username.isBlank() || username.length() > prop.getMaxUsernameLength()) {
                // Show an alert if the username is invalid
                infoLabel.setTextFill(Color.RED);
                infoLabel.setText("Invalid username - should not be empty or exceed " + prop.getMaxUsernameLength() + " characters.");
                infoLabel.setVisible(true);
                return;
            }
            // Perform login actions
            try {
                clientCanvasService.login(username);
            } catch (Exception e) {
                // Show an alert if login fails
                infoLabel.setTextFill(Color.RED);
                infoLabel.setText("Failed to login: " + e.getMessage());
                infoLabel.setVisible(true);
                return;
            }
            dialog.setResult("confirm");
        }, dialog -> {
            dialog.setResult("cancel");
            JavaFxLauncher.shutDownApplication();
        });

        // Show the dialog and wait for result
        loginDialog.showAndWait();
    }

    private void showLoginRequestDialog(CanvasProto.User user, CompletableFuture<Boolean> future) {
        Dialog<String> loginDialog = createDialog("New user joining!", new Label(user.getUsername() + " wants to join your canvas! \n confirm to allow or cancel to reject. \n Closing automatically with rejection."), dialog -> {
            dialog.setResult("confirm");
        }, dialog -> {
            dialog.setResult("cancel");
        });

        Timeline autoClose = new Timeline(new KeyFrame(Duration.seconds(prop.getLoginTimeout() - 1), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                loginDialog.setResult("cancel");
            }
        }));
        autoClose.setCycleCount(1);
        autoClose.play();

        // Show the dialog and wait for result
        loginDialog.showAndWait().ifPresent(result -> {
            if (result.equals("confirm")) {
                future.complete(true);
            } else {
                future.complete(false);
            }
        });
    }

    @FXML
    private void showLogoutDialog() {
        // Create the dialog
        Dialog<String> logoutDialog = createDialog("Logout confirmation", new Label("Are you sure you wanna logout?"), dialog -> {
            clientCanvasService.logout();
            dialog.setResult("confirm");
        }, dialog -> {
            dialog.setResult("cancel");
        });
        // Show the dialog and wait for result
        logoutDialog.showAndWait().ifPresent(result -> {
            if (result.equals("confirm")) {
                JavaFxLauncher.shutDownApplication();
            }
        });
    }

    @FXML
    private void showKickOutDialog(CanvasProto.User user) {
        if (!clientCanvasService.getCurrentUser().getIsHost()) {
            showAlert(Alert.AlertType.INFORMATION, "Only host can use this function", false);
        }
        // Create the dialog
        Dialog<String> logoutDialog = createDialog("Kick out confirmation", new Label("Are you sure you wanna kick out " + user.getUsername() + " ?"), dialog -> {
            clientCanvasService.kickUser(user.getUserId());
            dialog.setResult("confirm");
        }, dialog -> {
            dialog.setResult("cancel");
        });
        // Show the dialog and wait for result
        logoutDialog.showAndWait();
    }

    @FXML
    private void showColorDialog() {
        // Create controls for the dialog
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setValue(this.color);
        AtomicReference<Color> selectedColor = new AtomicReference<>(this.color);
        colorPicker.setOnAction(event -> {
            selectedColor.set(colorPicker.getValue());
        });
        Dialog<Color> colorDialog = createDialog("Pick your colors!", colorPicker, dialog -> {
            dialog.setResult(selectedColor.get());
        }, dialog -> {
            dialog.setResult(color);
        });
        // Show the dialog and wait for result
        // Set the background color of the colorPickerButton
        colorDialog.showAndWait().ifPresent(this::setDrawingColor);
    }

    @FXML
    private void showLineWidthDialog() {
        // Create controls for the dialog
        AtomicReference<Double> selectedLineWidth = new AtomicReference<>(this.lineWidth);
        Slider lineWidthSlider = new Slider(1, 12, 1);
        lineWidthSlider.setValue(this.lineWidth);
        lineWidthSlider.setShowTickLabels(true);
        lineWidthSlider.setShowTickMarks(true);
        lineWidthSlider.setMajorTickUnit(1);
        lineWidthSlider.setBlockIncrement(1);
        lineWidthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            selectedLineWidth.set(newValue.doubleValue());
        });
        Dialog<Double> lineWidthDialog = createDialog("Set line width!", lineWidthSlider, dialog -> {
            dialog.setResult(selectedLineWidth.get());
        }, dialog -> {
            dialog.setResult(lineWidth);
        });
        // Show the dialog and wait for result
        // Set the line width of the graphics context
        lineWidthDialog.showAndWait().ifPresent(this::setDrawingLineWidth);
    }

    private void showTextInputDialog(GraphicsContext context, MouseEvent event) {
        // Create controls for the dialog
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        Label promptLabel = new Label("Please enter your text:");
        TextField textField = new TextField();
        usernameTextField.setPromptText("Canvas text");
        Label infoLabel = new Label();
        infoLabel.setVisible(false);
        vBox.getChildren().addAll(promptLabel, usernameTextField, infoLabel);

        Dialog<String> loginDialog = createDialog("Set canvas text!", vBox, dialog -> {
            String text = usernameTextField.getText();
            if (text.isEmpty() || text.isBlank() || text.length() > prop.getMaxCanvasTextLength()) {
                // Show an alert if the text is invalid
                infoLabel.setTextFill(Color.RED);
                infoLabel.setText("Invalid text - should not be empty or exceed " + prop.getMaxCanvasTextLength() + " characters.");
                infoLabel.setVisible(true);
                return;
            }
            // trigger text input tool handler
            if (currentTool instanceof TextTool) {
                ((TextTool) currentTool).handleCanvasTextInput(context, text, event.getX(), event.getY());
            }

            dialog.setResult("confirm");
        }, dialog -> {
            dialog.setResult("cancel");
            JavaFxLauncher.shutDownApplication();
        });

        // Show the dialog and wait for result
        loginDialog.showAndWait();
    }

    private <T> Dialog<T> createDialog(String title, Region dialogContent, Consumer<Dialog<T>> onConfirm, Consumer<Dialog<T>> onCancel) {
        // Create the dialog
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);

        // Create layout for the dialog
        VBox dialogLayout = new VBox();
        dialogLayout.setPrefWidth(300);
        dialogLayout.setPadding(new Insets(10));
        dialogLayout.setSpacing(10);
        dialogLayout.setAlignment(Pos.CENTER);
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.setAlignment(Pos.CENTER);

        // Create controls for the dialog
        Button confirmButton = new Button("Confirm");
        Button cancelButton = new Button("Cancel");

        confirmButton.setOnAction(event -> {
            onConfirm.accept(dialog);
        });
        cancelButton.setOnAction(event -> {
            onCancel.accept(dialog);
        });

        buttonBox.getChildren().addAll(confirmButton, cancelButton);
        dialogLayout.getChildren().addAll(dialogContent, buttonBox);
        dialog.getDialogPane().setContent(dialogLayout);

        return dialog;
    }

    private void showAlert(Alert.AlertType alertType, String message, boolean exit) {
        Alert alert = new Alert(alertType);
        alert.setContentText(message);
        ButtonType confirmButton = new ButtonType("Confirm");
        if (exit) {
            alert.getButtonTypes().setAll(confirmButton);
            alert.setOnCloseRequest(e -> {
                JavaFxLauncher.shutDownApplication();
            });
        } else {
            alert.getButtonTypes().setAll(confirmButton);
        }

        alert.showAndWait();
    }

    // Method to create custom list cell for user list
    private Callback<ListView<CanvasProto.User>, ListCell<CanvasProto.User>> userCellFactory(ListView<CanvasProto.User> listView) {
        return new Callback<ListView<CanvasProto.User>, ListCell<CanvasProto.User>>() {
            @Override
            public javafx.scene.control.ListCell<CanvasProto.User> call(ListView<CanvasProto.User> listView) {
                return new ListCell<CanvasProto.User>() {
                    @Override
                    protected void updateItem(CanvasProto.User user, boolean empty) {
                        super.updateItem(user, empty);

                        if (empty || user == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox();
                            hbox.setSpacing(5);
                            hbox.setAlignment(Pos.CENTER_LEFT);

                            setText(null);
                            hbox.getChildren().addAll(new Label(user.getUsername()));
                            // Add kick out button to the cell
                            if (clientCanvasService.getCurrentUser().getIsHost() && !user.getIsHost()) {
                                Button kickOutButton = new Button("Kick Out");
                                kickOutButton.getStyleClass().add("kick-out-button");
                                kickOutButton.setOnAction(event -> {
                                    showKickOutDialog(user);
                                });
                                hbox.getChildren().addAll(kickOutButton);
                            }

                            setGraphic(hbox);
                        }
                    }
                };
            }
        };
    }

    // Method to create custom list cell for chat list
    private Callback<ListView<CanvasProto.TextMessage>, ListCell<CanvasProto.TextMessage>> chatCellFactory(ListView<CanvasProto.TextMessage> listView) {
        return new Callback<ListView<CanvasProto.TextMessage>, ListCell<CanvasProto.TextMessage>>() {
            @Override
            public javafx.scene.control.ListCell<CanvasProto.TextMessage> call(ListView<CanvasProto.TextMessage> listView) {
                return new ListCell<CanvasProto.TextMessage>() {
                    @Override
                    protected void updateItem(CanvasProto.TextMessage message, boolean empty) {
                        super.updateItem(message, empty);

                        if (empty || message == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            // Add username to the cell
                            setText(clientCanvasService.getUsername(message.getUserId()) + ": " + message.getMessage());

                        }
                    }
                };
            }
        };
    }

    @FXML
    private void sendTextMessage() {
        String message = messageTextField.getText();
        if (message.isEmpty() || message.isBlank() || message.length() > prop.getMaxMessageLength()) {
            return;
        }
        clientCanvasService.sendTextMessage(message);
        messageTextField.clear();
    }

    @FXML
    private void selectTool() {
        // Get the selected tool from toggle buttons
        Toggle selectedToggle = toolToggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            ((ToggleButton) selectedToggle).setStyle("-fx-background-color: " + toRGBCode(color));
        }
    }

    private void setDrawingColor(Color color) {
        this.color = color;
        colorPickerButton.setStyle("-fx-background-color: " + toRGBCode(color));
        Toggle selectedToggle = toolToggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            ((ToggleButton) selectedToggle).setStyle("-fx-background-color: " + toRGBCode(color));
        }
        drawingContext.setFill(color);
        drawingContext.setStroke(color);
    }

    private void setDrawingLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        drawingContext.setLineWidth(lineWidth);
    }

    private void drawCanvasItem(GraphicsContext context, CanvasProto.CanvasItem protoItem) {
        ITool tool = null;
        switch (protoItem.getItemCase()) {
            case DRAW -> tool = new DrawTool();
            case ERASER -> tool = new EraserTool();

        }
        if (tool != null) {
            tool.fromProto(protoItem);
            tool.draw(context);
        }
    }

    private void handleCanvasMousePressed(MouseEvent event) {
        Toggle selectedToggle = toolToggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            if (selectedToggle.equals(penToggleButton)) {
                currentTool = new DrawTool();
            } else if (selectedToggle.equals(eraserToggleButton)) {
                currentTool = new EraserTool();
            } else if (selectedToggle.equals(lineToggleButton)) {
                currentTool = new LineTool();
            } else if (selectedToggle.equals(rectangleToggleButton)) {
                currentTool = new RectTool();
            } else if (selectedToggle.equals(ovalToggleButton)) {
                currentTool = new OvalTool();
            } else if (selectedToggle.equals(circleToggleButton)) {
                currentTool = new CircleTool();
            } else if (selectedToggle.equals(textToggleButton)) {
                currentTool = new TextTool();
            }
        }
        if (currentTool == null) {
            return;
        }
        if (currentTool instanceof TextTool) {
            showTextInputDialog(drawingContext, event);
        } else {
            currentTool.handleCanvasMousePressed(drawingContext, event);
        }
    }

    private void handleCanvasMouseDragged(MouseEvent event) {
        if (currentTool == null) {
            return;
        }
        currentTool.handleCanvasMouseDragged(drawingContext, event);
    }

    private void handleCanvasMouseReleased(MouseEvent event) {
        if (currentTool == null) {
            return;
        }
        System.out.println("Mouse released!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        currentTool.handleCanvasMouseReleased(drawingContext, event);
        clientCanvasService.addCanvasItemToSync(currentTool.toProto());
        // clear drawing canvas
        drawingContext.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        // draw to stage canvas
        currentTool.draw(stageContext);
        currentTool = null;
    }

    @FXML
    private void saveCanvas() {
        if (!clientCanvasService.getCurrentUser().getIsHost()) {
            showAlert(Alert.AlertType.INFORMATION, "Only host can use this function", false);
        }
        clientCanvasService.saveCanvas(prop.getDefaultSavePath());
        showAlert(Alert.AlertType.INFORMATION, "Canvas saved successfully to " + prop.getDefaultSavePath() + " !!", false);
    }

    @FXML
    private void saveCanvasAs() {
        if (!clientCanvasService.getCurrentUser().getIsHost()) {
            showAlert(Alert.AlertType.INFORMATION, "Only host can use this function", false);
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Save File");

        // Set initial directory (optional)
        File initialDirectory = new File(System.getProperty("user.home"));
        fileChooser.setInitialDirectory(initialDirectory);

        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Save Files (*.bak)", "*.bak");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialFileName(prop.getDefaultSaveFileName());

        // Show file chooser dialog
        File selectedFile = fileChooser.showSaveDialog(JavaFxLauncher.primaryStage.getScene().getWindow());

        // Check if directory is selected
        if (selectedFile != null) {
            clientCanvasService.saveCanvas(selectedFile.getAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Canvas saved successfully to " + selectedFile.getAbsolutePath() + " !!", false);
        }
    }

    @FXML
    private void resetCanvas() {
        if (!clientCanvasService.getCurrentUser().getIsHost()) {
            showAlert(Alert.AlertType.INFORMATION, "Only host can use this function", false);
        }
        clientCanvasService.resetCanvas();
        showAlert(Alert.AlertType.INFORMATION, "New canvas created successfully !!", false);
    }

    @FXML
    private void importCanvas() {
        if (!clientCanvasService.getCurrentUser().getIsHost()) {
            showAlert(Alert.AlertType.INFORMATION, "Only host can use this function", false);
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Open File");

        // Set initial directory (optional)
        File initialDirectory = new File(System.getProperty("user.home"));
        fileChooser.setInitialDirectory(initialDirectory);

        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Save Files (*.bak)", "*.bak");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show file chooser dialog
        File selectedFile = fileChooser.showOpenDialog(JavaFxLauncher.primaryStage.getScene().getWindow());

        // Check if directory is selected
        if (selectedFile != null) {
            try {
                clientCanvasService.resetCanvas(true, selectedFile.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.WARNING, "Failed to import canvas from " + selectedFile.getAbsolutePath() + " !!\n" + e.getMessage(), false);
            }
            showAlert(Alert.AlertType.INFORMATION, "Canvas saved successfully to " + selectedFile.getAbsolutePath() + " !!", false);
        }
    }


    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private <V> List<V> getValuesSortedByKeys(Map<String, V> map) {
        List<V> values = new ArrayList<>();
        List<String> keys = new ArrayList<>(map.keySet());
        keys.sort(Comparator.naturalOrder());
        for (String key : keys) {
            if (!Objects.equals(key, clientCanvasService.getCurrentUser().getUserId())) {
                values.add(map.get(key));
            }
        }
        return values;
    }

    private <T> void mergeInPlace(List<T> src, List<T> target, Comparator<T> comparator) {
        var copySrc = new ArrayList<>(src);
        var copyTarget = new ArrayList<>(target);
        copyTarget.sort(comparator);
        var iter = copySrc.iterator();
        var targetIter = copyTarget.iterator();
        // add all items in target
        while (iter.hasNext() && targetIter.hasNext()) {
            var item = iter.next();
            while (targetIter.hasNext()) {
                var targetItem = targetIter.next();
                if (comparator.compare(targetItem, item) < 0) {
                    src.add(src.indexOf(item), targetItem);
                } else {
                    break;
                }
            }
        }
        while (targetIter.hasNext()) {
            src.add(targetIter.next());
        }
        // remove all item not in target
        src.removeIf(item -> !target.contains(item));
    }
}

