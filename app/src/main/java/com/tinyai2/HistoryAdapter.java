package com.tinyai2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Conversation> conversations;
    private final Context context;
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public HistoryAdapter(Context context, OnConversationClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    public void clearConversations() {
        if (conversations != null) {
            conversations.clear();
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (conversations != null && position < conversations.size()) {
            holder.bind(conversations.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView previewText;
        private final TextView timeText;
        private final TextView messageCountText;
        private final View conversationCard;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            conversationCard = itemView;
            titleText = itemView.findViewById(R.id.conversation_title);
            previewText = itemView.findViewById(R.id.conversation_preview);
            timeText = itemView.findViewById(R.id.conversation_time);
            messageCountText = itemView.findViewById(R.id.message_count);
        }

        public void bind(Conversation conversation) {
            titleText.setText(conversation.getTitle());

            String preview = conversation.getPreview();
            if (preview.length() > 50) {
                preview = preview.substring(0, 47) + "...";
            }
            previewText.setText(preview);

            timeText.setText(conversation.getFormattedTime());
            messageCountText.setText(context.getString(
                    R.string.message_count, conversation.getMessageCount()));

            conversationCard.setOnClickListener(v -> v.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                        if (listener != null) {
                            listener.onConversationClick(conversation);
                        }
                    })
                    .start());

            conversationCard.setOnLongClickListener(v -> {
                showConversationOptions(conversation);
                return true;
            });
        }

        private void showConversationOptions(Conversation conversation) {
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(context);
            builder.setTitle(R.string.conversation_options);

            String[] options = {
                    context.getString(R.string.share),
                    context.getString(R.string.export),
                    context.getString(R.string.delete)
            };

            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        shareConversation(conversation);
                        break;
                    case 1:
                        exportConversation(conversation);
                        break;
                    case 2:
                        deleteConversation(conversation);
                        break;
                }
            });

            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.show();
        }

        private void shareConversation(Conversation conversation) {
            ClipboardUtils.shareText(context, buildConversationText(conversation));
        }

        private void exportConversation(Conversation conversation) {
            ClipboardUtils.shareText(context, buildConversationText(conversation));
            Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show();
        }

        private String buildConversationText(Conversation conversation) {
            StringBuilder builder = new StringBuilder();
            builder.append(conversation.getTitle()).append("\n\n");
            for (Message message : conversation.getMessages()) {
                String sender = message.getType() == Message.TYPE_USER ? "你" : "AI";
                builder.append(sender).append(": ").append(message.getContent()).append('\n');
            }
            return builder.toString();
        }

        private void deleteConversation(Conversation conversation) {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(R.string.delete_conversation_title)
                    .setMessage(R.string.delete_conversation_message)
                    .setPositiveButton(R.string.delete, (dialog, which) ->
                            deleteConversationFromHistory(conversation))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        }

        private void deleteConversationFromHistory(Conversation conversation) {
            if (conversations == null) {
                return;
            }

            int position = getAdapterPosition();
            ConversationHistory.delete(context, conversation.getTimestamp());
            conversations.remove(conversation);

            if (position != RecyclerView.NO_POSITION) {
                notifyItemRemoved(position);
            } else {
                notifyDataSetChanged();
            }

            if (conversations.isEmpty() && context instanceof HistoryActivity) {
                ((HistoryActivity) context).showEmptyState();
            }

            Toast.makeText(context, R.string.conversation_deleted, Toast.LENGTH_SHORT).show();
        }
    }
}
