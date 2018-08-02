package de.upb.bibifi.verybest.common.communication.interfaces;

import java.io.File;
import java.io.IOException;

public interface IAuthFileGenerator {

    /**
     * Generates an auth file for an empty file and stores it into the provided file
     *
     * @param emptyFile the file to store the key
     *
     * @throws IOException in case that the file does not exists or is not empty
     */
    void generateAuthFile(File emptyFile) throws IOException;
}
