package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Module;

/**
 * Created by florent on 31/12/15.
 */
public abstract class BaseUIFunction implements UIFunction {
    private final String name;
    protected final Module cryptoModule;

    public BaseUIFunction(String name, Module cryptoModule) {
        this.name = name;
        this.cryptoModule = cryptoModule;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
