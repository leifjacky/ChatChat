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

public class AddDIalogFragment extends DialogFragment implements View.OnClickListener {
    public interface AddTypeListener{
        void onAddType(int type);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialogfragment_add, null);

        view.findViewById(R.id.dialog_add_btn_back).setOnClickListener(this);
        view.findViewById(R.id.dialog_add_btn_contact).setOnClickListener(this);
        view.findViewById(R.id.dialog_add_btn_share).setOnClickListener(this);

        return builder.setView(view).create();
    }

    public void onClick(View view) {
        AddTypeListener listener;

        dismiss();

        switch (view.getId()) {
            case R.id.dialog_add_btn_back:
                break;

            case R.id.dialog_add_btn_contact:
                listener = (AddTypeListener) getActivity();
                listener.onAddType(MainActivity.CONTACT);
                break;

            case R.id.dialog_add_btn_share:
                listener = (AddTypeListener) getActivity();
                listener.onAddType(MainActivity.SHARE);
                break;

            default:
        }
    }
}
