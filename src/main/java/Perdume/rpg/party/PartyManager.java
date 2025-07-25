package Perdume.rpg.party;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    private static final Map<UUID, Party> parties = new HashMap<>();
    private static final Map<UUID, Party> invites = new HashMap<>();

    public static Party getParty(Player player) {
        for (Party party : parties.values()) {
            if (party.isMember(player)) {
                return party;
            }
        }
        return null;
    }

    public static void createParty(Player leader) {
        if (getParty(leader) != null) {
            leader.sendMessage("§c이미 다른 파티에 소속되어 있습니다.");
            return;
        }
        Party party = new Party(leader);
        parties.put(leader.getUniqueId(), party);
        leader.sendMessage("§a파티를 생성했습니다! /p invite <이름> 으로 파티원을 초대하세요.");
    }

    public static void invitePlayer(Player inviter, Player target) {
        Party party = getParty(inviter);
        if (party == null || !party.isLeader(inviter)) {
            inviter.sendMessage("§c파티장이 아니면 초대할 수 없습니다.");
            return;
        }
        if (target == null || !target.isOnline()) {
            inviter.sendMessage("§c초대할 플레이어를 찾을 수 없거나 오프라인 상태입니다.");
            return;
        }
        if (getParty(target) != null) {
            inviter.sendMessage("§c" + target.getName() + "님은 이미 다른 파티에 있습니다.");
            return;
        }

        invites.put(target.getUniqueId(), party);
        inviter.sendMessage("§a" + target.getName() + "님을 파티에 초대했습니다.");
        target.sendMessage("§a" + inviter.getName() + "님에게서 파티 초대가 도착했습니다.");
        target.sendMessage("§e/p accept " + inviter.getName() + " §f명령어로 수락하세요.");
    }

    public static void acceptInvite(Player player, Player leader) {
        if (leader == null) {
            player.sendMessage("§c초대를 보낸 파티장을 찾을 수 없습니다.");
            return;
        }
        Party party = invites.get(player.getUniqueId());
        if (party == null || !party.getLeader().equals(leader)) {
            player.sendMessage("§c" + leader.getName() + "님에게 받은 파티 초대가 없습니다.");
            return;
        }

        party.addMember(player);
        invites.remove(player.getUniqueId());
        party.broadcast(player.getName() + "님이 파티에 참가했습니다.");
    }

    public static void leaveParty(Player player) {
        Party party = getParty(player);
        if (party == null) {
            player.sendMessage("§c소속된 파티가 없습니다.");
            return;
        }

        // 1. 파티장이 나갔을 경우
        if (party.isLeader(player)) {
            party.removeMember(player); // 먼저 파티장를 멤버 목록에서 제거

            // 2. 다른 파티원이 남아있는지 확인
            if (party.getMembers().isEmpty()) {
                // 아무도 없으면 파티 해산
                disbandParty(player);
            } else {
                // 다른 파티원이 있다면, 첫 번째 멤버를 새로운 파티장으로 임명
                Player newLeader = party.getMembers().get(0);
                party.setLeader(newLeader); // Party 클래스에 setLeader 메소드 추가 필요

                // 기존 파티 등록을 제거하고, 새로운 파티장을 키로 다시 등록
                parties.remove(player.getUniqueId());
                parties.put(newLeader.getUniqueId(), party);

                party.broadcast(player.getName() + "님이 파티를 떠났습니다.");
                party.broadcast(newLeader.getName() + "님이 새로운 파티장이 되었습니다.");
            }
        }
        // 3. 일반 파티원이 나갔을 경우
        else {
            party.removeMember(player);
            party.broadcast(player.getName() + "님이 파티에서 나갔습니다.");
            player.sendMessage("§e파티에서 탈퇴했습니다.");
        }
    }

    public static void disbandParty(Player leader) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null || !party.isLeader(leader)) {
            leader.sendMessage("§c파티장이 아닙니다.");
            return;
        }
        party.broadcast("파티장이 파티를 해산했습니다.");
        parties.remove(leader.getUniqueId());
    }
}