package de.upb.bibifi.verybest.atm.cli;

import org.apache.commons.lang3.tuple.Pair;

public interface CommandLineReader {
    Pair<CommandLineArgs, Integer> parseArgs(String[] args);
}
