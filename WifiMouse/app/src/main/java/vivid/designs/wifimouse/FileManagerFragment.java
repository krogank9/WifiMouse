package vivid.designs.wifimouse;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class FileManagerFragment extends Fragment {
    View baseFrame;
    View loadingOverlay;
    ProgressBar loadingProgressBar;
    TextView loadingText;
    ListView fileList;
    public static Context fileCtx = null;

    private ArrayList<FileManagerUtils.FileInfo> curFileList = new ArrayList<>();

    boolean meVisible = false;
    public void updateUi() {
        if(!meVisible)
            return;

        if(FileManagerUtils.fileListUpdated) {
            FileManagerUtils.fileListUpdated = false;
            curFileList.clear();
            curFileList.addAll(FileManagerUtils.latestFileList);
            FileAdapter fileListAdapter = (FileAdapter) fileList.getAdapter();
            fileListAdapter.notifyDataSetChanged();
        }

        Context ctx = getContext();

        if(ctx == null)
            return;

        if(FileManagerUtils.lastActionMessage != null) {
            TextView textView = new TextView(getContext());
            textView.setText(FileManagerUtils.lastActionMessage);
            textView.setPadding(40,40,40,40);
            FileManagerUtils.lastActionMessage = null;
            Dialog message = new android.app.AlertDialog.Builder(ctx)
                    .setPositiveButton("Ok", null)
                    .setView(textView)
                    .create();
            message.show();
        }
        if(FileManagerUtils.lastToastMessage != null) {
            Toast.makeText(ctx, FileManagerUtils.lastToastMessage, Toast.LENGTH_SHORT).show();
            FileManagerUtils.lastToastMessage = null;
        }

        if( FileManagerUtils.openFileIntent != null ) {
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, FileManagerUtils.openFileIntent.getData()));
            getActivity().startActivity(FileManagerUtils.openFileIntent);
            FileManagerUtils.openFileIntent = null;
        }

        long now = System.currentTimeMillis();
        if(now - FileManagerUtils.lastProgressUpdate < 500) {
            loadingText.setText(FileManagerUtils.loadingText);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.setClickable(true);
        }
        else {
            loadingOverlay.setVisibility(View.GONE);
            loadingOverlay.setClickable(false);
        }
    }

    public FileManagerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_file_manager, container, false);
        baseFrame = inflated.findViewById(R.id.base_frame);

        loadingOverlay = inflated.findViewById(R.id.file_loading_overlay);
        loadingProgressBar = (ProgressBar) inflated.findViewById(R.id.file_progress_bar);
        loadingText = (TextView) inflated.findViewById(R.id.file_loading_text);

        fileList = (ListView) inflated.findViewById(R.id.file_list_view);
        fileList.setAdapter(new FileAdapter(getContext(), curFileList));

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    FileManagerUtils.FileInfo fileInfo = FileManagerUtils.latestFileList.get(position);
                    WifiMouseApplication.networkConnection.sendMessage("FileManager Open " + fileInfo.name);
                    WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
                } catch (IndexOutOfBoundsException ex) {} // got a indexoutofbounds exception here..
            }
        });

        fileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(position < 0 || position >= FileManagerUtils.latestFileList.size())
                    return false;
                final FileManagerUtils.FileInfo fileInfo = FileManagerUtils.latestFileList.get(position);

                final CharSequence items[];
                if(fileInfo.folder)
                    items = new CharSequence[] {"Open", "Details", "Copy", "Cut", "Paste", "Delete"};
                else
                    items = new CharSequence[] {"Open", "Download", "Details", "Copy", "Cut", "Paste", "Delete"};
                Dialog fileMenu = new android.app.AlertDialog.Builder(baseFrame.getContext())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final CharSequence action = items[which];

                                if(action.equals("Download") && !verifyStoragePermissions(getActivity()))
                                    return;

                                if(action.equals("Delete")) {
                                    Dialog confirm = new android.app.AlertDialog.Builder(baseFrame.getContext())
                                            .setNegativeButton("No", null)
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    WifiMouseApplication.networkConnection.sendMessage("FileManager Delete " + fileInfo.name);
                                                    WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
                                                }
                                            })
                                            .setTitle("Delete " + fileInfo.name + " ?")
                                            .create();
                                    confirm.show();
                                }
                                else if(action.equals("Download") && !verifyStoragePermissions(getActivity())) {
                                    permissionGrantedRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            WifiMouseApplication.networkConnection.sendMessage("FileManager Download " + fileInfo.name);
                                        }
                                    };
                                }
                                else {
                                    WifiMouseApplication.networkConnection.sendMessage("FileManager " + action + " " + fileInfo.name);
                                    WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
                                }
                            }
                        })
                        .create();
                fileMenu.show();

                return true;
            }
        });

        inflated.findViewById(R.id.file_home_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiMouseApplication.networkConnection.sendMessage("FileManager Home");
                WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
            }
        });

        inflated.findViewById(R.id.file_up_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiMouseApplication.networkConnection.sendMessage("FileManager Up");
                WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
            }
        });

        inflated.findViewById(R.id.file_send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !verifyStoragePermissions(getActivity()) ){
                    permissionGrantedRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent()
                                    .setType("*/*")
                                    .setAction(Intent.ACTION_OPEN_DOCUMENT);

                            startActivityForResult(Intent.createChooser(intent, "Select a file"), 117);
                        }
                    };
                }
                else {
                    Intent intent = new Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_OPEN_DOCUMENT);

                    startActivityForResult(Intent.createChooser(intent, "Select a file"), 117);
                }
            }
        });

        baseFrame.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                if(baseFrame != null)
                    baseFrame.postDelayed(this, 300);
            }
        });

        return inflated;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getActivity().getBaseContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public int getFileSize(Uri uri) {
        int result = 0;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getActivity().getBaseContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getInt(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==117 && resultCode==RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            String fileName;

            try {
                FileManagerUtils.sendFileIS = getActivity().getBaseContext().getContentResolver().openInputStream(selectedfile);
                FileManagerUtils.sendFileSize = getFileSize(selectedfile);
                fileName = getFileName(selectedfile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            WifiMouseApplication.networkConnection.sendMessage("FileManager Send "+fileName);
            WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        WifiMouseApplication.networkConnection.sendMessage("FileManager Refresh");
        meVisible = true;
        fileCtx = getContext();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(loadingOverlay.getVisibility() == View.VISIBLE) {
            // Stop downloading / sending file if exit file manager
            WifiMouseApplication.networkConnection.closeSocket();
        }
        meVisible = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public static class FileAdapter extends ArrayAdapter<FileManagerUtils.FileInfo> {

        public FileAdapter(@NonNull Context context, ArrayList<FileManagerUtils.FileInfo> files) {
            super(context, 0, files);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(position < 0 || position >= FileManagerUtils.latestFileList.size()) {
                View v = new View(getContext());
                v.setVisibility(View.GONE);
                return v;
            }

            FileManagerUtils.FileInfo fileInfo = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_element_file, parent, false);
            }

            TextView fileName = (TextView) convertView.findViewById(R.id.file_element_name);
            if(fileName != null)
                fileName.setText(fileInfo.name);

            ImageView icon = (ImageView) convertView.findViewById(R.id.file_element_icon);
            if(icon != null)
                icon.setImageResource(fileInfo.folder? R.drawable.ic_folder:R.drawable.ic_file);

            return convertView;
        }

    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public boolean verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            requestPermissions(
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        }
        else
            return true;
    }

    Runnable permissionGrantedRunnable = new Runnable() { @Override public void run() {} };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    permissionGrantedRunnable.run();
                else
                    Toast.makeText(getContext(), "Can't send or receive files without permissions", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baseFrame = null;
    }
}
