import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;

import javax.naming.ServiceUnavailableException;


public class SimpleSecServer extends SimpleSec {
	public static final boolean isDEBUG_MODE = false;
	/**The Socket the Server listens on to receive Messages from the ATM*/
    private ServerSocket serverSocket;
    /**The Bank that opens this Server*/
    private Bank bank;


    public SimpleSecServer(int port, Bank bank, File encryptionKeyFile)
            throws IOException {
    	super();    	
        serverSocket = new ServerSocket(port);
        this.bank = bank;
        aesCrypter = new AESCrpyter(encryptionKeyFile);
    }

    /**
     * should be called after setup. Wait's for incoming ATM-Calls and starts to
     * handle them
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    protected void mainLoop() throws IOException, GeneralSecurityException {
        while (true) {
            try {
            	openConnection();
                String line;
                line = reader.readLine();
                handleMessage(line);
            } catch (Exception e) {
                System.out.println("protocol_error");
                closeConnection();
                
            }
        }
    }

    public void clearLog(){
    	handshakeHandler.clearLog();
	}

    /** Initialises Socket, writer and reader.
     * */
    private void openConnection() throws IOException {
        try {
			socket = serverSocket.accept();
			if(isDEBUG_MODE) {
				socket.setSoTimeout(500000);
			}else {
				socket.setSoTimeout(10000);
			}
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			throw e;
		}
        
	}

	/**
     * @throws GeneralSecurityException 
	 * @throws IOException when IO error expends. Means roll back the transaction and
     *                     throw IO Exception back to main loop
     */
    void sendMessage(String str) throws IOException, GeneralSecurityException {
		transmitMessage(str);
    }

	/**Decrypts the given String and forward it to the bank
	 * @param str String to decrypt
	 * @throws GeneralSecurityException 
	 * */
    protected void handleMessage(String str) throws IOException, GeneralSecurityException {
		String plainString = null;
		try {
			plainString = aesCrypter.decrypt(str);
		} catch (GeneralSecurityException e) {
		}
		
		try {
			if(handshakeHandler.checkHandshakeMSG(plainString)) {
				handshakeHandler.handleHandshake(plainString);
			}else {
				bank.handleMessage(plainString);
			}
		} catch (ServiceUnavailableException e) {
			System.exit(255);
		}	
    }

	public boolean checkHandshakeMSG(String str) {
		return handshakeHandler.checkHandshakeMSG(str);
	}
}
