package Perdume.rpg.core.party;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Party {

    private Player leader;
    private final List<Player> members = new ArrayList<>();

    public Party(Player leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public Player getLeader() {
        return leader;
    }

    public List<Player> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void addMember(Player player) {
        if (!members.contains(player)) {
            members.add(player);
        }
    }
    public void setLeader(Player player) { this.leader = player; }

    public void removeMember(Player player) {
        members.remove(player);
    }

    public boolean isLeader(Player player) {
        return this.leader.equals(player);
    }

    public boolean isMember(Player player) {
        return this.members.contains(player);
    }

    public void broadcast(String message) {
        for (Player member : members) {
            if (member.isOnline()) {
                member.sendMessage("§a[파티] §f" + message);
            }
        }
    }
}