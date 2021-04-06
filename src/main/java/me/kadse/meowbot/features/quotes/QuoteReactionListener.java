package me.kadse.meowbot.features.quotes;

import me.kadse.meowbot.configs.QuoteConfig;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

public class QuoteReactionListener implements ReactionAddListener {
    private QuoteConfig quoteConfig;
    private QuoteManager quoteManager;

    public QuoteReactionListener(QuoteConfig quoteConfig, QuoteManager quoteManager) {
        this.quoteConfig = quoteConfig;
        this.quoteManager = quoteManager;
    }

    public void onReactionAdd(ReactionAddEvent reactionAddEvent) {
        if (!reactionAddEvent.getEmoji().isUnicodeEmoji())
            return;

        try {
            if (reactionAddEvent.requestMessage().get().getContent().length() < 1) {
                if (reactionAddEvent.getEmoji().asUnicodeEmoji().get().equals(quoteConfig.deleteQuoteEmoji)) {
                    if (reactionAddEvent.getReaction().get().getCount() >= quoteConfig.deleteQuoteCount) {
                        String footer = reactionAddEvent.requestMessage().get().getEmbeds().get(0).getFooter().get().getText().get();
                        try {
                            if (quoteManager.getQuotesMap().containsKey(Long.parseLong(footer.split(" ")[1]))) {
                                deleteQuote(Long.parseLong(footer.split(" ")[1]), reactionAddEvent);
                                return;
                            } else {
                                return;
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (reactionAddEvent.getEmoji().asUnicodeEmoji().get().equals(quoteConfig.quoteEmoji)) {
                Message message = reactionAddEvent.requestMessage().get();
                quoteManager.saveQuote(message, reactionAddEvent.getUser().get());
                message.addReaction(quoteConfig.confirmationEmoji);
            } else if (reactionAddEvent.getEmoji().asUnicodeEmoji().get().equals(quoteConfig.deleteQuoteEmoji)) {
                if (reactionAddEvent.getReaction().get().getCount() >= quoteConfig.deleteQuoteCount) {
                    deleteQuote(reactionAddEvent.getMessageId(), reactionAddEvent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteQuote(Long quoteId, ReactionAddEvent reactionAddEvent) {
        quoteManager.deleteQuote(quoteId);

        EmbedBuilder embed = new EmbedBuilder()
                .setThumbnail("https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/120/apple/155/toilet_1f6bd.png")
                .setDescription("Quote " + quoteId + " gelöscht!");

        reactionAddEvent.getChannel().sendMessage(embed);
    }
}
