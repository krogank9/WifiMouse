package vivid.designs.wifimouse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TaskManagerFragment extends Fragment {
    private static ArrayList<Float> ramUsages = new ArrayList<>();
    private static ArrayList<ArrayList<Float>> cpuUsages = new ArrayList<>();
    private static int totalRamKBs = 0;

    private static class Process {
        int RAM;
        int CPU;
        String PID;
        String name;
    }
    private static ArrayList<Process> processes = new ArrayList<>();
    static int taskUpdateCounter = 0;

    View ramGraphView;
    View cpuGraphView;
    TextView ramGraphText;
    TextView ramGraphText2;
    TextView cpuGraphText;

    ListView processListView;
    ProcessListAdapter processListAdapter;
    int adapterUpdateCounter = 0;

    TextView cpuCategory, ramCategory, nameCategory;
    public void setSortCategory(View categoryTextView) {
        if(categoryTextView == nameCategory) {
            cpuCategory.setTypeface(null, Typeface.NORMAL);
            ramCategory.setTypeface(null, Typeface.NORMAL);
            nameCategory.setTypeface(null, Typeface.BOLD);
            sortProcessesBy = 0;
        }
        if(categoryTextView == cpuCategory) {
            cpuCategory.setTypeface(null, Typeface.BOLD);
            ramCategory.setTypeface(null, Typeface.NORMAL);
            nameCategory.setTypeface(null, Typeface.NORMAL);
            sortProcessesBy = 1;
        }
        if(categoryTextView == ramCategory) {
            cpuCategory.setTypeface(null, Typeface.NORMAL);
            ramCategory.setTypeface(null, Typeface.BOLD);
            nameCategory.setTypeface(null, Typeface.NORMAL);
            sortProcessesBy = 2;
        }
    }

    private static int sortProcessesBy = 1;
    private static void sortProcessList(ArrayList<Process> list) {
        for(int i=0; i<list.size(); i++) {
            Process a = list.get(i);
            for(int j=i+1; j<list.size(); j++) {
                Process b = list.get(j);
                if((sortProcessesBy == 0 && a.name.toLowerCase().compareTo(b.name.toLowerCase()) > 0)
                || (sortProcessesBy == 1 && a.CPU < b.CPU)
                || (sortProcessesBy == 2 && a.RAM < b.RAM)) {
                    list.set(i, b);
                    list.set(j, a);
                    a = b;
                }
            }
        }
    }

    public static void updateProcessList(String list) {
        ArrayList<Process> newList = new ArrayList<>();
        String[] lines = list.split("\n");
        for(int i=0; i<lines.length; i++) {
            String line = lines[i];
            String[] parts = line.split(" ");
            if(parts.length < 4)
                continue;
            Process p = new Process();
            p.PID = parts[0];
            p.CPU = (int) Double.valueOf(parts[1]).doubleValue();
            p.RAM = (int) Double.valueOf(parts[2]).doubleValue();
            // name can have spaces
            p.name = "";
            for(int j=3; j<parts.length; j++) {
                if(j != 1)
                    p.name += " ";
                p.name += parts[j];
            }
            newList.add(p);
        }
        sortProcessList(newList);
        taskUpdateCounter++;
        processes = newList;
    }

    public static void addRamUsage(String used_available) {
        try {
            String[] sp = used_available.split(" ");
            Integer used = Integer.valueOf( sp[0] );
            totalRamKBs = Integer.valueOf( sp[1] );
            float percent = Math.max( Math.min(((float)used)/((float) totalRamKBs), 1), 0 );
            ramUsages.add(percent);
        } catch(Exception e) {}

        if(ramUsages.size() > 100)
            ramUsages.remove(0);
    }

    public static void addCpuUsages(String percents) {
        String[] sPcts = percents.split(" ");
        for(int i=0; i<sPcts.length; i++) {
            try {
                float toAdd = ((float)Integer.valueOf(sPcts[i]))/100.0f;
                if (cpuUsages.size() <= i)
                    cpuUsages.add(new ArrayList<Float>());

                if(cpuUsages.get(i).size() > 0) { // prevent cpu spikes to look nicer
                    float last = cpuUsages.get(i).get(cpuUsages.get(i).size() - 1);
                    float diff = toAdd - last;
                    if(diff > 0.01f)
                        diff = 0.01f;
                    if(diff < -0.01f)
                        diff = -0.01f;
                    toAdd = last + diff;
                }
                cpuUsages.get(i).add(toAdd);
            } catch (Exception e) {}
        }

        for(int i=0; i<cpuUsages.size(); i++)
            if(cpuUsages.get(i).size() > 300)
                cpuUsages.get(i).remove(0);
    }

    public TaskManagerFragment() {
        ramUsages.clear();
        cpuUsages.clear();
    }

    private long lastProcessesUpdate = 0;

    private void updateUi() {
        ramGraphView.invalidate();
        cpuGraphView.invalidate();
        WifiMouseApplication.networkConnection.sendMessage("GetRamUsage");
        WifiMouseApplication.networkConnection.sendMessage("GetCpuUsage");
        if(System.currentTimeMillis() - lastProcessesUpdate > 1000) {
            WifiMouseApplication.networkConnection.sendMessage("GetTasks");
            lastProcessesUpdate = System.currentTimeMillis();
        }
        if(adapterUpdateCounter != taskUpdateCounter) {
            adapterUpdateCounter = taskUpdateCounter;
            processListAdapter.clear();
            processListAdapter.addAll(processes);
            processListAdapter.notifyDataSetChanged();
        }
        try {
            // update ram text
            float pctUsed = ramUsages.get(ramUsages.size()-1);
            String pctUsedStr = Math.round(pctUsed*100) + "%";
            float totalGB = (float)totalRamKBs / 1000.0f / 1000.0f;
            float usedGB = pctUsed*totalGB;
            usedGB = Math.round(usedGB*100.0f)/100.0f;
            totalGB = Math.round(totalGB*100.0f)/100.0f;
            String usedRam = String.format("%.02f", usedGB) + "GB";
            String totalRam = String.format("%.02f", totalGB) + "GB";
            ramGraphText.setText(usedRam+" / "+totalRam);
            ramGraphText2.setText(pctUsedStr);

            // update cpu text
            String htmlText = "";
            for(int i=0; i<cpuUsages.size(); i++) {
                if(i != 0)
                    htmlText += " ";
                ArrayList<Float> core = cpuUsages.get(i);
                if(core.size() == 0)
                    continue;
                int percent = (int)(100.0f * core.get(core.size() - 1));
                String hexColor = String.format("#%06X", getCpuColor(i, false));
                htmlText += "<font color='"+hexColor+"'>"+percent+"&percnt;</font>";
            }
            cpuGraphText.setText(Html.fromHtml(htmlText));
        } catch(Exception e){} //fuck threads
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_task_manager, container, false);

        processListView = (ListView) inflated.findViewById(R.id.task_manager_listview);
        processListAdapter = new ProcessListAdapter(inflated.getContext(), new ArrayList<Process>());
        processListView.setAdapter(processListAdapter);

        processListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final Process p = processListAdapter.getItem(position);
                Dialog confirmPressDialog = new AlertDialog.Builder(getContext(
                ))
                        .setTitle("Kill "+p.name+"?")
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        WifiMouseApplication.networkConnection.sendMessage("KillPID "+p.PID);
                                    }
                                }
                        )
                        .setNegativeButton("No", null)
                        .create();
                confirmPressDialog.show();
            }
        });

        cpuCategory = (TextView) inflated.findViewById(R.id.cpu_process_category);
        ramCategory = (TextView) inflated.findViewById(R.id.ram_process_category);
        nameCategory = (TextView) inflated.findViewById(R.id.name_process_category);
        View.OnClickListener categorySelectListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSortCategory(view);
            }
        };
        setSortCategory(cpuCategory);
        cpuCategory.setOnClickListener(categorySelectListener);
        ramCategory.setOnClickListener(categorySelectListener);
        nameCategory.setOnClickListener(categorySelectListener);

        ramGraphView = new RamGraphView(inflated.getContext());
        cpuGraphView = new CpuGraphView(inflated.getContext());

        ramGraphText = (TextView) inflated.findViewById(R.id.ram_graph_text);
        ramGraphText2 = (TextView) inflated.findViewById(R.id.ram_graph_text_right);
        cpuGraphText = (TextView) inflated.findViewById(R.id.task_manager_cpu_percentage);
        ((FrameLayout) inflated.findViewById(R.id.ram_graph_container)).addView(ramGraphView);
        ((FrameLayout) inflated.findViewById(R.id.cpu_graph_container)).addView(cpuGraphView);

        ramGraphView.post(new Runnable() {
            @Override
            public void run() {
                if(ramGraphView == null)
                    return;
                updateUi();
                ramGraphView.postDelayed(this, 50);
            }
        });
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        return inflated;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class RamGraphView extends View {
        Paint graphPaint;
        public RamGraphView(Context c) {
            super(c);
            graphPaint = new Paint();
            graphPaint.setStyle(Paint.Style.STROKE);
            graphPaint.setStrokeWidth(6);
            graphPaint.setStrokeJoin(Paint.Join.ROUND);
            graphPaint.setStrokeCap(Paint.Cap.ROUND);
            graphPaint.setColor(Color.parseColor("#bb5555"));
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if(ramUsages.size() == 0)
                return;
            canvas.save();
            canvas.scale(1, -1, canvas.getWidth()/2, canvas.getHeight()/2);

            Path ramGraph = new Path();
            try {
                for (int i = 0; i < ramUsages.size(); i++) {
                    if (i == 0)
                        ramGraph.moveTo(0, ramUsages.get(i) * canvas.getHeight());
                    ramGraph.lineTo((float) i / 100.0f * (canvas.getWidth()+4), ramUsages.get(i) * canvas.getHeight());
                }
                canvas.drawPath(ramGraph, graphPaint);
            } catch (Exception e) {
                return; // fuck threads
            }

            canvas.restore();
        }
    }

    private class CpuGraphView extends View {
        Paint graphPaint;
        public CpuGraphView(Context c) {
            super(c);
            graphPaint = new Paint();
            graphPaint.setDither(true);
            graphPaint.setStyle(Paint.Style.STROKE);
            graphPaint.setStrokeWidth(2);
            graphPaint.setStrokeJoin(Paint.Join.ROUND);
            graphPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if(ramUsages.size() == 0)
                return;
            canvas.save();
            canvas.scale(1, -1, canvas.getWidth()/2, canvas.getHeight()/2);

            try {
                for (int i = 0; i < cpuUsages.size(); i++) {
                    Path coreGraph = new Path();
                    ArrayList<Float> coreUsages = cpuUsages.get(i);
                    for (int j = 0; j < coreUsages.size(); j++) {
                        if (j == 0)
                            coreGraph.moveTo(0, coreUsages.get(j) * canvas.getHeight());
                        coreGraph.lineTo((float) j / 300.0f * (canvas.getWidth()+1), coreUsages.get(j) * canvas.getHeight());
                    }
                    graphPaint.setColor(getCpuColor(i, true));
                    canvas.drawPath(coreGraph, graphPaint);
                }
            } catch (Exception e) {
                return; // fuck threads and fuck java
            }

            canvas.restore();
        }
    }

    private static int getCpuColor(int coreNum, boolean androidFormat) {
        coreNum %= 8;
        switch (coreNum) {
            case 0:
                return androidFormat? Color.parseColor("#ff6e00") : 0xff6e00;
            case 1:
                return androidFormat? Color.parseColor("#cb0c29") : 0xcb0c29;
            case 2:
                return androidFormat? Color.parseColor("#49a835") : 0x49a835;
            case 3:
                return androidFormat? Color.parseColor("#2d7db3") : 0x2d7db3;
            case 4:
                return androidFormat? Color.parseColor("#f97db3") : 0xf97db3;
            case 5:
                return androidFormat? Color.parseColor("#805a9f") : 0x805a9f;
            case 6:
                return androidFormat? Color.parseColor("#ffcd00") : 0xffcd00;
            case 7:
            default:
                return androidFormat? Color.parseColor("#b3e5dc") : 0xb3e5dc;
        }
    }

    public static class ProcessListAdapter extends ArrayAdapter<Process> {

        public ProcessListAdapter(@NonNull Context context, List<Process> processes) {
            super(context, 0, processes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_element_process, parent, false);
            }

            Process p;
            try { p = (Process) getItem(position); }
            catch(Exception e) {return convertView;}

            TextView name = (TextView) convertView.findViewById(R.id.process_name);
            name.setText(p.name);
            TextView cpu = (TextView) convertView.findViewById(R.id.process_cpu);
            cpu.setText(p.CPU+"%");
            TextView ram = (TextView) convertView.findViewById(R.id.process_ram);
            ram.setText(p.RAM+"%");

            return convertView;
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ramGraphView = null;
        cpuGraphView = null;
        ramGraphText = null;
    }
}
