package de.upb.bibifi.verybest.atm.cli;

import de.upb.bibifi.verybest.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.math.BigDecimal;

import static de.upb.bibifi.verybest.common.util.InputValidationUtil.*;

public class CommandLineReaderImpl implements CommandLineReader {

    @SuppressWarnings("NullAway")
    @Option(names = "-a", description = "account", required = true)
    String account;

    @SuppressWarnings("NullAway")
    @Option(names = "-s", description = "auth-file")
    File authFile;

    @SuppressWarnings("NullAway")
    @Option(names = "-i", description = "ip-address")
    String ipAddress;

    @SuppressWarnings("NullAway")
    @Option(names = "-p", description = "port")
    String portStr;

    @SuppressWarnings("NullAway")
    @Option(names = "-c", description = "card-file")
    File cardFile;

    @SuppressWarnings("NullAway")
    @Option(names = "-n", description = "new-account-action")
    String initialBalanceStr;

    @SuppressWarnings("NullAway")
    @Option(names = "-d", description = "deposit-action")
    String depositAmountStr;

    @SuppressWarnings("NullAway")
    @Option(names = "-w", description = "withdraw-action")
    String withdrawAmountStr;

    @SuppressWarnings("NullAway")
    @Option(names = "-g", description = "get-account-balance-action")
    boolean getAccountBalance;

    @Override
    public Pair<CommandLineArgs, Integer> parseArgs(String[] args) {

        for (String arg : args) {
            if (StringUtils.isAllBlank(arg) || arg.length() > MAX_ARG_LENGTH) {
                System.err.println("Parameters cannot be empty or too large");
                return Pair.of(null, Constants.ERROR_CODE_USER);
            }
        }

        BigDecimal initialBalance = null;
        BigDecimal depositAmount = null;
        BigDecimal withdrawAmount = null;

        Integer port = null;

        try {
            CommandLine.populateCommand(this, args);

            if (!StringUtils.isAllBlank(initialBalanceStr)) {
                initialBalance = parseStringToDecimal(initialBalanceStr);
            }
            if (!StringUtils.isAllBlank(depositAmountStr)) {
                depositAmount = parseStringToDecimal(depositAmountStr);
            }
            if (!StringUtils.isAllBlank(withdrawAmountStr)) {
                withdrawAmount = parseStringToDecimal(withdrawAmountStr);
            }
            if (!StringUtils.isAllBlank(portStr)) {
                port = parsePort(portStr);
            }
            if (!StringUtils.isAllBlank(ipAddress) && !VALID_IP_FORMAT.asPredicate().test(ipAddress)) {
                throw new IllegalArgumentException(ipAddress);
            }

        } catch (CommandLine.MissingParameterException e) {
            System.err.println("Missing parameter: " + e.getMissing().get(0).paramLabel());
            return Pair.of(null, Constants.ERROR_CODE_USER);
        } catch (CommandLine.ParameterException e) {
            System.err.println("Problem with parameter: " + e.getMessage());
            return Pair.of(null, Constants.ERROR_CODE_USER);
        } catch (IllegalArgumentException e) {
            System.err.println("Input format is invalid: " + e.getMessage());
            return Pair.of(null, Constants.ERROR_CODE_USER);
        }

        CommandLineArgs.Action action = null;

        if (initialBalance != null) {
            action = new CommandLineArgsImpl.NewAccountActionImpl(initialBalance);
        }
        if (getAccountBalance) {
            if (action != null) {
                System.err.println("Invalid parameter: -g");
                return Pair.of(null, Constants.ERROR_CODE_USER);
            }
            action = new CommandLineArgsImpl.GetAccountBalanceActionImpl();
        }
        if (depositAmount != null) {
            if (action != null) {
                System.err.println("Invalid parameter: -d");
                return Pair.of(null, Constants.ERROR_CODE_USER);
            }
            action = new CommandLineArgsImpl.DepositActionImpl(depositAmount);
        }
        if (withdrawAmount != null) {
            if (action != null) {
                System.err.println("Invalid parameter: -w");
                return Pair.of(null, Constants.ERROR_CODE_USER);
            }
            action = new CommandLineArgsImpl.WithdrawActionImpl(withdrawAmount);
        }

        if (action == null) {
            System.err.println("Missing action!");
            return Pair.of(null, Constants.ERROR_CODE_USER);
        }

        CommandLineArgs parsedArgs = new CommandLineArgsImpl(account, port, cardFile, ipAddress, authFile, action);
        if (Verifier.isValid(parsedArgs)) {
            return Pair.of(parsedArgs, 0);
        } else {
            return Pair.of(parsedArgs, Constants.ERROR_CODE_USER);
        }
    }

    private Integer parsePort(String portStr) {
        if (!VALID_PORT_FORMAT.asPredicate().test(portStr)) {
            throw new IllegalArgumentException(portStr);
        }
        return Integer.parseInt(portStr);
    }

    private BigDecimal parseStringToDecimal(String encodedDecimal) {
        if (!VALID_NUMBER_FORMAT.asPredicate().test(encodedDecimal)) {
            throw new IllegalArgumentException(encodedDecimal);
        }
        return new BigDecimal(encodedDecimal);
    }

}