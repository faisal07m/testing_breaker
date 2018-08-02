package de.upb.bibifi.verybest.communication.impl;

import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.communication.interfaces.impl.EncryptionSigningHandlerImpl;
import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.CryptoInitException;
import de.upb.bibifi.verybest.common.exception.OperationFailedException;
import de.upb.bibifi.verybest.common.messages.Operation;
import de.upb.bibifi.verybest.common.models.*;
import de.upb.bibifi.verybest.communication.interfaces.IATMSocketHandler;
import de.upb.bibifi.verybest.communication.interfaces.IATMProtocolHandler;
import de.upb.bibifi.verybest.communication.interfaces.ICommunicationHandler;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;

public class ATMCommunicationHandler implements ICommunicationHandler {

    @SuppressWarnings("NullAway")
    private IATMProtocolHandler protocolHandler;

    @Override
    public void initialize(File authenticationFile, Inet4Address bankHost, int port) throws OperationFailedException, CommunicationFailedException {
        IEncryptionSigningHandler encryptionSigningHandler;
        try {
            encryptionSigningHandler = new EncryptionSigningHandlerImpl(authenticationFile);

            IATMSocketHandler httpHandler = new ATMSocketHandler();
            httpHandler.init(bankHost, port);
            protocolHandler = new ATMProtocolHandler(encryptionSigningHandler, httpHandler);
        } catch (CryptoInitException e) {
            //Since in this case, the crypto infrastructure is not available, thus a not recoverable error
            throw new OperationFailedException(e);
        } catch (IOException e) {
            throw new CommunicationFailedException(e);
        }
    }

    @Override
    public Account deposit(DepositAction depositAction, File userCard) throws OperationFailedException, CommunicationFailedException {
        return protocolHandler.performBasicAction(depositAction.name(), Operation.DEPOSIT, depositAction, userCard);
    }

    @Override
    public Account withdraw(WithdrawAction withdrawAction, File userCard) throws OperationFailedException, CommunicationFailedException {
        return protocolHandler.performBasicAction(withdrawAction.name(), Operation.WITHDRAW, withdrawAction, userCard);
    }

    @Override
    public Account get(GetBalanceAction getBalanceAction, File userCard) throws OperationFailedException, CommunicationFailedException {
        return protocolHandler.performBasicAction(getBalanceAction.name(), Operation.GET, getBalanceAction, userCard);
    }

    @Override
    public Account createAccount(AccountCreateActionTemplate accountCreateActionTemplate, File emptyCardFile) throws OperationFailedException, CommunicationFailedException {
        return protocolHandler.performAccountCreateAction(accountCreateActionTemplate, emptyCardFile);
    }
}
