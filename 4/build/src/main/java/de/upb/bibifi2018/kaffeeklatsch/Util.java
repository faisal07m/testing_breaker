package de.upb.bibifi2018.kaffeeklatsch;

import java.io.IOException;
import java.io.InputStream;
import okio.Buffer;
import okio.ByteString;

public class Util {

  public static ByteString readFullyLimited(InputStream input, int readLimit) throws IOException {
    byte[] bytes = new byte[1024];
    Buffer buffer = new Buffer();
    int totalBytes = 0;
    while (totalBytes <= readLimit) {
      int read = input.read(bytes, 0, Math.min(bytes.length, readLimit - totalBytes));
      if (read < 0) {
        break;
      }
      totalBytes += read;
      buffer.write(bytes, 0, read);
    }
    if (totalBytes > readLimit) {
      throw new IOException("file size over limit");
    }
    return buffer.readByteString();
  }
}
