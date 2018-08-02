package de.upb.bionicbeaver.bank.tx;

import de.upb.bionicbeaver.bank.exception.Error;
import de.upb.bionicbeaver.bank.exception.ServerException;
import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.TransactionType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author Siddhartha Moitra
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TxValidationFunction {

    private static final TxValidationFunction INSTANCE = new TxValidationFunction();

    public static final TxValidationFunction getInstance() {
        return INSTANCE;
    }

    private final UserAccountStore userAccountStore = UserAccountStore.getInstance();

    public void validate(Request request) {
        if (Objects.isNull(request)) {
            throw new ServerException(Error.BAD_REQUEST);
        }
        if (request.getRequestType() == TransactionType.ACK) {
            return;
        }
        if (request.getRequestType() != TransactionType.CREATE_ACCOUNT) {
            User user = userAccountStore.getUser(request.getAccountName()).orElseThrow(() -> new ServerException(Error.USER_NOT_PRESENT));
            // Check if user auth is valid
            if (!user.getCard().equals(request.getCard())) {
                throw new ServerException(Error.USER_AUTHENTICATION_FAILED);
            }
        } else {
            if(userAccountStore.getUser(request.getAccountName()).isPresent()) {
                throw new ServerException(Error.DUPLICATE_USER);
            }
            if(userAccountStore.userExistsByCard(request.getCard())) {
                throw new ServerException(Error.DUPLICATE_USER);
            }
            if(StringUtils.isBlank(request.getAccountName())) {
                throw new ServerException(Error.BAD_REQUEST);
            }
        }
    }
}
