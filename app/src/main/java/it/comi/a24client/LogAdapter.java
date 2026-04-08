package it.comi.a24client;

import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    private final List<LogEntry> entries = new ArrayList<>();

    public void add(LogEntry entry) {
        entries.add(entry);
        notifyItemInserted(entries.size() - 1);
    }

    public void clear() {
        int size = entries.size();
        entries.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(11.5f);
        tv.setPadding(4, 1, 4, 1);
        tv.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LogEntry entry = entries.get(position);
        holder.textView.setText(entry.timestamp + "  " + entry.text);
        holder.textView.setTextColor(LogEntry.colorFor(entry.type));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        ViewHolder(TextView tv) {
            super(tv);
            textView = tv;
        }
    }
}
