import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;

import javax.naming.ServiceUnavailableException;

public class SimpleSecClient extends SimpleSec{
	private static final boolean isDEBUG_MODE = false;
	private ATM atm;
	private boolean communicationIsFinished;

	public SimpleSecClient(String ip, int port, ATM atm, File encryptionKeyFile)
			throws IOException {
		
		super();
		if (!encryptionKeyFile.exists()) {
			System.exit(255);
		}
		
		//init Socket
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip, port), 10000);
		
		if(isDEBUG_MODE) {
			socket.setSoTimeout(500000);
		}else {
			socket.setSoTimeout(10000);
		}
		writer = new BufferedWriter(new OutputStreamWriter((socket.getOutputStream())));
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		this.atm = atm;
		aesCrypter = new AESCrpyter(encryptionKeyFile);
		
		
	}

	/**Encrypts the given String and writes it to "writer".
	 * Listens to answer after that.
	 * @param String to be sent
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 * */
	void sendMessage(String str) throws IOException, GeneralSecurityException{
		transmitMessage(str);
	}

	/**Waits for incoming Messages
	 * If the Message can not be handled the methods starts listening again
	 * @throws IOException */
	protected void listen() throws GeneralSecurityException, IOException {
		//Listens on the Socket
		//If incoming Message can't be handled starts listening again
		
		
		try {
			while(!communicationIsFinished) {
				String line = null;
				line = reader.readLine();

				try {
					handleMessage(line);
				} catch (NullPointerException e) {
					System.exit(63);
				} catch (IllegalArgumentException | IOException e) {
					System.exit(255);
				}
			}
		}catch(SocketTimeoutException e){
			throw e;
		}
	}


	/**Decrypts the given String and forward it to the ATM
	 * @param str String to decrypt
	 * @throws GeneralSecurityException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * */
	private void handleMessage(String str) throws GeneralSecurityException, IllegalArgumentException, IOException {
		String plainString = aesCrypter.decrypt(str);
		
		try {
			if(handshakeHandler.checkHandshakeMSG(plainString)) {
				if(handshakeHandler.handleHandshake(plainString)) {
					atm.endAction();
				}else {
				}
			}else {
				atm.handleMessage(plainString);
			}
		} catch (ServiceUnavailableException e) {
			System.exit(255);
		}		 

	}

	public void stopListening() {
		this.communicationIsFinished = true;
	}

}
