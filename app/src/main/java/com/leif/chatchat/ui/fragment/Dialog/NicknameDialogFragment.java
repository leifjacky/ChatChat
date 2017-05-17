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

public class NicknameDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "IMService";

    private EditText inNickname;

    private ServiceConnection conn;
    private IMService service;

    public interface ChangeNicknameListener {
        void onNicknameChangeComplete(String nickname);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(true);

        conn = new IMConnection();
        Intent intent = new Intent(getActivity(), IMService.class);
        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialogfragment_nickname, null);

        inNickname = (EditText) view.findViewById(R.id.dialog_nickname_text_nickname);
        inNickname.setText(IM.nickname);

        view.findViewById(R.id.dialog_nickname_btn_back).setOnClickListener(this);
        view.findViewById(R.id.dialog_nickname_btn_check).setOnClickListener(this);

        return builder.setView(view).create();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialog_nickname_btn_back:
                dismiss();
                break;
            case R.id.dialog_nickname_btn_check:
                new ChangeNicknameTask().execute(inNickname.getText().toString());
                break;
        }
    }

    private class ChangeNicknameTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(), null, "正在修改昵称,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.changSetting("&" + ContactsProvider.ContactColumns.ACCOUNT + "=" + IM.account
                                    + "&" + ContactsProvider.ContactColumns.NAME + "=" + strings[0]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(getActivity(), "修改成功", Toast.LENGTH_LONG).show();
                    ChangeNicknameListener listener = (ChangeNicknameListener) getActivity();
                    listener.onNicknameChangeComplete(inNickname.getText().toString().trim());
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
