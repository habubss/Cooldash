package hack.project.cooldash.Controller;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TtsApi {
    @POST("synthesize")
    Call<ResponseBody> synthesizeText(@Body JsonObject text);
}