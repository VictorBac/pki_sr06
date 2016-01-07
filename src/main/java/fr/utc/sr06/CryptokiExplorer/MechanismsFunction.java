package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by florent on 31/12/15.
 */
public class MechanismsFunction extends BaseUIFunction {
    private VBox ui = null;
    private ResourceBundle translations;
    private SimpleStringProperty mechanismInfo = new SimpleStringProperty();

    private ChoiceBox<Pair<Mechanism, MechanismInfo>> select;
    private ObservableList<Pair<Mechanism, MechanismInfo>> mechanisms;

    public MechanismsFunction(String name, ModuleManager manager) { // TODO: il nous faut un moyen d'afficher des erreurs
        super(name, manager);
        translations = ResourceBundle.getBundle("translations/mechanisms", Locale.getDefault());
    }

    @Override
    public void load(Slot slot) {
        ui = new VBox();

        select = new ChoiceBox<>();
        mechanisms = FXCollections.observableArrayList();
        select.setItems(mechanisms);
        select.setConverter(new StringConverter<Pair<Mechanism, MechanismInfo>>() {
            @Override
            public String toString(Pair<Mechanism, MechanismInfo> object) {
                return object.getKey().getName();
            }

            @Override
            public Pair<Mechanism, MechanismInfo> fromString(String string) {
                return null;
            }
        });
        select.getSelectionModel().selectedItemProperty().addListener((ov, old, new_val) -> {
            mechanismInfo.set(new_val.getValue().toString());
        });

        TextArea infoArea = new TextArea();
        infoArea.textProperty().bind(mechanismInfo);

        try {
            Token token = slot.getToken();
            if (token != null) {
                for (Mechanism mec: token.getMechanismList()) {
                    mechanisms.add(new Pair<>(mec, token.getMechanismInfo(mec)));
                }

                if (!mechanisms.isEmpty()) {
                    Platform.runLater(() -> select.getSelectionModel().selectFirst());
                    mechanismInfo.set(mechanisms.get(0).getValue().toString());
                }
            } else {
                mechanismInfo.set(translations.getString("noToken"));
            }
        } catch (TokenException e) {
            e.printStackTrace();

        }

        ui.getChildren().setAll(select, infoArea);
        ui.setVgrow(infoArea, Priority.ALWAYS);
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
