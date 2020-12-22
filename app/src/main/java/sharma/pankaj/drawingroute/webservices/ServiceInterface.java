package sharma.pankaj.drawingroute.webservices;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;
import sharma.pankaj.drawingroute.model.RouteModel;

public interface ServiceInterface {

    @GET()
    Call<RouteModel> getRoute(@Url String url);
}
