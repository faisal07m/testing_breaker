package de.upb.bibifi2018.kaffeeklatsch.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.atm.CardProtocolClient;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.bank.BankReaktor;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.BankingFailureException;
import de.upb.bibifi2018.kaffeeklatsch.bank.CardProtocolServer;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccount;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.math.BigDecimal;
import okio.ByteString;
import org.junit.jupiter.api.Test;

public class CardProtocolTest {
  private final ByteString bankPrivKey = Keys.generatePrivateKey();
  private final ByteString bankPubKey = Keys.generatePublicKey(bankPrivKey);


  @Test
  void name() throws BankingFailureException, ProtocolFailureException,
      NoSuchAccountException, AccountAlreadyExistsException, ReplayAttackException {

    String account = "test";
    BankReaktor bankReaktor = new BankReaktor();
    CreateAccountResult account1 = bankReaktor.createAccount(
        new CreateAccount(account, new BigDecimal("10.00"))
    );

    SimpleBox cardBox = new SimpleBox(bankPubKey, account1.getPrivateKey());


    CardProtocolServer server = new CardProtocolServer(bankPrivKey, account, bankReaktor);

    ByteString bankChallenge = ChallengeGenerator.generateChallenge();
    CardProtocolClient client = new CardProtocolClient(cardBox, account, bankChallenge);

    ByteString depositRequest = client.depositRequest(new BigDecimal("100000.00"));
    ByteString response = server.answerBankRequest(depositRequest, bankChallenge);
    SuccessResult successResult = client.depositResponse(response);
    assertTrue(successResult.isSuccess());



  }
}
