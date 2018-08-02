package de.upb.cs.bibifi.commons.data;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class AuthFile {
    private final String key;
    private final String salt;

    private AuthFile(String key, String salt) {
        this.key = key;
        this.salt = salt;
    }

    public String getKey() {
        return key;
    }

    public String getSalt() {
        return salt;
    }

    public static AuthFile getAuthFile(String authFilePath) {
        Gson gson = new Gson();
        String string;
        AuthFile auth = null;
        try {
            string = FileUtils.readFileToString(new File(authFilePath), "UTF-8");
            auth = gson.fromJson(string, AuthFile.class);
        } catch (IOException e) {
            System.exit(255);
        }
        return auth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;

        AuthFile that = (AuthFile) obj;
        return this.key.equals(that.key) && this.salt.equals(that.salt);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + key.hashCode();
        result = 31 * result + salt.hashCode();
        return result;
    }
}
