package de.upb.bibifi.verybest.bank.worker;

import de.upb.bibifi.verybest.common.models.Action;
import io.reactivex.Single;

import java.util.Optional;

public interface Worker {
    /**
     * This Single will always yield a WorkerResult.
     */
    Single<WorkerResult> enqueue(Action action);

    /**
     * The Optional is filled iff the account exists.
     */
    Single<Optional<byte[]>> publicKeyForAccountName(String accountName);
}

