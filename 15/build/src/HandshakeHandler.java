import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import javax.naming.ServiceUnavailableException;

public class HandshakeHandler {
	private String log = "";
	private SimpleSec ss;
	private boolean shaking = false;

	/**
	 * Creates HandshakeHandler
	 * @param simpleSec the SimpleSec the HandshakeHandler belongs to
	 */
	public HandshakeHandler(SimpleSec simpleSec) {
		ss = simpleSec;
	}

	/**
	 * Log a message to the HandshakeHandlers Log
	 * @param s the msg to log
	 */
	public void log(String s) {
		log+=s;
	}
	
	public void clearLog() {
		log="";
	}
	
	/**
	 * checks for a msg whether or not it is a handshakeMSG 
	 * @param msg the message to check
	 * @return true iff the message is a handshakeMSG
	 */
	public boolean checkHandshakeMSG(String msg) {
		return Pattern.matches("H,.*", msg);
	}
	
	/**
	 * Start a Handshake
	 * @return true iff handshake successfull
	 * @throws ServiceUnavailableException iff currently handshaking
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public boolean startHandshake() throws ServiceUnavailableException, IOException, GeneralSecurityException{
		if(shaking) {
			throw new ServiceUnavailableException(); //more meaningful exception
		} else {
			shaking = true;
			ss.transmitMessage("H,1,"+sign(log));
			boolean waitingForResponse = true;
			String response = "";
			while(waitingForResponse) {
				response = ss.read();
				if(Pattern.matches("H,2,.*", response)) {
					waitingForResponse = false;
				}
			}
			if(response.replaceFirst("H,2,", "").equals(sign(log))) {
				log(response);
				ss.transmitMessage("H,3,"+sign(log));
				shaking = false;
				clearLog();
				return true;
			} else {
				shaking = false;
				return false;
			}
		}
	}
	
	/**
	 * Handle incoming handshake
	 * @param msg the incoming handshake
	 * @return true iff handshake successful
	 * @throws ServiceUnavailableException iff currently handshaking
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public boolean handleHandshake(String msg) throws ServiceUnavailableException, IOException, GeneralSecurityException{
		if(shaking) {
			throw new ServiceUnavailableException(); //more meaningful exception
		}
		if(Pattern.matches("H,1,.*", msg)) {
			shaking = true;
			if(msg.replaceFirst("H,1,", "").equals(sign(log))) {
				log(msg);
				ss.transmitMessage("H,2,"+sign(log));
				boolean waitingForResponse = true;
				String response = "";
				while(waitingForResponse) {
					response = ss.read();
					if(Pattern.matches("H,3,.*", response)) {
						waitingForResponse = false;
					}
				}
				if(response.replaceFirst("H,3,", "").equals(sign(log))) {
					shaking = false;
					clearLog();
					return true;
				} else {
					shaking = false;
					return false;
				}
			} else {
				shaking = false;
				return false;
			}
		}
		throw new IllegalArgumentException();
	}
	
	/**
	 * hash a string to append to the handshake
	 * @param s the string to hash
	 * @return the hash value; currently equal to string
	 */
	public String sign(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(s.getBytes());
			String hashstring = "";
			for (byte current : hash)
				hashstring += Integer.toHexString(Byte.toUnsignedInt(current));
			return hashstring;
		} catch (NoSuchAlgorithmException e) {
			return s;
		}
	}
}