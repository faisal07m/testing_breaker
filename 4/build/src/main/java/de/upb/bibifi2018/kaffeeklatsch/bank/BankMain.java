package de.upb.bibifi2018.kaffeeklatsch.bank;

import com.codahale.xsalsa20poly1305.Keys;
import de.upb.bibifi2018.kaffeeklatsch.AuthFile;
import de.upb.bibifi2018.kaffeeklatsch.ExitCode;
import de.upb.bibifi2018.kaffeeklatsch.InputValidator;
import de.upb.bibifi2018.kaffeeklatsch.Printer;
import de.upb.bibifi2018.kaffeeklatsch.StrictArgParser;
import java.io.IOException;
import okio.ByteString;

public class BankMain {

  /**
   * The port the bank should listen to.
   */
  private int port;

  /**
   * Filename of the auth file.
   */
  private String authFile;

  public static void main(String[] args) {

    try {
      StrictArgParser options = new StrictArgParser(args, "", "ps");

      String portStr = options.getParameter('p', "3000");
      String bankFile = options.getParameter('s', "bank.auth");

      boolean valid = InputValidator.isPort(portStr)
          && InputValidator.isFileName(bankFile);

      if (!valid) {
        throw new Exception();
      }

      int port = Integer.parseInt(portStr);
      BankMain bankMain = new BankMain(port, bankFile);
      bankMain.run();

    } catch (Throwable t) {
      // ssshh now, quiet
      Runtime.getRuntime().halt(ExitCode.UNKNOWN_FAILURE.getExitCode());
    }
  }


  private BankMain(int port, String authFile) {
    this.authFile = authFile;
    this.port = port;
  }


  /**
   * Run the BankMain.
   */
  protected void run() {

    try {
      ByteString bankPrivKey = Keys.generatePrivateKey();
      ByteString bankPubKey = Keys.generatePublicKey(bankPrivKey);
      ByteString atmPrivKey = Keys.generatePrivateKey();
      ByteString atmPubKey = Keys.generatePublicKey(atmPrivKey);


      AuthFile authFile = new AuthFile(this.authFile);
      authFile.writeKeys(atmPrivKey, bankPubKey);


      Server server = new Server(this.port, atmPubKey, bankPrivKey);
      shutdownHook();
      Printer.printServerCreated();
      server.run();
    } catch (IOException e) {
      System.exit(ExitCode.ARGUMENT_VIOLATION.getExitCode());
    }

    System.exit(0);
  }

  /**
   * This will override all exit codes to 0 on JVM exit,
   * especially for SIGTERM.
   */
  private void shutdownHook() {
    Thread exitCleanly = new Thread(() -> Runtime.getRuntime().halt(0));
    Runtime.getRuntime().addShutdownHook(exitCleanly);
  }
}
