package de.upb.bionicbeaver.bank.tx;

import de.upb.bionicbeaver.bank.exception.Error;
import de.upb.bionicbeaver.bank.exception.NoAckRequiredException;
import de.upb.bionicbeaver.bank.exception.ServerException;
import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.Response;
import de.upb.bionicbeaver.bank.model.TransactionType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class ensures fair transaction processing for the user. Ensures account integrity is maintained.
 *
 * @author Siddhartha Moitra
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TxProcessingFunction {

    private static final TxProcessingFunction INSTANCE = new TxProcessingFunction();

    public static final TxProcessingFunction getInstance() {
        return INSTANCE;
    }

    private final AtomicBoolean createdLocked = new AtomicBoolean(false);
    private final AtomicBoolean ackLocked = new AtomicBoolean(false);

    private final UserAccountStore userAccountStore = UserAccountStore.getInstance();
    private final UnprocessedTxStore unprocessedTxStore = UnprocessedTxStore.getInstance();

    public Response process(Request request) {
        switch (request.getRequestType()) {
            case CREATE_ACCOUNT:
                // Try and acquire lock
                if (!tryAcquireLock(createdLocked, 0)) {
                    throw new ServerException(Error.PROTOCOL_ERROR);
                }

                if(userAccountStore.addUser(new User(request.getAccountName(), request.getCard(), request.getAmountAsBigDecimal()))) {
                    unprocessedTxStore.addTx(request);
                    // Release lock
                    lockRelease(createdLocked);
                    return Response.createValidResponse(
                            request.getId(),
                            request.getAccountName(),
                            request.getRequestType(),
                            request.getAmountAsBigDecimal().doubleValue()
                    );
                } else {
                    // Release lock
                    lockRelease(createdLocked);
                    throw new ServerException(Error.DUPLICATE_USER);
                }



            case GET_BALANCE:
            {
                User user = userAccountStore.getUser(request.getAccountName()).get();
                // Try and acquire lock
                if (!tryAcquireLock(user.getLock(), 0)) {
                    throw new ServerException(Error.PROTOCOL_ERROR);
                }

                BigDecimal balance = user.getBalance();
                for (Request unprocessedTx: unprocessedTxStore.getAllUnprocessedTxForUser(request.getAccountName())) {
                    balance = performComputationOnAccountBalance(unprocessedTx.getRequestType(), unprocessedTx.getAmountAsBigDecimal(), balance);
                }

                // Release lock
                lockRelease(user.getLock());
                return Response.createValidResponse(
                        request.getId(),
                        request.getAccountName(),
                        request.getRequestType(),
                        balance.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()
                );
            }
            case DEPOSIT:
            {
                User user = userAccountStore.getUser(request.getAccountName()).get();
                // Try and acquire lock
                if (!tryAcquireLock(user.getLock(), 0)) {
                    throw new ServerException(Error.PROTOCOL_ERROR);
                }
                boolean isTxAdded = unprocessedTxStore.addTx(request);
                BigDecimal balance = user.getBalance();
                for (Request unprocessedTx: unprocessedTxStore.getAllUnprocessedTxForUser(request.getAccountName())) {
                    balance = performComputationOnAccountBalance(unprocessedTx.getRequestType(), unprocessedTx.getAmountAsBigDecimal(), balance);
                }

                // Release lock
                lockRelease(user.getLock());
                if (!isTxAdded) {
                    throw new ServerException(Error.REQUEST_REPLAY);
                }
                return Response.createValidResponse(
                        request.getId(),
                        request.getAccountName(),
                        request.getRequestType(),
                        request.getAmount()
                );
            }
            case WITHDRAW:
            {
                User user = userAccountStore.getUser(request.getAccountName()).get();
                // Try and acquire lock
                if (!tryAcquireLock(user.getLock(), 0)) {
                    throw new ServerException(Error.PROTOCOL_ERROR);
                }

                // Only withdraw if there is sufficient fund
                BigDecimal balance = user.getBalance();
                for (Request unprocessedTx: unprocessedTxStore.getAllUnprocessedTxForUser(request.getAccountName())) {
                    balance = performComputationOnAccountBalance(unprocessedTx.getRequestType(), unprocessedTx.getAmountAsBigDecimal(), balance);
                }
                balance = performComputationOnAccountBalance(request.getRequestType(), request.getAmountAsBigDecimal(), balance);

                boolean isTxAdded;
                if(balance.compareTo(BigDecimal.ZERO) >= 0) {
                    // Tx possible
                    isTxAdded = unprocessedTxStore.addTx(request);
                } else {
                    lockRelease(user.getLock());
                    throw new ServerException(Error.INSUFFICIENT_BALANCE);
                }

                // Release lock
                lockRelease(user.getLock());
                if (!isTxAdded) {
                    throw new ServerException(Error.REQUEST_REPLAY);
                }
                return Response.createValidResponse(
                        request.getId(),
                        request.getAccountName(),
                        request.getRequestType(),
                        request.getAmount()
                );
            }
            case ACK:
            {
                if(tryAcquireLock(ackLocked, 0)) {
                    Optional<Request> unprocessedRequest = unprocessedTxStore.getTx(request.getId());
                    if(!unprocessedRequest.isPresent()) {
                        // Release lock
                        lockRelease(ackLocked);
                        throw new NoAckRequiredException();
                    } else {
                        // Do ack processing
                        User user = userAccountStore.getUser(request.getAccountName()).get();
                        switch (unprocessedRequest.get().getRequestType()) {
                            case CREATE_ACCOUNT:
                                user.enableAccount();
                                unprocessedTxStore.remove(unprocessedRequest.get().getId());
                                break;
                            case DEPOSIT:
                                BigDecimal userBalance = user.getBalance();
                                BigDecimal unprocessed = unprocessedRequest.get().getAmountAsBigDecimal();
                                user.setBalance(userBalance.add(unprocessed));
                                unprocessedTxStore.remove(unprocessedRequest.get().getId());
                                break;
                            case WITHDRAW:
                                BigDecimal balance = user.getBalance();
                                BigDecimal unprocessedAmount = unprocessedRequest.get().getAmountAsBigDecimal();
                                user.setBalance(balance.subtract(unprocessedAmount));
                                unprocessedTxStore.remove(unprocessedRequest.get().getId());
                                break;
                        }
                    }
                }
                // Release lock
                lockRelease(ackLocked);
                return Response.EMPTY_RESPONSE;
            }
            default:
                throw new IllegalArgumentException("Tx type not implemented: " + request.getRequestType().name());

        }
    }

    private BigDecimal performComputationOnAccountBalance(TransactionType transactionType, BigDecimal amount, BigDecimal lastComputedVal) {
        if(transactionType == TransactionType.DEPOSIT) {
            lastComputedVal = lastComputedVal.add(amount);
        } else if(transactionType == TransactionType.WITHDRAW) {
            lastComputedVal = lastComputedVal.subtract(amount);
        }
        return lastComputedVal;
    }

    private boolean tryAcquireLock(AtomicBoolean isLocked, int attempts) {
        if (attempts < 3) {
            boolean lockingSuccess = isLocked.compareAndSet(false, true);
            if(!lockingSuccess) {
                try {
                    Thread.sleep(5);
                    return tryAcquireLock(isLocked, ++attempts);
                } catch (InterruptedException e) {
                    return tryAcquireLock(isLocked, ++attempts);
                }
            } else  {
                return true;
            }
        } else {
            return false;
        }
    }

    private void lockRelease(AtomicBoolean lock) {
        lock.compareAndSet(true, false);
    }
}
