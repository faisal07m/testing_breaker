public class ATMjsonLogger {	
	public static void printN(String account, String amt) {
		System.out.println("{\"initial_balance\":" + amt + ",\"account\":\"" + account + "\"}");
		//System.out.println("{\"account\":\"" + account + "\",\"initial_balance\":" + amt + "}");
		System.out.flush();
	}
	public static void printD(String account, String amt) {
		System.out.println("{\"account\":\"" + account + "\",\"deposit\":" + amt + "}");
		System.out.flush();
	}
	public static void printW(String account, String amt) {
		System.out.println("{\"account\":\"" + account + "\",\"withdraw\":" + amt + "}");
		System.out.flush();
	}
	public static void printG(String account, String amt) {
		System.out.println("{\"account\":\"" + account + "\",\"balance\":" + amt + "}");
		System.out.flush();
	}
}