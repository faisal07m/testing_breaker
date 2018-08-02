package de.upb.bionicbeaver.bank.tx;

import de.upb.bionicbeaver.bank.exception.ServerException;
import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.TransactionType;
import org.junit.Test;

import java.util.UUID;

/**
 * Tests {@link ReplayAttackValidationFunction}
 *
 * @author Siddhartha Moitra
 */
public class ReplayAttackValidationFunctionTest {

    private final ReplayAttackValidationFunction subject = ReplayAttackValidationFunction.getInstance();
    private final UnprocessedTxStore unprocessedTxStore = UnprocessedTxStore.getInstance();
    private final ProcessedTxStore processedTxStore = ProcessedTxStore.getInstance();

    /**
     * Tests replay request on the one that have been already processed.
     */
    @Test(expected = ServerException.class)
    public void testReplayAttackOnProcessedTx() {
        UUID processedId = UUID.randomUUID();
        processedTxStore.addProcessedTxID(processedId);

        subject.validate(new Request(processedId, "testUser", "12345", TransactionType.DEPOSIT, 10.00));
    }

    /**
     * Tests replay request on the one that have been not been processed.
     */
    @Test(expected = ServerException.class)
    public void testReplayAttackOnUnprocessedTx() {
        UUID unprocessedId = UUID.randomUUID();
        Request unprocessed = new Request(unprocessedId, "testUser", "12345", TransactionType.DEPOSIT, 10.00);
        unprocessedTxStore.addTx(unprocessed);

        Request newRequest = new Request(unprocessedId, "testUser", "12345", TransactionType.WITHDRAW, 10.00);
        subject.validate(newRequest);
    }
}