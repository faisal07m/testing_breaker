package de.upb.cs.bibifi.bankapp.bank;

import java.io.InputStream;

public interface IAuthFileContentGenerator {
    InputStream generateAuthFileContent() throws Exception;
}
