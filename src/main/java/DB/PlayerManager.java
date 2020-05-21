package DB;

import Glum.BotConstants;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.sql.*;
import java.util.*;

public class PlayerManager {
    private Map<String, Player> playerMap = new HashMap<>();
    private List<Player> sortedPlayers = new ArrayList<>();
    private int gainExpEvery = 30;

    public PlayerManager() {
        initializePlayerList();
    }

    private void initializePlayerList() {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        System.out.println("INITIALIZING PLAYER LIST");
        String query = "select * from player";

        try {
            con = DriverManager.getConnection(BotConstants.DB_URL, BotConstants.DB_USER, BotConstants.DB_PASS);

            stmt = con.createStatement();

            rs = stmt.executeQuery(query);

            while(rs.next()) {
                Player aPlayer = new Player();
                aPlayer.setPlayerId(rs.getInt("playerId"));
                aPlayer.setDiscordId(rs.getString("discordId"));
                aPlayer.setPlayerName(rs.getString("playerName"));
                aPlayer.setServerId(rs.getString("serverId"));
                aPlayer.setExperience(rs.getInt("experience"));
                aPlayer.setLevel(rs.getInt("level"));
                aPlayer.setKarma(rs.getInt("karma"));
                aPlayer.setLastExpGain(DateTime.now().minusMinutes(2));
                aPlayer.setChanged(false);

                //Concatenate discordId and serverId to create unique playerMapId
                aPlayer.setPlayerMapId(aPlayer.getDiscordId()+aPlayer.getServerId());

                playerMap.put(aPlayer.getPlayerMapId(), aPlayer);

                //System.out.println("Initializing " + aPlayer.getPlayerName() + " Level " + aPlayer.getLevel() + " Experience " + aPlayer.getExperience());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if(null != con) con.close(); } catch(SQLException se) { /*can't do anything */ }
            try { if(null != stmt) stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { if(null != rs) rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }
    }

    public void addExperience(Message message, String userId, String userName) {
        boolean addExp = true;

        Player currentPlayer = getPlayerById(userId, userName, message.getChannelReceiver());

        //If the currentPlayer is null we won't be able to add experience
        //Only allow users to gain experience once every x seconds
        //Do not add experience for the bot
        if (userId.equals(BotConstants.BOT_ID) || currentPlayer == null ||
                Seconds.secondsBetween(currentPlayer.getLastExpGain(), DateTime.now()).getSeconds() <= gainExpEvery) {
            addExp = false;
        }

        if(addExp) {
            Random rand = new Random();

            //Add random experience amount between 3 and 8
            int exp = rand.nextInt(5) + 3;
            currentPlayer.setExperience(currentPlayer.getExperience() + exp);
            currentPlayer.setLastExpGain(DateTime.now());
            currentPlayer.setChanged(true);

            //Check to see if player leveled up from experience gain
            if (currentPlayer.getExperience() > nextLevel(currentPlayer.getLevel())) {
                currentPlayer.setLevel(currentPlayer.getLevel() + 1);
                message.reply("Congratulations " + message.getAuthor().getMentionTag() + " you have leveled up! You are now level " + currentPlayer.getLevel() + "!");
            }
        }
    }

    public void addKarma(Message message, String userId, int karmaChange) {
        //Loop through all mentions in message
        for(User user : message.getMentions()) {
            String karmaId = user.getId();
            //Make sure the mention isn't of the bot or of the user sending the message
            if(!karmaId.equalsIgnoreCase(BotConstants.BOT_ID) && !karmaId.equalsIgnoreCase((userId))) {
                Player currentPlayer = getPlayerById(karmaId, user.getName(), message.getChannelReceiver());

                if(currentPlayer != null) {
                    int oldKarma = currentPlayer.getKarma();
                    System.out.println(currentPlayer.getPlayerName() + " old karma: " + oldKarma);
                    System.out.println("Karma change: " + karmaChange);
                    currentPlayer.setKarma(oldKarma + karmaChange);
                    System.out.println(currentPlayer.getPlayerName() + " new karma: " + currentPlayer.getKarma());

                    currentPlayer.setChanged(true);
                }
            }
        }

    }

    private double nextLevel(int level) {
        double exponent = 1.88;
        double baseXP = 25;

        //Code used to print experience gain table
//        for(int i = 1; i < 101; i++) {
//            System.out.println("Total Points for level " + (i) + ": " + Math.floor(baseXP * (Math.pow(i, exponent))));
//            System.out.println("Additional points  needed to reach level " + (i+1) + " from previous level: " + (Math.floor(baseXP * (Math.pow(i+1, exponent))) - Math.floor(baseXP * (Math.pow(i, exponent)))));
//        }

        return Math.floor(baseXP * (Math.pow(level, exponent)));
    }

    private void addNewPlayer(Player newPlayer) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        System.out.println("ADDING NEW PLAYER TO DB!");
        String query =  " INSERT INTO player " +
                        " (playerId, discordId, playerName, experience, level, serverId) " +
                        " VALUES " +
                        " (?, ?, ?, ?, ?, ?) ";

        try {
            con = DriverManager.getConnection(BotConstants.DB_URL, BotConstants.DB_USER, BotConstants.DB_PASS);

            stmt = con.prepareStatement(query);
            stmt.setInt(1, newPlayer.getPlayerId());
            stmt.setString(2, newPlayer.getDiscordId());
            stmt.setString(3, newPlayer.getPlayerName());
            stmt.setInt(4, newPlayer.getExperience());
            stmt.setInt(5, newPlayer.getLevel());
            stmt.setString(6, newPlayer.getServerId());

            //Try to run the statement
            int success = stmt.executeUpdate();

            //If statement completed successfully, add new player to the playerMap
            if(success == 1) {
                playerMap.put(newPlayer.getDiscordId(), newPlayer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if(null != con) con.close(); } catch(SQLException se) { /*can't do anything */ }
            try { if(null != stmt) stmt.close(); } catch(SQLException se) { /*can't do anything */ }
            try { if(null != rs) rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }

    }

    private Player getPlayerById(String userId, String userName, Channel channel) {
        String serverId = "";
        Player currentPlayer = null;
        boolean addPlayer = false;


        //If the channel is null, this is a private message and we should not add a new player to the DB
        if (channel != null) {
            serverId = channel.getServer().getId();
            currentPlayer = playerMap.get(userId+serverId);

            //If the channel isn't null, but currentPlayer still is then we have a new player that needs to be added
            //Make sure the new player isn't the bot
            if(currentPlayer == null && !userId.equalsIgnoreCase(BotConstants.BOT_ID)) {
                addPlayer = true;
            }
        }

        if(addPlayer) {
            Player newPlayer = new Player(playerMap.size()+1, userId, userName, 0, 1, serverId);
            addNewPlayer(newPlayer);
            currentPlayer = newPlayer;
        }

        return currentPlayer;
    }

    private void sortPlayers(String serverId) {
        sortedPlayers = new ArrayList<Player>();

        //Only sort players who are from the same server as the message we received for accurate ranks and leaderboards
        for(Player aPlayer : playerMap.values()) {
            if(aPlayer.getServerId().equalsIgnoreCase(serverId)) {
                sortedPlayers.add(aPlayer);
            }
        }

        Collections.sort(sortedPlayers);
    }

    public String rank(Message message, String userId, String userName) {
        String returnString = "This functionality is not currently available.";
        //Get current player and add to DB if it doesn't already exist
        Player currentPlayer = getPlayerById(userId, userName, message.getChannelReceiver());

        //If current player or channel receiver are null then this is a private message and ranks are not available
        if(currentPlayer != null && message.getChannelReceiver() != null) {
            //Sort players after we're sure the latest has been added to DB
            sortPlayers(message.getChannelReceiver().getServer().getId());

            returnString =  "```" +
                            "Player: " + currentPlayer.getPlayerName() + "\n" +
                            "Rank: " + (sortedPlayers.indexOf(currentPlayer)+1) + "\n" +
                            "Experience: " + currentPlayer.getExperience() + "\n" +
                            "Level: " + currentPlayer.getLevel() + "\n" +
                            "Karma: " + currentPlayer.getKarma() + "\n" +
                            "```";
        }

        return returnString;
    }

    public String levelLeaders(Message message) {
        String returnString = "This functionality is not currently available.";

        //If channel receiver is null this is a private message and leaderboards are not available
        if(message.getChannelReceiver() != null) {
            sortPlayers(message.getChannelReceiver().getServer().getId());
            returnString = "```";

            //Display top 10 players by experience
            for (int i = 0; i < 10; i++) {
                //Make sure player count exceeds current iteration
                if (i < sortedPlayers.size()) {
                    Player aPlayer = sortedPlayers.get(i);
                    returnString += (i + 1) + ". " + aPlayer.getPlayerName() + " - Experience " + aPlayer.getExperience() + "\n";
                }
            }

            returnString += "```";
        }

        return returnString;
    }

    public void databaseSave() {
        System.out.println("PERFORMING DATABASE SAVE!");
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = " UPDATE player " +
                " SET " +
                " experience = ?, " +
                " level = ?, " +
                " karma = ? " +
                " WHERE " +
                " playerId = ? ";

        List<Player> changedPlayers = new ArrayList<Player>();

        //Check for changed players since last save
        for (Player aPlayer : playerMap.values()) {
            if (aPlayer.isChanged()) {
                changedPlayers.add(aPlayer);
            }
        }

        //If no changed players since last save, skip creating DB connection
        if (changedPlayers.size() > 0) {
            try {
                con = DriverManager.getConnection(BotConstants.DB_URL, BotConstants.DB_USER, BotConstants.DB_PASS);

                stmt = con.prepareStatement(query);
                for (Player aPlayer : changedPlayers) {
                    if (aPlayer.isChanged()) {
                        System.out.println("UPDATING DB ENTRY FOR " + aPlayer.getPlayerName());

                        stmt.setInt(1, aPlayer.getExperience());
                        stmt.setInt(2, aPlayer.getLevel());
                        stmt.setInt(3, aPlayer.getKarma());
                        stmt.setInt(4, aPlayer.getPlayerId());

                        //Attempt to run the statement
                        int success = stmt.executeUpdate();

                        //If successful, set changed to false
                        if (success == 1) {
                            aPlayer.setChanged(false);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != con) con.close();
                } catch (SQLException se) { /*can't do anything */ }
                try {
                    if (null != stmt) stmt.close();
                } catch (SQLException se) { /*can't do anything */ }
                try {
                    if (null != rs) rs.close();
                } catch (SQLException se) { /*can't do anything */ }
            }
        }
        else {
            System.out.println("No changed players since last save.");
        }
    }
}
