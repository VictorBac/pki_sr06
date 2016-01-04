package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.Object;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by marine on 04/01/16.
 */
public class ObjectsToken extends BaseUIFunction {
    private ComboBox<Object> ui = null;

    private ResourceBundle translations;
    private ObservableList<Object> obj1;


    public ObjectsToken(String name, ModuleManager manager) { // TODO: il nous faut un moyen d'afficher des erreurs
        super(name, manager);
        translations = ResourceBundle.getBundle("translations/object", Locale.getDefault());
    }


    @Override
    public void load(Slot slot) {
        ui = new ComboBox<Object>();
        obj1 = FXCollections.observableArrayList();
        ui.setItems(obj1);
        ui.setCellFactory((view) -> new ObjectListCell());
        // TODO lancer sur un thread
        try {
            Token token = slot.getToken();
            if (token != null) {
                for (Object objs: manager.availableObjects(token)) {
                    obj1.add(objs);
                }

            } else {
                //ui.getItems().addAll(translations.getString("noToken"));
            }
        } catch (TokenException e) {
            e.printStackTrace();
        }
    }

    private class ObjectListCell extends ListCell<Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getAttribute(Attribute.LABEL).toString());
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
