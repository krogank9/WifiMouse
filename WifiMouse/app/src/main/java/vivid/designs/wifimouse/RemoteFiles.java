package vivid.designs.wifimouse;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Objects;

public class RemoteFiles {

    static int[] remotes = { R.raw.remote_power, R.raw.remote_monitor, R.raw.remote_present, R.raw.remote_youtube };

    public static ArrayList<Object[]> getAllRemotes(Context c) {
        ArrayList<Object[]> all = new ArrayList<>();
        for(int i=0; i<remotes.length; i++) {
            String remoteFile = readRawTextFile(c, remotes[i]);
            if(!remoteFile.startsWith("#!"))
                continue;
            all.add(makeRemoteFromFile(c, remoteFile));
        }
        return all;
    }

    public static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    private static int drawableFromStr(Context c, String drawable) {
        if(drawable == null || drawable.length() == 0)
            return 0;
        else
            return c.getResources().getIdentifier("@drawable/"+drawable, "drawable", c.getPackageName());
    }

    public static Object[] makeRemoteFromFile(Context c, String file) {
        Object[] remote = {"Empty remote", file, R.drawable.ic_remote};
        String firstLine = file.split("\n")[0].substring(2);
        Hashtable<String, String> table = RemoteScript.createHashtableFromProps(firstLine, new String[]{"name", "icon", "minver", "linux", "windows", "mac"});
        if(table.containsKey("name"))
            remote[0] = table.get("name");
        if(table.containsKey("icon"))
            remote[2] = drawableFromStr(c, table.get("icon"));
        return remote;
    }
}
