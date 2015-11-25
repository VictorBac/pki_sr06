package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.TokenException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("il a pas dit bonjour.");

        try {
            Module m = Module.getInstance("/usr/local/lib/softhsm/libsofthsm2.so");
            m.initialize(null);

            Slot[] slots = m.getSlotList(Module.SlotRequirement.ALL_SLOTS);
            for (Slot slot : slots) {
                System.out.format("Slot: %d\n%s", slot.getSlotID(), slot.getModule());
            }

            m.finalize(null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TokenException e) {
            e.printStackTrace();
        }
    }

    public static int add(int a, int b) {
        return a + b;
    }
}