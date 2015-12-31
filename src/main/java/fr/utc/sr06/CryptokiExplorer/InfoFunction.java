package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Slot;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Created by florent on 31/12/15.
 */
public class InfoFunction extends BaseUIFunction {
    private Label ui;

    public InfoFunction(String name, Module cryptoModule) {
        super(name, cryptoModule);
    }

    @Override
    public void load(Slot slot) {
        ui = new Label(String.format("Slot n°%s", slot.getSlotID()));
    }

    @Override
    public Node getUI() {
        return ui;
    }

    @Override
    public void unload() {

    }
}
