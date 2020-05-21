package Glum;

import DB.PlayerManager;
import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Just putting the link to add the bot to a server here for future use
 * https://discordapp.com/api/oauth2/authorize?client_id=[botid]&scope=bot&permissions=0
 */
public class DiscordBot {
    private ResponseGenerator rg = new ResponseGenerator();
    private PlayerManager pm = new PlayerManager();
    private Timer timer = new Timer ();
    private TimerTask hourlyTask = new TimerTask () {
        @Override
        public void run () {
            pm.databaseSave();
        }
    };

    public DiscordBot() {
        DiscordAPI api = Javacord.getApi(BotConstants.BOT_TOKEN, true);
        timer.schedule(hourlyTask, 0l, 1000*60*1); //currently set to save every minute
        // connect
        api.connect(new FutureCallback<DiscordAPI>() {
            public void onSuccess(DiscordAPI api) {
                // register message listener
                api.registerListener(new MessageCreateListener() {
                    public void onMessageCreate(DiscordAPI api, Message message) {
                        pm.addExperience(message, message.getAuthor().getId(), message.getAuthor().getName());
                        // check if the message contains mentions
                        if(!message.getMentions().isEmpty()) {

                            // check if one of the mentions is of the bot
                            if(isMentioned(message)) {

                                //check for keywords and respond appropriately

                                if(message.getContent().trim().toLowerCase().endsWith("help")) {
                                    respond(api, message, rg.help());
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("sab queue")) {
                                    respond(api, message, rg.addSaboteurUser(message.getAuthor()));
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("sab list")) {
                                    respond(api, message, rg.listSaboteurUsers());
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("sab assign")) {
                                    respond(api, message, rg.assignSaboteurRole());
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("rank")) {
                                    respond(api, message, pm.rank(message, message.getAuthor().getId(), message.getAuthor().getName()));
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("leaderboard")) {
                                    respond(api, message, pm.levelLeaders(message));
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("++")) {
                                    pm.addKarma(message, message.getAuthor().getId(), 1);
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("--")) {
                                    pm.addKarma(message, message.getAuthor().getId(), -1);
                                }

                                //check for hidden keywords and responses
                                else if(message.getContent().trim().toLowerCase().endsWith("hello")) {
                                    respond(api, message, rg.hello(message));
                                }
                                else if(message.getContent().trim().toLowerCase().endsWith("whisper")) {
                                    message.getAuthor().sendMessage("Test Message.");
                                }


                            }
                        }
                    }
                });
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        DiscordBot bot = new DiscordBot();
    }

    public boolean isMentioned(Message message) {
        boolean mentioned = false;

        for(User user : message.getMentions()) {
            if(user.getId().equalsIgnoreCase(BotConstants.BOT_ID)) {
                mentioned = true;
            }
        }

        return mentioned;
    }

    public void respond(DiscordAPI api, Message message, String response) {
        if(message.isPrivateMessage()) {
            message.reply(response);
        } else if(message.getChannelReceiver().getServer().getId().equalsIgnoreCase(BotConstants.SERVER_ID)) {
            api.getServerById(BotConstants.SERVER_ID).getChannelById(BotConstants.BOT_CHANNEL_ID).sendMessage(response);
        } else {
            message.reply(response);
        }
    }

}
