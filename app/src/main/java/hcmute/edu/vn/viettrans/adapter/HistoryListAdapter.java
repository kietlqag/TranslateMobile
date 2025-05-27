package hcmute.edu.vn.viettrans.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.viettrans.R;
import hcmute.edu.vn.viettrans.model.TranslationHistory;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.ViewHolder> {
    private List<TranslationHistory> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TranslationHistory history);
    }

    public HistoryListAdapter(List<TranslationHistory> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translation_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranslationHistory history = historyList.get(position);
        holder.bind(history, listener);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateData(List<TranslationHistory> newHistoryList) {
        this.historyList = newHistoryList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSourceText;
        private TextView tvTranslatedText;
        private TextView tvLanguagePair;
        private TextView tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSourceText = itemView.findViewById(R.id.tvSourceText);
            tvTranslatedText = itemView.findViewById(R.id.tvTranslatedText);
            tvLanguagePair = itemView.findViewById(R.id.tvLanguagePair);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        public void bind(final TranslationHistory history, final OnItemClickListener listener) {
            tvSourceText.setText(history.getSourceText());
            tvTranslatedText.setText(history.getTranslatedText());
            tvLanguagePair.setText(history.getLanguagePair());
            
            // Format timestamp
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(history.getTimestamp());
                tvTimestamp.setText(outputFormat.format(date));
            } catch (Exception e) {
                tvTimestamp.setText(history.getTimestamp());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(history);
                }
            });
        }
    }
} 