package com.example.coupleexpensetracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coupleexpensetracker.R;
import com.example.coupleexpensetracker.models.Expense;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseVH> {

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    private final List<Expense> expenseList;
    private final OnExpenseClickListener listener;

    public ExpenseAdapter(List<Expense> expenseList, OnExpenseClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_expense, parent, false);
        return new ExpenseVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseVH holder, int position) {
        Expense e = expenseList.get(position);

        holder.tvCategory.setText(e.category);
        holder.tvAmount.setText("₹ " + e.amount);
        holder.tvDate.setText(e.date);

        holder.itemView.setOnClickListener(v -> listener.onExpenseClick(e));
    }

    @Override
    public int getItemCount() {
        return expenseList == null ? 0 : expenseList.size();
    }

    static class ExpenseVH extends RecyclerView.ViewHolder {

        TextView tvCategory, tvAmount, tvDate;

        ExpenseVH(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
