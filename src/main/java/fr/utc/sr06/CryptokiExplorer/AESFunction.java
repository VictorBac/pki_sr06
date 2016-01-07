package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.objects.AESSecretKey;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;

public class AESFunction extends BaseUIFunction {
    private Node ui = null;

    public AESFunction(String name, ModuleManager manager) {
        super(name, manager);
    }

    @Override
    public void load(Slot slot) {
        try {
            Token tok = slot.getToken();

            if (tok == null) {
                ui = new Label("No token present");
            } else {
                createUI(tok);
            }
        } catch (TokenException e) {
            e.printStackTrace();
        }
    }

    private void createUI(Token tok) {
        VBox bigBox = new VBox();
        bigBox.getStyleClass().add("spaced-box");

        Label messageLabel = new Label("");

        // pin, label and key length in bites
        HBox inputBar = new HBox();
        inputBar.getStyleClass().add("pin-bar");
        Label pinLabel = new Label("PIN");
        PasswordField pinInput = new PasswordField();

        Label labelLabel = new Label("Label");
        TextField labelInput = new TextField();

        Label lengthLabel = new Label("Length");
        ChoiceBox<Long> lengthInput = new ChoiceBox<>();
        lengthInput.setItems(FXCollections.observableArrayList(128L, 192L, 256L));
        Platform.runLater(() -> lengthInput.getSelectionModel().selectFirst());
        inputBar.getChildren().setAll(pinLabel, pinInput, labelLabel, labelInput, lengthLabel, lengthInput);
        inputBar.setHgrow(labelInput, Priority.ALWAYS);

        // validity date range
        DatePicker AskDateStart = new DatePicker(LocalDate.now());
        Label labelDateStart = new Label("StartDate");
        DatePicker AskDateEnd = new DatePicker(LocalDate.now());
        Label labelDateEnd = new Label("EndDate");

        HBox datesBar = new HBox();
        datesBar.getStyleClass().add("spaced-box");
        datesBar.getChildren().addAll(labelDateStart, AskDateStart,labelDateEnd,AskDateEnd);

        //
        Button createButton = new Button("Create Key");

        createButton.setOnAction((event) -> {
            try {
                AESSecretKey key = manager.generateAESkey(tok, pinInput.getText(), labelInput.getText(), lengthInput.getValue());
                messageLabel.setText("Success ! \n\n" + key.toString());
            } catch (TokenException e) {
                messageLabel.setText(e.getMessage());
                e.printStackTrace();
            }
        });

        bigBox.getChildren().setAll(inputBar, createButton, messageLabel);
        ui = bigBox;
    }


    @Override
    public Node getUI() {
        // TODO: l'appelant doit appeler load avant getUI
        return ui;
    }

    @Override
    public void unload() {

    }
}
