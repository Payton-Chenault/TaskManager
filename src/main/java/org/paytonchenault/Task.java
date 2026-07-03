package org.paytonchenault;

public class Task {
    private final String taskName;
    private final TaskState taskState;

    public Task(String taskName, TaskState taskState) {
        this.taskName = taskName;
        this.taskState = taskState;
    }

    public String getName() {
        return taskName;
    }

    public TaskState getState() {
        return taskState;
    }
}
