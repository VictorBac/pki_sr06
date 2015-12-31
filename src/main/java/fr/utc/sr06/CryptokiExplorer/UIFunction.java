package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.TokenException;
import javafx.scene.Node;

/*
Processus quand on veut un fichier une fonction:
load()
getUI() -> affichage dans la fenêtre
(unload() sur l'ancienne fonction)
 */

public interface UIFunction {
    void load(Slot slot);
    Node getUI();
    void unload();
    String toString();
}
