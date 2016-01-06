package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;
import java.util.ResourceBundle;

public class RSAFunction extends BaseUIFunction {
    private Node ui = null;

    public RSAFunction(String name, ModuleManager manager) {
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

        Label messageLabel = new Label("");

        HBox inputBar = new HBox();
        inputBar.getStyleClass().add("pin-bar");
        Label pinLabel = new Label("PIN");
        PasswordField pinInput = new PasswordField();

        Label labelLabel = new Label("Label");
        TextField labelInput = new TextField();

        Label modulusLabel = new Label("Length");
        ChoiceBox<Long> modulusInput = new ChoiceBox<>();
        modulusInput.setItems(FXCollections.observableArrayList(1024L, 2048L));
        Platform.runLater(() -> modulusInput.getSelectionModel().selectFirst());

        Button createButton = new Button("Create Pair");
        inputBar.getChildren().setAll(pinLabel, pinInput, labelLabel, labelInput, modulusLabel, modulusInput, createButton);
        inputBar.setHgrow(labelInput, Priority.ALWAYS);

        createButton.setOnAction((event) -> {
            try {
                manager.createRsaPairKey(tok, pinInput.getText(), labelInput.getText(), modulusInput.getValue());
                messageLabel.setText("Success !");
            } catch (TokenException e) {
                messageLabel.setText(e.getMessage());
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                messageLabel.setText(e.getMessage());
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                messageLabel.setText(e.getMessage());
                e.printStackTrace();
            }
        });

        bigBox.getChildren().setAll(inputBar, messageLabel);
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
