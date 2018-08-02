package de.upb.bibifi.verybest.communication.impl;

import de.upb.bibifi.verybest.common.communication.BankAPI;
import de.upb.bibifi.verybest.communication.interfaces.IATMSocketHandler;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Objects;

public class ATMHttpHandler implements IATMSocketHandler {
    //Deprecated for tests
    @Deprecated @SuppressWarnings("NullAway")
    private BankAPI api;
    public final static int MAXIMUM_READ_LENGTH = 10485760; // 10mb
    private final static int CHUNK_SIZE_BYTES = 8192;

    @SuppressWarnings("deprecation")
    protected BankAPI api() {
        return api;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void init(Inet4Address bankHost, int bankPort) {
        HttpUrl url = new HttpUrl.Builder().scheme("http").port(bankPort).host(bankHost.getHostAddress()).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(url).build();
        api = retrofit.create(BankAPI.class);
    }

    /**
     * @return Encrypted byte array
     * @throws IOException           Communication with the server failed
     * @throws IllegalStateException Response did not indicate success
     */
    @Override
    public byte[] sendInitRequest() throws IOException {
        Call<ResponseBody> apiCall = api().init();
        Response<ResponseBody> response = apiCall.execute();
        return getDataFromResponse(response);
    }

    /**
     * Sends an action request to the server
     *
     * @return Encrypted byte array
     */
    @Override
    public byte[] sendActionRequest(byte[] header, byte[] bodySignature, byte[] body) throws IOException {
        Buffer mergedBuf = new Buffer();
        mergedBuf.writeInt(header.length);
        mergedBuf.writeInt(bodySignature.length);
        mergedBuf.writeInt(body.length);
        mergedBuf.write(header);
        mergedBuf.write(bodySignature);
        mergedBuf.write(body);
        RequestBody requestBody = RequestBody
                .create(MediaType.parse("application/octet-stream"), mergedBuf.readByteArray());
        Call<ResponseBody> apiCall = api().postMessage(requestBody);
        Response<ResponseBody> response = apiCall.execute();
        return getDataFromResponse(response);
    }

    private byte[] getDataFromResponse(Response<ResponseBody> response) throws IOException {
        BufferedSource source;
        if (response.isSuccessful()) {
            source = Objects.requireNonNull(response.body()).source();
        } else {
            source = Objects.requireNonNull(response.errorBody()).source();
        }

        Buffer buf = new Buffer();
        try {
            while (!source.exhausted() && buf.size() < MAXIMUM_READ_LENGTH) {
                long transferred = source.read(buf, Math.min(CHUNK_SIZE_BYTES, MAXIMUM_READ_LENGTH - buf.size()));
                if (transferred < CHUNK_SIZE_BYTES) {
                    break;
                }
            }
        } finally {
            source.close();
        }

        return buf.readByteArray();
    }
}
