import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

import javax.naming.ServiceUnavailableException;


public abstract class SimpleSec {
	protected Socket socket;
	protected BufferedWriter writer;
	protected BufferedReader reader;
	protected AESCrpyter aesCrypter;
	protected HandshakeHandler handshakeHandler;
	
	public SimpleSec() {
		handshakeHandler = new HandshakeHandler(this);
	}

	/**Encrypts the given String and writes it to "writer".
	 * Listens to answer after that.
	 * @param str to be sent
	 * @throws GeneralSecurityException
	 * */
	protected void transmitMessage(String str) throws IOException, GeneralSecurityException {
		
			
		
			String encryptedString = aesCrypter.encrypt(str);

			
			try {
				writer.write(encryptedString);
				writer.write('\n');
				writer.flush();
			}catch (IOException e1) {
				throw e1;
			}
			logForHandshake(str);
	}
	
	protected void closeConnection() {
		try {
			if(writer != null)
				writer.close();
			if(reader != null)
				reader.close();
			if(socket != null)
				socket.close();
		} catch (IOException e) {
		}
	}
	

	

	/**reads one line form Socket
	 * @throws IOException */
	protected String read() throws IOException{
		String line = reader.readLine();
		String plainString = null;
		try {
			plainString = aesCrypter.decrypt(line);
		} catch (GeneralSecurityException e) {
		}
		return plainString;
	}
	
	protected boolean startHandshake() throws ServiceUnavailableException, IOException, GeneralSecurityException {
		return handshakeHandler.startHandshake();
	}
	public void logForHandshake(String str) {
		handshakeHandler.log(str);
	}
	
	protected boolean handleHandshake(String msg) throws Exception {
		return handshakeHandler.handleHandshake(msg);
	}
}
