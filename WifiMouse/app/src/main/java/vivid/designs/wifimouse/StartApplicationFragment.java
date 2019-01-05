package vivid.designs.wifimouse;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

public class StartApplicationFragment extends Fragment {

    private ListView applicationsListView;

    public static String applicationsListResult = null;
    private boolean receivedApplicationsList = false;
    private ArrayList<String> applicationsList = new ArrayList<>();

    private void tryLoadApplicationsList() {
        try {
            if (applicationsListResult == null || receivedApplicationsList)
                return;
            receivedApplicationsList = true;
            String[] apps = applicationsListResult.split("\n");
            for(int i=0; i<apps.length; i++)
                for(int j=i+1; j<apps.length; j++)
                    if(apps[i].compareTo(apps[j]) > 0) { // sort apps alphabetically
                        String tmp = apps[i];
                        apps[i] = apps[j];
                        apps[j] = tmp;
                    }
            applicationsList.addAll(Arrays.asList(apps));
            Log.d("appCount", "" + applicationsList.size());

            ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_application_name, applicationsList);
            applicationsListView.setAdapter(listViewAdapter);
            listViewAdapter.notifyDataSetChanged();
            applicationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    Log.d("frag", "starting application " + applicationsList.get(position));
                    WifiMouseApplication.networkConnection.sendMessage("StartApplication " + applicationsList.get(position));
                }
            });
        } catch(Exception e) {}
    }

    public StartApplicationFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_start_application, container, false);
        applicationsListView = (ListView) inflated.findViewById(R.id.applications_listview);
        applicationsListView.post(new Runnable() {
            @Override
            public void run() {
                if(applicationsListView != null && !receivedApplicationsList) {
                    tryLoadApplicationsList();
                    applicationsListView.postDelayed(this, 100);
                }
            }
        });
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        return inflated;
    }

    @Override
    public void onResume() {
        super.onResume();
        receivedApplicationsList = false;
        WifiMouseApplication.networkConnection.sendMessage("GetApplications");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
