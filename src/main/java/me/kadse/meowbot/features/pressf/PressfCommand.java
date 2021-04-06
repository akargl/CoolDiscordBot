/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.kadse.meowbot.features.pressf;

import me.kadse.meowbotframework.commands.Command;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alex
 */
public class PressfCommand extends Command {

    private static final String RESPECT_EMOJI = "🇫";
    private static final String PRESSF_CONFIRMATION_MSG = "Drücke " + RESPECT_EMOJI + " um Respekt für '%s' zu zahlen";
    private static final String PAID_RESPECT_MSG = "**%s hat Respekt für '%s' gezahlt**";
    private static final String FINAL_MSG = "**%d Respekt wurden für '%s' gezahlt**";

    private static final int PRESSF_DURATION_S = 2*60;

    private PressfManager pressfManager;

    public PressfCommand(PressfManager pressfManager) {
        super("pressf", new String[]{}, "Drücke " + RESPECT_EMOJI + " um Respekt zu zahlen");
        this.pressfManager = pressfManager;
    }

    private String replaceMentions(String msg, List<User> mentionedUsers) {
        String sanitizedMsg = msg;

        for (User u : mentionedUsers) {
            sanitizedMsg = sanitizedMsg.replace(u.getMentionTag(), u.getName());
            sanitizedMsg = sanitizedMsg.replace(u.getNicknameMentionTag(), u.getName());
        }

        sanitizedMsg = sanitizedMsg.replace("@", "@ ");

        return sanitizedMsg;
    }

    @Override
    public void execute(TextChannel channel, MessageAuthor sender, String[] args, List<User> mentionedUsers) {
        String reason = String.join(" ", args).trim();
        
        if (reason.length() == 0) {
            reply(channel, "Syntax: !pressf <Grund>");
            return;
        }

        String reasonWithoutMentions = replaceMentions(reason, mentionedUsers);

        CompletableFuture<Message> firstMessageFuture = channel.sendMessage(String.format(PRESSF_CONFIRMATION_MSG, reasonWithoutMentions));
        try {
            Message firstMessage = firstMessageFuture.get();

            long firstMessageId = firstMessage.getId();

            pressfManager.getActiveRespects().put(firstMessageId, new HashMap<>());

            firstMessage.addReaction(RESPECT_EMOJI);

            firstMessage.addReactionAddListener(reactionAddEvent -> {
                if (reactionAddEvent.getEmoji().equalsEmoji(RESPECT_EMOJI) && !reactionAddEvent.getUser().get().isYourself()) {
                    try {
                        Message userReactedMessage = reactionAddEvent.getChannel().sendMessage(
                                String.format(PAID_RESPECT_MSG, reactionAddEvent.getUser().get().getDisplayName(reactionAddEvent.getServer().get()), reasonWithoutMentions)).get();
                        pressfManager.getActiveRespects().get(firstMessageId).put(reactionAddEvent.getUser().get().getId(), userReactedMessage);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }).removeAfter(PRESSF_DURATION_S, TimeUnit.SECONDS);

            firstMessage.addReactionRemoveListener(reactionRemoveEvent -> {
                if (reactionRemoveEvent.getEmoji().equalsEmoji(RESPECT_EMOJI) && !reactionRemoveEvent.getUser().get().isYourself()) {
                    Message userReactedMessage = pressfManager.getActiveRespects().get(firstMessageId).remove(reactionRemoveEvent.getUser().get().getId());
                    userReactedMessage.delete("User removed pressF reaction");
                }
            }).removeAfter(PRESSF_DURATION_S, TimeUnit.SECONDS);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int amountRespectPaid = pressfManager.getActiveRespects().get(firstMessageId).size();
                    channel.sendMessage(String.format(FINAL_MSG, amountRespectPaid, reasonWithoutMentions));
                }
            }, PRESSF_DURATION_S * 1000);

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(PressfCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
