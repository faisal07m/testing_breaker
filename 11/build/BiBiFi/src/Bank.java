import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

public class Bank {
	private static String key, initVector;
	private static int PORT;
	private static String auth_file_name;
	private ArrayList<Customer> customer_list;
	private int account_id;
	private ServerSocket serverSocket;
	private BufferedWriter bw;
	private OutputStreamWriter osw;
	private Util utility;
	private Validations validations;

	public Bank() {
		customer_list = new ArrayList<Customer>();
		PORT = 3000;
		auth_file_name = "bank.auth";
		account_id = 434983;
		key = "test*12345678912";
		initVector = "1111111111111111";
		bw = null;
		osw = null;
		utility = new Util();
		validations = new Validations();
	}

	private void customPrint(String message) {
		/* For printing and flushing in stdout. */
		System.out.println(message);
		System.out.flush();
	}

	private String encrypt(String value) {
		/* Encrypts the input string with AES algorithm. */
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));

			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			byte[] encrypted = cipher.doFinal(value.getBytes());

			String s = new String(Base64.getEncoder().encode(encrypted));
			return s;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private String decrypt(String encrypted) {
		/* Decrypts the input string with AES algorithm. */
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));

			return new String(original);
		} catch (Exception ex) {
			return "";
		}
	}

	private void generateAuthFile() throws IOException {
		/*
		 * Generating the auth file with details necessary for the atm to communicate
		 * successfully.
		 */
		String currentDirectory = System.getProperty("user.dir");
		currentDirectory = currentDirectory.concat("/");
		File authFile = null;
		authFile = new File(currentDirectory.concat(auth_file_name));
		authFile.createNewFile();
		JSONObject jsonOutput = new JSONObject();
		try {
			jsonOutput.put("Port", PORT);
			jsonOutput.put("Key", key);
			jsonOutput.put("InitVector", initVector);
			PrintWriter writeToFile = new PrintWriter(authFile.toString(), "UTF-8");
			writeToFile.println(jsonOutput.toString());
			writeToFile.close();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		customPrint("created");
	}

	private boolean isCustomerAlreadyPresent(String customer_name) {
		/* Checks if the customer is already present in the array list. */
		for (Customer c : customer_list) {
			if (c.getCustomer_name().equalsIgnoreCase(customer_name)) {
				return true;
			}				
		}
		return false;
	}

	private String handleAccountRequest(JSONObject obj, boolean isRollBack) throws JSONException {
		/*
		 * Handles the create account functionality of bank. If customer is not present
		 * with same details, then we create a new customer object and store it in the
		 * array list.
		 */
		String encrypted_string = encrypt("");
		if (!isRollBack) {
			if (!isCustomerAlreadyPresent(obj.getString(Constants.ACCOUNT_NAME))) {
				Customer cust = new Customer();
				cust.setAccount_id(account_id++);
				cust.setBalance(obj.getDouble(Constants.INITIAL_BALANCE));
				cust.setCustomer_name(obj.getString(Constants.ACCOUNT_NAME));
				cust.setCard_pin(obj.getInt(Constants.CARD_PIN));
				customer_list.add(cust);

				JSONObject response = new JSONObject();
				response.put(Constants.ACCOUNT_NAME, obj.getString(Constants.ACCOUNT_NAME));
				response.put(Constants.INITIAL_BALANCE, obj.getDouble(Constants.INITIAL_BALANCE));
				customPrint(response.toString());
				encrypted_string = encrypt(response.toString());
				return encrypted_string;
			}
		} else {
			int card_Pin = obj.getInt(Constants.CARD_PIN);
			customer_list.removeIf(x -> x.getCard_pin() == card_Pin);
			return encrypted_string;
		}
		return encrypt("Account already exists");
	}

	private String handleBalanceRequest(JSONObject obj) throws JSONException {
		/*
		 * Handles the balance functionality of bank. Based on the customer details in
		 * the input parameter, balance of the respective customer is returned. And
		 * invokes sendToAtm method with the response string if customer is found, with
		 * null if customer is not found or mismatch of details.
		 */
		Customer temp = null;
		for (Customer c : customer_list) {
			if (obj.getString(Constants.ACCOUNT_NAME).equals(c.getCustomer_name())
					&& obj.getInt(Constants.CARD_PIN) == c.getCard_pin()) {
				temp = c;
				break;
			}
		}

		if (temp != null) {
			JSONObject response = new JSONObject();
			response.put(Constants.ACCOUNT_NAME, temp.getCustomer_name());
			response.put(Constants.BALANCE, temp.getBalance());
			String encrypted_string = encrypt(response.toString());
			customPrint(response.toString());
			return encrypted_string;
		}
		return encrypt("");
	}

	private String handleWithdrawRequest(JSONObject obj, boolean isRollBack) throws JSONException {
		/*
		 * Handles the withdraw functionality of bank. Based on the customer details in
		 * the input parameter, amount is withdrew from respective customer object. And
		 * invokes sendToAtm method with the response string if customer is found, with
		 * null if customer is not found or mismatch of details.
		 */
		Customer temp = null;
		double withdraw_amount;
		String encrypted_string = encrypt("");
		for (Customer c : customer_list) {
			if (obj.getString(Constants.ACCOUNT_NAME).equals(c.getCustomer_name())
					&& obj.getInt(Constants.CARD_PIN) == c.getCard_pin()) {
				if (isRollBack) {
					withdraw_amount = obj.getDouble(Constants.DEPOSIT);
				} else {
					withdraw_amount = obj.getDouble(Constants.WITHDRAW);
				}
				withdraw_amount = obj.getDouble(Constants.WITHDRAW);
				double balance = c.getBalance();
				if (balance - withdraw_amount >= 0) {
					double res = c.getBalance() - withdraw_amount;
					c.setBalance(utility.round(res, 2));
					temp = c;
					break;
				}
			}
		}

		if (!isRollBack) {
			if (temp != null) {
				JSONObject response = new JSONObject();
				response.put(Constants.ACCOUNT_NAME, temp.getCustomer_name());
				response.put(Constants.WITHDRAW, obj.getDouble(Constants.WITHDRAW));
				encrypted_string = encrypt(response.toString());
				customPrint(response.toString());
			} else {
				JSONObject withdrawErrorResponse = new JSONObject();
				withdrawErrorResponse.put("ErrorMessage", "Withdraw amount greater than balance");
				encrypted_string = encrypt(withdrawErrorResponse.toString());
			}
		}
		return encrypted_string;
	}

	private String handleDepositRequest(JSONObject obj, boolean isRollBack) throws JSONException {
		/*
		 * Handles the deposit functionality of bank. Based on the customer details in
		 * the input parameter, amount is deposited in respective customer object. And
		 * invokes sendToAtm method with the response string if customer is found, with
		 * null if customer is not found or mismatch of details.
		 */
		Customer temp = null;
		double deposit_amount;
		String encrypted_string = encrypt("");
		for (Customer c : customer_list) {
			if (obj.getString(Constants.ACCOUNT_NAME).equals(c.getCustomer_name())
					&& obj.getInt(Constants.CARD_PIN) == c.getCard_pin()) {
				if (isRollBack) {
					deposit_amount = obj.getDouble(Constants.WITHDRAW);
				} else {
					deposit_amount = obj.getDouble(Constants.DEPOSIT);
				}
				deposit_amount = obj.getDouble(Constants.DEPOSIT);
				double res = c.getBalance() + deposit_amount;
				c.setBalance(utility.round(res, 2));
				temp = c;
				break;
			}
		}

		if (!isRollBack) {
			if (temp != null) {
				JSONObject response = new JSONObject();
				response.put(Constants.ACCOUNT_NAME, temp.getCustomer_name());
				response.put(Constants.DEPOSIT, obj.getDouble(Constants.DEPOSIT));
				encrypted_string = encrypt(response.toString());
				customPrint(response.toString());
				return encrypted_string;
			}
		}
		return encrypted_string;
	}

	private boolean validateInput(String[] commandLineInput) throws IOException {
		boolean validInput = true;
		if (commandLineInput.length > 4) {
			return false;
		}

		if (!containsDuplicate(commandLineInput)) {
			for (String argument : commandLineInput) {
				if (Arrays.asList(Constants.bankCommands).contains(argument) && validInput) {
					switch (argument) {
					case "-p":
						try {
							int indexOfCommand = Arrays.asList(commandLineInput).indexOf("-p");
							String parameter = commandLineInput[indexOfCommand + 1];
							boolean isValidPort = validations.validPort(parameter);
							PORT = isValidPort ? Integer.valueOf(parameter) : -1;
							if (PORT == -1) {
								validInput = false;
							}								
						} catch (IndexOutOfBoundsException e) {
							validInput = false;
						}
						break;
					case "-s":
						try {
							int indexOfCommand = Arrays.asList(commandLineInput).indexOf("-s");
							String parameter = commandLineInput[indexOfCommand + 1];
							boolean isValidFileName = validations.validAuthFileName(parameter);
							auth_file_name = isValidFileName ? parameter : "";
							if (auth_file_name.isEmpty()) {
								validInput = false;
							}
						} catch (IndexOutOfBoundsException e) {
							validInput = false;
						}
						break;
					default:
						validInput = false;
						break;
					}
				} else {
					String noSpaceArgumentsCommand = argument.length() > 3 ? argument.substring(0, 2) : "";
					if (Arrays.asList(Constants.bankCommands).contains(noSpaceArgumentsCommand)) {
						switch (noSpaceArgumentsCommand) {
						case "-p":
							String portNumber = argument.substring(2, argument.length());
							boolean isValidPort = validations.validPort(portNumber);
							PORT = isValidPort ? Integer.valueOf(portNumber) : -1;
							if (PORT == -1) {
								validInput = false;
							}								
							break;
						case "-s":
							String fileName = argument.substring(2, argument.length());
							boolean isValidFileName = validations.validAuthFileName(fileName);
							auth_file_name = isValidFileName ? fileName : "";
							if (auth_file_name.isEmpty()) {
								validInput = false;
							}							
							break;
						default:
							validInput = false;
							break;
						}
					} else {
						if (!argument.isEmpty() && argument.charAt(0) == '-') {
							int indexOfArgument = Arrays.asList(commandLineInput).indexOf(argument);
							String command = commandLineInput[indexOfArgument - 1];
							if (!command.equals("-s")) {
								return false;
							}
						}
					}
				}
			}
		} else {
			validInput = false;
		}
		return validInput;
	}

	private boolean containsDuplicate(String[] commandLineInput) {
		boolean containsDuplicate = false;
		Set<String> commandLineInputSet = new HashSet<String>(Arrays.asList(commandLineInput));
		if (commandLineInputSet.size() != commandLineInput.length) {
			return true;
		}

		for (String argument : commandLineInput) {
			if (Arrays.asList(Constants.bankCommands).contains(argument)) {
				for (String temp : commandLineInput) {
					if (!argument.equals(temp)) {
						if (argument.length() == 2) {
							char command = argument.charAt(1);
							int charPosition = temp.indexOf(command);
							if (charPosition == 1) {
								return true;
							}
						}
					}
				}
			} else {
				String noSpaceArgumentsCommand = argument.length() > 3 ? argument.substring(0, 2) : "";
				if (Arrays.asList(Constants.bankCommands).contains(noSpaceArgumentsCommand)) {
					for (String temp : commandLineInput) {
						if (!argument.equals(temp)) {
							char command = noSpaceArgumentsCommand.charAt(1);
							int charPosition = temp.indexOf(command);
							if (charPosition == 1) {
								return true;
							}
						}
					}
				}
			}
		}
		return containsDuplicate;
	}

	private void cleanUp(int code) {
		try {
			serverSocket.close();
			if (bw != null) {
				bw.close();
			}
			if (osw != null) {
				osw.close();
			}
		} catch (IOException e) {
			System.exit(code);
		}
	}

	private String processInput(JSONObject obj) throws JSONException {
		String encrytedString = "";
		if (obj.getString(Constants.OPERATION).equals("-d")) {
			encrytedString = handleDepositRequest(obj, false);
		} else if (obj.getString(Constants.OPERATION).equals("-n")) {
			encrytedString = handleAccountRequest(obj, false);
		} else if (obj.getString(Constants.OPERATION).equals("-w")) {
			encrytedString = handleWithdrawRequest(obj, false);
		} else if (obj.getString(Constants.OPERATION).equals("-g")) {
			encrytedString = handleBalanceRequest(obj);
		}
		return encrytedString;
	}

	protected void startServerAndListen() throws IOException, JSONException {
		generateAuthFile();
		serverSocket = new ServerSocket(PORT);
		Socket socket = null;
		String recievedText = "";
		while (true) {
			JSONObject obj = null;
			try {
				socket = serverSocket.accept();
				socket.setSoTimeout(10 * 1000);
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();

				byte[] buffer = new byte[1024];
				int read;
				while ((read = is.read(buffer)) != -1) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < read; i++) {
						sb.append((char) buffer[i]);
					}
					recievedText = sb.toString();
					break;
				}

				String encrytedString = "";
				String sendEncrytedString = "";
				recievedText = recievedText.replace("\n", "");
				recievedText = decrypt(recievedText);
				if (!recievedText.equals("")) {
					obj = new JSONObject(recievedText);
					if (validations.validateRequestJson(obj)) {
						encrytedString = processInput(obj);
					} else {
						customPrint(Constants.PROTOCOL_ERROR);
						encrytedString = encrypt(Constants.PROTOCOL_ERROR);
					}
					osw = new OutputStreamWriter(os);
					bw = new BufferedWriter(osw);
					sendEncrytedString = encrytedString + "\n";
					bw.write(sendEncrytedString);
					bw.flush();
				} else {
					customPrint(Constants.PROTOCOL_ERROR);
				}
			} catch (SocketTimeoutException e) {
				customPrint(Constants.PROTOCOL_ERROR);
				// Code to rollback the changes
				if (obj != null && obj.getString(Constants.OPERATION).equals("-d")) {
					handleWithdrawRequest(obj, true);
				} else if (obj != null && obj.getString(Constants.OPERATION).equals("-w")) {
					handleDepositRequest(obj, true);
				} else if (obj != null && obj.getString(Constants.OPERATION).equals("-n")) {
					handleAccountRequest(obj, true);
				}
				continue;
			} catch (SocketException e) {
				customPrint(Constants.PROTOCOL_ERROR);
				// Code to rollback the changes
				if (obj != null && obj.getString(Constants.OPERATION).equals("-d")) {
					handleWithdrawRequest(obj, true);
				} else if (obj != null && obj.getString(Constants.OPERATION).equals("-w")) {
					handleDepositRequest(obj, true);
				} else if (obj != null && obj.getString(Constants.OPERATION).equals("-n")) {
					handleAccountRequest(obj, true);
				}
				continue;
			} finally {
				if (bw != null) {
					bw.flush();
				}
				if (osw != null) {
					osw.flush();
				}
				socket.close();
			}
		}
	}

	public static void main(String... args) {
		try {
			Bank b = new Bank();
			if (b.validateInput(args)) {
				b.startServerAndListen();
			} else {
				b.cleanUp(255);
				System.exit(255);
			}
		} catch (JSONException e) {
			System.exit(255);
		} catch (IOException e) {
			System.exit(255);
		}
	}
}