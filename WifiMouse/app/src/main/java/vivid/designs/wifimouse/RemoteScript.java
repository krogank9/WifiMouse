package vivid.designs.wifimouse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class RemoteScript {

    /*
     *
     * Parser code
     *
     */

    Context ctx;

    String[] layoutModes = {"buttons"};
    String[][] layoutDefaults = { {"text", "icon", "onclick", "confirm", "bg", "fg"} };
    String[] getLayoutDefaults(String type) {
        for(int i=0; i<layoutModes.length; i++)
            if(layoutModes[i].equals(type))
                return layoutDefaults[i];
        return new String[0];
    }

    private static String escapeStr(String str) {
        str = replaceNonEscaped(str, "=", "\\=");
        str = replaceNonEscaped(str, ",", "\\,");
        str = replaceNonEscaped(str, "<", "\\<");
        str = replaceNonEscaped(str, ">", "\\>");
        return str;
    }
    private static String unescapeStr(String str) {
        return str.replace("\\=","=").replace("\\,",",").replace("\\<","<").replace("\\>",">");
    }

    private static String[] splitNonEscaped(String toSplit, String regex) {
        String[] split = toSplit.split(regex);
        for(int i=0; i<(split.length-1); i++) {
            if(split[i].endsWith("\\")) {
                // if was split at an escaped character, remake split array
                // with the two neighboring strings joined
                String[] copy = new String[split.length - 1];
                for(int j=0; j<copy.length; j++) {
                    if(j < i)
                        copy[j] = split[j];
                    else if(j == i)
                        copy[j] = split[j] + split[j+1];
                    else if(j > i)
                        copy[j] = split[j+1];
                }
                split = copy;
                i = -1; // restart loop after remaking array
            }
        }
        return split;
    }

    public static Hashtable<String, String> createHashtableFromProps(String props_str) {
        return createHashtableFromProps(props_str, new String[]{});
    }
    public static Hashtable<String, String> createHashtableFromProps(String props_str, String[] defaults) {
        Hashtable<String, String> hashtable = new Hashtable<>();
        String[] props = splitNonEscaped(props_str, ",");
        for(int i=0; i<props.length; i++) {
            String[] a = splitNonEscaped(props[i], "=");
            if(a.length == 2) {
                // set property by name
                hashtable.put(a[0].trim(), unescapeStr(a[1]).trim());
            }
            else if(a.length == 1) {
                // if property name not specified, use next unset default property
                for(int j=0; j<defaults.length; j++) {
                    String propName = defaults[j];
                    if(!hashtable.containsKey(propName) || j==defaults.length-1) {
                        hashtable.put(propName, a[0].trim());
                        break;
                    }
                }
            }
        }
        return hashtable;
    }

    RemoteScript(Context c, String parse) {
        this.ctx = c;
        String[] lines = parse.split("\n");
        String curMode = "buttons";
        String curPlatform = "all";

        for(int i=0; i<lines.length; i++) {
            String line = lines[i];

            if(line.trim().length() == 0) {
                Hashtable<String, String> newrow = new Hashtable<>();
                newrow.put("type", "newrow");
                layouts.add(newrow);
            }
            else if(line.charAt(0) == '#') {
                continue;
            }
            else if(line.trim().endsWith(":")) {
                curPlatform = line.startsWith("linux") ? "linux"
                            : line.startsWith("windows") ? "windows"
                            : line.startsWith("mac") ? "mac" : "any";
                String[] modes = line.split(":");
                curMode = modes[modes.length - 1];
            }
            else if(curMode.equals("functions")) {
                Hashtable<String, String> func = createHashtableFromProps(line, new String[]{"name", curPlatform});
                if(userFunctions.containsKey(func.get("name")))
                    userFunctions.get(func.get("name")).putAll(func);
                else
                    userFunctions.put(func.get("name"), func);
            }
            else {
                Hashtable<String, String> layout_entry = createHashtableFromProps(line, getLayoutDefaults(curMode));
                layout_entry.put("type", curMode);
                layouts.add(layout_entry);
            }
        }
    }

    ArrayList<Hashtable<String, String>> layouts = new ArrayList<>();
    Hashtable<String, Hashtable<String, String>> userFunctions = new Hashtable<>();

    /*
     *
     * Functions code
     *
     */

    public static abstract class FunctionCallback {
        abstract void functionCallback(String result);
    }

    private static int nonEscapedIndexOf(char index, String in) {
        for(int i=0; i<in.length(); i++)
            if((i == 0 || in.charAt(i-1) != '\\') && in.charAt(i) == index)
                return i;
        return -1;
    }

    // split function into String[]{"name", "args"}
    private String[] splitFunc(String funcWithArgs) {
        if(funcWithArgs.startsWith("<") && funcWithArgs.endsWith(">"))
            funcWithArgs = funcWithArgs.substring(1,funcWithArgs.length()-1);
        int i=0;
        for(; i<funcWithArgs.length(); i++) {
            char c = funcWithArgs.charAt(i);
            if(c == ',' || c == ' ' || c == '>')
                break;
            if(i==funcWithArgs.length()-1)
                return new String[] {funcWithArgs, ""};
        }
        return new String[] {funcWithArgs.substring(0, i), funcWithArgs.substring(i+1).trim()};
    }

    public boolean needsEval(String func) {
        return splitNonEscaped(func, "<").length > 1;
    }
    public boolean needsEval(String[] funcs) {
        for(int i=0; i<funcs.length; i++)
            if(needsEval(funcs[i]))
                return true;
        return false;
    }

    // split string into top level functions
    public String[] splitStringFunctions(String str) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("");
        int nest_count = 0;
        char prev_c = ' ';
        for(int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if(c == '<' && prev_c != '\\') {
                if(nest_count == 0)
                    parts.add("");
                nest_count++;
            }
            int last_elem = parts.size() - 1;
            parts.set(last_elem, parts.get(last_elem) + c);
            if(c == '>' && prev_c != '\\') {
                nest_count--;
                if(nest_count == 0)
                    parts.add("");
            }
            prev_c = c;
        }
        // discard empty strs
        parts.removeAll(Collections.singleton(""));
        return (String[]) parts.toArray(new String[parts.size()]);
    }

    private static String replaceNonEscaped(String str, String toReplace, String replaceWith) {
        if(str.length() == 0) return "";
        String[] parts = splitNonEscaped(str, toReplace);

        ArrayList<String> remadeArr = new ArrayList<>();
        for(int i=0; i<parts.length; i++) {
            remadeArr.add(parts[i]);
            if(i < parts.length-1)
                remadeArr.add(replaceWith);
        }

        String remadeStr = "";
        for(int i=0; i<remadeArr.size(); i++)
            remadeStr += remadeArr.get(i);

        return remadeStr;
    }

    private void evalPrompt(String promptType, String promptTitle, final FunctionCallback callback) {
        final EditText promptEditText = new EditText(ctx);
        if(promptType.equals("NumPrompt"))
            promptEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LinearLayout editTextContainer = new LinearLayout(ctx);
        editTextContainer.setLayoutParams(params);
        promptEditText.setLayoutParams(params);

        int pad_top = ctx.getResources().getDimensionPixelSize(R.dimen.prompt_padding_top);
        int pad_side = ctx.getResources().getDimensionPixelSize(R.dimen.prompt_padding_side);
        editTextContainer.setPadding(pad_side, pad_top, pad_side, 0);
        editTextContainer.addView(promptEditText);
        Dialog promptDialog = new AlertDialog.Builder(ctx)
                .setTitle(promptTitle)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String valEntered = escapeStr(promptEditText.getText().toString());
                                callback.functionCallback(valEntered);
                            }
                        }
                )
                .setView(editTextContainer)
                .setNegativeButton("Cancel", null)
                .create();
        promptDialog.show();
        promptEditText.post(new Runnable() {
            @Override
            public void run() {
                promptEditText.setFocusableInTouchMode(true);
                promptEditText.requestFocus();
                final InputMethodManager inputMethodManager = (InputMethodManager) ctx
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(promptEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    // Evaluate a function that is guaranteed to have no functions left to eval inside it.
    private void evalExpandedFunction(String[] nameAndArgs, final FunctionCallback callback) {
        final String platform = WifiMouseApplication.networkConnection.serverOs;
        final String name = nameAndArgs[0];
        final String all_args = nameAndArgs[1];
        final String[] args_arr = splitNonEscaped(nameAndArgs[1], ",");
        final Hashtable<String, String> args_table = createHashtableFromProps(all_args, new String[0]);

        if(userFunctions.containsKey(name)
           && ( userFunctions.get(name).containsKey(platform)
                || userFunctions.get(name).containsKey("any")) ) {
            Hashtable<String, String> userFunc = userFunctions.get(name);
            String function_contents = "";
            if(userFunc.containsKey(platform))
                function_contents = userFunc.get(platform);
            else if(userFunc.containsKey("any"))
                function_contents = userFunc.get("any");
            function_contents = replaceNonEscaped(function_contents, "<$>", all_args);
            // need to try recurse again if evaluated a user function
            evalFunctions(function_contents, callback);
        }
        else if(name.equals("RunCommand")) {
            WifiMouseApplication.networkConnection.sendMessageForResult("Command Run " + all_args, new FunctionCallback() {
                @Override
                void functionCallback(String result) {
                    callback.functionCallback(result);
                }
            });
        }
        else if(name.equals("NumPrompt")) {
            evalPrompt("NumPrompt", all_args, callback);
        }
        else if(name.equals("TextPrompt")) {
            evalPrompt("TextPrompt", all_args, callback);
        }
        else if(name.equals("Power")) {
            WifiMouseApplication.networkConnection.sendMessage("Power "+all_args);
            callback.functionCallback("");
        }
        else if(name.equals("KeyTap")) {
            WifiMouseApplication.networkConnection.sendMessage("SpecialKey Tap " + all_args);
            callback.functionCallback("");
        }
        else if(name.equals("KeyDown")) {
            WifiMouseApplication.networkConnection.sendMessage("SpecialKey Down "+all_args);
            callback.functionCallback("");
        }
        else if(name.equals("KeyUp")) {
            WifiMouseApplication.networkConnection.sendMessage("SpecialKey Up "+all_args);
            callback.functionCallback("");
        }
        else if(name.equals("KeyCombo")) {
            WifiMouseApplication.networkConnection.sendMessage("SpecialKeyCombo "+all_args);
            callback.functionCallback("");
        }
        else if(name.equals("TypeString")) {
            WifiMouseApplication.networkConnection.sendMessage("TypeString "+all_args);
            callback.functionCallback("");
        }
        else
            callback.functionCallback("");
    }

    // Evaluate single function with args etc
    private void evalFunction(String function, final FunctionCallback callback) {
        if(function == null) function = "";
        if(function.startsWith("<")) function = function.substring(1);
        if(function.endsWith(">")) function = function.substring(0, function.length()-1);
        final String[] nameAndArgs = splitFunc(function);

        // before evaluating the function, evaluate any functions inside of it
        evalFunctions(nameAndArgs[1], new FunctionCallback() {
            @Override
            void functionCallback(String result) {
                evalExpandedFunction(new String[]{nameAndArgs[0], result}, callback);
            }
        });
    }

    // Recursively evaluate multiple functions
    public void evalFunctions(String function, FunctionCallback callback) {
        if(function == null) function = "";
        if(callback == null) callback = new FunctionCallback() { @Override void functionCallback(String result) {} };
        evalFunctions(splitStringFunctions(function), callback);
    }
    private void evalFunctions(final String[] functions, final FunctionCallback callback) {
        String platform = WifiMouseApplication.networkConnection.serverOs;

        // loop through and don't call final callback until all evaluated
        for(int i=0; i<functions.length; i++) {
            if(needsEval(functions[i])) {
                final int index = i;
                evalFunction(functions[i], new FunctionCallback() {
                    @Override
                    void functionCallback(String result) {
                        functions[index] = result;
                        evalFunctions(functions, callback);
                    }
                });
                return;
            }
        }
        // if none of the functions looped through had to eval, callback with final result
        String totalResult = "";
        for(int j=0; j<functions.length; j++)
            totalResult += functions[j];
        callback.functionCallback(totalResult);
    }

    /*
     *
     * Layout inflater code
     *
     */

    interface ScriptableView {
        void updateProps();
    }

    public boolean stopLoop = false;
    public static RemoteScript lastRemoteScript = null;

    public LinearLayout makeLayout() {
        final LinearLayout baseView = new LinearLayout(ctx);
        ScriptableButton.lastMargin = 3; // default to 3 margin for each remote

        baseView.setOrientation(LinearLayout.VERTICAL);
        int padding_in_dp = 3;
        final float scale = ctx.getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
        baseView.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);

        final ArrayList<ScriptableView> scriptableViews = new ArrayList<>();
        LinearLayout curRow = null;
        boolean makeNewRow = true;
        for(int i=0; i<layouts.size(); i++) {
            Hashtable<String, String> curLayoutItem = layouts.get(i);
            if("newrow".equals(curLayoutItem.get("type"))) {
                makeNewRow = true;
                continue;
            }

            // create the new row and look ahead to choose a height: wrap_content or a specified weight
            if(makeNewRow) {
                makeNewRow = false;
                curRow = new LinearLayout(ctx);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                boolean paramsChanged = false;
                for(int j=i+1; j<layouts.size(); j++) {
                    Hashtable<String, String> obj = layouts.get(j);
                    if("newrow".equals(obj.get("type")))
                        break;
                    if(obj.containsKey("height")) {
                        try {
                            int weight = Integer.parseInt(obj.get("height"));
                            if(weight >= 0) {
                                params.weight = weight;
                                paramsChanged = true;
                                break;
                            }
                        } catch (NumberFormatException ex) {}
                        if("wrap_content".equals(obj.get("height"))) {
                            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            paramsChanged = true;
                        }
                        break;
                    }
                }
                if(!paramsChanged)
                    params.weight = 1;
                curRow.setLayoutParams(params);
                baseView.addView(curRow);
            }

            if("buttons".equals(curLayoutItem.get("type"))) {
                ScriptableButton b = new ScriptableButton(ctx, curLayoutItem, this);
                curRow.addView(b);
                scriptableViews.add(b);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                params.weight = 1;
                b.setLayoutParams(params);
            }
        }

        if(lastRemoteScript != null)
            lastRemoteScript.stopLoop = true;
        lastRemoteScript = this;
        baseView.post(new Runnable() {
            @Override
            public void run() {
                if(baseView == null || stopLoop)
                    return;
                for(int i=0; i<scriptableViews.size(); i++)
                    scriptableViews.get(i).updateProps();
                baseView.postDelayed(this, 1000);
            }
        });
        return baseView;
    }
}
