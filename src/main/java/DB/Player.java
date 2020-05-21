package DB;

import org.joda.time.DateTime;

public class Player implements Comparable<Player> {
    private int playerId;
    private String discordId;
    private String playerName;
    private String serverId;
    private String playerMapId;
    private int experience;
    private int level;
    private int karma;
    private DateTime lastExpGain;
    private boolean changed;

    public Player() {

    }

    public Player(int pid, String did, String pname, int exp, int lvl, String sid) {
        this.playerId = pid;
        this.discordId = did;
        this.playerName = pname;
        this.serverId = sid;
        this.experience = exp;
        this.level = lvl;
        this.lastExpGain = DateTime.now().minusMinutes(2);
        this.changed = false;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getServerId() { return serverId; }

    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getPlayerMapId() { return playerMapId; }

    public void setPlayerMapId(String playerMapId) { this.playerMapId = playerMapId; }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getKarma() { return karma; }

    public void setKarma(int karma) { this.karma = karma; }

    public DateTime getLastExpGain() {
        return lastExpGain;
    }

    public void setLastExpGain(DateTime lastExpGain) {
        this.lastExpGain = lastExpGain;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public int compareTo(Player comparestu) {
        int compareexp=((Player)comparestu).getExperience();
        return compareexp-this.experience;
    }
}
