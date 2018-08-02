import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class Validations {
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

	@SuppressWarnings("unchecked")
	public boolean validateRequestJson(JSONObject obj) {
		/* Validate the received json object for invalid keys received. */
		Iterator<String> keysItr = obj.keys();
		while (keysItr.hasNext()) {
			if (!Arrays.asList(Constants.atmRequestKeys).contains(keysItr.next())) {
				return false;
			}				
		}
		return true;
	}
}
