package org.paytonchenault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public abstract class App implements CloseableApp {
    private final TaskDatabase taskDb;

    public App() {
        taskDb = new TaskDatabase();
    }

    protected void createTask(String taskName, TaskState initialState) {
        handleDatabaseAction(() -> taskDb.createTask(taskName, initialState));
    }

    protected void createTask(String taskName) {
        handleDatabaseAction(() -> taskDb.createTask(taskName));
    }

    protected void updateTask(String taskName, TaskState newState) {
        handleDatabaseAction(() -> taskDb.updateTask(taskName, newState));
    }

    protected void updateTaskToTodo(String taskName) {
        handleDatabaseAction(() -> taskDb.setTaskToToDo(taskName));
    }

    protected void updateTaskToInProgress(String taskName) {
        handleDatabaseAction(() -> taskDb.setTaskToInProgress(taskName));
    }

    protected void updateTaskToDone(String taskName) {
        handleDatabaseAction(() -> taskDb.setTaskToDone(taskName));
    }

    protected void deleteTask(String taskName) {
        handleDatabaseAction(() -> taskDb.deleteTask(taskName));
    }

    protected ArrayList<Task> getTasks() {
        return taskDb.getTasks();
    }


    private void handleDatabaseAction(Supplier<Boolean> dbQueryAction) {
        if(dbQueryAction == null) {
            System.err.println("No DB Query Given");
            return;
        }
        if(!dbQueryAction.get()) {
            terminate();
        }
    }


 }
