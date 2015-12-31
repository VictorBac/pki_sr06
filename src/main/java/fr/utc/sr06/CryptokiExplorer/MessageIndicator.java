package fr.utc.sr06.CryptokiExplorer;

import javafx.scene.control.Label;

import java.util.Optional;

/**
 * Created by florent on 31/12/15.
 */
public class MessageIndicator extends Label {
    public void error(String message, Object... args) {
        setText(String.format(message, args));
    }

    public void info(String message, Object... args) {
        setText(String.format(message, args));
    }
}
