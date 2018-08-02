package de.upb.bibifi.verybest.communication.interfaces;

import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.OperationFailedException;
import de.upb.bibifi.verybest.common.messages.Operation;
import de.upb.bibifi.verybest.common.models.Account;
import de.upb.bibifi.verybest.common.models.AccountCreateActionTemplate;
import de.upb.bibifi.verybest.common.models.Action;

import java.io.File;

/**
 * Performs the protocol on the socket
 */
public interface IATMProtocolHandler {

    Account performBasicAction(String userName, Operation operation, Action action, File userCard) throws OperationFailedException, CommunicationFailedException;

    Account performAccountCreateAction(AccountCreateActionTemplate toPerform, File emptyCardFile) throws OperationFailedException, CommunicationFailedException;

}
