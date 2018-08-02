package de.upb.bionicbeaver.bank.tx;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory storeage for user details and account information
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccountStore {

    private static final UserAccountStore INSTANCE = new UserAccountStore();

    public static final UserAccountStore getInstance() {
        return INSTANCE;
    }

    private final Map<String, User> userMap = new HashMap<>();

    boolean addUser(User user) {
        if(!userMap.containsKey(user.getAccountName())) {
            userMap.put(user.getAccountName(), user);
            return true;
        }
        return false;
    }

    Optional<User> getUser(String accountName) {
        return Optional.ofNullable(userMap.get(accountName));
    }

    boolean userExistsByCard(String userCard) {
        Optional<String> userAccount = userMap.keySet().stream()
                .filter(userAcc -> userMap.get(userAcc).getCard().equals(userCard))
                //.filter(userAcc -> userMap.get(userAcc).isEnabled())
                .findFirst();
        if(userAccount.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    int getTotalUserCount() {
        return userMap.size();
    }

    /**
     * NOTE: This is only written for testing. Do not use it unless you want to clean your datasets.
     */
    void purge() {
        this.userMap.clear();
    }
}
