package com.th3s7r4ng3r.spen_to_pc;
import retrofit2.Call;
import retrofit2.http.GET;

public interface APIService {
    @GET("updates")
    Call<AppData> getJsonObject();
}
