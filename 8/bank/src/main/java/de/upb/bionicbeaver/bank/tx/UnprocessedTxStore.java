package de.upb.bionicbeaver.bank.tx;

import de.upb.bionicbeaver.bank.model.Request;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * In-memory store for user's unprocessed transactions.
 *
 * @author Siddhartha Moitra
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UnprocessedTxStore {

    private static final UnprocessedTxStore INSTANCE = new UnprocessedTxStore();

    public static final UnprocessedTxStore getInstance() {
        return INSTANCE;
    }

    private final Map<UUID, Request> unprocessedTxMap = new HashMap<>();

    boolean addTx(Request request) {
        if(!unprocessedTxMap.containsKey(request.getId())) {
            unprocessedTxMap.put(request.getId(), request);
            return true;
        }
        return false;
    }

    Optional<Request> getTx(UUID requestId) {
        return Optional.ofNullable(this.unprocessedTxMap.get(requestId));
    }

    List<Request> getAllUnprocessedTxForUser(String accountName) {
        if(unprocessedTxMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Request> requests = unprocessedTxMap.values().stream()
                .filter(Objects::nonNull)
                .filter(request -> request.getAccountName().equals(accountName))
                .collect(Collectors.toList());
        if(Objects.nonNull(requests)) {
            return Collections.unmodifiableList(requests);
        }

        return Collections.emptyList();
    }

    Request remove(UUID requestId) {
        Request removedReq = this.unprocessedTxMap.get(requestId);
        this.unprocessedTxMap.remove(requestId);
        return removedReq;
    }

    /**
     * NOTE: This is only written for testing. Do not use it unless you want to clean your datasets.
     */
    void purge() {
        this.unprocessedTxMap.clear();
    }
}
