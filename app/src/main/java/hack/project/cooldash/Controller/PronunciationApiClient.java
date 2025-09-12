package hack.project.cooldash.Controller;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PronunciationApiClient {
    private static final String BASE_URL = "http://95.183.13.188:5003/";
    private static PronunciationApi instance;

    public static PronunciationApi getApi() {
        if (instance == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(PronunciationApi.class);
        }
        return instance;
    }
}