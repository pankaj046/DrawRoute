package sharma.pankaj.drawingroute.webservices;

import android.content.Context;
import android.widget.Toast;
import es.dmoral.toasty.Toasty;


public abstract class ResponseHandler<T> {
    Context mContext;
    public ResponseHandler(Context context){
        super();
        this.mContext = context;
    }


    public abstract void onResponse(final T response);

    public void onError(final String errorResponse) {
        errorMasseage(mContext, errorResponse);
    }

    public void onFailure(Throwable throwable) {
        errorMasseage(mContext, throwable.getMessage());
    }

    public void  onInternetUnavailable(){
        errorMasseage(mContext, "Make sure you have an active data connection");
    }

    private void errorMasseage(Context context, String msg){
        Toasty.error(context, ""+msg, Toast.LENGTH_LONG, true).show();
    }
    private void successMasseage(Context context, String msg){
        Toasty.success(context, ""+msg, Toast.LENGTH_LONG, true).show();
    }

}
