package de.upb.bionicbeaver.bank;

import de.upb.bionicbeaver.bank.encryption.ClientBasedSecretKey;
import de.upb.bionicbeaver.bank.encryption.EncryptionManager;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Creates {@link de.upb.bionicbeaver.bank.encryption.ClientBasedSecretKey}, initializes {@link EncryptionManager}
 * and writes it to the desired location.
 *
 * @author Siddhartha Moitra
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SecurityManager implements Runnable {

    private final String authFileName;
    private String authFileLocation;

    @Override
    public void run() {
        ClientBasedSecretKey clientBasedSecretKey = new ClientBasedSecretKey();
        EncryptionManager.init(clientBasedSecretKey);

        if(StringUtils.isBlank(this.authFileLocation)) {
            this.authFileLocation = System.getProperty("user.dir");
        }
        if(!this.authFileLocation.endsWith(File.separator)) {
            this.authFileLocation = this.authFileLocation + File.separator;
        }

        String file = this.authFileLocation + authFileName;
        File fileObject = new File(file);
        if(fileObject.exists()) {

            Try.run(() -> {
                if(!fileObject.delete()) {
                    System.exit(255);
                }
            }).onFailure(failure -> System.exit(255));
        }

        FileChannel rwChannel = Try.of(() -> new RandomAccessFile(file, "rw").getChannel())
                .onFailure(failure -> {
                    System.exit(255);
                })
                .get();
        Try.run(() -> {
            byte[] content = clientBasedSecretKey.getCombinedKey().getBytes();
            ByteBuffer wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, content.length);
            wrBuf.put(content);
            System.out.println("created");

        }).andFinally(() -> Try.run(() -> rwChannel.close()));
    }
}
