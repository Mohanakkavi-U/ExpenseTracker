package main;

import java.time.LocalDate;

public class Expense {
    private int id;
    private LocalDate date;
    private String item;
    private double amount;

    // For creating new expenses (ID will be set by database)
    public Expense(LocalDate date, String item, double amount) {
        this(-1, date, item, amount);
    }

    // For loading existing expenses from database
    public Expense(int id, LocalDate date, String item, double amount) {
        this.id = id;
        this.date = date;
        this.item = item;
        this.amount = amount;
    }

    public int getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getItem() { return item; }
    public double getAmount() { return amount; }
}