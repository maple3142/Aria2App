package com.gianlu.aria2app.Activities.AddDownload;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class Base64Fragment extends FragmentWithDialog {
    private final int FILE_SELECT_CODE = 7;
    private TextView path;
    private Uri fileUri;
    private String name;

    @NonNull
    public static Base64Fragment getInstance(Context context, boolean torrent, @Nullable Uri uri) {
        Base64Fragment fragment = new Base64Fragment();
        Bundle args = new Bundle();
        args.putBoolean("torrent", torrent);
        args.putString("title", context.getString(R.string.file));
        if (uri != null) args.putParcelable("uri", uri);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static Base64Fragment getInstance(Context context, @NonNull AddBase64Bundle bundle) {
        return getInstance(context, bundle instanceof AddTorrentBundle, bundle.fileUri());
    }

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (getArguments() == null || getArguments().getBoolean("torrent", true))
            intent.setType("application/x-bittorrent");
        else
            intent.setType("application/metalink4+xml,application/metalink+xml");

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            showToast(Toaster.build().message(R.string.noFilemanager).ex(ex));
            return;
        }

        name = null;
        path.setText(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == Activity.RESULT_OK && intent.getData() != null) {
                    if (intent.getData() != null) setFilename(intent.getData());
                }
                break;
        }
    }

    private void setFilename(@NonNull Uri uri) {
        fileUri = uri;

        if (getContext() == null) return;

        try {
            name = AddBase64Bundle.extractFilename(getContext(), uri);
        } catch (AddBase64Bundle.CannotReadException ex) {
            showToast(Toaster.build().message(R.string.invalidFile).ex(ex));
            return;
        }

        path.setText(name);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() == null) return;
        Uri uri = getArguments().getParcelable("uri");
        if (uri != null) setFilename(uri);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_base64, container, false);
        path = layout.findViewById(R.id.base64Fragment_path);
        Button pick = layout.findViewById(R.id.base64Fragment_pick);
        pick.setOnClickListener(v -> {
            if (getActivity() == null) return;

            AskPermission.ask(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE, new AskPermission.Listener() {
                @Override
                public void permissionGranted(@NonNull String permission) {
                    showFilePicker();
                }

                @Override
                public void permissionDenied(@NonNull String permission) {
                    showToast(Toaster.build().message(R.string.readPermissionDenied).error(true));
                }

                @Override
                public void askRationale(@NonNull AlertDialog.Builder builder) {
                    builder.setTitle(R.string.readExternalStorageRequest_title)
                            .setMessage(R.string.readExternalStorageRequest_base64Message);
                }
            });
        });

        SuperTextView help = layout.findViewById(R.id.base64Fragment_help);
        if (getArguments() == null || getArguments().getBoolean("torrent", true))
            help.setHtml(R.string.pickTorrent_help);
        else
            help.setHtml(R.string.pickMetalink_help);

        return layout;
    }

    @Nullable
    public String getFilenameOnDevice() {
        return name;
    }

    @Nullable
    public String getBase64() throws InvalidFieldException {
        if (getContext() == null) return null;

        if (fileUri == null)
            throw new InvalidFieldException(Base64Fragment.class, R.id.base64Fragment_pick, getString(R.string.base64NotSelected));

        try {
            return AddBase64Bundle.readBase64(getContext(), fileUri);
        } catch (AddBase64Bundle.CannotReadException ex) {
            showToast(Toaster.build().message(R.string.invalidFile).ex(ex));
            return null;
        }
    }

    @Nullable
    public Uri getFileUri() {
        return fileUri;
    }
}
