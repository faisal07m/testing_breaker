package de.upb.bibifi2018.kaffeeklatsch.atm;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import de.upb.bibifi2018.kaffeeklatsch.commands.Balance;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.Deposit;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.Withdraw;
import de.upb.bibifi2018.kaffeeklatsch.protocol.ChallengeProtocol;
import de.upb.bibifi2018.kaffeeklatsch.protocol.MessageType;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.math.BigDecimal;
import okio.ByteString;

/**
 * This implements the cryptographic authenticated channel from the card to the bank.
 * <br><br>
 *
 * This is built with the scenario in mind that the atm could otherwise alter
 * requests from the card holder to the bank or back.
 * <br><br>
 *
 * This means if the card file would be a smartcard or hardware security device,
 * it would sign its own requests and verify the integrity of the answer of the bank
 * independently of the atm.
 * <br><br>
 *
 * This feature is not fully realized, since the Atm is handling all the operations,
 * but was a reasonable step to take in the given spec/problem domain.
 */
public class CardProtocolClient extends ChallengeProtocol {
  private final String accountId;
  private final ByteString bankChallenge;

  public CardProtocolClient(SimpleBox keypair, String accountId, ByteString bankChallenge) {
    super(keypair);
    this.accountId = accountId;
    this.bankChallenge = bankChallenge;
  }

  private ByteString packMessage(MessageType type, JsonObject cmd) {
    JsonObject packed = new JsonObject();
    packed.addProperty(JsonConstants.COMMAND_TYPE_KEY, type.name());


    // See CardProtocolServer.checkInnerMatchesOuterAccountId(..)
    packed.addProperty(JsonConstants.BANK_CHALLENGE_KEY, this.bankChallenge.base64());

    packed.add(JsonConstants.COMMAND_KEY, cmd);
    return sendMessage(packed);
  }


  public ByteString balanceRequest() {
    Balance command = new Balance(this.accountId);
    return packMessage(MessageType.BALANCE, command.toJson());
  }

  public ByteString withdrawRequest(BigDecimal amount) {
    Withdraw command = new Withdraw(this.accountId, amount);
    return packMessage(MessageType.WITHDRAW, command.toJson());
  }

  public ByteString depositRequest(BigDecimal amount) {
    Deposit command = new Deposit(this.accountId, amount);
    return packMessage(MessageType.DEPOSIT, command.toJson());
  }

  public BalanceResult balanceResponse(ByteString message)
      throws ProtocolFailureException, ReplayAttackException {

    JsonObject jsonObject = receiveMessage(message).getAsJsonObject();
    return BalanceResult.fromJson(jsonObject);
  }

  public SuccessResult withdrawResponse(ByteString message)
      throws ProtocolFailureException, ReplayAttackException {

    JsonObject jsonObject = receiveMessage(message).getAsJsonObject();
    return SuccessResult.fromJson(jsonObject);
  }

  public SuccessResult depositResponse(ByteString message)
      throws ProtocolFailureException, ReplayAttackException {

    JsonObject jsonObject = receiveMessage(message).getAsJsonObject();
    return SuccessResult.fromJson(jsonObject);
  }

}
