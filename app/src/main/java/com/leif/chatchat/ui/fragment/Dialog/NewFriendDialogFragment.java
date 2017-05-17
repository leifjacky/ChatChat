package com.leif.chatchat.ui.fragment.Dialog;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.ui.NewFriendActivity;

public class NewFriendDialogFragment extends DialogFragment implements View.OnClickListener {

    private static final int ACCEPT = 1;
    private static final int IGNORE = 0;

    public interface FriendResultListener {
        void onFriendResult(int type, String fromAccount, String toAccount);
    }

    private TextView inName;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialogfragment_newfriend, null);

        view.findViewById(R.id.dialog_newfriend_btn_accept).setOnClickListener(this);
        view.findViewById(R.id.dialog_newfriend_btn_ignore).setOnClickListener(this);

        inName = (TextView) view.findViewById(R.id.dialog_newfriend_text_name);
        inName.setText(preferences.getString("newfriend_name", ""));

        return builder.setView(view).create();
    }

    public void onClick(View view) {
        FriendResultListener listener;

        dismiss();

        switch (view.getId()) {
            case R.id.dialog_newfriend_btn_accept:
                listener = (FriendResultListener) getActivity();
                listener.onFriendResult(ACCEPT, IM.account, preferences.getString("newfriend_account", ""));
                break;

            case R.id.dialog_newfriend_btn_ignore:
                listener = (FriendResultListener) getActivity();
                listener.onFriendResult(IGNORE, IM.account, preferences.getString("newfriend_account", ""));
                break;

            default:
        }
    }
}
