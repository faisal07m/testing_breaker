import java.math.BigDecimal;
public class InputChecker {
//test method
//	public static void main (String[] args) throws IllegalArgumentException {
//		String[] input={"-gakarl", "-cmain.card","-p","2222","-i","198.168.0.1"};
//		System.out.println(checkInput(input).toString());
//	}
	public static final String DEFAULT_AUTHFILE = "bank.auth";
	public static final String DEFAULT_IPADRESS = "127.0.0.1";
	public static final String DEFAULT_PORT = "3000";
	
	public static InputData checkInput(String[] input) throws IllegalArgumentException{
		InputData inputData = new InputData();
		input = addWhitespaces(input);
		for(int i=0; i<input.length; i++) {
			if(input[i].charAt(0)=='-' && input[i].length()==2) {
				switch(input[i].charAt(1)) {
				case 's':
					if(RegExChecker.checkFileName(input[i+1].replaceFirst("\\..*", ""))) {
						inputData.autpath = input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'i':
					if(!inputData.ipadress.equals("")) {
						throw new IllegalArgumentException();
					}
					if(RegExChecker.checkIP(input[i+1])) {
						inputData.ipadress=input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'p':
					if(!inputData.port.equals("")) {
						throw new IllegalArgumentException();
					}
					if(RegExChecker.checkPort(input[i+1])) {
						inputData.port=input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'c':
					if(RegExChecker.checkFileName(input[i+1].replaceFirst("\\..*", ""))){
						inputData.cardpath = input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'a':
					if(!inputData.account.equals("")) {
						throw new IllegalArgumentException();
					}
					if(RegExChecker.checkAccountName(input[i+1])) {
						inputData.account=input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'n':
					if(!inputData.command.equals("")) {
						throw new IllegalArgumentException();
					}
					if(RegExChecker.checkCurrencyAmt(input[i+1])) {
						inputData.command="n";
						inputData.argument=input[i+1];
                        BigDecimal bD = new BigDecimal(inputData.argument);
                        if(bD.compareTo(new BigDecimal(10.0)) < 0)
                            throw new IllegalArgumentException();
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'd':
					if(!inputData.command.equals("")) {
						throw new IllegalArgumentException();
					}
					if(RegExChecker.checkCurrencyAmt(input[i+1])) {
						inputData.command="d";
						inputData.argument=input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'w':
					if(!inputData.command.equals("")) {
						throw new IllegalArgumentException();
					}
					if(RegExChecker.checkCurrencyAmt(input[i+1])) {
						inputData.command="w";
						inputData.argument=input[i+1];
						i++;
					} else {
						throw new IllegalArgumentException();
					}
					break;
				case 'g':
					if(!inputData.command.equals("")) {
						throw new IllegalArgumentException();
					}
					inputData.command="g";
					break;
				default:
					throw new IllegalArgumentException();
				}
			} else if(input[i].charAt(0)=='-' && input[i].length()==3) {
				if(input[i].charAt(1)=='g') {
					inputData.command="g";
					switch(input[i].charAt(2)) {
					case 's':
						if(RegExChecker.checkFileName(input[i+1].replaceFirst("\\..*", "")) && input[i+1].substring(input[i+1].lastIndexOf('.')).equals(".auth")) {
							inputData.autpath = input[i+1];
							i++;
						} else {
							throw new IllegalArgumentException();
						}
						break;
					case 'i':
						if(!inputData.ipadress.equals("")) {
							throw new IllegalArgumentException();
						}
						if(RegExChecker.checkIP(input[i+1])) {
							inputData.ipadress=input[i+1];
							i++;
						} else {
							throw new IllegalArgumentException();
						}
						break;
					case 'p':
						if(!inputData.port.equals("")) {
							throw new IllegalArgumentException();
						}
						if(RegExChecker.checkPort(input[i+1])) {
							inputData.port=input[i+1];
							i++;
						} else {
							throw new IllegalArgumentException();
						}
						break;
					case 'c':
						if(RegExChecker.checkFileName(input[i+1].replaceFirst("\\..*", ""))  && input[i+1].substring(input[i+1].lastIndexOf('.')).equals(".card")){
							inputData.cardpath = input[i+1];
							i++;
						} else {
							throw new IllegalArgumentException();
						}
						break;
					case 'a':
						if(!inputData.account.equals("")) {
							throw new IllegalArgumentException();
						}
						if(RegExChecker.checkAccountName(input[i+1])) {
							inputData.account=input[i+1];
							i++;
						} else {
							throw new IllegalArgumentException();
						}
						break;
					default:
						throw new IllegalArgumentException();
					}
				} else {
					throw new IllegalArgumentException();
				}
			} else {
				throw new IllegalArgumentException();
			}
		}
		if(inputData.account.equals("") || inputData.command.equals("")) {
			throw new IllegalArgumentException();
		}
		if(inputData.autpath.equals("")) {
			inputData.autpath = DEFAULT_AUTHFILE;
		}
		if(inputData.ipadress.equals("")) {
			inputData.ipadress = DEFAULT_IPADRESS;
		}
		if(inputData.port.equals("")) {
			inputData.port = DEFAULT_PORT;
		}
		if(inputData.cardpath.equals("")) {
			inputData.cardpath = inputData.account + ".card";
		}
		return inputData;
	}
	
	private static String[] addWhitespaces(String[] input) {
		String helper = "";
		for(String s : input) {
			if(s.charAt(0)=='-') {
				if(s.charAt(1)=='g') {
					if(s.length()>3) {
						helper+=s.substring(0,3)+"|";
						helper+=s.substring(3)+"|";
					} else {
						helper+=s+"|";
					}
				} else if(s.length()>2) {
					helper+=s.substring(0,2)+"|";
					helper+=s.substring(2)+"|";
				} else {
					helper+=s+"|";
				}
			} else {
				helper+=s+"|";
			}
		}
		helper=helper.substring(0, helper.length()-1);
		return helper.split("\\|");
	}

	public static class InputData {
		public String autpath="";
		public String ipadress="";
		public String port="";
		public String cardpath="";
		public String account="";
		public String command="";
		public String argument="";
		
		public String toString() {
			return autpath + "," + ipadress  + "," + port + "," + cardpath + "," + account + "," + command + "," + argument;
		}
	}
}