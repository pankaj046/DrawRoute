package sharma.pankaj.drawingroute.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;

import sharma.pankaj.drawingroute.R;


public class ProgressDialogCustom {

    public static ProgressDialog progressDialog;

    public static ProgressDialog showProgressDialog(@NonNull Context context) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context, R.style.StyleProgressDialog);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    progressDialog = null;
                }
            });
            try {
                progressDialog.show();
            } catch (Exception e) {
            }
        }
        return progressDialog;
    }

    public static ProgressDialog showProgressDialogTransparent(@NonNull Context context) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context, R.style.StyleProgressDialogtransparent);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    progressDialog = null;
                }
            });
            try {
                progressDialog.show();
            } catch (Exception e) {
            }
        }
        return progressDialog;
    }


    public static void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
            }
        }
        progressDialog = null;
    }
}
