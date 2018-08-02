
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Hashtable;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.naming.ServiceUnavailableException;

public class Bank {

    private Hashtable<String, AccData> customers = new Hashtable<String, AccData>();
    private static SimpleSecServer secServer = null;
    private String sAuthFile = "bank.auth";
    private int port = 3000;
    private static File authFile;

    public static void main(String[] args) {

        //Handling Ctrl+C = SIGTERM from cmd.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                /*Close an ongoing connection to SecServer*/
                if (secServer != null)
                    secServer.closeConnection();
            }
        });
        Bank bank = new Bank();
        bank.handleArguments(args);
        bank.genAuthfile();

    }

    /**
     * Generates the Auth file at location sAuthFile by creating an AES key and saving it to the file and creates a new channel to listen on.
     * Program exits if the Auth file already exists or if there were problems in creating the file.
     */
    private void genAuthfile() {
        //Auth file generation
        authFile = new File(this.getsAuthFile());
        if (authFile.exists()) {
            System.exit(255);
        }
        //Try-catch with resources, automatically releases resources after exception
        try (ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(authFile))) {
            //Create AES key and write it into the auth file
            //Is it possible for an attacker to hijack the key generator ?
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();

            authFile.createNewFile();
            //The key is written as an actual object instead of a String
            //What happens if there is not enough memory to save the file ?
            oout.writeObject(secretKey);
            System.out.print("created\n");
            System.out.flush();
            secServer = new SimpleSecServer(port, this, authFile);
            secServer.mainLoop();
        } catch (NoSuchAlgorithmException e) {
            //Shouldn't be possible to happen, comes from AES key generation
            System.exit(255);
        } catch (IOException e) {
            //File couldn't be read or written
            System.exit(255);
        } catch (NumberFormatException e) {
            System.exit(255);
        } catch (GeneralSecurityException e) {
            System.exit(255);
        } finally {
            //Clean up resources, exit program
            //System.exit(255);
        }
    }

    /**
     * Handles the command line parameters. -p: Port number the server should listen on, expected to be an integer between 1024 and 65535.
     * -s: Name of the Auth file that will be created
     *
     * @param args An array of Strings that should be handled
     */
    private void handleArguments(String[] args) {
        //Used to make sure every argument is only called one time
        boolean bPort = true;
        boolean bAuth = true;

        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            String arg = args[i++];

            //Handle the -s argument
            if (bAuth && arg.equals("-s") && i < args.length) {
                sAuthFile = args[i++];
                bAuth = false;
            } else if (bAuth && arg.startsWith("-s")) {
                sAuthFile = arg.substring(2);
                bAuth = false;
            }
            //Handle the -p argument
            else if (bPort && arg.equals("-p") && i < args.length && args[i].matches("0|[1-9][0-9]*")) {
                port = Integer.valueOf(args[i++]);
                bPort = false;
            } else if (bPort && arg.startsWith("-p") && arg.substring(2).matches("0|[1-9][0-9]*")) {
                port = Integer.valueOf(arg.substring(2));
                bPort = false;
            }
            //String didn't fit into specification
            else {
                System.exit(255);
            }
        }
        if (port < 1024 || port > 65535)
            //Port is not in the specified range
            System.exit(255);
        if (!sAuthFile.matches("[_\\-\\.0-9a-z]*") || sAuthFile.equals(".") || sAuthFile.equals(".."))
            //Auth file argument is not a file name
            System.exit(255);
    }

    /**
     * Handle the received user command send by atm
     *
     * @param str User command in string format.
     */
    void handleMessage(String str) {
        String[] userCommand = str.split(",");
        secServer.logForHandshake(str);

        if((userCommand.length == 3 && userCommand[0].equals("G") && userCommand[1].matches("[_\\-\\.0-9a-z]*") && userCommand[2] != null)
        		|| (userCommand.length == 4 & userCommand[0].matches("N|D|W") && userCommand[1].matches("[_\\-\\.0-9a-z]*") && userCommand[2].matches("(0|([1-9][0-9]*))\\.[0-9][0-9]?") && userCommand[3] != null)) {
	        switch (userCommand[0]) {
	            case "N": //account creation
	                boolean successN = createAccount(userCommand);
	                try {
	                    if (successN && secServer.startHandshake()) {
	                        // System.out.println("{\"account\":\"" + customers.get(userCommand[1]).name + "\",\"initial_balance\":" + customers.get(userCommand[1]).balance + "}");
	                        System.out.println("{\"initial_balance\":" + customers.get(userCommand[1]).balance + ",\"account\":\"" + customers.get(userCommand[1]).name + "\"}");
	                        System.out.flush();
	                    } else {
	                        secServer.sendMessage("F");
	                        secServer.clearLog();
	                    }
	                } catch (Exception e) {
	                    try {
	                        secServer.sendMessage("F");
	                    } catch (IOException | GeneralSecurityException e1) {
	                    }
	                    secServer.clearLog();
	                }
	                break;
	
	            case "D": //deposit money
	                boolean successD = false;
	                if (authenticateUser(userCommand[1], userCommand[0] + "," + userCommand[1] + "," + userCommand[2] + ",", userCommand[3])) {
                        successD = depositMoney(userCommand, true);
                        try {
                            if (successD && secServer.startHandshake()) {
                                System.out.println("{\"account\":\"" + customers.get(userCommand[1]).name + "\",\"deposit\":" + userCommand[2] + "}");
                                // System.out.println("{\"deposit\":\"" + userCommand[2] + "\",\"account\":" +  customers.get(userCommand[1]).name  + "}");
                                System.out.flush();
                            } else {
                                secServer.sendMessage("F");
                                secServer.clearLog();
                                depositMoney(userCommand, false);
                            }
                        } catch (Exception e) {
                            try {
                                secServer.sendMessage("F");
                            } catch (IOException | GeneralSecurityException e1) {
                            }
                            secServer.clearLog();
                            depositMoney(userCommand, false);
                        }
                    }  else {
                        try {
                            secServer.sendMessage("F");
                            secServer.clearLog();
                        } catch (IOException |GeneralSecurityException e) {}
                    }
	                break;
	
	            case "W": //withdraw money
	                boolean successW = false;
	                if (authenticateUser(userCommand[1], userCommand[0] + "," + userCommand[1] + "," + userCommand[2] + ",", userCommand[3])) {
                        successW = withdrawMoney(userCommand, true);
                        try {
                            if (successW && secServer.startHandshake()) {
                                System.out.println("{\"account\":\"" + customers.get(userCommand[1]).name + "\",\"withdraw\":" + userCommand[2] + "}");
                                //System.out.println("{\"withdraw\":\"" +userCommand[2] + "\",\"account\":" + customers.get(userCommand[1]).name + "}");
                                System.out.flush();
                            } else {
                                secServer.sendMessage("F");
                                secServer.clearLog();
                                withdrawMoney(userCommand, false); //don't reset money here
                            }
                        } catch (Exception e) {
                            try {
                                secServer.sendMessage("F");
                            } catch (IOException | GeneralSecurityException e1) {
                            }
                            secServer.clearLog();
                            withdrawMoney(userCommand, false);
                        }
                    } else {
                        try {
                            secServer.sendMessage("F");
                            secServer.clearLog();
                        } catch (IOException |GeneralSecurityException e) {}
                    }
	                break;
	
	            case "G": //get balance
	                if (authenticateUser(userCommand[1], userCommand[0] + "," + userCommand[1] + ",", userCommand[2])) {
	                    try {
	                        sendBalance(userCommand);
	                        secServer.startHandshake();
	                    } catch (ServiceUnavailableException e) {
	                    } catch (IOException e) {
	                    } catch (GeneralSecurityException e) {
	                    }
	                } else {
	                    try {
	                        secServer.sendMessage("F");
	                    } catch (IOException | GeneralSecurityException e) {
	                    }
	                }
	        }
        }
        else {
        	try {
				secServer.sendMessage("F");
			} catch (IOException | GeneralSecurityException e) {
			}
        }
    }

    /**
     * Decides whether a given (name, msg)-pair is signed by the given signature.
     *
     * @param name   Name of the user, normally userCommand[1]
     * @param msg    Message that was sent to verify
     * @param strSig Signature of the user, normally userCommand[3]
     * @return True if the authentication was successful, false otherwise
     */
    private boolean authenticateUser(String name, String msg, String strSig) {

        byte[] sigBytes = Base64.getDecoder().decode(strSig);
        try {
            boolean result = false;
            if (customers.containsKey(name)) {
                //Recreate public key from hash table
                byte[] publicBytes = Base64.getDecoder().decode(customers.get(name).pk);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBytes));

                //Create signature to verify with
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update(msg.getBytes());

                result = sig.verify(sigBytes);
            } else {
                secServer.sendMessage("F");
            }
            return result;
        } catch (IOException | GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * Function that handles the deposit of money to given users balance.
     *
     * @param userCommand User command in string array format.
     * @param flag        Flag denotes if money needs to be added to the account (true) or the previous action needs to be reversed (false) once the handshake fails.
     * @return Returns true if the execution was successful, otherwise false.
     */
    private boolean depositMoney(String[] userCommand, boolean flag) {
        boolean success = false;
        BigDecimal amount = checkBigDecimal(userCommand[2]);
        if ((customers.containsKey(userCommand[1]) && amount.compareTo(new BigDecimal(0.0)) > 0)) {
            AccData user = customers.get(userCommand[1]);
            if (flag) {
                user.balance = user.balance.add(amount);
            } else {
                user.balance = user.balance.subtract(amount);
            }
            customers.put(userCommand[1], user);
            success = true;
        }
        return success;
    }

    /**
     * Function that handles the withdraw of money to given users balance.
     *
     * @param userCommand User command in string array format.
     * @param flag        Flag denotes if money needs to be taken from the account (true) or the previous action needs to be reversed (false) once the handshake fails.
     * @return Returns true if the execution was successful, otherwise false.
     */
    private boolean withdrawMoney(String[] userCommand, boolean flag) {
        boolean success = false;
        BigDecimal amount = checkBigDecimal(userCommand[2]);
        if ((customers.containsKey(userCommand[1])) && amount.compareTo(new BigDecimal(0.0)) > 0) {
            AccData user = customers.get(userCommand[1]);
            if (user.balance.compareTo(amount) >= 0) {
                if (flag) {
                    user.balance = user.balance.subtract(amount);
                } else {
                    user.balance = user.balance.add(amount);
                }
                customers.put(userCommand[1], user);
                success = true;
            }
        }
        return success;
    }

    /**
     * Function that provides the current balance to given users by sending a message to the atm client.
     *
     * @param userCommand User commands in string array format.
     */
    private void sendBalance(String[] userCommand) {
        if ((customers.containsKey(userCommand[1]))) {
            AccData user = customers.get(userCommand[1]);
            BigDecimal balance = user.balance;
            String msg = "B,";
            msg += balance.toString();
            try {
                secServer.sendMessage(msg);
                System.out.println("{\"account\":\"" + user.name + "\",\"balance\":" + user.balance + "}");
                System.out.flush();
            } catch (IOException e) {
            } catch (GeneralSecurityException e) {
            }
        }
    }

    /**
     * @param str String containing the float value of the user command.
     * @return returns the given string as a BigDecimal value.
     */
    private BigDecimal checkBigDecimal(String str) {
        BigDecimal amount = new BigDecimal(0.0);
        if (str.matches("(0|([1-9][0-9]*))\\.[0-9][0-9]?")) {
            amount = new BigDecimal(str);

        }
        return amount;
    }

    /**
     * Creates a new Object of AccData and stores the information into the hashtable customers of bank only if the user name does not already exist.
     *
     * @param userCommand User commands in string array format.
     * @return Returns true if the user did not exist in the hashtable before, otherwise false. Return value only needed for account creation (flag = true)
     */
    private boolean createAccount(String[] userCommand) {
        boolean success = false;
        BigDecimal balance = checkBigDecimal(userCommand[2]);
        if (!customers.containsKey(userCommand[1]) && (!(balance.compareTo(new BigDecimal(10.00)) < 0))) { //check if userName already exists and initial balance greater or equal 10.00f
            AccData user = new AccData(userCommand[1], balance, userCommand[3]);
            customers.put(userCommand[1], user);
            success = true;
        }
        return success;
    }

    /**
     * Simple Getter for the sAuthFile.
     *
     * @return the sAuthFile.
     */
    private String getsAuthFile() {
        return sAuthFile;
    }

    /**
     * Nested class AccData whose objects store account data of created users
     */
    private class AccData {
        private final String name;
        private BigDecimal balance;
        private final String pk;

        private AccData(String name, BigDecimal balance, String pk) {
            this.name = name;
            this.balance = balance;
            this.pk = pk;
        }
    }
}
