import java.util.regex.Pattern;
public class RegExChecker {
	public static boolean checkNumber(String number) {
		return Pattern.matches("0|([1-9]\\d*)", number);
	}
	public static boolean checkFraction(String fraction) {
		return Pattern.matches("\\d{2}", fraction);
	}
	public static boolean checkCurrencyAmt(String currencyAmt) {
		
		String[] numbers = currencyAmt.split("\\.");
		if(numbers.length==2) {
			if(checkNumber(numbers[0]) && checkFraction(numbers[1])) {
				double amt = Double.parseDouble(currencyAmt);
				if (0<=amt && amt<=4294967295.99) {
					return true;
				}
			}
		}
		return false;
	}
	public static boolean checkFileName(String fileName){
		if(fileName.equals(".") || fileName.equals("..") || fileName.length() > 127) {
			return false;
		}
		return Pattern.matches("[_\\-\\.0-9a-z]+", fileName);
	}
	public static boolean checkAccountName(String accountName){
		return Pattern.matches("[_\\-\\.0-9a-z]{1,122}", accountName);
	}
	public static boolean checkIP(String ip){
		String[] numbers = ip.split("\\.");
		if(numbers.length==4) {
			for (String number : numbers) {
				if (!checkNumber(number)) {
					return false;
				} else {
					int intnumber = Integer.parseInt(number);
					if (0 > intnumber || intnumber > 255) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	public static boolean checkPort(String port) {
		if(checkNumber(port)) {
			int intport = Integer.parseInt(port);
			if (1024<=intport && intport<=65535) {
				return true;
			}
		}
		return false;
	}
}