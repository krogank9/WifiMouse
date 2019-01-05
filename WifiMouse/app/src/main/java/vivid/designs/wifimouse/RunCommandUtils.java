package vivid.designs.wifimouse;

import java.util.ArrayList;
import java.util.Arrays;

public class RunCommandUtils {
    public static String lastCommandOutput = "";
    public static ArrayList<String> commandCompleteSuggestions = new ArrayList<>();
    public static int suggestionsId = 0;
    public static int outputId = 0;
    public static void runCommandForResult(NetworkConnection connection, String message) {
        connection.sendStringOverNetwork(message, true);
        lastCommandOutput = connection.readStringFromNetwork(true);
        outputId++;
    }
    public static void getCommandCompleteSuggestions(NetworkConnection connection, String message) {
        connection.sendStringOverNetwork(message, true);
        String[] sugArr = connection.readStringFromNetwork(true).split("\n");
        commandCompleteSuggestions.clear();
        commandCompleteSuggestions.addAll(Arrays.asList(sugArr));
        suggestionsId++;
    }

    public static void callHelperFunc(NetworkConnection connection, String message) {
        if(message.startsWith("Command Run "))
            runCommandForResult(connection, message);
        else if(message.startsWith("Command Suggest "))
            getCommandCompleteSuggestions(connection, message);
    }
}
