package de.upb.bibifi.verybest.bank.cli;

import de.upb.bibifi.verybest.common.Constants;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.File;

public class CommandLineReaderImpl implements CommandLineReader {

    @Nullable
    @CommandLine.Option(names = "-s", description = "auth-file")
    File authFile;

    @Nullable
    @CommandLine.Option(names = "-p", description = "port")
    Integer port;

    @Override
    public Pair<CommandLineArgs, Integer> parseArgs(String[] args) {
        try {
            CommandLine.populateCommand(this, args);
        } catch (CommandLine.ParameterException e) {
            System.err.println("Parameter is invalid: " + e.getArgSpec().paramLabel());
            return Pair.of(null, Constants.ERROR_CODE_USER);
        } catch (CommandLine.PicocliException e) {
            System.err.println("Invalid parameters.");
            return Pair.of(null, Constants.ERROR_CODE_USER);
        }

        CommandLineArgs parsedArgs = new CommandLineArgsImpl(port, authFile);
        if (!Verifier.isValid(parsedArgs)) {
            return Pair.of(null, Constants.ERROR_CODE_USER);
        }

        return Pair.of(parsedArgs, 0);
    }

}
