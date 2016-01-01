package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Module;

/**
 * Created by florent on 31/12/15.
 */
public abstract class BaseUIFunction implements UIFunction {
    private final String name;
    protected final ModuleManager manager;

    public BaseUIFunction(String name, ModuleManager manager) {
        this.name = name;
        this.manager = manager;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
