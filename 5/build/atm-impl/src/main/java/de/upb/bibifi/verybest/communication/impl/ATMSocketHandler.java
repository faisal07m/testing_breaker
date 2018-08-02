package de.upb.bibifi.verybest.communication.impl;

import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.communication.interfaces.IATMSocketHandler;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;

public class ATMSocketHandler implements IATMSocketHandler {

    @SuppressWarnings("NullAway")
    private Socket s;

    @SuppressWarnings("NullAway")
    private BufferedSource source;

    @SuppressWarnings("NullAway")
    private BufferedSink sink;

    private final static int MAXIMUM_READ_LENGTH = 10485760; // 10mb
    private final static int CHUNK_SIZE_BYTES = 8192;

    @Override
    public void init(Inet4Address bankHost, int bankPort) throws IOException {
        s = new Socket(bankHost.getHostAddress(), bankPort);
        s.setSoTimeout(Constants.TIMEOUT_MS);
        source = Okio.buffer(Okio.source(s));
        sink = Okio.buffer(Okio.sink(s));
    }

    @Override
    public byte[] sendInitRequest() throws IOException {
        try {
            int length = source.readInt();
            return getDataFromResponse(source, length);
        } catch (IOException e) {
            try {
                s.close();
            } catch (IOException ignored) {
                // We don't care about this
                ignored.printStackTrace();
            }
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public byte[] sendActionRequest(byte[] header, byte[] bodySignature, byte[] body) throws IOException {
        try {
            sink.writeInt(header.length);
            sink.writeInt(bodySignature.length);
            sink.writeInt(body.length);
            sink.write(header);
            sink.write(bodySignature);
            sink.write(body);
            sink.flush();

            int length = source.readInt();
            return getDataFromResponse(source, length);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                source.close();
                sink.close();
                s.close();
            } catch (IOException e) {
                // We don't care if this fails
                e.printStackTrace();
            }
        }
    }

    private byte[] getDataFromResponse(BufferedSource source, int length) throws IOException {
        if (length <= 0) {
            return new byte[0];
        }
        if (length > MAXIMUM_READ_LENGTH) {
            throw new IOException("Maximum read length exceeded! " + length);
        }

        Buffer buf = new Buffer();
        while (!source.exhausted() && buf.size() < length) {
            long transferred = source.read(buf, Math.min(CHUNK_SIZE_BYTES, length - buf.size()));
            if (transferred < CHUNK_SIZE_BYTES) {
                break;
            }
        }

        return buf.readByteArray();
    }
}
