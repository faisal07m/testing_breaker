package de.upb.bibifi2018.kaffeeklatsch.bank;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccount;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.BankingFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.protocol.ChallengeProtocol;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import de.upb.bibifi2018.kaffeeklatsch.protocol.ResultStatus;
import okio.ByteString;

public class BankProtocolServer extends ChallengeProtocol {
  private final ByteString bankPrivateKey;
  private final BankReaktor bankReaktor;

  public BankProtocolServer(
      SimpleBox bankKeyPair, ByteString bankPrivateKey, BankReaktor bankReaktor
  ) {

    super(bankKeyPair);
    this.bankPrivateKey = bankPrivateKey;
    this.bankReaktor = bankReaktor;
  }


  public ByteString sendChallenge() {
    return super.sendHello();
  }

  public ByteString processRequest(ByteString message)
      throws ProtocolFailureException, ReplayAttackException {

    JsonObject jsonElement = super.receiveMessage(message).getAsJsonObject();

    if (jsonElement.has(JsonConstants.CARD_CONTAINER)) {
      String accountId = jsonElement.getAsJsonPrimitive(JsonConstants.ACCOUNT_KEY).getAsString();

      ByteString cardMessage = ByteString.decodeBase64(
          jsonElement.getAsJsonPrimitive(JsonConstants.CARD_CONTAINER).getAsString()
      );
      JsonObject response = handleCardRequest(cardMessage, accountId);
      return sendMessage(response);
    } else if (jsonElement.has(JsonConstants.CREATE_KEY)) {
      JsonObject createCommand = jsonElement.getAsJsonObject(JsonConstants.CREATE_KEY);
      JsonObject response = handleCreate(createCommand);
      return sendMessage(response);
    } else {
      throw new ProtocolFailureException("invalid atm request");
    }
  }

  private JsonObject handleCreate(JsonObject createJson) {
    CreateAccount createAccount = CreateAccount.fromJson(createJson);

    JsonObject response = new JsonObject();

    try {
      CreateAccountResult account = this.bankReaktor.createAccount(createAccount);
      response.addProperty(JsonConstants.RESULT_STATUS_KEY, ResultStatus.SUCCESS.name());
      response.add(JsonConstants.RESULT_KEY, account.toJson());
    } catch (AccountAlreadyExistsException e) {
      response.addProperty(
          JsonConstants.RESULT_STATUS_KEY,
          ResultStatus.ACCOUNT_ALREADY_EXISTS.name()
      );
    }

    return response;
  }

  private JsonObject handleCardRequest(ByteString cardMessage, String accountId)
      throws ProtocolFailureException {

    JsonObject response = new JsonObject();
    try {
      CardProtocolServer cardProtocolServer = new CardProtocolServer(
          this.bankPrivateKey, accountId, bankReaktor
      );


      ByteString cardResponse = cardProtocolServer.answerBankRequest(
          cardMessage, this.getLocalChallenge()
      );

      response.addProperty(JsonConstants.RESULT_STATUS_KEY, ResultStatus.SUCCESS.name());
      response.addProperty(JsonConstants.RESULT_KEY, cardResponse.base64());
    } catch (BankingFailureException e) {
      response.addProperty(JsonConstants.RESULT_STATUS_KEY, ResultStatus.CARD_INVALID.name());
    } catch (NoSuchAccountException e) {
      response.addProperty(JsonConstants.RESULT_STATUS_KEY, ResultStatus.INVALID_ACCOUNT.name());
    } catch (ReplayAttackException e) {
      throw new ProtocolFailureException(e);
    }

    return response;
  }
}
