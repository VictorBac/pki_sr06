package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Created by florent on 31/12/15.
 */
public class InfoFunction extends BaseUIFunction {
    private Label ui;

    public InfoFunction(String name, ModuleManager cryptoModule) {
        super(name, cryptoModule);
    }

    @Override
    public void load(Slot slot) {
        String description = String.format("Slot n°%s", slot.getSlotID());

        try {
            SlotInfo info = slot.getSlotInfo();
            description += String.format("\nDescription: ", info.getSlotDescription());

            Token tok = slot.getToken();

            description = info.toString();

            if (tok != null) {
                TokenInfo tokInfo = tok.getTokenInfo();

                description += "\n\nToken present:\n" + tokInfo.toString();

                //description += String.format("\n\n", tokInfo.getLabel());
            }
        } catch (TokenException e) {
            e.printStackTrace();
        }

        ui = new Label(description);
    }

    @Override
    public Node getUI() {
        return ui;
    }

    @Override
    public void unload() {

    }
}
