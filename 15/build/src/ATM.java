import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.naming.ServiceUnavailableException;



/**The ATM controles the user input and communicates with the bank.*/
public class ATM {
	/**Name of the auth file which was received from the bank and secures the communication */
    private String authpath;
    /**IP address of the Bank */
    private String ipadress;
    /**Port the bank listens on*/
    private String port;
    /**Name of the cardFile authentication the current user*/
    private String cardpath;
    /**Account name of the current user*/
    private String account;
    /**Argument of the current action*/
    private String argument;
    /**String representation of the command type/flag */
    private String command;
    /**KeyPair that authenticates the current user.*/
    private KeyPair keyPair;
    /**PrivateKey stored in the card file. Authenticates the current user.*/
    private PrivateKey privateKey;
    /**Network Class that controls the communication with the bank.*/
    static SimpleSecClient secClient = null;
    /**Creates the strings send to the Bank. */
    private ATMStringHandler atmStringHandler;
    
    
    /**Creates an ATM Object which performes the actions specified in the inputData Object.
     * @param inputData containing the values given by the start of ATM**/
    public ATM(InputChecker.InputData inputData) {
        /*Is ALWAYS called before atm is completely shutdown (DO CLEAN UP WITHIN VOID RUN())*/
    	Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                //Close the open Socket to secClient
                if (secClient != null)
                    secClient.closeConnection();
            }
        });

    	
    	atmStringHandler = new ATMStringHandler();
    	loadInputData(inputData);
    	

    }
    
    /**
     * Creates a SimpleSecClient and starts communication
     */
    private void startCommunication() {

        File authFile = new File(authpath);
        try {
            secClient = new SimpleSecClient(ipadress, Integer.parseInt(port), this, authFile);
        }catch (IOException e){
            System.exit(63);
		} catch (NumberFormatException e) {
            System.exit(255);
		}
    }
    

    /**Checks the inputs with the InputChecker Class.
     * Creates an ATM Object with the sanitised input.
     * */
    public static void main(String[] args) {
        InputChecker.InputData inputData = null;
        try {
            inputData = InputChecker.checkInput(args);
        } catch (Exception e) {
            System.exit(255);
        }
        
        ATM atm = new ATM(inputData);
        try {
            atm.handleArguments();
        }catch (SocketTimeoutException | ConnectException e) {
			System.exit(63);
		} catch (NumberFormatException | GeneralSecurityException |ServiceUnavailableException e) {
            System.exit(255);
        }
        catch (IOException e) {
        	System.exit(63);
        }

        
        System.exit(0);
    }
    


    /** Performs the communication with the Bank corresponding to the inputData.*/
    private void handleArguments() throws ServiceUnavailableException,
    GeneralSecurityException, NumberFormatException, IOException {

        
        
        if (command.equals("n")) {
        	handleNmessage();
        }else {
        	privateKey = getPrivateKeyFromCardFile();
        	startCommunication();
        	String msgUnsigned = getCommandMsg();
            String msg = msgUnsigned + getSignature(msgUnsigned, privateKey);  
            secClient.sendMessage(msg);
            secClient.listen();
        }
    }


    /**Sends a Message to create a new account to the bank.
     * Then the method waits for the 3 way handshake. 
     * @throws IOException if communication with bank fails
     * @throws GeneralSecurityException if decryption or encryption fails*/
	private void handleNmessage() throws IOException, GeneralSecurityException {
    	String msg = null;
    	Path path = getCardFilePath();
    	if(!Files.exists(path)) {
    	    startCommunication();
            keyPair = generateKeys();
            msg = createNMessage(keyPair.getPublic());

            //startCommunication();
            secClient.sendMessage(msg);
            secClient.listen();
            writePrivateKeyToCardFile(keyPair.getPrivate(),path);
    	}else {
    	    System.exit(255);
    	} 
	}

	/** Creates the String for the "-n" message
     *
     * @param publicKey will be send to the Bank
     * @return the Message for the Bank
     */
    private String createNMessage(PublicKey publicKey) {
        byte[] pkBytes = publicKey.getEncoded();
        String pk = Base64.getEncoder().encodeToString(pkBytes);
        String msg = "N," + account + "," + argument + "," + pk;
        return msg;
    }


    /**
     * Generates a Signature for a String.
     * uses "SHA256withRSA"
     *
     * @param msg        Message that will be signed
     * @param privateKey RSA Key which will be used to sign the Message
     * @return the Signature
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     */
    private String getSignature(String msg, PrivateKey privateKey) throws GeneralSecurityException {
        Signature privateSignature;
        byte[] signature;
        String signatureString = null;

        try {
            privateSignature = Signature.getInstance("SHA256withRSA");
            privateSignature.initSign(privateKey);
            privateSignature.update(msg.getBytes());
            signature = privateSignature.sign();
            signatureString = Base64.getEncoder().encodeToString(signature);
        } catch (InvalidKeyException e) {
            throw e;
        }
        return signatureString;
    }


    /**Creates a file and writes the PrivateKey into it.
     * The created file is the cardfile.
     *
     * @param privateKey that is written to the file
     * @throws IOException
     */
    private void writePrivateKeyToCardFile(PrivateKey privateKey,Path cardFilePath) throws IOException {
        if (Files.exists(cardFilePath)) {
            System.exit(255);
        } else {
            Files.write(cardFilePath, privateKey.getEncoded());
        }
    }
    
    /**Returns a Path "account.card" if no card file name is given.
     * Else returns the specified card file name.
     * @return path to the card file */
    private Path getCardFilePath() {
    	if (cardpath == null || cardpath.equals("")) {
            cardpath = "account" + ".card";
        }
        Path cardFilePath = Paths.get(cardpath);
		return cardFilePath;
    }


    /** Generates a RSA Key Pair 
     * @return KeyPair that will be used to identify the user, printed to the cardfile
     */
    private KeyPair generateKeys() {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyGenarator = KeyPairGenerator.getInstance("RSA");
       //     SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            SecureRandom random = new SecureRandom();
            keyGenarator.initialize(1024, random);
            keyPair = keyGenarator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.exit(255);
        }
    //    } catch (NoSuchProviderException e) {
     //       System.exit(255);
     //   }
        return keyPair;
    }

    /** Gets the private Key stored in the cardfile
     * @return privateKey that was in the file with the name cardpath
     */
    private PrivateKey getPrivateKeyFromCardFile() {
        PrivateKey privateKey = null;
        Path cardFilePathName = Paths.get(cardpath);
        if(!Files.exists(cardFilePathName))
        		System.exit(255);
        try {
            byte[] privateKeyInBytes = Files.readAllBytes(cardFilePathName);
            try {
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyInBytes));
            } catch (InvalidKeySpecException e) {
                System.exit(255);
            } catch (NoSuchAlgorithmException e) {
                System.exit(255);
            }
        } catch (IOException e1) {
        	System.exit(255);
        }
        return privateKey;
    }


    /**Returns the Message corresponding to the command flag, that can be send to the Server(Bank)
     * @return message that can be send to the Bank*/
    private String getCommandMsg() {
        String message = null;
        switch (command) {
            case "d":
                message = atmStringHandler.getDmsg(account, new BigDecimal(argument));
                break;
            case "w":
                message = atmStringHandler.getWmsg(account, new BigDecimal(argument));
                break;
            case "g":
                message = atmStringHandler.getGmsg(account);
                break;
        }
        return message;
    }

    /**Prints the Log Message to std:out*/
    void handleMessage(String str){
        String[] receivedMsg = str.split(",");
        switch (receivedMsg[0]) {
            case "B": //Getting balance
	        	secClient.logForHandshake(str);
	    		System.out.println("{\"account\":\"" + account + "\",\"balance\":" + receivedMsg[1] + "}");
	    		System.out.flush();
	            break;
            case "F":
                System.exit(255);
            default:
                System.exit(255);
        }
    }

    private void loadInputData(InputChecker.InputData inputData) {
        command = inputData.command;
        authpath = inputData.autpath;
        ipadress = inputData.ipadress;
        port = inputData.port;
        cardpath = inputData.cardpath;
        account = inputData.account;
        argument = inputData.argument;
    }

    /**Prints the Log Message to std:out and let the SecClient stop listening.
     * */
	public void endAction() {
		switch (command) {
		case "n":
			ATMjsonLogger.printN(account, argument);
			break;
		case "w":
			ATMjsonLogger.printW(account, argument);
			break;
		case "d":
			ATMjsonLogger.printD(account, argument);
			break;
		}
		secClient.stopListening();
	}

}

