package com.bunny.ml.smartchef.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.activities.AIChatActivity;
import com.bunny.ml.smartchef.models.Chat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ChatHistoryViewHolder> {
    private final List<Chat> chatList;
    private final Context context;
    private final SimpleDateFormat dateFormat;

    public ChatHistoryAdapter(Context context) {
        this.context = context;
        this.chatList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ChatHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_history, parent, false);
        return new ChatHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHistoryViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Set last message
        String lastMessage = chat.getLastMessage();
        if (lastMessage.length() > 50) {
            lastMessage = lastMessage.substring(0, 47) + "...";
        }
        holder.lastMessageText.setText(lastMessage);

        // Set time
        if (chat.getLastMessageTime() != null) {
            holder.timeText.setText(dateFormat.format(chat.getLastMessageTime().toDate()));
        }

        // Set sender indicator
        if (chat.isAiLastMessage()) {
            holder.senderImage.setImageResource(R.drawable.ic_ai);
        } else {
            holder.senderImage.setImageResource(R.drawable.ic_person);
        }

        holder.starIcon.setVisibility(chat.isStarred() ? View.VISIBLE : View.GONE);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AIChatActivity.class);
            intent.putExtra("documentId", chat.getDocumentId());
            intent.putExtra("chatId", chat.getChatId());
            context.startActivity(intent);
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).overridePendingTransition(
                        R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    public Chat getChatAt(int position) {
        return chatList.get(position);
    }

    public void removeChat(int position) {
        chatList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void setChatList(List<Chat> chats) {
        chatList.clear();
        chatList.addAll(chats);
        notifyDataSetChanged();
    }

    public static class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView lastMessageText;
        TextView timeText;
        ImageView senderImage, starIcon;

        ChatHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
            timeText = itemView.findViewById(R.id.timeText);
            senderImage = itemView.findViewById(R.id.senderImage);
            starIcon = itemView.findViewById(R.id.starIcon);
        }
    }
}

