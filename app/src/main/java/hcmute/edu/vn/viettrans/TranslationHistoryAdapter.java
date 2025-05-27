package hcmute.edu.vn.viettrans;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.viettrans.model.TranslationHistory;

public class TranslationHistoryAdapter extends RecyclerView.Adapter<TranslationHistoryAdapter.ViewHolder> {

    private List<TranslationHistory> historyList;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(TranslationHistory history);
    }

    public TranslationHistoryAdapter(List<TranslationHistory> historyList,
                                     OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translate_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranslationHistory history = historyList.get(position);
        holder.bind(history, listener);
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView languagePair;
        private TextView timestamp;
        private TextView originalText;
        private TextView translatedText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            languagePair = itemView.findViewById(R.id.languagePair);
            timestamp = itemView.findViewById(R.id.timestamp);
            originalText = itemView.findViewById(R.id.originalText);
            translatedText = itemView.findViewById(R.id.translatedText);
        }

        public void bind(TranslationHistory history, OnHistoryItemClickListener listener) {
            languagePair.setText(history.getLanguagePair());
            timestamp.setText(history.getTimestamp());
            originalText.setText(history.getOriginalText());
            translatedText.setText(history.getTranslatedText());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemClick(history);
                }
            });
        }
    }
}
