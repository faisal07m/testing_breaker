import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;



public class AESCrpyter {
	SecretKey secretKeySpec;
	Cipher chipher;
	
	/*
	public AESCrpyter(File encryptionKeyFile) {
		
		try (InputStream in = new FileInputStream(encryptionKeyFile);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
			
			this.chipher = Cipher.getInstance("AES");
			String keyString = reader.readLine();
			byte[] keyBytes = Base64.getDecoder().decode(keyString);
			secretKeySpec = new SecretKeySpec(keyBytes, "AES");
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
		}
	}
	*/
	
	
	public AESCrpyter(File encryptionKeyFile) {
		
		try (ObjectInputStream oInStream = new ObjectInputStream(new FileInputStream(encryptionKeyFile))) {
			
			secretKeySpec = (SecretKey) oInStream.readObject();
			
			this.chipher = Cipher.getInstance("AES");
			
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
		} catch (ClassNotFoundException e) {
		}
	}
	
	
	public String encrypt(String str) throws GeneralSecurityException {
		byte[] plainBytes = null;
		plainBytes = str.getBytes();
		byte[] encryptedBytes = null;
		
		try {
			chipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			encryptedBytes = chipher.doFinal(plainBytes);
		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
			throw e;
		}
		String encryptedString = Base64.getEncoder().encodeToString(encryptedBytes);
		return encryptedString;
	}
	
	public String decrypt(String str) throws GeneralSecurityException {
		byte[] encodedBytes = Base64.getDecoder().decode(str);
		byte[] decryptedBytes = null;
		
		try {
			chipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
			decryptedBytes = chipher.doFinal(encodedBytes);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw e;
		}

		String decryptedString = null;
		try {
			decryptedString = new String(decryptedBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		};
		return decryptedString;
	}
	
}
