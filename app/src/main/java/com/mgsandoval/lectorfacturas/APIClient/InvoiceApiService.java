package com.mgsandoval.lectorfacturas.APIClient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface InvoiceApiService {
    @POST("api/bills")
    Call<Void> saveBill(@Body Invoice invoice);
}

