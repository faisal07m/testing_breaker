package de.upb.bionicbeaver.bank.tx;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This class stores all the process tx IDs (request IDs) such that no replay attack could be made
 * for any processed transactions.
 *
 * @author Siddhartha Moitra
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessedTxStore {
    private static final ProcessedTxStore INSTANCE = new ProcessedTxStore();

    public static final ProcessedTxStore getInstance() {
        return INSTANCE;
    }


    private final List<UUID> processedTxList = new ArrayList<>();

    void addProcessedTxID(UUID id) {
        if(Objects.nonNull(id)) {
            this.processedTxList.add(id);
        }
    }

    List<UUID> getAllProcessedTx() {
        return Collections.unmodifiableList(this.processedTxList);
    }
}
