package de.upb.bibifi.verybest;

import de.upb.bibifi.verybest.common.communication.BankAPI;
import de.upb.bibifi.verybest.communication.impl.ATMHttpHandler;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ATMHttpHandlerTest {
    private ATMHttpHandler dummyHandler;
    private ATMHttpHandler maliciousHandler;
    private static MediaType mediaType = MediaType.parse("application/octet-stream");

    @BeforeEach
    void init() {
        DummyBankAPI dummyApi = new DummyBankAPI();
        dummyHandler = new ATMHttpHandler() {
            @Override
            protected BankAPI api() {
                return dummyApi;
            }
        };

        MaliciousBankAPI maliciousApi = new MaliciousBankAPI();
        maliciousHandler = new ATMHttpHandler() {
            @Override
            protected BankAPI api() {
                return maliciousApi;
            }
        };
    }

    @Test
    void testSendActionRequest() throws IOException {
        byte[] array = dummyHandler.sendActionRequest(new byte[]{}, new byte[]{}, new byte[]{});
        byte[] expected = new byte[]{0, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 3, 5, 5, 5, 5, 5, 5, 5, 5, 5};
        assertArrayEquals(expected, array);
    }

    @Test
    void testUnlimitedWriting() throws IOException {
        byte[] array = maliciousHandler.sendInitRequest();
        assertEquals(ATMHttpHandler.MAXIMUM_READ_LENGTH, array.length);
    }

    private static class DummyBankAPI implements BankAPI {
        @Override
        public Call<ResponseBody> init() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> postMessage(RequestBody array) {
            byte[] body = new byte[]{5, 5, 5};
            byte[] header = new byte[]{5, 5, 5};
            byte[] bodySignature = new byte[]{5, 5, 5};
            Buffer mergedBuf = new Buffer();
            mergedBuf.writeInt(header.length);
            mergedBuf.writeInt(bodySignature.length);
            mergedBuf.writeInt(body.length);
            mergedBuf.write(header);
            mergedBuf.write(bodySignature);
            mergedBuf.write(body);

            return new MockCall<>(ResponseBody.create(mediaType, mergedBuf.readByteArray()));
        }
    }

    private static class MaliciousBankAPI implements BankAPI {
        @Override
        public Call<ResponseBody> init() {
            BufferedSource source = Okio.buffer(new Source() {
                @Override
                public long read(Buffer sink, long byteCount) {
                    int length = (int) Math.min(Integer.MAX_VALUE, byteCount);
                    sink.write(new byte[length]);
                    return length;
                }

                @Override
                public Timeout timeout() {
                    return Timeout.NONE;
                }

                @Override
                public void close() {

                }
            });

            return new MockCall<>(ResponseBody.create(mediaType, -1, source));
        }

        @Override
        public Call<ResponseBody> postMessage(RequestBody array) {
            throw new UnsupportedOperationException();
        }
    }

    private static class MockCall<T> implements Call<T> {

        private final T body;

        private MockCall(T body) {
            this.body = body;
        }

        @Override
        public Response<T> execute() {
            return Response.success(body);
        }

        @Override
        public void enqueue(Callback<T> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExecuted() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCanceled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<T> clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Request request() {
            throw new UnsupportedOperationException();
        }
    }
}
