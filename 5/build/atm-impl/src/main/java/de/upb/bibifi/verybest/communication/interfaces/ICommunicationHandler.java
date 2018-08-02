package de.upb.bibifi.verybest.communication.interfaces;

import de.upb.bibifi.verybest.common.exception.*;
import de.upb.bibifi.verybest.common.models.*;

import java.io.File;
import java.net.Inet4Address;

public interface ICommunicationHandler {

    /**
     * Initializes the CommunicationHandler, thus reads the necessary keys, and opens the socket in a thread.
     *
     * @param authenticationFile the name of the auth file to read.
     * @param bankHost           the host of the bank
     * @param port               the port to listen to

     */
    void initialize(File authenticationFile, Inet4Address bankHost, int port) throws OperationFailedException, CommunicationFailedException;

    /**
     * Executes the  {@link DepositAction} on the bank.
     *
     * @param depositAction the action to perform
     * @param userCard      the card of the user

     */
    Account deposit(DepositAction depositAction, File userCard) throws OperationFailedException, CommunicationFailedException;


    /**
     * Executes the {@link WithdrawAction} on the bank.
     *
     * @param withdrawAction the action to perform
     * @param userCard       the card of the user
     */
    Account withdraw(WithdrawAction withdrawAction, File userCard) throws OperationFailedException, CommunicationFailedException;

    /**
     * Executes the {@link GetBalanceAction} on the bank.
     *
     * @param getBalanceAction the action to perform
     * @param userCard         the card of the user
     * @return the action that was eventually performed (i.e. the return value of the server that should be printed to std. out)
     */
    Account get(GetBalanceAction getBalanceAction, File userCard) throws OperationFailedException, CommunicationFailedException;

    /**
     * Creates the account on the bank
     *
     * @param accountCreateActionTemplate the action to perform
     * @return the Account that was created

     */
    Account createAccount(AccountCreateActionTemplate accountCreateActionTemplate, File emptyCardFile) throws OperationFailedException, CommunicationFailedException;
}

