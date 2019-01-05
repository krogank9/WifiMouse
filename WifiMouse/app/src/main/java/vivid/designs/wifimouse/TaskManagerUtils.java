package vivid.designs.wifimouse;

public class TaskManagerUtils {
    public static void getTasks(NetworkConnection connection) {
        connection.sendStringOverNetwork("GetTasks", true);
        String tasks = connection.readStringFromNetwork(true);
        TaskManagerFragment.updateProcessList(tasks);
    }

    public static void getRamUsage(NetworkConnection connection) {
        connection.sendStringOverNetwork("GetRamUsage", true);
        String ramUsageBytes = connection.readStringFromNetwork(true);
        TaskManagerFragment.addRamUsage(ramUsageBytes);
    }

    public static void getCpuUsage(NetworkConnection connection) {
        connection.sendStringOverNetwork("GetCpuUsage", true);
        String cpuCoreUsagePercents = connection.readStringFromNetwork(true);
        TaskManagerFragment.addCpuUsages(cpuCoreUsagePercents);
    }
}
