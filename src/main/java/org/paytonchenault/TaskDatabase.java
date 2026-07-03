package org.paytonchenault;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public class TaskDatabase {
    private static final String TASK_DB_FILE_PATH = "tasks.db";
    private static final String TASK_DB_NAME_KEY = "TSK_NAME";
    private static final String TASK_DB_STATUS_KEY = "TSK_STATUS";
    private static final String TASK_DB_NAME = "TASKS";
    private static final ArrayList<Task> TASK_LIST = new ArrayList<>();
    private static Consumer<Boolean> databaseTerminationHandler;
    private static Connection taskDbConnection;

    public TaskDatabase(Consumer<Boolean> onDatabaseTerminate) {
        Optional<Connection> possibleTaskDb = initializeDatabase();
        databaseTerminationHandler = onDatabaseTerminate;
        possibleTaskDb.ifPresentOrElse(c -> taskDbConnection = c, this::terminateApp);
        populateTaskListInMem();
    }

    public TaskDatabase() {
        this(b -> System.exit(1));
    }

    private void populateTaskListInMem() {
        TASK_LIST.clear();

        String sql = String.format("SELECT %s, %s FROM %s", TASK_DB_NAME_KEY, TASK_DB_STATUS_KEY, TASK_DB_NAME);

        try(Statement stmt = taskDbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                TASK_LIST.add(new Task(rs.getString(TASK_DB_NAME_KEY), TaskState.valueOf(rs.getString(TASK_DB_STATUS_KEY))));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            terminateApp();
        }
    }

    private Optional<Connection> initializeDatabase() {
        System.out.printf("Creating New Database? : %s%n", !(new File(TASK_DB_FILE_PATH).exists()));
        Connection connection = null;

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", TASK_DB_FILE_PATH));
            connection.setAutoCommit(false);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage());
            return Optional.empty();
        }

        try {
            Statement statement = connection.createStatement();
            String sql = String.format(
                    "CREATE TABLE IF NOT EXISTS %s ("
                            + " ID INTEGER PRIMARY KEY,"
                            + " %s text NOT NULL,"
                            + " %s text NOT NULL"
                            + ");"
                    , TASK_DB_NAME
                    , TASK_DB_NAME_KEY
                    , TASK_DB_STATUS_KEY
            );

            statement.execute(sql);
            connection.commit();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
        System.out.println("Successfully Opened DB");
        return Optional.of(connection);
    }

    public boolean createTask(String taskName) {
        return createTask(taskName, TaskState.TODO);
    }

    public boolean createTask(String taskName, TaskState initialState) {
        if(checkIfTaskExists(taskName)) {
            System.out.println("Task Already Created");
            return true;
        }

        try {
            String sql = String.format("INSERT OR IGNORE INTO %s(%s, %s) VALUES(?,?)", TASK_DB_NAME, TASK_DB_NAME_KEY, TASK_DB_STATUS_KEY);
            PreparedStatement pstmt = taskDbConnection.prepareStatement(sql);

            pstmt.setString(1, taskName);
            pstmt.setString(2, initialState.toString());
            pstmt.executeUpdate();

            taskDbConnection.commit();

            populateTaskListInMem();

            System.out.printf("Created New Task %s%n", taskName);
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            terminateApp();
        }
        return false;
    }

    public boolean updateTask(String taskName, TaskState newState) {

        if(!checkIfTaskExists(taskName)) {
            System.out.printf("There is no task by the name %s%n", taskName);
            return false;
        }

        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ?", TASK_DB_NAME, TASK_DB_STATUS_KEY, TASK_DB_NAME_KEY);
        try (PreparedStatement pstmt = taskDbConnection.prepareStatement(sql)) {
            pstmt.setString(1, newState.toString());
            pstmt.setString(2, taskName);
            pstmt.executeUpdate();

            taskDbConnection.commit();

            populateTaskListInMem();

            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            System.err.printf("ATTEMPTED SQL QUERY -> %s%n", sql);
            terminateApp();
        }

        return false;
    }

    public boolean setTaskToToDo(String taskName) {
        return updateTask(taskName, TaskState.TODO);
    }

    public boolean setTaskToInProgress(String taskName) {
        return updateTask(taskName, TaskState.IN_PROGRESS);
    }

    public boolean setTaskToDone(String taskName) {
        return updateTask(taskName, TaskState.DONE);
    }

    public boolean deleteTask(String taskName) {
        if(!checkIfTaskExists(taskName)) {
            System.err.printf("Task with name %s does not exist to delete%n", taskName);
            return false;
        }

        String sql = String.format("DELETE FROM %s WHERE %s = ?", TASK_DB_NAME, TASK_DB_NAME_KEY);

        try(PreparedStatement pstmt = taskDbConnection.prepareStatement(sql)) {
            pstmt.setString(1, taskName);
            pstmt.executeUpdate();

            taskDbConnection.commit();

            populateTaskListInMem();

            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            terminateApp();
        }

        return false;
    }

    private boolean checkIfTaskExists(String taskName) {
        boolean taskExists = false;
        for (Task t : TASK_LIST) {
            if (t.getName().equals(taskName)) {
                taskExists = true;
                break;
            }
        }
        return taskExists;
    }

    public ArrayList<Task> getTasks() {
        return TASK_LIST;
    }

    private void terminateApp() {
        boolean freedDatabase = false;
        if(taskDbConnection != null) {
            try {
                taskDbConnection.rollback();
                taskDbConnection.close();
                freedDatabase = true;
            } catch (SQLException e) {
                System.err.println("ISSUE HANDLING TASK DATABASE: " + e.getMessage());
            }
        }
        databaseTerminationHandler.accept(freedDatabase);
    }
}