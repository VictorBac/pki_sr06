package fr.utc.sr06.CryptokiExplorer;

import com.sun.org.apache.xpath.internal.operations.Mod;
import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.objects.KeyPair;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.RSAPublicKey;
import iaik.pkcs.pkcs11.wrapper.Functions;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Implementation;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 * Created by victor on 31/12/15.
 */
public class ModuleManager {

    protected Module m;

    public ModuleManager() {}

    public ModuleManager(String path) throws IOException, TokenException {
        m = Module.getInstance(path);
        m.initialize(null);
    }

    public void setPathModule(String path) throws IOException, TokenException {
        m = Module.getInstance(path);
        m.initialize(null);
    }

    public Module getModule() {
        return this.m;
    }

    public Slot[] getAllSlots() throws TokenException {
        return m.getSlotList(Module.SlotRequirement.ALL_SLOTS);
    }

    public Slot getFreeSlot() throws TokenException {
        Slot[] slots = m.getSlotList(Module.SlotRequirement.ALL_SLOTS);
        return slots[slots.length-1];
    }

    public Slot[] getSlotsWithToken() throws TokenException {
        return m.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
    }

    public Token getLastToken() throws TokenException {
        Slot[] slots = getSlotsWithToken();
        return slots[slots.length-1].getToken();
    }

    public Token getToken(int id) throws TokenException {
        Slot[] slots = getSlotsWithToken();
            return slots[id].getToken();
    }


    public Token createToken(String label, String soPin, String userPin) throws TokenException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {

        PKCS11Implementation pkcs11 = (PKCS11Implementation) m.getPKCS11Module();

        Slot slot = getFreeSlot();
        Token tok = slot.getToken();

        //Init the token (erase all data), set the label and the so-pin
        tok.initToken(soPin.toCharArray(), label);

        //Get the session.
        Session session = session = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);

        //Log pkcs11 on the token
        pkcs11.C_Login(session.getSessionHandle(), 0, soPin.toCharArray(), true);
        //Init the user pin :
        pkcs11.C_InitPIN(session.getSessionHandle(), userPin.toCharArray(), false);

        //Finally we close the session
        session.closeSession();

        return tok;
    }


    public void changeUserPin(Token tok, String oldPin, String newPin) throws TokenException {

        Session session = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        session.login(Session.UserType.USER, oldPin.toCharArray());

        //We set the new ping :
        session.setPIN(oldPin.toCharArray(), newPin.toCharArray());
        session.closeSession();
        System.out.println("Pin changed");
    }


    public void createRsaPairKey(Token tok, String userPin) throws TokenException, NoSuchAlgorithmException, InvalidKeySpecException {

        Session session2 = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        session2.login(Session.UserType.USER, userPin.toCharArray());

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
        Object[] foundPublicKeys = session2.findObjects(1);
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
        //m.finalize(null);

    }

    public void end() throws TokenException {
        m.finalize(null);
    }
}
