import java.math.BigDecimal;
import java.util.regex.Pattern;
public class ATMStringHandler {
	public String getNmsg(String account, BigDecimal amt, String pk) {
		return "N," + account + "," + amt + "," + pk;
	}
	public String getDmsg(String account, BigDecimal amt) {
		return "D," + account + "," + amt + ",";
	}
	public String getWmsg(String account, BigDecimal amt) {
		return "W," + account + "," + amt + ",";
	}
	public String getGmsg(String account) {
		return "G," + account + ",";
	}
	public String getCmsg(String random) {
		return "C," + random;
	}
	public boolean handleMsg(String msg) {
		return Pattern.matches("[NXCDWG],\\d*(\\.\\d{2})?", msg);
	}
}