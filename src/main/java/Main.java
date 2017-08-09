import api.SlackAPI;
import beans.Channel;
import beans.Reaction;
import beans.events.Message;

import java.util.*;

public class Main {
    private static SlackAPI api;

    private static List<Reaction> reactions;
    private static List<Channel> channels;
    private static List<Message> messages;

    private static int reactionsCountTotal;

    private static void fetchChannel() throws Exception {
        channels = api.listChannels();
    }

    public static void main(String args[]) throws Exception {
        api = new SlackAPI(false);
        reactions = new ArrayList<>();
        messages = new ArrayList<>();
        reactionsCountTotal = 0;

        fetchChannel();
        System.out.println(channels.size() + " Channels");

        for(Channel channel : channels) {
            try {
                messages.addAll(api.fetchAllMessages(channel.getId(), null));
            } catch(Exception E) {
                E.printStackTrace();
            }
        }
        System.out.println(messages.size() + " Messages");

        for(Message message : messages) {
            List<Reaction> reactionsMessage = message.getReactions();
            if(reactionsMessage != null) {
                for(Reaction reaction : message.getReactions()) {
                    Optional<Reaction> reac = reactions.stream().filter(r -> reaction.getName().equals(r.getName())).findFirst();
                    if(reac.isPresent()) {
                        reac.get().setCount(reac.get().getCount() + reaction.getCount());
                        reac.get().getUsers().addAll(reaction.getUsers());
                    } else
                        reactions.add(reaction);
                }
            }
        }

        for(Reaction reaction : reactions)
            reactionsCountTotal += reaction.getCount();
        System.out.println(reactionsCountTotal + " Réactions");

        reactions.sort(Comparator.comparingInt(Reaction::getCount));

        System.out.println(reactions.get(reactions.size()-1));
    }
}
