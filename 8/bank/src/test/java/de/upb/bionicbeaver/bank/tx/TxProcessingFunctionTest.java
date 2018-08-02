package de.upb.bionicbeaver.bank.tx;

import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.Response;
import de.upb.bionicbeaver.bank.model.TransactionType;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link TxProcessingFunction} with concurrent requests.
 * The test assumes that requests are validated based on their values and patterns because request value validation
 * happens before {@link TxProcessingFunction} call.
 *
 * @author Siddhartha Moitra
 */
public class TxProcessingFunctionTest {

    private final TxProcessingFunction subject = TxProcessingFunction.getInstance();
    private final UserAccountStore userAccountStore = UserAccountStore.getInstance();
    private final UnprocessedTxStore unprocessedTxStore = UnprocessedTxStore.getInstance();

    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    @Before
    public void setup() {
        userAccountStore.purge();
        unprocessedTxStore.purge();
    }

    /**
     * Tests create user with initial balance of 10.00.
     * Expected result: user is successfully created with disabled account.
     */
    @Test
    public void testCreateUser_SingleConcurrency() {
        UUID requestId = UUID.randomUUID();
        Request request = new Request(requestId, "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        Response response = subject.process(request);

        // Assertions
        assertNotNull(response);
        assertEquals("testUser", response.getAccountName());
        assertEquals(requestId, request.getId());
        assertEquals(TransactionType.CREATE_ACCOUNT, response.getResponseType());

        assertTrue(userAccountStore.getUser("testUser").isPresent());
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);
        User insertedUser = userAccountStore.getUser("testUser").get();

        assertNotNull(insertedUser);
        assertEquals("testUser", insertedUser.getAccountName());
        assertEquals("12345", insertedUser.getCard());
        assertEquals(TransactionType.CREATE_ACCOUNT, response.getResponseType());
        assertFalse(insertedUser.isEnabled());
    }

