package main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ExpenseTrackerController {

    @FXML private DatePicker datePicker;
    @FXML private TextField itemField;
    @FXML private TextField amountField;
    @FXML private TableView<Expense> tableView;
    @FXML private TableColumn<Expense, LocalDate> dateColumn;
    @FXML private TableColumn<Expense, String> itemColumn;
    @FXML private TableColumn<Expense, String> amountColumn;
    @FXML private TableColumn<Expense, Void> actionColumn;
    @FXML private Label totalLabel;
    @FXML private DatePicker filterDatePicker;

    private ObservableList<Expense> allExpenses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            DatabaseHelper.initDatabase();
            
            // Set up cell value factories
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
            itemColumn.setCellValueFactory(new PropertyValueFactory<>("item"));
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
            
            // Load initial data
            allExpenses.addAll(DatabaseHelper.getAllExpenses());
            tableView.setItems(allExpenses);
            updateTotal();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error initializing application: " + e.getMessage(), "Error", Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleDeleteSelected() {
        Expense selectedExpense = tableView.getSelectionModel().getSelectedItem();
        if (selectedExpense != null) {
            handleDeleteExpense(selectedExpense);
        } else {
            showAlert("Please select an expense to delete.", "No Selection", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleAddExpense() {
        LocalDate date = datePicker.getValue();
        String item = itemField.getText();
        String amountText = amountField.getText();

        if (date == null || item.isEmpty() || amountText.isEmpty()) {
            showAlert("All fields are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            Expense expense = new Expense(date, item, amount);
            DatabaseHelper.insertExpense(expense);
            allExpenses.add(expense);
            clearInputs();
            updateTotal();
        } catch (NumberFormatException e) {
            showAlert("Amount must be a number.");
        }
    }

    @FXML
    private void handleFilterByDate() {
        LocalDate filterDate = filterDatePicker.getValue();
        if (filterDate == null) return;
        List<Expense> filteredList = DatabaseHelper.getExpensesByDate(filterDate);
        ObservableList<Expense> filtered = FXCollections.observableArrayList(filteredList);
        tableView.setItems(filtered);
        updateTotal(filtered);
    }

    @FXML
    private void handleClearFilter() {
        filterDatePicker.setValue(null);
        tableView.setItems(allExpenses);
        updateTotal();
    }
    
    @FXML
    private void handleExportToTxt() {
        try {
            // Create a file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Expenses Report");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            fileChooser.setInitialFileName("expenses_" + java.time.LocalDate.now() + ".txt");
            
            // Show save file dialog
            Stage stage = (Stage) tableView.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                // Ensure the file has .txt extension
                String filePath = file.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".txt")) {
                    file = new File(filePath + ".txt");
                }
                
                // Write expenses to file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    // Write header
                    writer.write(String.format("%-12s | %-30s | %-10s%n", "Date", "Item", "Amount (₹)"));
                    writer.write("-".repeat(60) + "\n");
                    
                    // Write each expense
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
                    
                    double total = 0;
                    for (Expense expense : tableView.getItems()) {
                        String dateStr = expense.getDate().format(dateFormatter);
                        String amountStr = amountFormat.format(expense.getAmount());
                        writer.write(String.format("%-12s | %-30s | %10s%n", 
                            dateStr, expense.getItem(), amountStr));
                        total += expense.getAmount();
                    }
                    
                    // Write total
                    writer.write("-".repeat(60) + "\n");
                    writer.write(String.format("%-45s | %10s%n", "TOTAL:", amountFormat.format(total)));
                    
                    showAlert("Expenses exported successfully to:\n" + file.getAbsolutePath(), 
                            "Export Successful", Alert.AlertType.INFORMATION);
                } catch (IOException e) {
                    showAlert("Error writing to file: " + e.getMessage(), 
                            "Export Error", Alert.AlertType.ERROR);
                }
            }
        } catch (Exception e) {
            showAlert("Error during export: " + e.getMessage(), 
                    "Export Error", Alert.AlertType.ERROR);
        }
    }

    private void updateTotal() {
        updateTotal(tableView.getItems());
    }

    private void updateTotal(ObservableList<Expense> list) {
        double total = list.stream().mapToDouble(Expense::getAmount).sum();
        totalLabel.setText(String.format("Total: ₹%.2f", total));
    }

    private void clearInputs() {
        datePicker.setValue(null);
        itemField.clear();
        amountField.clear();
    }

    private void handleDeleteExpense(Expense expense) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Expense");
        alert.setContentText("Are you sure you want to delete this expense?\n" +
                          "Item: " + expense.getItem() + "\n" +
                          "Amount: ₹" + expense.getAmount());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (DatabaseHelper.deleteExpense(expense.getId())) {
                allExpenses.remove(expense);
                updateTotal();
                showAlert("Expense deleted successfully!", "Success", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Failed to delete expense.", "Error", Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String msg) {
        showAlert(msg, "Input Error", Alert.AlertType.WARNING);
    }
    
    private void showAlert(String msg, String title, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}