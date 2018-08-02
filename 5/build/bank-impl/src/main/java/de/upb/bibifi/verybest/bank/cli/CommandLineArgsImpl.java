package de.upb.bibifi.verybest.bank.cli;

import javax.annotation.Nullable;
import java.io.File;

public class CommandLineArgsImpl implements CommandLineArgs {

    private final int port;
    private final File authFile;

    CommandLineArgsImpl(@Nullable Integer port, @Nullable File authFile) {
        if (port != null) {
            this.port = port;
        } else {
            this.port = 3000;
        }

        if (authFile == null) {
            this.authFile = new File("bank.auth");
        } else {
            this.authFile = authFile;
        }
    }

    @Override
    public File authFile() {
        return authFile;
    }

    @Override
    public int port() {
        return port;
    }


}
