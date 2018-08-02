import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Atm {

	private static String account;
	private static String auth_file_name;
	private static String card_file_name;
	private static int numberOfCommandsCounter;
	private String IP, key, initVector;
	private int PORT;

	// Different type of transaction flags
	private static boolean newAccountCreation = false;
	private static boolean depositTransaction = false;
	private static boolean withdrawTransaction = false;
	private static boolean accountDetails = false;
	private static boolean portNumberProvided = false;
	private double amount = 0.0f;

	public Atm() {
		IP = "127.0.0.1";
		PORT = 3000;
		key = "test*12345678912";
		auth_file_name = "bank.auth";
		card_file_name = "";
		initVector = "1111111111111111";
	}

	private String encrypt(String value) {
		try {
			Cipher cipher = Cipher.getInstance(Constants.EncryptionType);
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			byte[] encrypted = cipher.doFinal(value.getBytes());
			String s = new String(Base64.getEncoder().encode(encrypted));
			return s;
		} catch (Exception ex) {
			System.exit(255);
		}
		return null;
	}

	private String decrypt(String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(Constants.CharacterEncodingType));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(Constants.CharacterEncodingType),
					Constants.EncrytionName);
			Cipher cipher = Cipher.getInstance(Constants.EncryptionType);
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
			return new String(original);
		} catch (Exception ex) {
			System.exit(63);
		}
		return null;
	}

	private CardFileObject readCardFile(String custAccountName, String cardFileName) throws IOException {
		String currentDirectory = System.getProperty(Constants.GetCurrentDirectory);
		currentDirectory = currentDirectory.concat("/");
		if (cardFileName.isEmpty()) {
			cardFileName = account.concat(Constants.CardExtension);
		}
		boolean isCardFileExists = checkCardFileExists(custAccountName, cardFileName);
		CardFileObject cardFile = null;
		if (isCardFileExists) {
			cardFile = getFileContents(currentDirectory.concat(cardFileName));
		} else {
			System.exit(255);
		}
		return cardFile;
	}

	private void sendAccountRequest(String account, double balance, String authFile) throws IOException {
		if (checkCardFileExists(account, card_file_name)) {
			System.exit(255);
		}

		if (balance >= 10.0) {
			if (card_file_name.equals(""))
				card_file_name = account.concat(Constants.CardExtension);
			createCardFile(account, card_file_name);
			JSONObject jsonOutput = new JSONObject();
			try {
				jsonOutput.put(Constants.Account, account);
				jsonOutput.put(Constants.INITIAL_BALANCE, balance);
				CardFileObject obj = readCardFile(account, card_file_name);
				jsonOutput.put(Constants.CARD_PIN, obj.pin);
				jsonOutput.put(Constants.OPERATION, Constants.New_Account_Parameter);
			} catch (JSONException e) {
				System.exit(255);
			}
			sendToBank(jsonOutput.toString());
		} else {
			System.exit(255);
		}
	}

	private void sendDepositRequest(String account, double amount, String authFile, String cardFile)
			throws IOException {
		if (amount > 0.0) {
			JSONObject jsonOutput = new JSONObject();
			try {
				jsonOutput.put(Constants.Account, account);
				jsonOutput.put(Constants.Deposit, amount);
				CardFileObject obj = readCardFile(account, cardFile);
				jsonOutput.put(Constants.CARD_PIN, obj.pin);
				jsonOutput.put(Constants.OPERATION, Constants.Deposit_Parameter);
			} catch (JSONException e) {
				System.exit(255);
			}
			sendToBank(jsonOutput.toString());
		} else {
			System.exit(255);
		}
	}

	private void sendWithdrawRequest(String account, double amount, String authFile, String cardFile)
			throws IOException {
		if (amount > 0.0) {
			JSONObject jsonOutput = new JSONObject();
			try {
				jsonOutput.put(Constants.Account, account);
				jsonOutput.put(Constants.WITHDRAW, amount);
				CardFileObject obj = readCardFile(account, cardFile);
				jsonOutput.put(Constants.CARD_PIN, obj.pin);
				jsonOutput.put(Constants.OPERATION, Constants.Withdraw_Parameter);
			} catch (JSONException e) {
				System.exit(255);
			}
			sendToBank(jsonOutput.toString());
		} else {
			System.exit(255);
		}
	}

	private void sendBalanceRequest(String account, String authFile, String cardFile) throws IOException {
		JSONObject jsonOutput = new JSONObject();
		try {
			jsonOutput.put(Constants.Account, account);
			CardFileObject obj = readCardFile(account, cardFile);
			jsonOutput.put(Constants.CARD_PIN, obj.pin);
			jsonOutput.put(Constants.OPERATION, Constants.Balance_Parameter);
		} catch (JSONException e) {
			System.exit(255);
		}
		sendToBank(jsonOutput.toString());
	}

	@SuppressWarnings("unchecked")
	private boolean validateResonseJson(JSONObject obj) {
		Iterator<String> keysItr = obj.keys();
		while (keysItr.hasNext()) {
			String temp = keysItr.next();
			if (!Arrays.asList(Constants.BankResponseKeys).contains(temp)) {
				return false;
			}			
		}
		return true;
	}

	private void sendToBank(String request) {
		readAuthFile();
		try {
			String enc = encrypt(request.toString());
			Socket authSocket = new Socket(IP, PORT);
			authSocket.setSoTimeout(10 * 1000);

			// Send the message to the server
			OutputStream os = authSocket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			String sendMessage = enc + "\n";
			bw.write(sendMessage);
			bw.flush();

			// Get the return message from the server
			InputStream is = authSocket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String message = br.readLine();
			message = decrypt(message);
			br.close();
			isr.close();
			is.close();
			authSocket.close();
			if (message.isEmpty() || message.equals("") || message.contains(Constants.Withdraw_Message)
					|| message.contains(Constants.AccountExistsMessage)) {
				System.exit(255);
			} else if (message.equals(Constants.ProtocolErrorMessage)) {
				System.exit(63);
			} else {
				JSONObject obj = new JSONObject(message);
				if (validateResonseJson(obj)) {
					System.out.println(message);
					System.out.flush();
				} else {
					System.exit(255);
				}					
			}
		} catch (SocketException e) {
			deleteCardFile(request);
			System.exit(63);
		} catch (JSONException e) {
			System.exit(255);
		} catch (SocketTimeoutException e) {
			deleteCardFile(request);
			System.exit(63);
		} catch (IOException e) {
			System.exit(255);
		}
		System.exit(0);
	}

	private void deleteCardFile(String cardFileRequest) {
		if (cardFileRequest.contains("-n")) {
			String currentDirectory = System.getProperty("user.dir");
			currentDirectory = currentDirectory.concat(File.separator);
			File cardFile = new File(currentDirectory.concat(card_file_name));
			if (cardFile.exists() && !cardFile.isDirectory()) {
				System.gc();
				cardFile.delete();
			}
		}
	}

	public boolean processInput(String[] commandLineArguments) throws IOException, JSONException {
		boolean validInputInformation = true;
		boolean validInputDataHandling = true;
		if (commandLineArguments.length > 12 || commandLineArguments.length < 2) {
			return false;
		}
		if (!containsDuplicate(commandLineArguments)) {
			for (String argument : commandLineArguments) {
				validInputInformation = informationCollection(commandLineArguments, argument, validInputInformation);
			}
			if (account != null && !account.isEmpty()) {
				for (String argument : commandLineArguments) {
					validInputDataHandling = dataHandling(commandLineArguments, argument, validInputDataHandling);
				}
				if (validInputInformation && validInputDataHandling && numberOfCommandsCounter < 2) {
					if (newAccountCreation && amount >= 10.00) {
						sendAccountRequest(account, amount, auth_file_name);
					} else if (depositTransaction) {
						sendDepositRequest(account, amount, auth_file_name, card_file_name);
					} else if (withdrawTransaction) {
						sendWithdrawRequest(account, amount, auth_file_name, card_file_name);
					} else if (accountDetails) {
						sendBalanceRequest(account, auth_file_name, card_file_name);
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			System.exit(255);
		}
		return true;
	}

	private boolean informationCollection(String[] commandLineArguments, String argument, boolean validInput) {
		if (Arrays.asList(Constants.AtmCommands).contains(argument)) {
			switch (argument) {
			case "-ga":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-ga");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					account = validAccountName(parameter) ? parameter : "";
					if (account.isEmpty()) {
						validInput = false;
					}					
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-a":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-a");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					account = validAccountName(parameter) ? parameter : "";
					if (account.isEmpty()) {
						validInput = false;
					}						
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-s":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-s");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					auth_file_name = validAuthFileName(parameter) ? parameter : "";
					if (auth_file_name.isEmpty()) {
						return false;
					}
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-i":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-i");
					String parameter = commandLineArguments[indexOfCommand + 1];
					IP = validIpAddress(parameter) ? parameter : "";
					if (IP.isEmpty()) {
						System.exit(255);
					}						
					// account name validations
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-p":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-p");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					PORT = validPort(parameter) ? Integer.valueOf(parameter) : -1;
					if (PORT == -1) {
						System.exit(255);
					}						
					portNumberProvided = true;
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-c":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-c");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					card_file_name = validAuthFileName(parameter) ? parameter : "";
					if (card_file_name.isEmpty()) {
						return false;
					}					
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			default:
				break;
			}
		} else {
			String noSpaceArgumentsCommand = argument.length() > 3 ? argument.substring(0, 2) : "";
			if (Arrays.asList(Constants.AtmCommands).contains(noSpaceArgumentsCommand)) {
				switch (noSpaceArgumentsCommand) {
				case "-p":
					String portNumber = argument.substring(2, argument.length());
					boolean isValidPort = validPort(portNumber);
					PORT = isValidPort ? Integer.valueOf(portNumber) : -1;
					if (PORT == -1) {
						System.exit(255);
					}					
					portNumberProvided = true;
					break;
				case "-s":
					String fileName = argument.substring(2, argument.length());
					boolean isValidFileName = validAuthFileName(fileName);
					auth_file_name = isValidFileName ? fileName + Constants.AuthExtension : "";
					if (auth_file_name.isEmpty()) {
						validInput = false;
					}						
					break;
				case "-ga":
					try {
						String parameter = argument.substring(2, argument.length());
						// account name validations
						account = validAccountName(parameter) ? parameter : "";
						if (account.isEmpty()) {
							validInput = false;
						}							
					} catch (IndexOutOfBoundsException e) {
						validInput = false;
						System.exit(255);
					}
					break;
				case "-a":
					try {
						String parameter = argument.substring(2, argument.length());
						// account name validations
						account = validAccountName(parameter) ? parameter : "";
						if (account.isEmpty()) {
							validInput = false;
						}							
					} catch (IndexOutOfBoundsException e) {
						validInput = false;
						System.exit(255);
					}
					break;
				case "-i":
					try {
						String parameter = argument.substring(2, argument.length());
						IP = validIpAddress(parameter) ? parameter : "";
						if (IP.isEmpty()) {
							System.exit(255);
						}							
						// account name validations
					} catch (IndexOutOfBoundsException e) {
						validInput = false;
						System.exit(255);
					}
					break;
				case "-c":
					try {
						String parameter = argument.substring(2, argument.length());
						// account name validations
						card_file_name = validAuthFileName(parameter) ? parameter : "";
						if (card_file_name.isEmpty()) {
							return false;
						}							
					} catch (IndexOutOfBoundsException e) {
						validInput = false;
						System.exit(255);
					}
					break;
				default:
					break;
				}
			} else {
				if (!argument.isEmpty() && argument.charAt(0) == '-'
						&& !Arrays.asList(Constants.AtmCommands).contains(argument)) {
					System.exit(255);
				}
			}
		}
		return validInput;
	}

	private boolean dataHandling(String[] commandLineArguments, String argument, boolean validInput) {
		if (Arrays.asList(Constants.AtmCommands).contains(argument)) {
			switch (argument) {
			case "-n":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-n");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					if (validInitialAmount(parameter)) {
						amount = Double.valueOf(Constants.DecimalFormat.format(Double.valueOf(parameter)));
						validInput = true;
						numberOfCommandsCounter++;
						newAccountCreation = true;
					}
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-d":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-d");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					if (validAmount(parameter)) {
						amount = Double.valueOf(Constants.DecimalFormat.format(Double.valueOf(parameter)));
						validInput = true;
						numberOfCommandsCounter++;
						depositTransaction = true;
					}
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-w":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-w");
					String parameter = commandLineArguments[indexOfCommand + 1];
					// account name validations
					if (validAmount(parameter)) {
						amount = Double.valueOf(Constants.DecimalFormat.format(Double.valueOf(parameter)));
						numberOfCommandsCounter++;
						withdrawTransaction = true;
						validInput = true;
					}
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			case "-g":
				try {
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-g");
					String parameter = commandLineArguments[indexOfCommand + 1];
					if (!Arrays.asList(Constants.AtmCommands).contains(parameter)) {
						validInput = false;
					}
					numberOfCommandsCounter++;
					accountDetails = true;
				} catch (IndexOutOfBoundsException e) {
					validInput = true;
					numberOfCommandsCounter++;
					accountDetails = true;
				}
				break;
			case "-ga":
				try {
					validInput = false;
					int indexOfCommand = Arrays.asList(commandLineArguments).indexOf("-ga");
					String parameter = commandLineArguments[indexOfCommand + 1];
					account = validAccountName(parameter) ? parameter : "";
					if (!account.isEmpty()) {
						validInput = true;
						numberOfCommandsCounter++;
						sendBalanceRequest(account, auth_file_name, card_file_name);
					}
				} catch (IndexOutOfBoundsException e) {
					validInput = false;
					System.exit(255);
				} catch (IOException e) {
					validInput = false;
					System.exit(255);
				}
				break;
			default:
				break;
			}
		} else {
			String noSpaceArgumentsCommand = argument.length() > 3 ? argument.substring(0, 2) : "";
			if (Arrays.asList(Constants.AtmCommands).contains(noSpaceArgumentsCommand)) {
				switch (noSpaceArgumentsCommand) {
				case "-n":
					String amountNew = argument.substring(2, argument.length());
					if (validInitialAmount(amountNew)) {
						amount = Double.valueOf(Constants.DecimalFormat.format(Double.valueOf(amountNew)));
						validInput = true;
						numberOfCommandsCounter++;
						newAccountCreation = true;
					}
					break;
				case "-d":
					String amountDeposit = argument.substring(2, argument.length());
					if (validAmount(amountDeposit)) {
						amount = Double.valueOf(Constants.DecimalFormat.format(Double.valueOf(amountDeposit)));
						validInput = true;
						numberOfCommandsCounter++;
						depositTransaction = true;
					}
					break;
				case "-w":
					String amountWithdraw = argument.substring(2, argument.length());
					if (validAmount(amountWithdraw)) {
						amount = Double.valueOf(Constants.DecimalFormat.format(Double.valueOf(amountWithdraw)));
						validInput = true;
						numberOfCommandsCounter++;
						withdrawTransaction = true;
					}
					break;
				default:
					break;
				}
			} else {
				if (!argument.isEmpty() && argument.charAt(0) == '-'
						&& !Arrays.asList(Constants.AtmCommands).contains(argument)) {
					System.exit(255);
				}
			}
		}
		return validInput;
	}

	private static boolean containsDuplicate(String[] commandLineInput) {
		boolean containsDuplicate = false;
		Set<String> commandLineInputSet = new HashSet<String>(Arrays.asList(commandLineInput));
		if (commandLineInputSet.size() != commandLineInput.length) {
			return true;
		}

		for (String argument : commandLineInput) {
			if (Arrays.asList(Constants.AtmCommands).contains(argument)) {
				for (String temp : commandLineInput) {
					if (!argument.equals(temp) && !temp.isEmpty() && temp.charAt(0) == '-') {
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
				if (Arrays.asList(Constants.AtmCommands).contains(noSpaceArgumentsCommand)) {
					for (String temp : commandLineInput) {
						if (!argument.equals(temp) && !temp.isEmpty() && temp.charAt(0) == '-') {
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

	private static boolean validPort(String port) {
		try {
			String regularExp = "[1-9][0-9]*";
			if (!Pattern.matches(regularExp, port)) {
				return false;
			}
			Integer portNumber = Integer.valueOf(port);
			if (portNumber < 1024 || portNumber > 65535) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private static boolean validAuthFileName(String authFileName) {
		if (authFileName.equals(".") || authFileName.equals("..") || authFileName.length() < 1
				|| authFileName.length() > 127) {
			return false;
		}
		for (int i = 0; i < authFileName.length(); i++) {
			String stringTemp = String.valueOf(authFileName.charAt(i));
			if (!Pattern.matches(Constants.ValidAuthFileNameRegularExpression, stringTemp)) {
				return false;
			}
		}
		return true;
	}

	private static boolean validAccountName(String accountName) {
		if (accountName.length() < 1 || accountName.length() > 122) {
			return false;
		}
		String regularExp = Constants.ValidPortRegularExpression;
		for (int i = 0; i < accountName.length(); i++) {
			String stringTemp = String.valueOf(accountName.charAt(i));
			if (!Pattern.matches(regularExp, stringTemp)) {
				return false;
			}
		}
		return true;
	}

	private static boolean validIpAddress(String iPAddress) {
		Pattern pattern = Pattern.compile(Constants.IPADDRESS_PATTERN);
		Matcher matcher = pattern.matcher(iPAddress);
		return matcher.matches();
	}

	private static boolean validAmount(String amount) {
		try {
			if (!Pattern.matches(Constants.ValidAmountRegularExpression, amount)
					&& !Pattern.matches("[0-9]\\.[0-9]{2}", amount)) {
				return false;
			}

			double amountFromString = Double.parseDouble(Constants.DecimalFormat.format(Double.valueOf(amount)));
			double min = 0.00;
			double max = 4294967295.99;

			if (amountFromString < min || amountFromString > max) {
				System.exit(255);
				return false;
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean validInitialAmount(String amount) {
		try {
			if (!Pattern.matches(Constants.AmountRegularExpression, amount)) {
				return false;
			}
			
			double amountFromString = Double.parseDouble(Constants.DecimalFormat.format(Double.valueOf(amount)));
			double min = 0.00;
			double max = 4294967295.99;

			if (amountFromString < min || amountFromString > max) {
				System.exit(255);
				return false;
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean checkCardFileExists(String custAccountName, String cardFileName) throws IOException {
		String currentDirectory = System.getProperty(Constants.GetCurrentDirectory);
		boolean isCardFileExists = false;
		File cardFile = new File(currentDirectory.concat(File.separator).concat(cardFileName));

		if (cardFile.exists() && !cardFile.isDirectory()) {
			isCardFileExists = true;
		}
		return isCardFileExists;
	}

	private void readAuthFile() {
		String currentDirectory = System.getProperty("user.dir");
		String authString = "";
		File authFile = new File(currentDirectory.concat(File.separator).concat(auth_file_name));
		if (authFile.exists() && !authFile.isDirectory()) {
			try {
				authString = new String(Files.readAllBytes(Paths.get(authFile.getAbsolutePath())));
				JSONObject obj = new JSONObject(authString);
				if (portNumberProvided == false) {
					PORT = obj.getInt(Constants.Port);
				}

				key = obj.getString(Constants.KEY);
				initVector = obj.getString(Constants.INITVECTOR);
			} catch (FileNotFoundException e) {
				System.exit(255);
			} catch (JSONException e) {
				System.exit(255);
			} catch (IOException e) {
				System.exit(255);
			}
		}
	}

	private CardFileObject getFileContents(String cardFileName) throws IOException {
		File cardFile = new File(cardFileName);
		CardFileObject fileObject = new CardFileObject();
		BufferedReader reader = null;
		if (cardFile.exists() && !cardFile.isDirectory()) {
			try {
				reader = new BufferedReader(new FileReader(cardFileName));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();
				while (line != null) {
					sb.append(line);
					sb.append("\n");
					line = reader.readLine();
				}
				Gson gson = new GsonBuilder().create();
				fileObject = gson.fromJson(sb.toString(), CardFileObject.class);
			} catch (FileNotFoundException e) {
				System.exit(255);
			} finally {
				reader.close();
			}
		}
		return fileObject;
	}

	private boolean createCardFile(String custAccountName, String cardFileName) {
		String currentDirectory = System.getProperty(Constants.GetCurrentDirectory);
		currentDirectory = currentDirectory.concat("/");
		File cardFile = null;
		if (cardFileName.isEmpty()) {
			cardFile = new File(currentDirectory.concat(cardFileName));
		} else if (!cardFileName.isEmpty()) {
			cardFile = new File(currentDirectory.concat(cardFileName));
		}

		try {
			boolean isFileCreated = cardFile.createNewFile();
			if (isFileCreated) {
				Random r = new Random();
				int randomPin = 0 + r.nextInt(9999);
				JSONObject jsonOutput = new JSONObject();
				try {
					jsonOutput.put(Constants.ACCOUNT_NAME, custAccountName);
					jsonOutput.put(Constants.PIN, String.valueOf(randomPin));
				} catch (JSONException e) {
					e.printStackTrace();
				}

				PrintWriter writeToFile = new PrintWriter(cardFile.toString(), Constants.CharacterEncodingType);
				writeToFile.println(jsonOutput.toString());
				writeToFile.close();
			}
		} catch (IOException e) {
			System.exit(255);
		}
		return true;
	}

	public static void main(String... args) throws IOException {
		Atm atm = new Atm();
		try {
			if (!atm.processInput(args)) {
				System.exit(255);
			}			
		} catch (JSONException e) {
			System.exit(255);
		} catch (IOException e) {
			System.exit(255);
		}
	}

	public class CardFileObject {
		String accountname;
		String pin;
	}
}
