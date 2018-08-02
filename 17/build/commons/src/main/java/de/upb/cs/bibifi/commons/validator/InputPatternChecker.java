package de.upb.cs.bibifi.commons.validator;

public class InputPatternChecker {
    public static boolean check(String[] args) {
        String commandArg = String.join(" ", args);
        return check(commandArg);
    }

    public static boolean check(String input){
        String pattern = "^(-[aipcs]\\s*\\S+\\s*)*(-[nwd]\\s*[0-9]+.?[0-9]*\\s*|-g\\s*)(-[aipcs]\\s*\\S+\\s*)*$";
        return input.matches(pattern);
    }

}
