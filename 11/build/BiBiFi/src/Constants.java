import java.text.DecimalFormat;

public class Constants {
	public final static String ACCOUNT_NAME = "account";
	public final static String INITIAL_BALANCE = "initial_balance";
	public final static String BALANCE = "balance";
	public final static String DEPOSIT = "deposit";
	public final static String CARD_PIN = "card_pin";
	public final static String WITHDRAW = "withdraw";
	public final static String OPERATION = "operation";
	public final static String KEY = "Key";
	public final static String INITVECTOR = "InitVector";
	public final static String IP_STRING = "ip";
	public final static String PORT_NUMBER = "port_number";
	public final static String New_Account_Parameter = "-n";
	public final static String Account = "account";
	public final static String Initial_Balance = "initial_balance";
	public final static String Deposit_Parameter = "-d";
	public final static String Deposit = "deposit";
	public final static String Withdraw = "withdraw";
	public final static String Withdraw_Parameter = "-w";
	public final static String Balance_Parameter = "-g";
	public final static String AccountName = "accountname";
	public final static String PIN = "pin";
	public final static String Port = "Port";

	// ATM Constant Fields
	final static String CardExtension = ".card";
	final static String AuthExtension = ".auth";
	final static String EncrytionName = "AES";
	final static String GetCurrentDirectory = "user.dir";
	final static String CharacterEncodingType = "UTF-8";
	final static String EncryptionType = "AES/CBC/PKCS5PADDING";
	final static String AtmCommands[] = { "-a", "-s", "-i", "-p", "-c", "-n", "-d", "-w", "-g", "-ga" };
	final static String BankResponseKeys[] = { "account", "initial_balance", "balance", "withdraw", "deposit" };
	final static DecimalFormat DecimalFormat = new DecimalFormat(".##");
	final static String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
			+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	// Constants Messages
	public final static String Withdraw_Message = "Withdraw amount greater than balance";
	public final static String AccountExistsMessage = "Account already exists";
	public final static String ProtocolErrorMessage = "protocol_error";

	// Amount Regex
	public final static String AmountRegularExpression = "[1-9][0-9]*\\.[0-9]{2}";
	public final static String ValidAuthFileNameRegularExpression = "[_\\-\\.0-9a-z]";
	public final static String ValidPortRegularExpression = "[_\\-\\.0-9a-z]";
	public final static String ValidAmountRegularExpression = "^[1-9][0-9]*\\.[0-9]{2}";
	public final static String PROTOCOL_ERROR = "protocol_error";
	public static final String atmRequestKeys[] = { "account", "initial_balance", "card_pin", "operation", "deposit",
			"withdraw" };
	public static final String bankCommands[] = { "-p", "-s", "-ps", "-sp" };

}
