package fr.utc.sr06.CryptokiExplorer;

public class Main {
    public static void main(String[] args) {
        Window.main(args);

        /*
        //Exemple of use :
        String path = "/home/victor/sr06_2/softhsm/lib/softhsm/libsofthsm2.so";
        String userPin = "0000";
        String soPin = "123456";

        try {
            //Create the moduleManager and set the path
            ModuleManager moduleMan = new ModuleManager();
            moduleMan.setPathModule(path);

            //Create a new token
            Token tok = moduleMan.createToken("Test token ultime", soPin, userPin);

            //Change the user pin
            String newUserPin = "1234";
            moduleMan.changeUserPin(tok, userPin, newUserPin);

            //Create RSA key pair on the token
            moduleMan.createRsaPairKey(tok, newUserPin);

            //Finalise properly
            moduleMan.end();
        } catch (IOException | TokenException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }*/
    }
}
