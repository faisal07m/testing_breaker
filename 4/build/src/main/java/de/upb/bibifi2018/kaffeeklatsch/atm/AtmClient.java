package de.upb.bibifi2018.kaffeeklatsch.atm;

import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.AuthKeys;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.protocol.CryptoConnection;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import okio.ByteString;

public class AtmClient implements Closeable {
  private final AuthKeys atmKeys;
  private final String serverIp;
  private final int serverPort;
  private final String account;
  private Socket socket;
  private CryptoConnection connection;
  private BankProtocolClient bankProtocolClient;
  private State state;

  public AtmClient(AuthKeys atmKeys, String serverIp, int serverPort, String account) {
    this.atmKeys = atmKeys;
    this.serverIp = serverIp;
    this.serverPort = serverPort;
    this.account = account;
    this.state = State.CREATED;
  }

  public void connectWithCard(SimpleBox card) throws IOException, ProtocolFailureException {

    checkState(State.CREATED);
    this.socket = new Socket(this.serverIp, this.serverPort);
    this.connection = new CryptoConnection(this.socket, this.atmKeys.asSimpleBox());

    this.bankProtocolClient = new BankProtocolClient(
        this.atmKeys.asSimpleBox(), card, this.account
    );
    ByteString hello = this.connection.readMessage();
    this.bankProtocolClient.receiveHello(hello);
    this.state = State.CONNECTED_CARD;

  }

  public void connectWithoutCard() throws IOException, ProtocolFailureException {

    checkState(State.CREATED);
    this.socket = new Socket(this.serverIp, this.serverPort);
    this.connection = new CryptoConnection(this.socket, this.atmKeys.asSimpleBox());

    this.bankProtocolClient = new BankProtocolClient(
        this.atmKeys.asSimpleBox(), this.account
    );
    ByteString hello = this.connection.readMessage();
    this.bankProtocolClient.receiveHello(hello);
    this.state = State.CONNECTED_NO_CARD;
  }

  public SuccessResult deposit(BigDecimal amount)
      throws IOException, ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    checkState(State.CONNECTED_CARD);
    ByteString message = this.bankProtocolClient.sendDeposit(amount);
    this.connection.sendMessage(message);
    ByteString response = this.connection.readMessage();
    this.state = State.DEAD;
    return this.bankProtocolClient.receiveDepositResult(response);

  }

  public SuccessResult withdraw(BigDecimal amount)
      throws IOException, ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    checkState(State.CONNECTED_CARD);
    ByteString message = this.bankProtocolClient.sendWithdraw(amount);
    this.connection.sendMessage(message);
    ByteString response = this.connection.readMessage();
    this.state = State.DEAD;
    return this.bankProtocolClient.receiveWithdrawResult(response);
  }

  public BalanceResult balance()
      throws IOException, ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    checkState(State.CONNECTED_CARD);
    ByteString message = this.bankProtocolClient.sendBalance();
    this.connection.sendMessage(message);
    ByteString response = this.connection.readMessage();
    this.state = State.DEAD;
    return this.bankProtocolClient.receiveBalanceResult(response);
  }

  public CreateAccountResult createAccount(BigDecimal initialBalance)
      throws IOException, ProtocolFailureException,
      AccountAlreadyExistsException, ReplayAttackException {

    checkState(State.CONNECTED_NO_CARD);
    ByteString message = this.bankProtocolClient.sendCreate(initialBalance);
    this.connection.sendMessage(message);
    ByteString response = this.connection.readMessage();
    this.state = State.DEAD;
    return this.bankProtocolClient.receiveCreateResult(response);
  }


  @Override
  public void close() {
    if (this.socket != null) {
      try {
        this.socket.close();
      } catch (IOException e) {
        System.err.println("error closing client socket");
        // ignore, when we are closing the socket everything is over anyway
      }
    }
  }

  private enum State {
    CREATED,
    CONNECTED_NO_CARD,
    CONNECTED_CARD,
    DEAD
  }

  private void checkState(State expectedState) throws IllegalStateException {
    if (this.state != expectedState) {
      this.state = State.DEAD;
      throw new IllegalStateException("invalid state of AtmClient");
    }
  }
}
