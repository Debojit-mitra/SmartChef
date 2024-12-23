package com.bunny.ml.smartchef.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bunny.ml.smartchef.R;
import com.bunny.ml.smartchef.models.ChatMessage;
import com.bunny.ml.smartchef.utils.RecipeTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private final List<ChatMessage> messages;
    private final SimpleDateFormat timeFormat;
    private static final int VIEW_TYPE_LOADING = 3;
    private static final int VIEW_TYPE_TYPING = 4;
    private boolean isTyping = false;
    private int lastAnimatedPosition = -1;
    private boolean isLoadingMore = false;
    private boolean shouldAnimate = false;
    private OnLoadMoreListener loadMoreListener;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }

    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_LOADING:
                View loadingView = inflater.inflate(R.layout.item_loading, parent, false);
                return new LoadingViewHolder(loadingView);
            case VIEW_TYPE_USER:
                View userView = inflater.inflate(R.layout.item_chat_user, parent, false);
                return new MessageViewHolder(userView);
            case VIEW_TYPE_AI:
                View aiView = inflater.inflate(R.layout.item_chat_ai, parent, false);
                return new MessageViewHolder(aiView);
            case VIEW_TYPE_TYPING:
                View typingView = inflater.inflate(R.layout.item_chat_typing, parent, false);
                return new TypingViewHolder(typingView);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MessageViewHolder) {
            ChatMessage message = messages.get(position);
            MessageViewHolder messageHolder = (MessageViewHolder) holder;

            if (getItemViewType(position) == VIEW_TYPE_AI) {
                // For AI messages, use setRecipeText
                messageHolder.messageText.setFormattedText(message.getContent());
            } else if (getItemViewType(position) == VIEW_TYPE_USER) {
                // For User messages, use setText
                messageHolder.messageText.setText(message.getContent());
            }
            if (message.getTimestamp() != null) {
                messageHolder.timeText.setText(timeFormat.format(message.getTimestamp()));
            }

            setAnimation(holder.itemView, position);

        } else if (holder instanceof TypingViewHolder) {
            // Restart animations when typing indicator is bound
            ((TypingViewHolder) holder).startAnimations();
            setAnimation(holder.itemView, position);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            // If this is a content update, just update the text without animation
            if (holder instanceof MessageViewHolder && position < messages.size()) {
                MessageViewHolder messageHolder = (MessageViewHolder) holder;
                ChatMessage message = messages.get(position);
                if (getItemViewType(position) == VIEW_TYPE_AI) {
                    messageHolder.messageText.setFormattedText(message.getContent());
                } else {
                    messageHolder.messageText.setText(message.getContent());
                }
            }
        } else {
            // Call the simple version if no payloads
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (shouldAnimate && position > lastAnimatedPosition && position >= getItemCount() - 2) {
            Animation slideIn = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.slide_in_up);
            viewToAnimate.startAnimation(slideIn);
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size() + (isTyping ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == messages.size() && isTyping) {
            return VIEW_TYPE_TYPING;
        }
        if (messages.get(position) == null) {
            return VIEW_TYPE_LOADING;
        }
        return messages.get(position).isAi() ? VIEW_TYPE_AI : VIEW_TYPE_USER;
    }

    public void showTyping(boolean show) {
        if (isTyping != show) {
            isTyping = show;
            if (show) {
                notifyItemInserted(messages.size());
            } else {
                notifyItemRemoved(messages.size());
            }
        }
    }

    public void addMessages(List<ChatMessage> newMessages, boolean isFirstPage) {
        shouldAnimate = false;
        if (isFirstPage) {
            List<ChatMessage> updatedList = new ArrayList<>(newMessages);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new MessageDiffCallback(messages, updatedList));
            messages.clear();
            messages.addAll(updatedList);
            diffResult.dispatchUpdatesTo(this);
        } else {
            // For pagination, we're adding messages at the beginning
            List<ChatMessage> updatedList = new ArrayList<>();
            // Remove loading indicator if present
            if (!messages.isEmpty() && messages.get(0) == null) {
                updatedList.add(null);  // Keep loading indicator at top
                updatedList.addAll(newMessages);
                updatedList.addAll(messages.subList(1, messages.size()));
            } else {
                updatedList.addAll(newMessages);
                updatedList.addAll(messages);
            }

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new MessageDiffCallback(messages, updatedList), true);
            messages.clear();
            messages.addAll(updatedList);
            diffResult.dispatchUpdatesTo(this);
        }
    }

    public void setLoading(boolean loading) {
        if (isLoadingMore != loading) {
            isLoadingMore = loading;
            if (loading) {
                if (messages.isEmpty() || messages.get(0) != null) {
                    messages.add(0, null);
                    notifyItemInserted(0);
                }
            } else {
                if (!messages.isEmpty() && messages.get(0) == null) {
                    messages.remove(0);
                    notifyItemRemoved(0);
                }
            }
        }
    }

    public void addMessage(ChatMessage message) {
        shouldAnimate = true;
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateLastMessage(String content) {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            ChatMessage lastMessage = messages.get(lastIndex);
            if (lastMessage != null) {
                lastMessage.setContent(content);
                shouldAnimate = false;
                notifyItemChanged(lastIndex, "content_update"); // Remove the payload to ensure full update
            }
        }
    }

    public void setMessages(List<ChatMessage> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(messages, newMessages));
        messages.clear();
        messages.addAll(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        MessageDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Handle null case for loading indicator
            if (oldList.get(oldItemPosition) == null && newList.get(newItemPosition) == null) {
                return true;
            }
            if (oldList.get(oldItemPosition) == null || newList.get(newItemPosition) == null) {
                return false;
            }

            ChatMessage oldMessage = oldList.get(oldItemPosition);
            ChatMessage newMessage = newList.get(newItemPosition);

            if (oldMessage.getTimestamp() != null && newMessage.getTimestamp() != null) {
                return oldMessage.getTimestamp().equals(newMessage.getTimestamp());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Handle null case for loading indicator
            if (oldList.get(oldItemPosition) == null && newList.get(newItemPosition) == null) {
                return true;
            }
            if (oldList.get(oldItemPosition) == null || newList.get(newItemPosition) == null) {
                return false;
            }

            ChatMessage oldMessage = oldList.get(oldItemPosition);
            ChatMessage newMessage = newList.get(newItemPosition);

            if (oldMessage.getContent() == null || newMessage.getContent() == null) {
                return false;
            }

            return oldMessage.getContent().equals(newMessage.getContent());
        }
    }


    public static class TypingViewHolder extends RecyclerView.ViewHolder {
        private final View dot1, dot2, dot3;

        public TypingViewHolder(@NonNull View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        public void startAnimations() {
            // Clear any existing animations
            dot1.clearAnimation();
            dot2.clearAnimation();
            dot3.clearAnimation();

            // Start animations with delays
            startDotAnimation(dot1, 0);
            startDotAnimation(dot2, 200);
            startDotAnimation(dot3, 400);
        }

        private void startDotAnimation(View dot, int delay) {
            Animation animation = AnimationUtils.loadAnimation(dot.getContext(), R.anim.typing_dot);
            animation.setStartOffset(delay);
            animation.setRepeatCount(Animation.INFINITE); // Make sure animation repeats
            dot.startAnimation(animation);
        }
    }

    public static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        RecipeTextView messageText;
        TextView timeText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}