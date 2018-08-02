package de.upb.bibifi2018.kaffeeklatsch.atm;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccount;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.protocol.ChallengeProtocol;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import de.upb.bibifi2018.kaffeeklatsch.protocol.ResultStatus;
import java.math.BigDecimal;
import okio.ByteString;

public class BankProtocolClient extends ChallengeProtocol {
  private final SimpleBox card;
  private final String accountId;
  private CardProtocolClient cardClient;

  public BankProtocolClient(SimpleBox keyPair, SimpleBox card, String accountId) {
    super(keyPair);
    this.card = card;
    this.accountId = accountId;
  }

  public BankProtocolClient(SimpleBox keyPair, String accountId) {
    super(keyPair);
    this.card = null;
    this.accountId = accountId;
  }

  public void receiveHello(ByteString helloMessage) throws ProtocolFailureException {
    super.receiveHello(helloMessage);
  }


  private ByteString sendCardCommand(ByteString cardCommand) {
    JsonObject request = new JsonObject();
    request.addProperty(JsonConstants.CARD_CONTAINER, cardCommand.base64());
    request.addProperty(JsonConstants.ACCOUNT_KEY, this.accountId);
    return sendMessage(request);
  }

  private ByteString receiveCardResponse(ByteString response)
      throws ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    JsonObject res = receiveMessage(response).getAsJsonObject();
    ResultStatus resultStatus = ResultStatus.valueOf(
        res.getAsJsonPrimitive(JsonConstants.RESULT_STATUS_KEY).getAsString()
    );

    switch (resultStatus) {
      case CARD_INVALID:
        throw new InvalidCardException();
      case INVALID_ACCOUNT:
        throw new NoSuchAccountException();
      case SUCCESS:
        String resultStr = res.getAsJsonPrimitive(JsonConstants.RESULT_KEY).getAsString();
        ByteString result = ByteString.decodeBase64(resultStr);
        return result;
      default:
        throw new ProtocolFailureException();
    }
  }

  public ByteString sendCreate(BigDecimal amount) {
    CreateAccount createAccount = new CreateAccount(this.accountId, amount);
    JsonObject request = new JsonObject();
    request.add(JsonConstants.CREATE_KEY, createAccount.toJson());
    return sendMessage(request);
  }

  public CreateAccountResult receiveCreateResult(ByteString message)
      throws ProtocolFailureException, AccountAlreadyExistsException, ReplayAttackException {

    JsonObject res = receiveMessage(message).getAsJsonObject();
    ResultStatus resultStatus = ResultStatus.valueOf(
        res.getAsJsonPrimitive(JsonConstants.RESULT_STATUS_KEY).getAsString()
    );

    switch (resultStatus) {
      case ACCOUNT_ALREADY_EXISTS:
        throw new AccountAlreadyExistsException();
      case SUCCESS:
        CreateAccountResult accountResult = CreateAccountResult.fromJson(
            res.get(JsonConstants.RESULT_KEY).getAsJsonObject()
        );
        return accountResult;
      default:
        throw new ProtocolFailureException();
    }
  }

  public ByteString sendWithdraw(BigDecimal amount) {
    CardProtocolClient client = getCardClient();
    ByteString cardRequest = client.withdrawRequest(amount);
    return sendCardCommand(cardRequest);
  }

  public ByteString sendDeposit(BigDecimal amount) {
    CardProtocolClient client = getCardClient();
    ByteString cardRequest = client.depositRequest(amount);
    return sendCardCommand(cardRequest);
  }

  public ByteString sendBalance() {
    CardProtocolClient client = getCardClient();
    ByteString cardRequest = client.balanceRequest();
    return sendCardCommand(cardRequest);
  }

  public BalanceResult receiveBalanceResult(ByteString response)
      throws ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    CardProtocolClient cardClient = getCardClient();
    return cardClient.balanceResponse(receiveCardResponse(response));
  }

  public SuccessResult receiveWithdrawResult(ByteString response)
      throws ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    CardProtocolClient cardClient = getCardClient();
    return cardClient.withdrawResponse(receiveCardResponse(response));
  }

  public SuccessResult receiveDepositResult(ByteString response)
      throws ProtocolFailureException, InvalidCardException,
      NoSuchAccountException, ReplayAttackException {

    CardProtocolClient cardClient = getCardClient();
    return cardClient.depositResponse(receiveCardResponse(response));
  }

  private final CardProtocolClient getCardClient() {
    if (this.card == null) {
      throw new IllegalStateException("need card");
    }
    if (this.cardClient == null) {
      this.cardClient = new CardProtocolClient(
          this.card, this.accountId, this.getRemoteChallenge()
      );
    }
    return this.cardClient;
  }
}
