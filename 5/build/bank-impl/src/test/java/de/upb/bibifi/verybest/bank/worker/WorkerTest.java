package de.upb.bibifi.verybest.bank.worker;

import de.upb.bibifi.verybest.common.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WorkerTest {

    private WorkerResult createAccount(Worker worker) {
        Action accountCreate = new AccountCreateAction(new byte[0], "test", BigInteger.valueOf(1000));
        return worker.enqueue(accountCreate).blockingGet();
    }

    @Test
    void testAccountCreate() {
        Worker worker = new WorkerImpl();

        WorkerResult workerResult = createAccount(worker);
        assertEquals(WorkerResult.Kind.Success, workerResult.kind());

        Account account = workerResult.success().newAccountStatus();
        assertEquals(Account.create("test", new byte[0], BigInteger.valueOf(1000)), account);
    }

    @Test
    void testInvalidAccountCreate() {
        Worker worker = new WorkerImpl();

        Action accountCreate = new AccountCreateAction(new byte[0], "test", BigInteger.valueOf(999));
        WorkerResult workerResult = worker.enqueue(accountCreate).blockingGet();
        assertEquals(WorkerResult.Kind.Failure, workerResult.kind());

        WorkerResult.Failure.Reason reason = workerResult.failure().reason();
        assertEquals(WorkerResult.Failure.Reason.CreateBalanceTooLow, reason);
    }

    @Test
    void testDeposit() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Action deposit = new DepositAction("test", BigInteger.ONE);
        WorkerResult workerResult = worker.enqueue(deposit).blockingGet();
        assertEquals(WorkerResult.Kind.Success, workerResult.kind());

        Account account = workerResult.success().newAccountStatus();
        assertEquals(Account.create("test", new byte[0], BigInteger.valueOf(1001)), account);
    }

    @Test
    void testZeroDeposit() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Action withdraw = new DepositAction("test", BigInteger.ZERO);
        WorkerResult workerResult = worker.enqueue(withdraw).blockingGet();
        assertEquals(WorkerResult.Kind.Failure, workerResult.kind());

        WorkerResult.Failure.Reason reason = workerResult.failure().reason();
        assertEquals(WorkerResult.Failure.Reason.NonPositiveDeposit, reason);
    }

    @Test
    void testWithdraw() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Action withdraw = new WithdrawAction("test", BigInteger.ONE);
        WorkerResult workerResult = worker.enqueue(withdraw).blockingGet();
        assertEquals(WorkerResult.Kind.Success, workerResult.kind());

        Account account = workerResult.success().newAccountStatus();
        assertEquals(Account.create("test", new byte[0], BigInteger.valueOf(999)), account);
    }

    @Test
    void testZeroWithdraw() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Action withdraw = new WithdrawAction("test", BigInteger.ZERO);
        WorkerResult workerResult = worker.enqueue(withdraw).blockingGet();
        assertEquals(WorkerResult.Kind.Failure, workerResult.kind());

        WorkerResult.Failure.Reason reason = workerResult.failure().reason();
        assertEquals(WorkerResult.Failure.Reason.NonPositiveWithdraw, reason);
    }

    @Test
    void testGetBalance() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Action getBalance = new GetBalanceAction("test");
        WorkerResult workerResult = worker.enqueue(getBalance).blockingGet();
        assertEquals(WorkerResult.Kind.Success, workerResult.kind());

        Account account = workerResult.success().newAccountStatus();
        assertEquals(Account.create("test", new byte[0], BigInteger.valueOf(1000)), account);
    }

    @Test
    void testInvalidWithdraw() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Action withdraw = new WithdrawAction("test", BigInteger.valueOf(1001));
        WorkerResult workerResult = worker.enqueue(withdraw).blockingGet();
        assertEquals(WorkerResult.Kind.Failure, workerResult.kind());

        WorkerResult.Failure.Reason reason = workerResult.failure().reason();
        assertEquals(WorkerResult.Failure.Reason.Overdraft, reason);
    }

    @Test
    void testPublicKeyRequest() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Optional<byte[]> optionalPublicKey = worker.publicKeyForAccountName("test").blockingGet();
        assertTrue(optionalPublicKey.isPresent());

        byte[] publicKey = optionalPublicKey.get();
        assertArrayEquals(new byte[0], publicKey);
    }

    @Test
    void testInvalidPublicKeyRequest() {
        Worker worker = new WorkerImpl();

        createAccount(worker);
        Optional<byte[]> optionalPublicKey = worker.publicKeyForAccountName("does not exist").blockingGet();
        assertFalse(optionalPublicKey.isPresent());
    }
}
