import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public class Util {
	public double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}		
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public boolean validPort(String port) {
		try {
			Integer portNumber = Integer.valueOf(port);
			if (portNumber < 1024 || portNumber > 65535) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public boolean validAuthFileName(String authFileName) {
		if (authFileName.equals(".") || authFileName.equals("..") || authFileName.length() < 1
				|| authFileName.length() > 127) {
			return false;
		}
		String regularExp = "[_\\-\\.0-9a-z]";
		for (int i = 0; i < authFileName.length(); i++) {
			String stringTemp = String.valueOf(authFileName.charAt(i));
			if (!Pattern.matches(regularExp, stringTemp)) {
				return false;
			}
		}
		return true;
	}
}