    /**
     * Tests create user with initial balance of 10.00. Once the user is created , an ACK is also sent.
     * Expected result: user is successfully created with enabled account.
     */
    @Test
    public void testCreateUser_SingleConcurrency_WithACK() {
        UUID requestId = UUID.randomUUID();
        Request request = new Request(requestId, "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);

        User insertedUser = userAccountStore.getUser("testUser").get();
        assertFalse(insertedUser.isEnabled());

        // Now ACK
        Request ack = new Request(requestId, "testUser", "12345", TransactionType.ACK, null);
        Response ackResponse = subject.process(ack);
        assertTrue(ackResponse == Response.EMPTY_RESPONSE);
        assertTrue(insertedUser.isEnabled());
    }

    /**
     * Tests create same user concurrently.
     * Expected result: user is successfully created ONLY ONCE with disabled account.
     */
    @Test
    public void testCreateUser_MultipleConcurrency() throws InterruptedException {

        final int NUM_TASKS = 10;
        List<Callable<Response>> callables = IntStream.range(0, NUM_TASKS)
                .mapToObj(i -> new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00))
                .map(request -> (Callable<Response>) () -> subject.process(request))
                .collect(Collectors.toList());

        List<Response> successfulResponses = new ArrayList<>();
        List<ExecutionException> failures = new ArrayList<>();
        this.executorService.invokeAll(callables)
                .stream()
                .forEach(responseFuture ->
                        Try.of(() -> responseFuture.get())
                                .andThen(successfulResponses::add)
                                .onFailure(failure -> {
                                    if(failure instanceof ExecutionException) {
                                        failures.add((ExecutionException) failure);
                                    }
                                })
                );

        // Assertions
        assertTrue(successfulResponses.size() == 1);
        assertTrue(failures.size() == 9);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);
        assertTrue(userAccountStore.getUser("testUser").isPresent());
        assertFalse(userAccountStore.getUser("testUser").get().isEnabled());
    }

    /**
     * Tests create same user concurrently with same request ID.
     * Expected result: user is successfully created ONLY ONCE with disabled account.
     */
    @Test
    public void testCreateUserSameRequestID_MultipleConcurrency() throws InterruptedException {
        UUID requestId = UUID.randomUUID();
        final int NUM_TASKS = 10;
        List<Callable<Response>> callables = IntStream.range(0, NUM_TASKS)
                .mapToObj(i -> new Request(requestId, "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00))
                .map(request -> (Callable<Response>) () -> subject.process(request))
                .collect(Collectors.toList());

        List<Response> successfulResponses = new ArrayList<>();
        List<ExecutionException> failures = new ArrayList<>();
        this.executorService.invokeAll(callables)
                .stream()
                .forEach(responseFuture ->
                        Try.of(() -> responseFuture.get())
                                .andThen(successfulResponses::add)
                                .onFailure(failure -> {
                                    if(failure instanceof ExecutionException) {
                                        failures.add((ExecutionException) failure);
                                    }
                                })
                );

        // Assertions
        assertTrue(successfulResponses.size() == 1);
        assertTrue(failures.size() == 9);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);
        assertTrue(userAccountStore.getUser("testUser").isPresent());
        assertFalse(userAccountStore.getUser("testUser").get().isEnabled());
    }

    /**
     * Tests a single deposit to the user account. Because the operation depends on the ACK from client before changing
     * the user account balance, the account will still show old balance. But a tx entry will be made in unprocessed
     * store.
     */
    @Test
    public void testDepositIntoAccount_SingleConcurrency() {
        // Create a user
        Request request = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);

        // Make a deposit
        Request depositRequest = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.DEPOSIT, 10.00);
        subject.process(depositRequest);

        // Assert
        assertTrue(userAccountStore.getUser("testUser").get().getBalance().equals(BigDecimal.valueOf(10.00).setScale(2, BigDecimal.ROUND_HALF_UP)));
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 2);
    }


    @Test
    public void testDepositIntoAccount_WithAck() {
        // Create a user
        Request request = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);
        subject.process(new Request(request.getId(), "testUser", "12345", TransactionType.ACK, null));
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 0);

        // Make a deposit
        Request depositRequest = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.DEPOSIT, 10.00);
        subject.process(depositRequest);
        subject.process(new Request(depositRequest.getId(), "testUser", "12345", TransactionType.ACK, null));

        // Assert
        assertTrue(userAccountStore.getUser("testUser").get().getBalance().equals(BigDecimal.valueOf(20.00).setScale(2, BigDecimal.ROUND_HALF_UP)));
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 0);
    }

    /**
     * Similar to the test above but now with multiple threads depositing.
     *
     */
    @Test
    public void testDepositIntoAccount_MultipleConcurrency() throws InterruptedException {
        // Create a user
        Request request = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);

        // Make a deposit
        final int NUM_TASKS = 10;
        List<Callable<Response>> callables = IntStream.range(0, NUM_TASKS)
                .mapToObj(i -> new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.DEPOSIT, 10.00))
                .map(req -> (Callable<Response>) () -> subject.process(req))
                .collect(Collectors.toList());

        List<Response> successfulResponses = new ArrayList<>();
        List<ExecutionException> failures = new ArrayList<>();
        this.executorService.invokeAll(callables)
                .stream()
                .forEach(responseFuture ->
                        Try.of(() -> responseFuture.get())
                                .andThen(successfulResponses::add)
                                .onFailure(failure -> {
                                    if(failure instanceof ExecutionException) {
                                        failures.add((ExecutionException) failure);
                                    }
                                })
                );

        // Assertions
        assertTrue(userAccountStore.getUser("testUser").get().getBalance().equals(BigDecimal.valueOf(10.00).setScale(2, BigDecimal.ROUND_HALF_UP)));
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 11);
        assertTrue(successfulResponses.size() == 10);
        assertTrue(failures.size() == 0);
    }

    /**
     * Tries to withdraw money within limits.
     */
    @Test
    public void testWithdraw_SingleConcurrency() {
        // Create a user
        Request request = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);

        // Make a deposit
        Request depositRequest = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.WITHDRAW, 1.00);
        subject.process(depositRequest);

        // Assert
        assertTrue(userAccountStore.getUser("testUser").get().getBalance().equals(BigDecimal.valueOf(10.00).setScale(2, BigDecimal.ROUND_HALF_UP)));
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 2);
    }

    @Test
    public void testWithdraw_WithAck() {
        // Create a user
        Request request = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);
        subject.process(new Request(request.getId(), "testUser", "12345", TransactionType.ACK, null));
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 0);

        // Make a deposit
        Request depositRequest = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.WITHDRAW, 10.00);
        subject.process(depositRequest);
        subject.process(new Request(depositRequest.getId(), "testUser", "12345", TransactionType.ACK, null));

        // Assert
        assertTrue(userAccountStore.getUser("testUser").get().getBalance().equals(BigDecimal.valueOf(0.00).setScale(2, BigDecimal.ROUND_HALF_UP)));
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 0);
    }

    /**
     * One transaction here tries to withdraw money when it is not sufficient.
     *
     * @throws InterruptedException
     */
    @Test
    public void testWithdraw_MultipleConcurrency() throws InterruptedException {
        // Create a user
        Request request = new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.CREATE_ACCOUNT, 10.00);
        subject.process(request);
        assertTrue(userAccountStore.getTotalUserCount() == 1);
        assertTrue(unprocessedTxStore.getAllUnprocessedTxForUser("testUser").size() == 1);

        // Make a deposit
        final int NUM_TASKS = 11;
        List<Callable<Response>> callables = IntStream.range(0, NUM_TASKS)
                .mapToObj(i -> new Request(UUID.randomUUID(), "testUser", "12345", TransactionType.WITHDRAW, 1.00))
                .map(req -> (Callable<Response>) () -> subject.process(req))
                .collect(Collectors.toList());

        List<Response> successfulResponses = new ArrayList<>();
        List<ExecutionException> failures = new ArrayList<>();
        this.executorService.invokeAll(callables)
                .stream()
                .forEach(responseFuture ->
                        Try.of(() -> responseFuture.get())
                                .andThen(successfulResponses::add)
                                .onFailure(failure -> {
                                    if(failure instanceof ExecutionException) {
                                        failures.add((ExecutionException) failure);
                                    }
                                })
                );

        assertTrue(successfulResponses.size() == 10);
        assertTrue(failures.size() == 1);
        //assertEquals("Someone attacked us with replay.", failures.get(0).getCause().getMessage());
    }
}