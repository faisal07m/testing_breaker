package de.upb.bionicbeaver.bank.tx;

import de.upb.bionicbeaver.bank.exception.Error;
import de.upb.bionicbeaver.bank.exception.ServerException;
import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.TransactionType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Validates if the request has not been processed earlier. Mitigates replay attack vector.
 *
 * @author Siddhartha Moitra
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReplayAttackValidationFunction {

    private static final ReplayAttackValidationFunction INSTANCE = new ReplayAttackValidationFunction();

    public static final ReplayAttackValidationFunction getInstance() {
        return INSTANCE;
    }

    private final UnprocessedTxStore unprocessedTxStore = UnprocessedTxStore.getInstance();
    private final ProcessedTxStore processedTxStore = ProcessedTxStore.getInstance();

    private final AtomicBoolean lock = new AtomicBoolean(false);

    public void validate(Request request) {
        // If the request is ACK,
        if(request.getRequestType() == TransactionType.ACK) {
            return;
        }
        // If the request is not ACK
        if(tryAcquireLock(lock, 0)) {
            // Check
            Stream.concat(
                    unprocessedTxStore.getAllUnprocessedTxForUser(request.getAccountName()).stream().map(Request::getId),
                    processedTxStore.getAllProcessedTx().stream())

                    .filter(requestId -> request.getId().equals(requestId))
                    .findFirst()
                    .ifPresent(alreadyExistingID -> {
                        // Lock released
                        lockRelease(lock);
                        throw new ServerException(Error.REQUEST_REPLAY);
                    });

            // Lock released
            lockRelease(lock);
        }
    }

    private boolean tryAcquireLock(AtomicBoolean isLocked, int attempts) {
        if (attempts < 3) {

            boolean lockSuccess = isLocked.compareAndSet(false, true);
            if(!lockSuccess) {
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
