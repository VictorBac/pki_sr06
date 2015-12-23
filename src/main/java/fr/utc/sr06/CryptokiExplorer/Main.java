package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("il a pas dit bonjour.");

        try {
            Module m = Module.getInstance("/usr/local/lib/softhsm/libsofthsm2.so");
            m.initialize(null);

            Slot[] slots = m.getSlotList(Module.SlotRequirement.ALL_SLOTS);
            for (Slot slot : slots) {
                long id = slot.getSlotID();
                System.out.format("Slot: %d\n%s\n", id, slot.getModule());

                if (slot.getSlotInfo().isTokenPresent()) {
                    Token t = slot.getToken(); // un slot = un token (ou null)

                    for (Mechanism mech: t.getMechanismList()) {
                        MechanismInfo mi = t.getMechanismInfo(mech);
                        System.out.format("Mechanism: %s\n", mech.getName());
                        System.out.println(mi);
                    }
                }

                /*System.out.println("C_Mechanism:");
                for (long mech_id : m.getPKCS11Module().C_GetMechanismList(id)) {
                    Mechanism mech = new Mechanism(mech_id);
                    System.out.format("Mechanism: %s\n", mech.getName());
                }*/
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