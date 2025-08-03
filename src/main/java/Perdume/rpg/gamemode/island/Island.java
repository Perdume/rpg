package Perdume.rpg.gamemode.island;

import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 하나의 스카이블럭 섬에 대한 모든 영구 데이터를 저장하는 클래스입니다.
 * 이 객체는 IslandInstance에 의해 관리되고, SkyblockManager를 통해 파일로 저장/로드됩니다.
 */
public class Island {

    private final String id; // 랜덤 문자열 ID
    private UUID owner;
    private final List<UUID> members = new ArrayList<>();
    private transient World world; // 'transient' 키워드처럼, 이 필드는 파일에 저장되지 않음

    /**
     * 새로운 섬을 생성할 때 사용하는 생성자입니다.
     * @param owner 섬을 생성한 플레이어
     */
    public Island(Player owner) {
        // 예측 불가능한 고유 ID를 생성합니다.
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.owner = owner.getUniqueId();
        this.members.add(owner.getUniqueId());
    }

    /**
     * 파일에서 데이터를 불러와 기존 섬을 복원할 때 사용하는 생성자입니다.
     * @param id 섬의 고유 ID
     * @param owner 주인의 UUID
     * @param members 팀원들의 UUID 목록
     */
    public Island(String id, UUID owner, List<UUID> members) {
        this.id = id;
        this.owner = owner;
        this.members.addAll(members);
    }

    // --- Getter 메소드 ---

    public String getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<UUID> getMembers() {
        // 외부에서 리스트를 직접 수정하는 것을 방지하기 위해, 수정 불가능한 복사본을 반환합니다.
        return Collections.unmodifiableList(members);
    }

    /**
     * 현재 메모리에 로드된 월드 객체를 반환합니다.
     * @return World 객체, 언로드 상태이면 null
     */
    public World getWorld() {
        return world;
    }

    /**
     * 이 섬의 인스턴스 월드 이름을 반환합니다.
     * @return 월드 이름 (예: "Island--a4f8e1b2")
     */
    public String getWorldName() {
        return "Island--" + this.id;
    }
    
    /**
     * 이 섬의 데이터가 저장될 템플릿 폴더/파일 이름을 반환합니다.
     * @return 템플릿 이름 (예: "a4f8e1b2")
     */
    public String getTemplateFolderName() {
        return this.id;
    }

    // --- Setter 및 유틸리티 메소드 ---

    public void setWorld(World world) {
        this.world = world;
    }

    public boolean isMember(Player player) {
        return members.contains(player.getUniqueId());
    }

    public void addMember(Player player) {
        if (!isMember(player)) {
            members.add(player.getUniqueId());
        }
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
    }

    public void setOwner(Player player) {
        this.owner = player.getUniqueId();
        // 새로운 주인이 멤버 목록에 없으면 추가
        addMember(player);
    }
}