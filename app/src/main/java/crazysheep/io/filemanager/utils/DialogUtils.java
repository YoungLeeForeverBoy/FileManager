package crazysheep.io.filemanager.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;

import crazysheep.io.filemanager.R;

/**
 * dialog utils
 * <p/>
 * Created by crazysheep on 15/11/16.
 */
public class DialogUtils {

    public static interface ButtonAction {
        public String getTitle();

        public void onClick(DialogInterface dialog);
    }

    public static Dialog showConfirmDialog(Activity activity, String title, String content,
                                           final ButtonAction okAction,
                                           final ButtonAction cancelAction) {
        final Dialog confirmDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.MyDialogTheme);
        if (!TextUtils.isEmpty(title))
            builder.setTitle(title);
        if (!TextUtils.isEmpty(content))
            builder.setMessage(content);
        if (okAction != null) {
            builder.setPositiveButton(okAction.getTitle(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    okAction.onClick(dialog);
                }
            });
        }
        if (cancelAction != null) {
            builder.setNegativeButton(cancelAction.getTitle(),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            cancelAction.onClick(dialog);
                        }
                    });
        }

        confirmDialog = builder.create();
        confirmDialog.setOwnerActivity(activity);

        if (confirmDialog.getOwnerActivity() != null
                && !confirmDialog.getOwnerActivity().isFinishing())
            confirmDialog.show();

        return confirmDialog;
    }

    /**
     * show custom dialog
     * */
    public static Dialog showCustomDialog(Activity activity, @NonNull View contentView) {
        Dialog dialog = new Dialog(activity, R.style.MyDialogTheme);
        dialog.setOwnerActivity(activity);
        dialog.setContentView(contentView);

        if(dialog.getOwnerActivity() != null && !dialog.getOwnerActivity().isFinishing())
            dialog.show();

        return dialog;
    }

    /**
     * dismiss dialog safety
     * */
    public static void dismissDialog(@NonNull Dialog dialog) {
        if(dialog.getOwnerActivity() != null && !dialog.getOwnerActivity().isFinishing())
            dialog.dismiss();
    }
}
