package de.upb.bibifi2018.kaffeeklatsch.bank;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import de.upb.bibifi2018.kaffeeklatsch.atm.CardProtocolClient;
import de.upb.bibifi2018.kaffeeklatsch.commands.AccountCommand;
import de.upb.bibifi2018.kaffeeklatsch.commands.Balance;
import de.upb.bibifi2018.kaffeeklatsch.commands.Deposit;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import de.upb.bibifi2018.kaffeeklatsch.commands.Withdraw;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.BankingFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.protocol.ChallengeProtocol;
import de.upb.bibifi2018.kaffeeklatsch.protocol.MessageType;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import okio.ByteString;

/**
 * @see CardProtocolClient
 */
public class CardProtocolServer extends ChallengeProtocol {
  private final String accountId;
  private final BankReaktor bankReaktor;

  public CardProtocolServer(ByteString bankPrivateKey, String accountId, BankReaktor bankReaktor)
      throws NoSuchAccountException {

    super(new SimpleBox(bankReaktor.getAccountPublicKey(accountId), bankPrivateKey));
    this.accountId = accountId;
    this.bankReaktor = bankReaktor;
  }

  /**
   * @param expectedBankChallenge we include the bank challenge inside the card container to
   *                              assure the Atm cannot do replay attacks using old
   *                              card transactions without the card-file.
   */
  public ByteString answerBankRequest(ByteString message, ByteString expectedBankChallenge)
      throws BankingFailureException, ProtocolFailureException,
      NoSuchAccountException, ReplayAttackException {

    JsonObject json;
    try {
      json = this.receiveMessage(message).getAsJsonObject();
    } catch (ProtocolFailureException e) {
      // atm was given wrong card file
      throw new BankingFailureException("wrong card for account");
    }
    ByteString bankChallengeEcho = ByteString.decodeBase64(
        json.getAsJsonPrimitive(JsonConstants.BANK_CHALLENGE_KEY).getAsString()
    );

    if (!expectedBankChallenge.equals(bankChallengeEcho)) {
      throw new ReplayAttackException("wrong bank challenge in card request");
    }

    MessageType messageType = MessageType.valueOf(
        json.getAsJsonPrimitive(JsonConstants.COMMAND_TYPE_KEY).getAsString()
    );
    JsonObject commandJson = json.getAsJsonObject(JsonConstants.COMMAND_KEY);

    return sendMessage(handleCommand(messageType, commandJson));


  }

  private JsonObject handleCommand(MessageType messageType, JsonObject command)
      throws NoSuchAccountException, ProtocolFailureException {


    if (messageType == MessageType.WITHDRAW) {

      Withdraw withdraw = Withdraw.fromJson(command);
      checkInnerMatchesOuterAccountId(withdraw);
      return bankReaktor.withdraw(withdraw).toJson();

    } else if (messageType == MessageType.DEPOSIT) {

      Deposit deposit = Deposit.fromJson(command);
      checkInnerMatchesOuterAccountId(deposit);
      return this.bankReaktor.deposit(deposit).toJson();

    } else if (messageType == MessageType.BALANCE) {

      Balance balance = Balance.fromJson(command);
      checkInnerMatchesOuterAccountId(balance);
      return this.bankReaktor.balance(balance).toJson();

    } else {
      throw new ProtocolFailureException("invalid card command");
    }
  }

  /**
   * Check that the Atm has access to the private key of the card by checking that
   * the bank challenge of the {@link BankProtocolServer} is contained in the signed
   * message of the card. This is to assure the Atm cannot do replay attacks using
   * old card transactions without the card-file.
   */
  private void checkInnerMatchesOuterAccountId(AccountCommand command)
      throws ProtocolFailureException {

    if (!this.accountId.equals(command.getAccountId())) {
      throw new ProtocolFailureException("command account id does not match envelope");
    }
  }
}
