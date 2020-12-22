package sharma.pankaj.drawingroute.webservices;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sharma.pankaj.drawingroute.utils.Constants;
import sharma.pankaj.drawingroute.utils.ProgressDialogCustom;

public class ServiceWrapper {

    Context context;
    public ServiceWrapper(Context context) {
        this.context = context;
    }

    public Retrofit getRetrofit() {

        //10 MB
        int cacheSize = 10 * 1024 * 1024;
        Cache cache = new Cache(context.getCacheDir(), cacheSize);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .cache(cache)
                .addNetworkInterceptor(REWRITE_RESPONSE_INTERCEPTOR)
                .addInterceptor(REWRITE_RESPONSE_INTERCEPTOR_OFFLINE)
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        okhttp3.Response response = chain.proceed(request);
                        if (response.code()==400){

                        }

                        return response;
                    }
                })
                .build();

        Gson gson = new GsonBuilder().serializeNulls().setLenient().create();
        return new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();
    }


    private final Interceptor REWRITE_RESPONSE_INTERCEPTOR = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Response originalResponse = chain.proceed(chain.request());
            String cacheControl = originalResponse.header("Cache-Control");
            if (cacheControl == null || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                    cacheControl.contains("must-revalidate") || cacheControl.contains("max-age=0")) {
                return originalResponse.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=" + 50)
                        .build();
            } else {
                return originalResponse;
            }
        }
    };

    private final Interceptor REWRITE_RESPONSE_INTERCEPTOR_OFFLINE = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (!isAvailable()) {
                request = request.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, only-if-cached")
                        .build();
            }
            return chain.proceed(request);
        }
    };

    //Api call Method
    public  <T> void HandleResponse(Call<T> call, final ResponseHandler<T> responseHandler) {

        if (isAvailable()) {
            ProgressDialogCustom.showProgressDialog(context);
            call.enqueue(new Callback<T>() {
                @Override
                public void onResponse(final Call<T> call, final Response<T> response) {

                    ProgressDialogCustom.dismissProgressDialog();
                    if (response.isSuccessful()) {
                        if (responseHandler != null) {
                            responseHandler.onResponse(response.body());
                        }
                    } else {
                        if (responseHandler != null) {
                            try {
                                Log.e("pankaj=====", "onResponse: " +response.errorBody().string());
                            }catch (Exception e){

                            }
                            switch (response.code()) {
                                case 400:
                                    responseHandler.onError("The request hostname is invalid");
                                    break;
                                case 401:
                                    responseHandler.onError("The request requires user authentication");
                                    break;
                                case 403:
                                    responseHandler.onError("The server understood the request, but is refusing to fulfill it");
                                    break;
                                case 404:
                                    responseHandler.onError("The server has not found anything matching the Request-URI");
                                    break;
                                case 409:
                                    responseHandler.onError("The request could not be completed due to a conflict with the current state of the resource");
                                    break;
                                case 500:
                                    responseHandler.onError("The server encountered an unexpected condition which prevented it from fulfilling the request");
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                }

                @Override
                public void onFailure(final Call<T> call, final Throwable throwable) {
                    ProgressDialogCustom.dismissProgressDialog();
                    if (responseHandler != null) {
                        responseHandler.onFailure(throwable);
                    }
                }
            });
        }else {
            if (responseHandler != null) {
                responseHandler.onInternetUnavailable();
            }
        }
    }

    private boolean isAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // convert into text/plain
    private RequestBody convertPlainString(String data) {
        RequestBody requestBody = null;
        if (data != null) {
            requestBody = RequestBody.create(MediaType.parse("text/plain"), data);
        }
        return requestBody;
    }
}
