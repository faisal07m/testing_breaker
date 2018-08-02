package de.upb.bibifi.verybest.bank.communication;

import de.upb.bibifi.verybest.bank.communication.impl.BankSocketHandler;
import de.upb.bibifi.verybest.common.communication.BankAPI;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpHandlerTest {
    private BankSocketHandler handler;

    private HttpUrl buildTestUrl(String path) {
        return new HttpUrl.Builder().scheme("http").port(8000).host("localhost").addPathSegment(path).build();
    }

    private BankAPI initializeAPI() {
        HttpUrl url = buildTestUrl("");
        Retrofit retrofit = new Retrofit.Builder().baseUrl(url).build();
        return retrofit.create(BankAPI.class);
    }

    @BeforeAll
    public void init() {
        handler = new BankSocketHandler("localhost", 8000, null);
        handler.start();
    }

    @Test
    public void testInitRequest() {
        BankAPI api = initializeAPI();
        Call<ResponseBody> apiCall = api.init();
        Assertions.assertDoesNotThrow(() -> apiCall.execute());
    }

    @Test
    public void testActionRequest() {
        BankAPI api = initializeAPI();
        Buffer mergedBuf = new Buffer();
        mergedBuf.writeInt(1);
        mergedBuf.writeInt(1);
        mergedBuf.writeInt(1);
        mergedBuf.write(new byte[]{1});
        mergedBuf.write(new byte[]{1});
        mergedBuf.write(new byte[]{1});

        RequestBody requestBody = RequestBody
                .create(MediaType.parse("application/octet-stream"), mergedBuf.readByteArray());
        Call<ResponseBody> call = api.postMessage(requestBody);
        Assertions.assertDoesNotThrow(() -> call.execute());
    }

    @Test
    public void testInvalidRequest() throws IOException {
        HttpUrl url = buildTestUrl("test");
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        okhttp3.Response response = httpClient.newCall(request).execute();
        Assertions.assertFalse(response.isSuccessful());
        Assertions.assertEquals(response.code(), 400);
    }
}
