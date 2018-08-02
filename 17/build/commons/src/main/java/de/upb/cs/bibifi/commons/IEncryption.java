package de.upb.cs.bibifi.commons;

import java.io.IOException;

/**
 * Interface for conveniently storing any representable in a stream securely
 */
public interface IEncryption {

	/**
	 * Serializes an encrypted representable to an output stream
	 *
	 * @param message The message which should be encrypted
	 * @return  outputStream  The output stream which should receive the encrypted representable
	 */
	  String encryptMessage(String message) throws IOException;

	/**
	 * Deserialize a representable by decrypting it from an input stream
	 *
	 * @param inputStream The input stream which can be read in order to receive the encrypted representable
	 *
	 * @return The Representation which was read and decrypted from the input stream
	 */
	  String decryptMessage(String inputStream) throws IOException;
}
