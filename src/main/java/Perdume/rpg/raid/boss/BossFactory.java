package Perdume.rpg.raid.boss;

import Perdume.rpg.Rpg;

public class BossFactory {

    /**
     * [수정] 보스 ID와 '파티 인원 수'에 맞는 보스 객체를 생성합니다.
     * @param bossId 생성할 보스의 고유 ID
     * @param plugin 메인 플러그인 인스턴스
     * @param partySize 레이드 시작 시점의 파티 인원 수
     * @return 생성된 Boss 객체. ID를 찾지 못하면 null.
     */
    public static Boss createBoss(String bossId, Rpg plugin, int partySize) {
        return switch (bossId.toLowerCase()) {
            // GolemKing 생성 시, partySize를 함께 전달합니다.
            case "golemking" -> (Boss) new GolemKing(plugin, partySize);
            // case "firedragon" -> (Boss) new FireDragon(plugin, partySize); // 나중에 새로운 보스 추가
            default -> null;
        };
    }

    /**
     * 해당 보스 ID가 Factory에 등록되어 있는지 확인하는 유틸리티 메소드
     */
    public static boolean isValidBossId(String bossId, Rpg plugin) {
        // 인원 수를 1로 가정하여, 보스 객체가 생성되는지만 확인
        return createBoss(bossId, plugin, 1) != null;
    }
}
