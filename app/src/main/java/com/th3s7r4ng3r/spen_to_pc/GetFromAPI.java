package com.th3s7r4ng3r.spen_to_pc;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GetFromAPI {
    // defining instance variables
    public String appVersion;
    public String appChangedLog;
    public String dataRetrieved = "false";

    //class constructor
    public GetFromAPI(){
        fetchData();
    }

    public void fetchData() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://spentopc.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        APIService apiService = retrofit.create(APIService.class);

        Call<AppData> call = apiService.getJsonObject();
        call.enqueue(new Callback<AppData>() {
            @Override
            public void onResponse(Call<AppData> call, Response<AppData> response) {
                if (response.isSuccessful()) {
                    AppData resultObject = response.body();

                    // Access keys and values
                    if (resultObject != null) {
                        dataRetrieved = "true";
                        appVersion = resultObject.getAndroidVersion();
                        appChangedLog = resultObject.getAndroidChangedLog();
                    }
                } else {
                    dataRetrieved = "empty object";
                    return;
                }
            }

            @Override
            public void onFailure(Call<AppData> call, Throwable t) {
                dataRetrieved = "failed to retrieve";
                return;
            }
        });
    }
}
