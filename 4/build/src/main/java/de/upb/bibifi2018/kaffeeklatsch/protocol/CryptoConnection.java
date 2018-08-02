package de.upb.bibifi2018.kaffeeklatsch.protocol;

import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import okio.Sink;
import okio.Source;

/**
 * This class implements an encrypted channel using NaCl crypto primitives.
 * <br><br>
 *
 * While one should not create ones own cryptography, it is a bit simpler since we are in
 * a pre-shared secret situation.
 * <br><br>
 *
 * This class will take a socket, set appropriate read/write timeouts and authenticate
 * messages and their length using the pre-shared secret, a keypair.
 * <br><br>
 *
 * To avoid reading unbounded attacker messages, first an unauthenticated length field is read,
 * which contains the length of the authenticated length message following it.
 * This is done since our actual payload messages may be unbounded and therefore the size of the
 * payload message cannot be statically limited to avoid denial of service.
 * Rather we authenticate the length of the payload using a separate message,
 * which is limited in size, containing the real length of the payload which we can trust.
 * <pre>
 *   [4byte length m of authLength>][m-byte authLength p of payload][p-bytes payload]
 * </pre>
 */
public class CryptoConnection implements Closeable {

  private final Socket socket;
  private final SimpleBox keyPair;
  private final BufferedSink sink;
  private final BufferedSource source;

  public CryptoConnection(Socket socket, SimpleBox simpleBox) throws IOException {
    this.socket = socket;
    this.keyPair = simpleBox;
    Sink sink = Okio.sink(socket);
    sink.timeout().timeout(10, TimeUnit.SECONDS);
    this.sink = Okio.buffer(sink);
    Source source = Okio.source(socket);
    source.timeout().timeout(10, TimeUnit.SECONDS);
    this.source = Okio.buffer(source);
  }

  public void sendMessage(ByteString message) throws IOException {

    byte[] plainSizeBytes = ByteBuffer.allocate(4).putInt(message.size()).array();
    ByteString cipherSize = this.keyPair.seal(ByteString.of(plainSizeBytes));
    sink.writeInt(cipherSize.size());
    sink.write(cipherSize);
    sink.write(message);
    sink.flush();
  }

  public ByteString readMessage() throws IOException, ProtocolFailureException {

    source.timeout().deadline(10, TimeUnit.SECONDS);
    int headerSize = source.readInt();
    if (headerSize > 1024 * 1024 || headerSize < 1) {
      throw new IOException("invalid message header length");
    }
    ByteString cipherHeader = source.readByteString(headerSize);
    ByteString restoredHeader = this.keyPair.open(cipherHeader)
        .orElseThrow(() -> new ProtocolFailureException("message length invalid"));


    int authenticatedMessageLength = restoredHeader.asByteBuffer().getInt();

    ByteString message = source.readByteString(authenticatedMessageLength);
    return message;
  }


  @Override
  public void close() {
    try {
      this.socket.close();
    } catch (IOException e) {
      // ignore, this is called after errors are handled
    }
  }
}
