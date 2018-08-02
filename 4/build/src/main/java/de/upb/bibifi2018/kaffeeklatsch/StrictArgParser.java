package de.upb.bibifi2018.kaffeeklatsch;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StrictArgParser {

  private String[] input;
  private final String flags;
  private final String arguments;
  private int index;

  private final Set<Character> foundFlags;
  private final Map<Character, String> foundArguments;


  public StrictArgParser(String[] input, String flags, String arguments)
      throws StrictParserException {

    this.input = input;
    this.flags = flags;
    this.arguments = arguments;
    this.index = 0;
    this.foundFlags = new HashSet<>();
    this.foundArguments = new HashMap<>();

    boolean intersection = flags.chars().anyMatch(ch -> arguments.indexOf(ch) >= 0);
    if (intersection) {
      throw new IllegalArgumentException("flags and arguments intersect");
    }
    parse();
  }

  private void parse() throws StrictParserException {
    while (hasNext()) {
      parseArg();
    }
  }

  private boolean hasNext() {
    return this.index < this.input.length;
  }

  private String next() throws StrictParserException {
    if (this.index >= this.input.length) {
      throw new StrictParserException();
    }
    return this.input[index++];
  }

  private void parseArg() throws StrictParserException {
    String token = next();

    if (token.isEmpty() || token.charAt(0) != '-') {
      throw new StrictParserException();
    }


    String remaining = parseFlags(token.substring(1));

    if (remaining.isEmpty()) {
      return;
    }

    char param = remaining.charAt(0);
    if (this.arguments.indexOf(param) < 0) {
      throw new StrictParserException();
    }

    if (this.foundArguments.containsKey(param)) {
      throw new StrictParserException();
    }

    String argument = remaining.substring(1);
    if (argument.isEmpty()) {
      argument = next();
      if (!argument.isEmpty() && argument.charAt(0) == '-') {
        throw new StrictParserException(); // param looks like option
      }
    }

    this.foundArguments.put(param, argument);
  }

  private String parseFlags(String token) throws StrictParserException {

    String remaining = token;
    while (!remaining.isEmpty() && this.flags.indexOf(remaining.charAt(0)) >= 0) {
      char flag = remaining.charAt(0);
      remaining = remaining.substring(1);

      if (this.foundFlags.contains(flag)) {
        throw new StrictParserException();
      }
      this.foundFlags.add(flag);
    }
    return remaining;
  }

  public Set<Character> getFoundFlags() {
    return Collections.unmodifiableSet(this.foundFlags);
  }

  public Map<Character, String> getFoundArguments() {
    return Collections.unmodifiableMap(foundArguments);
  }


  public boolean hasParameter(Character option) {
    return this.foundArguments.containsKey(option);
  }

  public String getParameter(Character option) throws StrictParserException {
    String arg = this.foundArguments.get(option);
    if (arg == null) {
      throw new StrictParserException();
    }
    return arg;
  }

  public String getParameter(Character option, String defaultValue) {
    return this.foundArguments.getOrDefault(option, defaultValue);
  }

  public boolean getFlag(Character flag) {
    return this.foundFlags.contains(flag);
  }


  public void hasExactlyOneOf(String argsOrFlags) throws StrictParserException {

    long found = argsOrFlags.chars()
        .mapToObj(ch -> Character.valueOf((char) ch))
        .filter(ch -> this.foundFlags.contains(ch) || this.foundArguments.containsKey(ch))
        .count();

    if (found != 1) {
      throw new StrictParserException();
    }
  }

  public void hasAll(String argsOrFlags) throws StrictParserException {
    boolean foundMissing = argsOrFlags.chars()
        .mapToObj(ch -> Character.valueOf((char) ch))
        .anyMatch(ch -> !this.foundFlags.contains(ch) && !this.foundArguments.containsKey(ch));

    if (foundMissing) {
      throw new StrictParserException();
    }
  }


  public class StrictParserException extends Exception {
  }
}
