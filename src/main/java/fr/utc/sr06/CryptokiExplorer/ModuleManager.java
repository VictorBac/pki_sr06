package fr.utc.sr06.CryptokiExplorer;

import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.objects.*;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.parameters.InitializationVectorParameters;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Implementation;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;


/**
 * Created by victor on 31/12/15.
 */
public class ModuleManager {

    protected Module m;
    protected String path;

    public ModuleManager() {}

    public ModuleManager(String path) throws IOException, TokenException {
        setPathModule(path);
    }

    public void setPathModule(String path) throws IOException, TokenException {
        m = Module.getInstance(path);
        m.initialize(null);
        this.path = path;
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

    public Map<Mechanism, MechanismInfo> getMechanismsInfo(Token tok) throws TokenException {
        Mechanism[] mechs = tok.getMechanismList();
        HashMap<Mechanism, MechanismInfo> result = new HashMap<>(mechs.length);

        for(Mechanism m : mechs) {
            result.put(m, tok.getMechanismInfo(m));
        }

        return result;
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

    public void createRsaPairKey(Token tok, String userPin, String label, long modulusArg) throws TokenException, NoSuchAlgorithmException, InvalidKeySpecException {
        Session session = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        session.login(Session.UserType.USER, userPin.toCharArray());

        // first check out what attributes of the keys we may set
        HashSet supportedMechanisms = new HashSet<>(Arrays.asList(tok.getMechanismList()));

        MechanismInfo signatureMechanismInfo;
        if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_PKCS))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism.get(PKCS11Constants.CKM_RSA_PKCS));
        } else if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_X_509))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism.get(PKCS11Constants.CKM_RSA_X_509));
        } else if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_9796))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism.get(PKCS11Constants.CKM_RSA_9796));
        } else if (supportedMechanisms.contains(Mechanism.get(PKCS11Constants.CKM_RSA_PKCS_OAEP))) {
            signatureMechanismInfo = tok.getMechanismInfo(Mechanism.get(PKCS11Constants.CKM_RSA_PKCS_OAEP));
        } else {
            signatureMechanismInfo = null;
        }

        Mechanism keyPairGenerationMechanism = Mechanism
                .get(PKCS11Constants.CKM_RSA_PKCS_KEY_PAIR_GEN);
        RSAPublicKey rsaPublicKeyTemplate = new RSAPublicKey();
        RSAPrivateKey rsaPrivateKeyTemplate = new RSAPrivateKey();

        // set the general attributes for the public key
        rsaPublicKeyTemplate.getModulusBits().setLongValue(modulusArg);
        byte[] publicExponentBytes = { 0x01, 0x00, 0x01 }; // 2^16 + 1
        rsaPublicKeyTemplate.getPublicExponent().setByteArrayValue(publicExponentBytes);
        rsaPublicKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
        byte[] id = new byte[20];
        new Random().nextBytes(id);
        rsaPublicKeyTemplate.getId().setByteArrayValue(id);
        rsaPublicKeyTemplate.getLabel().setCharArrayValue(label.toCharArray());

        rsaPrivateKeyTemplate.getSensitive().setBooleanValue(Boolean.TRUE);
        rsaPrivateKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
        rsaPrivateKeyTemplate.getPrivate().setBooleanValue(Boolean.TRUE);
        rsaPrivateKeyTemplate.getId().setByteArrayValue(id);
        rsaPrivateKeyTemplate.getLabel().setCharArrayValue(label.toCharArray());

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

        KeyPair generatedKeyPair = session.generateKeyPair(keyPairGenerationMechanism,
                rsaPublicKeyTemplate, rsaPrivateKeyTemplate);
        RSAPublicKey generatedRSAPublicKey = (RSAPublicKey) generatedKeyPair.getPublicKey();
        RSAPrivateKey generatedRSAPrivateKey = (RSAPrivateKey) generatedKeyPair
                .getPrivateKey();
        // no we may work with the keys...

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

        // now we try to search for the generated keys
        // set the search template for the public key
        RSAPublicKey exportRsaPublicKeyTemplate = new RSAPublicKey();
        exportRsaPublicKeyTemplate.getId().setByteArrayValue(id);

        session.findObjectsInit(exportRsaPublicKeyTemplate);
        Object[] foundPublicKeys = session.findObjects(1);
        session.findObjectsFinal();

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

        session.logout();
        session.closeSession();
    }

    private List<Object> listObjects(Session sessionObj) throws TokenException {
        List<Object> obj = new ArrayList<>();
        sessionObj.findObjectsInit(null);
        Object[] objTok=sessionObj.findObjects(1);

        while (objTok.length!=0){ //il reste des objets non listés
            obj.add(objTok[0]);
            System.out.println(objTok[0].toString());
            objTok=sessionObj.findObjects(1);
        }
        sessionObj.findObjectsFinal();
        return  obj;
    }

    public List<Object> availableObjects (Token tok) throws TokenException {
        Session sessionObj = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        List<Object> obj = listObjects(sessionObj);
        return obj;
    }

    public List<Object> availableObjects (Token tok, String pin) throws TokenException {
        List<Object> obj = availableObjects(tok);

        char[] mdp = pin.toCharArray();
        Session sessionObj = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        sessionObj.login(Session.UserType.USER, mdp);

        obj.addAll(listObjects(sessionObj));
        return  obj;
    }

    @FunctionalInterface
    public interface SessionFunction<R> {
        R apply(Session s) throws TokenException;
    }

    private <R> R withUserSession(Token tok, String pinS, SessionFunction<R> body) throws TokenException {
        char[] pin = pinS.toCharArray();

        Session sessionObj = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        sessionObj.login(Session.UserType.USER, pin);

        R result = body.apply(sessionObj);

        sessionObj.logout();
        sessionObj.closeSession();

        return result;
    }

    public void destroyObject (Token tok, Object objToDestroy, String pinS) throws TokenException {
        char[] mdp = pinS.toCharArray();

        Session sessionObj = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        sessionObj.destroyObject(objToDestroy);
        System.out.println("destruction2");
    }

    public void objectDeconnection (Token tok) throws  TokenException {
        Session sessionObj = tok.openSession(Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RW_SESSION, null, null);
        sessionObj.logout();
        sessionObj.closeSession();
    }

    public AESSecretKey generateAESkey (Token token, String pin, String Label, long bitesLength) throws TokenException {
        return withUserSession(token, pin, (session) -> {
            Mechanism keyGenerationMechanism = Mechanism.get(PKCS11Constants.CKM_AES_KEY_GEN);
            AESSecretKey keyTemplate = new AESSecretKey();

            byte[] id = new byte[20];
            new Random().nextBytes(id);
            keyTemplate.getId().setByteArrayValue(id);

            keyTemplate.getValueLen().setLongValue(bitesLength / 8L);
            keyTemplate.getLabel().setCharArrayValue(Label.toCharArray());
            keyTemplate.getToken().setBooleanValue(true);
            keyTemplate.getModifiable().setBooleanValue(false); // ?
            //keyTemplate.getStartDate().setDateValue(Date.from(StartDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
            //keyTemplate.getEndDate().setDateValue(Date.from(EndDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));

            MechanismInfo mechInfo = token.getMechanismInfo(keyGenerationMechanism);
            keyTemplate.getVerify().setBooleanValue(mechInfo.isVerify());
            keyTemplate.getEncrypt().setBooleanValue(mechInfo.isEncrypt());
            keyTemplate.getDerive().setBooleanValue(mechInfo.isDerive());
            keyTemplate.getDecrypt().setBooleanValue(mechInfo.isDecrypt());
            keyTemplate.getWrap().setBooleanValue(mechInfo.isWrap());

            AESSecretKey aesKey = (AESSecretKey) session.generateKey(keyGenerationMechanism, keyTemplate);

            // search for newly generated key
            AESSecretKey aesSearchTemplate = new AESSecretKey();
            aesSearchTemplate.getId().setByteArrayValue(id);

            session.findObjectsInit(aesSearchTemplate);
            Object[] foundPublicKeys = session.findObjects(1);
            session.findObjectsFinal();

            if (foundPublicKeys.length != 1) {
                System.out.println("Error: Cannot find the key under the given ID!");
            } else {
                System.out.println("Found the key!");
                System.out.println(foundPublicKeys[0]);
            }

            return aesKey;
        });
    }

    public void encryptAES(Token token, String pin, String FileToEncrypt, String EncryptedFile, AESSecretKey encryptionKey) throws TokenException, IOException {
        Session session = openAuthorizedSession(token, Token.SessionReadWriteBehavior.RW_SESSION, pin);

        Mechanism encryptionMechanism = Mechanism.get(PKCS11Constants.CKM_AES_CBC_PAD);
        byte[] encryptInitializationVector = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        InitializationVectorParameters encryptInitializationVectorParameters = new InitializationVectorParameters(
                encryptInitializationVector);
        encryptionMechanism.setParameters(encryptInitializationVectorParameters);

        // initialize for encryption
        session.encryptInit(encryptionMechanism, encryptionKey);

        String raww = "/media/raphael/DISQUEDUR/pki_sr06/src/main/resources/FileToEncrypt/testencrypt.txt";
        InputStream dataInputStream = new FileInputStream(FileToEncrypt);

        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();

        // feed in all data from the input stream
        while ((bytesRead = dataInputStream.read(dataBuffer)) >= 0) {
            streamBuffer.write(dataBuffer, 0, bytesRead);
        }

        Arrays.fill(dataBuffer, (byte) 0); // ensure that no data is left in the
        // memory
        streamBuffer.flush();

        byte[] rawData = streamBuffer.toByteArray();
        byte[] encryptedData = session.encrypt(rawData);
        streamBuffer.close();


        String encryptedDataa = "/media/raphael/DISQUEDUR/pki_sr06/src/main/resources/FileToEncrypt/encryptedtext.txt";

InputStream dataOut = new ByteArrayInputStream(encryptedData);
        OutputStream output = new FileOutputStream(EncryptedFile);
        while ((bytesRead = dataOut.read(dataBuffer)) >= 0) {
            output.write(dataBuffer, 0, bytesRead);
        }

        Arrays.fill(dataBuffer, (byte) 0); // ensure that no data is left in the
output.close();


        System.out
                .println("################################################################################");

        System.out
                .println("################################################################################");
        System.out.println("trying to decrypt");

        // Cipher des3Cipher = Cipher.getInstance("DES3/CBC/PKCS5Padding");

        Mechanism decryptionMechanism = Mechanism.get(PKCS11Constants.CKM_AES_CBC_PAD);
        byte[] decryptInitializationVector = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        InitializationVectorParameters decryptInitializationVectorParameters = new InitializationVectorParameters(
                decryptInitializationVector);
        decryptionMechanism.setParameters(decryptInitializationVectorParameters);

        // initialize for decryption
        session.decryptInit(decryptionMechanism, encryptionKey);

        byte[] decryptedData = session.decrypt(encryptedData);

        // compare initial data and decrypted data
        boolean equal = false;
        if (rawData.length != decryptedData.length) {
            equal = false;
        } else {
            equal = true;
            for (int i = 0; i < rawData.length; i++) {
                if (rawData[i] != decryptedData[i]) {
                    equal = false;
                    break;
                }
            }
        }
        System.out.println((equal) ? "successful" : "ERROR");

        session.closeSession();

    }

    public static Session openAuthorizedSession(Token token, boolean rwSession,
                                                 String pin) throws TokenException,
            IOException {
        if (token == null) {
            throw new NullPointerException("Argument \"token\" must not be null.");
        }


        System.out
                .println("################################################################################");
        System.out.println("opening session");
        Session session = token.openSession(Token.SessionType.SERIAL_SESSION, rwSession,
                null, null);

        TokenInfo tokenInfo = token.getTokenInfo();
        if (tokenInfo.isLoginRequired()) {
            if (tokenInfo.isProtectedAuthenticationPath()) {
                System.out.print("Please enter the user-PIN at the PIN-pad of your reader.");
                System.out.flush();
                session.login(Session.UserType.USER, null); // the token prompts the PIN by other means;
                // e.g. PIN-pad
            } else {
                System.out.print("Enter user-PIN and press [return key]: ");
                System.out.flush();
                String userPINString;
                if (null != pin) {
                    userPINString = pin;
                    System.out.println(pin);
                    session.login(Session.UserType.USER, userPINString.toCharArray());
                }
            }
        }
        System.out
                .println("################################################################################");

        return session;
    }



    public void wrapAES (Slot item, String pin, String FileToEncrypt, String EncryptedFile) throws TokenException, IOException {
        Token token = item.getToken();

        Session session;
        session = openAuthorizedSession(token, Token.SessionReadWriteBehavior.RW_SESSION, pin);


        System.out
                .println("################################################################################");
        System.out.println("generate secret encryption/decryption key");
        Mechanism keyMechanism = Mechanism.get(PKCS11Constants.CKM_AES_KEY_GEN);
        AESSecretKey secretEncryptionKeyTemplate = new AESSecretKey();
        byte[] id = new byte[20];

        new Random().nextBytes(id);
        secretEncryptionKeyTemplate.getId().setByteArrayValue(id);
        secretEncryptionKeyTemplate.getLabel().setCharArrayValue("WRAPENCRIPTKEY".toCharArray());
        secretEncryptionKeyTemplate.getWrapWithTrusted().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getToken().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getValueLen().setLongValue(new Long(16));
        secretEncryptionKeyTemplate.getEncrypt().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getDecrypt().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getPrivate().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getSensitive().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getExtractable().setBooleanValue(Boolean.TRUE);
        secretEncryptionKeyTemplate.getWrap().setBooleanValue(Boolean.TRUE);

        AESSecretKey encryptionKey = (AESSecretKey) session.generateKey(keyMechanism,
                secretEncryptionKeyTemplate);

        System.out
                .println("################################################################################");

        System.out
                .println("################################################################################");
        System.out.println("encrypting data from file: " + FileToEncrypt);

        InputStream dataInputStream = new FileInputStream(FileToEncrypt);

        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();

        // feed in all data from the input stream
        while ((bytesRead = dataInputStream.read(dataBuffer)) >= 0) {
            streamBuffer.write(dataBuffer, 0, bytesRead);
        }
        Arrays.fill(dataBuffer, (byte) 0); // ensure that no data is left in the
        // memory
        streamBuffer.flush();
        streamBuffer.close();
        byte[] rawData = streamBuffer.toByteArray();

        // be sure that your token can process the specified mechanism
        Mechanism encryptionMechanism = Mechanism.get(PKCS11Constants.CKM_AES_CBC_PAD);
        byte[] encryptInitializationVector = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        InitializationVectorParameters encryptInitializationVectorParameters = new InitializationVectorParameters(
                encryptInitializationVector);
        encryptionMechanism.setParameters(encryptInitializationVectorParameters);

        // initialize for encryption
        session.encryptInit(encryptionMechanism, encryptionKey);

        byte[] encryptedData = session.encrypt(rawData);

        System.out
                .println("################################################################################");

        System.out.println("generate secret wrapping key");

        AESSecretKey wrappingKey = (AESSecretKey) session.generateKey(keyMechanism,
                secretEncryptionKeyTemplate);

        System.out.println("wrapping key");

        byte[] wrappedKey = session.wrapKey(encryptionMechanism, wrappingKey, encryptionKey);
        AESSecretKey keyTemplate = new AESSecretKey();
        keyTemplate.getDecrypt().setBooleanValue(Boolean.TRUE);

        System.out.println("unwrapping key");

        AESSecretKey unwrappedKey = (AESSecretKey) session.unwrapKey(encryptionMechanism,
                wrappingKey, wrappedKey, keyTemplate);

        System.out
                .println("################################################################################");
        System.out.println("trying to decrypt");

        Mechanism decryptionMechanism = Mechanism.get(PKCS11Constants.CKM_AES_CBC_PAD);
        byte[] decryptInitializationVector = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        InitializationVectorParameters decryptInitializationVectorParameters = new InitializationVectorParameters(
                decryptInitializationVector);
        decryptionMechanism.setParameters(decryptInitializationVectorParameters);

        // initialize for decryption
        session.decryptInit(decryptionMechanism, unwrappedKey);

        byte[] decryptedData = session.decrypt(encryptedData);

        // compare initial data and decrypted data
        boolean equal = false;
        if (rawData.length != decryptedData.length) {
            equal = false;
        } else {
            equal = true;
            for (int i = 0; i < rawData.length; i++) {
                if (rawData[i] != decryptedData[i]) {
                    equal = false;
                    break;
                }
            }
        }

        System.out.println((equal) ? "successful" : "ERROR");

        System.out
                .println("################################################################################");

        session.closeSession();


    }

    public void end() throws TokenException {
        if (m != null) {
            m.finalize(null);
            m = null;
        }
    }
}

