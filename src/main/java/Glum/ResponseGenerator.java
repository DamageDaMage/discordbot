package Glum;

import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ResponseGenerator {

    public ResponseGenerator() {}
    private List<User> saboteurUsers = new ArrayList<User>();

    //Can be used to delete the bot's own message after a given amount of time if needed
    public void deleteResponseMessage(Message message, String id) {
        Channel channel = message.getChannelReceiver();
        Future<Message> deletable = channel.getMessageById(id);
        try {
            Message deletableMessage = deletable.get();
            Thread.sleep(2500L);
            deletableMessage.delete().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public String addSaboteurUser(User user) {
        String returnString = "Feature not available at this time.";
        boolean addUser = true;

        //Make sure the user isn't already in the list
        for(User aUser : saboteurUsers) {
            if(user.getName().equalsIgnoreCase(aUser.getName())) {
                addUser = false;
            }
        }

        //If the user isn't in the list, add them. Otherwise tell them they're already in.
        if(addUser) {
            saboteurUsers.add(user);
            returnString = user.getName() + " has been added to the queue. Queue now has " + saboteurUsers.size() + " players.";
        }
        else {
            returnString = "You are already in the queue.";
        }

        return returnString;
    }

    public String listSaboteurUsers() {
        String returnString = "```Queue currently contains: \n";

        //Grab the name of each user in the list for display
        for(User aUser : saboteurUsers) {
            returnString += aUser.getName();
        }

        returnString += "```";

        return returnString;
    }

    public String assignSaboteurRole() {
        Random rand = new Random();
        String returnString = "There are no players queued. Cannot assign roles.";

        if(saboteurUsers.size() > 0) {
            returnString = "Roles assigned.";

            //pick a random user in the saboteurUsers list
            int saboteur = rand.nextInt(saboteurUsers.size());

            //Go through the list of saboteurUsers and send each one a private message with their role
            for (int i = 0; i < saboteurUsers.size(); i++) {
                if (i == saboteur) {
                    saboteurUsers.get(i).sendMessage("You are the saboteur.");
                } else {
                    saboteurUsers.get(i).sendMessage("You are a normal player.");
                }
            }

            //Roles have been assigned, clear the list in preparation for the next queue
            saboteurUsers.clear();
        }

        return returnString;
    }

    public String hello(Message message) {
        return "hello, " + message.getAuthor().getMentionTag();
    }

    public String help() {
        return BotConstants.HELP_MESSAGE;
    }

}
