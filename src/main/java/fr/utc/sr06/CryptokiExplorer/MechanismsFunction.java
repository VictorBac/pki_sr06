package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by florent on 31/12/15.
 */
public class MechanismsFunction extends BaseUIFunction {
    private TextArea ui = null;
    private ResourceBundle translations;
    private SimpleStringProperty mechanims=new SimpleStringProperty();

    public MechanismsFunction(String name, ModuleManager manager) { // TODO: il nous faut un moyen d'afficher des erreurs
        super(name, manager);
        translations = ResourceBundle.getBundle("translations/mechanisms", Locale.getDefault());
    }

    @Override
    public void load(Slot slot) {
        ui = new TextArea();
        ui.textProperty().bind(mechanims);
        // TODO lancer sur un thread
        try {
            Token token = slot.getToken();
            if (token != null) {
                StringBuilder data = new StringBuilder();

                for (Mechanism mec: token.getMechanismList()) {
                    data.append(mec.getName());
                    data.append('\n');
                    data.append(token.getMechanismInfo(mec).toString());
                    data.append("\n\n");
                }
                mechanims.set(data.toString());
                //ui.setText(data.toString());
            } else {
                mechanims.set(translations.getString("noToken"));
                //ui.setText(translations.getString("noToken"));
            }
        } catch (TokenException e) {
            e.printStackTrace();
        }

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
