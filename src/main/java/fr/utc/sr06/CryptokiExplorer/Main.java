package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.objects.*;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.wrapper.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("il a pas dit bonjour.");

        try {
            //CREE UN TOKEN AVEC 2 CLES RSA (PUBLIC ET PRIVEE);
            createToken();

            Module m = Module.getInstance("/home/victor/sr06_2/softhsm/lib/softhsm/libsofthsm2.so");
            m.initialize(null);
            PKCS11 pkcs11 = m.getPKCS11Module();

            Slot[] slots = m.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);

            //On prend le dernier slot contenant un token
            Token tok = slots[slots.length-1].getToken();

            //Liste des mecanismes sur ce token
            for (Mechanism mech: tok.getMechanismList()) {
                MechanismInfo mi = tok.getMechanismInfo(mech);
                //System.out.format("Mechanism: %s\n", mech.getName());
                //System.out.println(mi);
            }

            m.finalize(null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TokenException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public static void createToken() throws TokenException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {

        Module m = Module.getInstance("/home/victor/sr06_2/softhsm/lib/softhsm/libsofthsm2.so");
        m.initialize(null);

        PKCS11Implementation pkcs11 = (PKCS11Implementation) m.getPKCS11Module();

        Slot[] slots =  m.getSlotList(Module.SlotRequirement.ALL_SLOTS);

        //Le dernier slot est le slot vide.
        Slot slot = slots[slots.length-1];
        Token tok = slot.getToken();

        char[] pin = "123456".toCharArray();
        //Init le token (efface toutes les donnees) avec le mot de passe admin (so)
        tok.initToken(pin, "TokenMortel1");

        //Obtenir la session du token.
        Session session = session = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);

        //On login pkcs11 sur le token
        pkcs11.C_Login(session.getSessionHandle(), 0, "123456".toCharArray(), true);
        //On init le pin du user :
        pkcs11.C_InitPIN(session.getSessionHandle(), "0000".toCharArray(), false);
        session.closeSession();

        Session session2 = session = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        session2.login(Session.UserType.USER, "0000".toCharArray());
        //ON CHANGE LE PIN USER POUR METTRE 1234
        session2.setPIN("0000".toCharArray(), "1234".toCharArray());


        System.out
                .println("################################################################################");
        System.out.print("Generating new 2048 bit RSA key-pair... ");
        System.out.flush();

        // first check out what attributes of the keys we may set
        HashSet supportedMechanisms = new HashSet(Arrays.asList(tok.getMechanismList()));

        MechanismInfo signatureMechanismInfo;
        if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_PKCS))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism
                    .get(PKCS11Constants.CKM_RSA_PKCS));
        } else if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_X_509))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism
                    .get(PKCS11Constants.CKM_RSA_X_509));
        } else if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_9796))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism
                    .get(PKCS11Constants.CKM_RSA_9796));
        } else if (supportedMechanisms.contains(Mechanism
                .get(PKCS11Constants.CKM_RSA_PKCS_OAEP))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism
                    .get(PKCS11Constants.CKM_RSA_PKCS_OAEP));
        } else {
            signatureMechanismInfo = null;
        }

        Mechanism keyPairGenerationMechanism = Mechanism
                .get(PKCS11Constants.CKM_RSA_PKCS_KEY_PAIR_GEN);
        RSAPublicKey rsaPublicKeyTemplate = new RSAPublicKey();
        RSAPrivateKey rsaPrivateKeyTemplate = new RSAPrivateKey();

        // set the general attributes for the public key
        rsaPublicKeyTemplate.getModulusBits().setLongValue(new Long(2048));
        byte[] publicExponentBytes = { 0x01, 0x00, 0x01 }; // 2^16 + 1
        rsaPublicKeyTemplate.getPublicExponent().setByteArrayValue(publicExponentBytes);
        rsaPublicKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
        byte[] id = new byte[20];
        new Random().nextBytes(id);
        rsaPublicKeyTemplate.getId().setByteArrayValue(id);
        // rsaPublicKeyTemplate.getLabel().setCharArrayValue(args[2].toCharArray());

        rsaPrivateKeyTemplate.getSensitive().setBooleanValue(Boolean.TRUE);
        rsaPrivateKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
        rsaPrivateKeyTemplate.getPrivate().setBooleanValue(Boolean.TRUE);
        rsaPrivateKeyTemplate.getId().setByteArrayValue(id);
        // byte[] subject = args[1].getBytes();
        // rsaPrivateKeyTemplate.getSubject().setByteArrayValue(subject);
        // rsaPrivateKeyTemplate.getLabel().setCharArrayValue(args[2].toCharArray());

        // set the attributes in a way netscape does, this should work with most tokens
        if (signatureMechanismInfo != null) {
            rsaPublicKeyTemplate.getVerify().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isVerify()));
            rsaPublicKeyTemplate.getVerifyRecover().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isVerifyRecover()));
            rsaPublicKeyTemplate.getEncrypt().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isEncrypt()));
            rsaPublicKeyTemplate.getDerive().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isDerive()));
            rsaPublicKeyTemplate.getWrap().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isWrap()));

            rsaPrivateKeyTemplate.getSign().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isSign()));
            rsaPrivateKeyTemplate.getSignRecover().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isSignRecover()));
            rsaPrivateKeyTemplate.getDecrypt().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isDecrypt()));
            rsaPrivateKeyTemplate.getDerive().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isDerive()));
            rsaPrivateKeyTemplate.getUnwrap().setBooleanValue(
                    new Boolean(signatureMechanismInfo.isUnwrap()));
        } else {
            // if we have no information we assume these attributes
            rsaPrivateKeyTemplate.getSign().setBooleanValue(Boolean.TRUE);
            rsaPrivateKeyTemplate.getDecrypt().setBooleanValue(Boolean.TRUE);

            rsaPublicKeyTemplate.getVerify().setBooleanValue(Boolean.TRUE);
            rsaPublicKeyTemplate.getEncrypt().setBooleanValue(Boolean.TRUE);
        }

        // netscape does not set these attribute, so we do no either
        rsaPublicKeyTemplate.getKeyType().setPresent(false);
        rsaPublicKeyTemplate.getObjectClass().setPresent(false);

        rsaPrivateKeyTemplate.getKeyType().setPresent(false);
        rsaPrivateKeyTemplate.getObjectClass().setPresent(false);

        KeyPair generatedKeyPair = session2.generateKeyPair(keyPairGenerationMechanism,
                rsaPublicKeyTemplate, rsaPrivateKeyTemplate);
        RSAPublicKey generatedRSAPublicKey = (RSAPublicKey) generatedKeyPair.getPublicKey();
        RSAPrivateKey generatedRSAPrivateKey = (RSAPrivateKey) generatedKeyPair
                .getPrivateKey();
        // no we may work with the keys...

        System.out.println("Success");
        System.out.println("The public key is");
        System.out
                .println("_______________________________________________________________________________");
        System.out.println(generatedRSAPublicKey);
        System.out
                .println("_______________________________________________________________________________");
        System.out.println("The private key is");
        System.out
                .println("_______________________________________________________________________________");
        System.out.println(generatedRSAPrivateKey);
        System.out
                .println("_______________________________________________________________________________");

        // write the public key to file
        System.out
                .println("################################################################################");
        //System.out.println("Writing the public key of the generated key-pair to file: "+ args[1]);
        RSAPublicKey exportableRsaPublicKey = generatedRSAPublicKey;
        BigInteger modulus = new BigInteger(1, exportableRsaPublicKey.getModulus()
                .getByteArrayValue());
        BigInteger publicExponent = new BigInteger(1, exportableRsaPublicKey
                .getPublicExponent().getByteArrayValue());
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, publicExponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        java.security.interfaces.RSAPublicKey javaRsaPublicKey = (java.security.interfaces.RSAPublicKey) keyFactory
                .generatePublic(rsaPublicKeySpec);
        X509EncodedKeySpec x509EncodedPublicKey = (X509EncodedKeySpec) keyFactory.getKeySpec(
                javaRsaPublicKey, X509EncodedKeySpec.class);

        /*FileOutputStream publicKeyFileStream = new FileOutputStream(args[1]);
        publicKeyFileStream.write(x509EncodedPublicKey.getEncoded());
        publicKeyFileStream.flush();
        publicKeyFileStream.close();
        */
        System.out
                .println("################################################################################");

        // now we try to search for the generated keys
        System.out
                .println("################################################################################");
        System.out
                .println("Trying to search for the public key of the generated key-pair by ID: "
                        + Functions.toHexString(id));
        // set the search template for the public key
        RSAPublicKey exportRsaPublicKeyTemplate = new RSAPublicKey();
        exportRsaPublicKeyTemplate.getId().setByteArrayValue(id);

        session2.findObjectsInit(exportRsaPublicKeyTemplate);
        Object[] foundPublicKeys = session.findObjects(1);
        session2.findObjectsFinal();

        if (foundPublicKeys.length != 1) {
            System.out.println("Error: Cannot find the public key under the given ID!");
        } else {
            System.out.println("Found public key!");
            System.out
                    .println("_______________________________________________________________________________");
            System.out.println(foundPublicKeys[0]);
            System.out
                    .println("_______________________________________________________________________________");
        }

        System.out
                .println("################################################################################");

        session2.closeSession();
        m.finalize(null);

    }

    public static int add(int a, int b) {
        return a + b;
    }
}
