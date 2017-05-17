package com.leif.chatchat.ui.fragment.Dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.leif.chatchat.IM;
import com.leif.chatchat.IMService;
import com.leif.chatchat.MainActivity;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.ui.LoginActivity;

public class PasswordDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "IMService";

    private EditText inOldPassword, inPassword, inRePassword;
    private ServiceConnection conn;
    private IMService service;

    public interface ChangePasswordListener {
        void onPasswordChangeComplete(String password);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        conn = new IMConnection();
        Intent intent = new Intent(getActivity(), IMService.class);
        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialogfragment_password, null);

        inOldPassword = (EditText) view.findViewById(R.id.dialog_password_input_oldpassword);
        inPassword = (EditText) view.findViewById(R.id.dialog_password_input_password);
        inRePassword = (EditText) view.findViewById(R.id.dialog_password_input_repassword);

        view.findViewById(R.id.dialog_password_btn_back).setOnClickListener(this);
        view.findViewById(R.id.dialog_password_btn_check).setOnClickListener(this);

        return builder.setView(view).create();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_password_btn_back:
                dismiss();
                break;
            case R.id.dialog_password_btn_check:
                String oldPasswordStr = inOldPassword.getText().toString().trim();
                String passwordStr = inPassword.getText().toString().trim();
                String rePasswordStr = inRePassword.getText().toString().trim();

                if (TextUtils.isEmpty(oldPasswordStr)) {
                    Toast.makeText(getActivity(), "旧密码不能为空", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!oldPasswordStr.equals(IM.password)){
                    Toast.makeText(getActivity(), "旧密码输入不正确", Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(passwordStr)) {
                    Toast.makeText(getActivity(), "新密码不能为空", Toast.LENGTH_LONG).show();
                    return;
                }

                char[] pass = (passwordStr).toCharArray();
                boolean flag = true;
                for (char c : pass) {
                    if (c >= '0' && c <= '9') ;
                    else if (c >= 'a' && c <= 'z') ;
                    else if (c >= 'A' && c <= 'Z') ;
                    else {
                        flag = false;
                        break;
                    }
                }
                if (!flag) {
                    Toast.makeText(getActivity(), "用户名、密码只能由英文字母和数字组成", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!passwordStr.equals(rePasswordStr)) {
                    Toast.makeText(getActivity(), "两次输入的新密码不同", Toast.LENGTH_LONG).show();
                    return;
                }

                new ChangePasswordTask().execute(inPassword.getText().toString());
                break;
        }
    }

    private class ChangePasswordTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(), null, "正在修改密码,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.changSetting("&" + ContactsProvider.ContactColumns.ACCOUNT + "=" + IM.account
                    + "&" + ContactsProvider.ContactColumns.PASSWORD + "=" + strings[0]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(getActivity(), "修改成功", Toast.LENGTH_LONG).show();
                    ChangePasswordListener listener = (ChangePasswordListener) getActivity();
                    listener.onPasswordChangeComplete(inPassword.getText().toString().trim());
                    dismiss();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(getActivity(), "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(getActivity(), "未知错误", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
        }
    }

    public class IMConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service Connect");
            service = ((IMService.IMBinder)iBinder).getService();
        }

        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    public void onDestroyView() {
        getActivity().unbindService(conn);
        super.onDestroyView();
    }
}
