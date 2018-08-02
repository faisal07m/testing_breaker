package de.upb.bibifi2018.kaffeeklatsch.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.AuthKeys;
import de.upb.bibifi2018.kaffeeklatsch.atm.BankProtocolClient;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardException;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import okio.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BankProtocolIntegrationTest {
  private final ByteString bankPrivKey = Keys.generatePrivateKey();
  private final ByteString bankPubKey = Keys.generatePublicKey(bankPrivKey);
  private final ByteString atmPrivKey = Keys.generatePrivateKey();
  private final ByteString atmPubKey = Keys.generatePublicKey(atmPrivKey);

  private BankReaktor reaktor;
  private ByteString cardPrivKey;

  private final AuthKeys authKeys = new AuthKeys(atmPrivKey, bankPubKey);

  @BeforeEach
  void setUp() {
    reaktor = new BankReaktor();
    cardPrivKey = null;
  }

  @Test
  void testCreateAccount() throws ProtocolFailureException,
      NoSuchAccountException, AccountAlreadyExistsException,
      ReplayAttackException, InvalidCardException {

    SimpleBox simpleBox = new SimpleBox(atmPubKey, bankPrivKey);
    BankProtocolServer server = new BankProtocolServer(simpleBox, bankPrivKey, reaktor);

    BankProtocolClient client = new BankProtocolClient(authKeys.asSimpleBox(), "1337");

    client.receiveHello(server.sendChallenge());

    BigDecimal amount = new BigDecimal("100.00");
    ByteString atmRequest = client.sendCreate(amount);
    ByteString bankAnswer = server.processRequest(atmRequest);
    CreateAccountResult createAccountResult = client.receiveCreateResult(bankAnswer);

    ByteString cardPrivKey = createAccountResult.getPrivateKey();

    checkBalance(cardPrivKey, amount);

    this.cardPrivKey = cardPrivKey;
  }

  @Test
  void testBalance() throws ProtocolFailureException, AccountAlreadyExistsException,
      NoSuchAccountException, ReplayAttackException, InvalidCardException {

    testCreateAccount();

    checkBalance(this.cardPrivKey, new BigDecimal("100.00"));
  }

  void checkBalance(ByteString cardPrivKey, BigDecimal expected)
      throws ProtocolFailureException, ReplayAttackException,
      InvalidCardException, NoSuchAccountException {

    SimpleBox simpleBox = new SimpleBox(atmPubKey, bankPrivKey);
    BankProtocolServer server = new BankProtocolServer(simpleBox, bankPrivKey, reaktor);

    BankProtocolClient client = new BankProtocolClient(
        authKeys.asSimpleBox(),new SimpleBox(bankPubKey,cardPrivKey), "1337"
    );


    client.receiveHello(server.sendChallenge());
    ByteString balanceResultMessage = server.processRequest(client.sendBalance());
    BalanceResult balanceResult = client.receiveBalanceResult(balanceResultMessage);

    assertEquals(
        expected.setScale(2, RoundingMode.UNNECESSARY),
        balanceResult.getBalance()
    );
  }

  @Test
  void testWithdraw() throws ProtocolFailureException, AccountAlreadyExistsException,
      NoSuchAccountException, ReplayAttackException, InvalidCardException {

    testCreateAccount();


    SimpleBox simpleBox = new SimpleBox(atmPubKey, bankPrivKey);
    BankProtocolServer server = new BankProtocolServer(simpleBox, bankPrivKey, reaktor);

    BankProtocolClient client = new BankProtocolClient(
        authKeys.asSimpleBox(), new SimpleBox(this.bankPubKey, this.cardPrivKey),"1337"
    );


    client.receiveHello(server.sendChallenge());
    ByteString balanceResultMessage = server.processRequest(
        client.sendWithdraw(new BigDecimal("86.63"))
    );
    SuccessResult withdrawResult = client.receiveWithdrawResult(balanceResultMessage);
    assertNotNull(withdrawResult);

    assertTrue(withdrawResult.isSuccess());
    checkBalance(this.cardPrivKey, new BigDecimal("13.37"));

  }


  @Test
  void testDeposit() throws ProtocolFailureException, NoSuchAccountException,
      AccountAlreadyExistsException, ReplayAttackException, InvalidCardException {

    testWithdraw();

    SimpleBox simpleBox = new SimpleBox(atmPubKey, bankPrivKey);
    BankProtocolServer server = new BankProtocolServer(simpleBox, bankPrivKey, reaktor);

    BankProtocolClient client = new BankProtocolClient(
        authKeys.asSimpleBox(), new SimpleBox(this.bankPubKey, this.cardPrivKey),"1337"
    );


    client.receiveHello(server.sendChallenge());
    ByteString balanceResultMessage = server.processRequest(
        client.sendDeposit(new BigDecimal("1323.63"))
    );
    SuccessResult depositResult = client.receiveDepositResult(balanceResultMessage);
    assertNotNull(depositResult);

    assertTrue(depositResult.isSuccess());
    checkBalance(this.cardPrivKey, new BigDecimal("1337"));

  }
}
