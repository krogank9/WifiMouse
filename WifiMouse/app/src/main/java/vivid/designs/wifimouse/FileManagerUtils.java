package vivid.designs.wifimouse;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FileManagerUtils {

    public static class FileInfo {
        String name;
        boolean folder;
        public FileInfo(String name, boolean folder) {
            this.name = name;
            this.folder = folder;
        }
    }

    public static boolean fileListUpdated = false;
    public static ArrayList<FileInfo> latestFileList = new ArrayList<>();

    // Boolean -- null for no action started, false for not done, true for completed
    public static String lastActionMessage = null;
    public static String lastToastMessage = null;
    public static Intent openFileIntent = null;

    public static String loadingText = "";
    public static long lastProgressUpdate = 0;
    private static void setLoadingProgress() {
        lastProgressUpdate = System.currentTimeMillis();
    }

    public static void home(NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Home", true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void up(NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Up", true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void refresh(NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Refresh", true);
        String result = connection.readStringFromNetwork(true);
        if(result.length() > 0) {
            int numFiles;
            try {
                numFiles = Integer.valueOf(result);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                connection.closeSocket();
                return;
            }
            Log.d("Refreshing", "numFiles: "+numFiles);
            final ArrayList<FileInfo> tmpFileList = new ArrayList<>();
            while(numFiles-- > 0) {
                String name = connection.readStringFromNetwork(true);
                String isFolder = connection.readStringFromNetwork(true);

                if(name.length() == 0 || isFolder.length() == 0) {
                    Log.d("FileManagerRefresh", "invalid file");
                    break;
                }
                else
                    tmpFileList.add(new FileInfo(name, Boolean.parseBoolean(isFolder)));
            }
            fileListUpdated = true;
            latestFileList.clear();
            latestFileList.addAll(tmpFileList);
        }
    }

    public static void open(String name, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Open "+name, true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void copy(String name, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Copy "+name, true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void cut(String name, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Cut "+name, true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void paste(String destination, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Paste "+destination, true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void delete(String name, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Delete "+name, true);
        String result = connection.readStringFromNetwork(true);
    }

    public static void details(String name, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Details "+name, true);
        String result = connection.readStringFromNetwork(true);
        if(result.length() > 0 && !result.equals("Failed")) {
            lastActionMessage = result;
        }
    }

    public static void download(String name, NetworkConnection connection) {
        connection.sendStringOverNetwork("FileManager Download "+name, true);
        if(connection.readStringFromNetwork(true).equals("Sending") == false) {
            lastToastMessage = "Couldn't download " + name;
            return;
        }
        loadingText = "Downloading " + name;

        Log.d("download", "download started");
        // Download file from computer onto phone
        byte[] file = connection.readDataEncrypted();
        Log.d("download", "download ended");

        if(file.length == 0)
            lastToastMessage = "Couldn't download file";
        // Open file after downloaded
        if(file.length > 0) {
            try {
                File downloaded = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
                downloaded.createNewFile();
                FileOutputStream out = new FileOutputStream(downloaded);
                out.write(file);
                out.close();
                lastToastMessage = "Downloaded file " + name;
                if(name.indexOf(".") >= 0 && FileManagerFragment.fileCtx != null) {
                    String ext = name.substring(name.indexOf(".") + 1);

                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    Log.d("mimetype", ext);
                    Uri fileURI = FileProvider.getUriForFile(FileManagerFragment.fileCtx, FileManagerFragment.fileCtx.getApplicationContext().getPackageName() + ".vivid.designs.wifimouse.provider", downloaded);
                    String mimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
                    newIntent.setDataAndType(fileURI,mimeType);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    openFileIntent = newIntent;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("Download", "couldn't write file");
            }
        }
    }

    public static InputStream sendFileIS = null;
    public static int sendFileSize = 0;
    public static void send_file(String name, NetworkConnection connection) {
        if(sendFileSize > NetworkConnection.IO_MAX_FILE_SIZE) {
            Log.d("send_file", "file too large");
            return;
        }

        // Read all of file into bytes first
        int readFromFile = 0;
        byte[] fileBytes = new byte[sendFileSize];
        while(readFromFile < sendFileSize) {
            try {
                int numRead = sendFileIS.read(fileBytes, readFromFile, fileBytes.length - readFromFile);
                if(numRead <= 0) {
                    Log.d("send_file", "couldn't read file to bytes "+numRead);
                    return;
                }
                else
                    readFromFile += numRead;
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // Make sure ready to receive file & can write
        connection.sendStringOverNetwork("FileManager Send "+name, true);
        String result = connection.readStringFromNetwork(true);
        if(result.equals("Ready") == false) {
            Log.d("FileManagerUtils", "didn't receive ready signal from server");
            return;
        }

        loadingText = "Sending " + name;

        Log.d("send_file", "sending file...");
        // Get file size & start uploading file to computer
        connection.sendDataEncrypted(fileBytes);
        Log.d("send_file", "sent encrypted file");

        // Receive confirmation
        if(connection.readStringFromNetwork(true).equals("Success")) {
            lastToastMessage = "Sent file " + name;
            Log.d("FileManagerUtils", "successfully wrote file");
        }
    }

    public static void fileManagerCommand(String command, NetworkConnection connection) {
        if(command.equals("Refresh")) {
            refresh(connection);
        }
        else if(command.equals("Home")) {
            home(connection);
        }
        else if(command.equals("Up")) {
            up(connection);
        }
        else if(command.startsWith("Open ")) {
            open(command.substring("Open ".length()), connection);
        }
        else if(command.startsWith("Copy ")) {
            copy(command.substring("Copy ".length()), connection);
        }
        else if(command.startsWith("Cut ")) {
            cut(command.substring("Cut ".length()), connection);
        }
        else if(command.startsWith("Paste ")) {
            paste(command.substring("Paste ".length()), connection);
        }
        else if(command.startsWith("Delete ")) {
            delete(command.substring("Delete ".length()), connection);
        }
        else if(command.startsWith("Details ")) {
            details(command.substring("Details ".length()), connection);
        }
        else if(command.startsWith("Download ")) {
            download(command.substring("Download ".length()), connection);
        }
        else if(command.startsWith("Send ")) {
            send_file(command.substring("Send ".length()), connection);
        }
    }
}
