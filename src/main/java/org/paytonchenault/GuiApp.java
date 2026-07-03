package org.paytonchenault;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class GuiApp extends App {
    private JFrame frame;
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JTextField taskNameField;
    private JComboBox<String> stateComboBox;

    public GuiApp() {
        super();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel.");
        }

        createAndShowGUI();
        refreshTaskTable();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Task Manager Pro");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout(15, 15));

        ((JPanel)frame.getContentPane()).setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("Task Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setBorder(new EmptyBorder(10, 0, 15, 0));
        frame.add(titleLabel, BorderLayout.NORTH);

        String[] columnNames = {"Task Name", "Current Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        taskTable = new JTable(tableModel);

        taskTable.setRowHeight(35);
        taskTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        taskTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        taskTable.getTableHeader().setBackground(new Color(240, 240, 240));
        taskTable.setSelectionBackground(new Color(173, 216, 230)); // Light blue selection
        taskTable.setSelectionForeground(Color.BLACK);
        taskTable.setShowVerticalLines(false);

        JScrollPane scrollPane = new JScrollPane(taskTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.setBorder(new EmptyBorder(10, 0, 5, 0));

        inputPanel.add(new JLabel("Task Name:"));
        taskNameField = new JTextField(15);
        taskNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(taskNameField);

        inputPanel.add(new JLabel("State:"));
        String[] states = {"TODO", "IN_PROGRESS", "DONE"};
        stateComboBox = new JComboBox<>(states);
        stateComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(stateComboBox);

        JButton addButton = new JButton("Create Task");
        styleButton(addButton, new Color(46, 204, 113), Color.WHITE); // Green
        addButton.addActionListener(e -> handleCreateTask());
        inputPanel.add(addButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton btnTodo = new JButton("Move to To-Do");
        JButton btnProgress = new JButton("Move to In Progress");
        JButton btnDone = new JButton("Move to Done");
        JButton btnDelete = new JButton("Delete Task");

        styleButton(btnTodo, new Color(52, 152, 219), Color.WHITE); // Blue
        styleButton(btnProgress, new Color(241, 196, 15), Color.BLACK); // Yellow
        styleButton(btnDone, new Color(46, 204, 113), Color.WHITE); // Green
        styleButton(btnDelete, new Color(231, 76, 60), Color.WHITE); // Red

        btnTodo.addActionListener(e -> handleUpdateState("TODO"));
        btnProgress.addActionListener(e -> handleUpdateState("IN_PROGRESS"));
        btnDone.addActionListener(e -> handleUpdateState("DONE"));
        btnDelete.addActionListener(e -> handleDeleteTask());

        actionPanel.add(btnTodo);
        actionPanel.add(btnProgress);
        actionPanel.add(btnDone);
        actionPanel.add(btnDelete);

        bottomPanel.add(inputPanel);
        bottomPanel.add(actionPanel);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    private void styleButton(JButton button, Color bg, Color fg) {
        button.setBackground(bg);
        button.setForeground(fg);

        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }


    private void handleCreateTask() {
        String name = taskNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Task name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedState = (String) stateComboBox.getSelectedItem();
        TaskState state = TaskState.valueOf(selectedState);

        createTask(name, state);
        taskNameField.setText("");
        refreshTaskTable();
    }

    private void handleUpdateState(String targetState) {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select a task from the table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String taskName = (String) tableModel.getValueAt(selectedRow, 0);

        switch (targetState) {
            case "TODO":
                updateTaskToTodo(taskName);
                break;
            case "IN_PROGRESS":
                updateTaskToInProgress(taskName);
                break;
            case "DONE":
                updateTaskToDone(taskName);
                break;
        }
        refreshTaskTable();
    }

    private void handleDeleteTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        String taskName = (String) tableModel.getValueAt(selectedRow, 0);
        deleteTask(taskName);
        refreshTaskTable();
    }

    private void refreshTaskTable() {
        tableModel.setRowCount(0);

        ArrayList<Task> tasks = getTasks();

        System.out.println("Debug: refreshTaskTable called. Tasks found in DB: " + (tasks == null ? 0 : tasks.size()));

        if (tasks != null) {
            for (Task task : tasks) {
                tableModel.addRow(new Object[]{task.getName(), task.getState()});
            }
        }

        tableModel.fireTableDataChanged();
    }

    @Override
    public void terminate() {
        JOptionPane.showMessageDialog(frame, "Database error. Closing application.", "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}