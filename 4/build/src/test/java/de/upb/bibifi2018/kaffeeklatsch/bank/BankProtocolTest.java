package de.upb.bibifi2018.kaffeeklatsch.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.atm.BankProtocolClient;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardException;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.math.BigDecimal;
import okio.ByteString;
import org.junit.jupiter.api.Test;

public class BankProtocolTest {
  private final ByteString bankPrivKey = Keys.generatePrivateKey();
  private final ByteString bankPubKey = Keys.generatePublicKey(bankPrivKey);
  private final ByteString atmPrivKey = Keys.generatePrivateKey();
  private final ByteString atmPubKey = Keys.generatePublicKey(atmPrivKey);
  private final SimpleBox bankKeyPair = new SimpleBox(atmPubKey, bankPrivKey);
  private final SimpleBox atmKeyPair = new SimpleBox(bankPubKey, atmPrivKey);

  @Test
  void createTest() throws ProtocolFailureException, AccountAlreadyExistsException, InvalidCardException, NoSuchAccountException, ReplayAttackException {
    BankReaktor reaktor = new BankReaktor();

    BankProtocolServer server = new BankProtocolServer(bankKeyPair, bankPrivKey, reaktor);

    BankProtocolClient clientCreate = new BankProtocolClient(atmKeyPair, "test1");

    clientCreate.receiveHello(server.sendChallenge());

    ByteString byteString = clientCreate.sendCreate(new BigDecimal("100.00"));
    ByteString response = server.processRequest(byteString);

    CreateAccountResult createAccountResult = clientCreate.receiveCreateResult(response);

    SimpleBox card = new SimpleBox(bankPubKey, createAccountResult.getPrivateKey());
    BankProtocolClient balanceClient = new BankProtocolClient(atmKeyPair, card, "test1");

    server = new BankProtocolServer(bankKeyPair, bankPrivKey, reaktor);

    balanceClient.receiveHello(server.sendChallenge());

    ByteString responseBalance = server.processRequest(balanceClient.sendBalance());
    BalanceResult balanceResult = balanceClient.receiveBalanceResult(responseBalance);

    assertEquals(new BigDecimal("100.00"), balanceResult.getBalance());
  }
}
