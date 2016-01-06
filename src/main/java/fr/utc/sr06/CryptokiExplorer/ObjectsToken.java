package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.Object;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by marine on 04/01/16.
 */
public class ObjectsToken extends BaseUIFunction {
    private VBox ui = null;
    private ChoiceBox<Object> select = null;

    private ResourceBundle translations;
    private ObservableList<Object> obj1;


    public ObjectsToken(String name, ModuleManager manager) {
        super(name, manager);
        translations = ResourceBundle.getBundle("translations/object", Locale.getDefault());
    }

    private String t_(String key) {
        return translations.getString(key);
    }

    @Override
    public void load(Slot slot) {
        ui = new VBox();

        HBox pinBar = new HBox();
        pinBar.getStyleClass().add("pin-bar");
        Label pinLabel = new Label(t_("pinLabel"));
        PasswordField pinInput = new PasswordField();
        Button loadButton = new Button(t_("loadPrivateButton"));
        pinBar.getChildren().setAll(pinLabel, pinInput, loadButton);
        pinBar.setHgrow(pinInput, Priority.ALWAYS);

        select = new ChoiceBox<>();
        obj1 = FXCollections.observableArrayList();
        select.setItems(obj1);
        // display
        select.setConverter(new StringConverter<Object>() {
            @Override
            public String toString(Object object) {
                return String.format("%s %s \"%s\" (%s)", object.getAttribute(Attribute.KEY_TYPE), object.getAttribute(Attribute.CLASS), object.getAttribute(Attribute.LABEL), object.getAttribute(Attribute.ID));
            }

            @Override
            public Object fromString(String string) {
                return obj1.stream().filter(object -> this.toString(object).equals(string)).findFirst().orElse(null);
            }
        });


        TextArea description = new TextArea();
        select.getSelectionModel().selectedItemProperty().addListener((ov, old_value, new_value) -> {
            if (new_value != null) {
                description.setText(new_value.toString());
            }
        });

        Label messageLabel = new Label();

        pinInput.setOnAction((event) -> loadObjets(pinInput.getText(), slot, pinBar, messageLabel, description));
        loadButton.setOnMouseClicked((event) -> loadObjets(pinInput.getText(), slot, pinBar, messageLabel, description));

        try {
            Token token = slot.getToken();
            if (token != null) {
                obj1.setAll(manager.availableObjects(token));

                if (obj1.isEmpty()) {
                    messageLabel.setText(t_("noPublicObjects"));
                    ui.getChildren().setAll(pinBar, messageLabel);
                } else {
                    ui.getChildren().setAll(pinBar, select, description);
                    ui.setFillWidth(true);
                    ui.setVgrow(description, Priority.ALWAYS);
                    Platform.runLater(() -> select.getSelectionModel().selectFirst());
                }
            }
        } catch (TokenException e) {
            e.printStackTrace();
        }
    }

    private void loadObjets(String pin, Slot slot, HBox pinBar, Label messageLabel, TextArea description) {
        try {
            Token token = slot.getToken();
            if (token != null) {
                obj1.setAll(manager.availableObjects(token, pin));

                System.out.println("Objects: " + obj1.size());

                if (obj1.isEmpty()) {
                    messageLabel.setText(t_("noObjects"));
                    ui.getChildren().setAll(pinBar, messageLabel);
                } else {
                    ui.getChildren().setAll(pinBar, select, description);
                    ui.setFillWidth(true);
                    ui.setVgrow(description, Priority.ALWAYS);
                    Platform.runLater(() -> select.getSelectionModel().selectFirst());
                    description.setText(obj1.get(0).toString());
                }
            }
        } catch (TokenException e) {
            if (e.getMessage().equals("CKR_PIN_INCORRECT")) {
                messageLabel.setText(t_("wrongPin"));
                ui.getChildren().setAll(pinBar, messageLabel);
            } else {

                e.printStackTrace();
            }
        }
    }

    @Override
    public Node getUI() {
        return ui;
    }

    @Override
    public void unload() {

    }
}
