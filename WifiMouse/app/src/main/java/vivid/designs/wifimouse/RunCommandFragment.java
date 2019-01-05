package vivid.designs.wifimouse;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class RunCommandFragment extends Fragment {

    AutoCompleteTextView commandEntry;
    Button sendButton;
    TextView commandOutput;
    ArrayAdapter<String> suggestAdapter;

    public RunCommandFragment() {}

    private ArrayList<String> cmdSuggestions = new ArrayList<>();
    int suggestionsId = 0;
    int outputId = 0;

    private void updateUi() {
        if(outputId != RunCommandUtils.outputId) {
            commandOutput.setText(RunCommandUtils.lastCommandOutput);
            outputId = RunCommandUtils.outputId;
        }
        if(suggestionsId != RunCommandUtils.suggestionsId) {
            suggestAdapter.clear();
            suggestAdapter.addAll(RunCommandUtils.commandCompleteSuggestions);
            suggestAdapter.notifyDataSetChanged();
            suggestionsId = RunCommandUtils.suggestionsId;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_run_command, container, false);
        RunCommandUtils.lastCommandOutput = "";
        commandEntry = (AutoCompleteTextView) inflated.findViewById(R.id.run_command_entry);
        commandOutput = (TextView) inflated.findViewById(R.id.run_command_output);
        sendButton = (Button) inflated.findViewById(R.id.run_command_send);

        suggestAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_element_cmdsuggest, cmdSuggestions);
        commandEntry.setAdapter(suggestAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiMouseApplication.networkConnection.sendMessage("Command Run "+commandEntry.getText());
                commandEntry.setText("");
            }
        });

        commandEntry.post(new Runnable() {
            @Override
            public void run() {
                if(commandEntry == null)
                    return;
                else
                    updateUi();
                commandEntry.postDelayed(this, 300);
            }
        });

        commandEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                WifiMouseApplication.networkConnection.sendMessage("Command Suggest "+commandEntry.getText());
            }
        });

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        return inflated;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        commandEntry = null;
    }
}
