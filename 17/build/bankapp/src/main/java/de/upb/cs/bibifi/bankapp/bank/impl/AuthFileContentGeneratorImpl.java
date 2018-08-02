package de.upb.cs.bibifi.bankapp.bank.impl;

import de.upb.cs.bibifi.bankapp.bank.IAuthFileContentGenerator;
import de.upb.cs.bibifi.commons.constants.AppConstants;
import org.json.JSONObject;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.util.Base64;
import java.util.Random;

public class AuthFileContentGeneratorImpl implements IAuthFileContentGenerator {

    private static AuthFileContentGeneratorImpl generator;

    private AuthFileContentGeneratorImpl() {
    }
    static {
        generator = null;
    }

    @Override
    public InputStream generateAuthFileContent() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AppConstants.CRYPTO_ALGORITHM_NAME);
        keyGenerator.init(128);

        SecretKey key = keyGenerator.generateKey();
        byte[] secretKeyBytesArray = key.getEncoded();

        Random random = new Random();
        byte[] saltBytesArray = new byte[16];
        random.nextBytes(saltBytesArray);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", Base64.getEncoder().encodeToString(secretKeyBytesArray));
        jsonObject.put("salt", new String(saltBytesArray));
        String jsonString = jsonObject.toString();

        return new ByteArrayInputStream(jsonString.getBytes());
    }

    public static IAuthFileContentGenerator getGenerator() {
        if (generator == null) {
            generator = new AuthFileContentGeneratorImpl();
        }
        return generator;
    }

}
