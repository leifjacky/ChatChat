package com.leif.chatchat.ui.fragment.Dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.leif.chatchat.R;
import com.leif.chatchat.ui.SettingActivity;

public class AvatarDialogFragment extends DialogFragment implements View.OnClickListener {

    public interface SelectTypeListener{
        void onSelectType(int type);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialogfragment_avatar, null);

        view.findViewById(R.id.dialog_avatar_btn_back).setOnClickListener(this);
        view.findViewById(R.id.dialog_avatar_btn_takePhoto).setOnClickListener(this);
        view.findViewById(R.id.dialog_avatar_btn_pickPhoto).setOnClickListener(this);

        return builder.setView(view).create();
    }

    @Override
    public void onClick(View view) {
        SelectTypeListener listener;

        dismiss();

        switch (view.getId()) {
            case R.id.dialog_avatar_btn_back:
                break;

            case R.id.dialog_avatar_btn_takePhoto:
                listener = (SelectTypeListener) getActivity();
                listener.onSelectType(SettingActivity.TAKE_PHOTO);
                break;

            case R.id.dialog_avatar_btn_pickPhoto:
                listener = (SelectTypeListener) getActivity();
                listener.onSelectType(SettingActivity.PICK_PHOTO);
                break;

            default:
        }
    }
}
