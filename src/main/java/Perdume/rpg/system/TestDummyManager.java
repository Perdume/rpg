package Perdume.rpg.system;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestDummyManager {

    // 테스트용 허수아비 엔티티의 UUID를 저장하는 Set
    private static final Set<UUID> testDummies = new HashSet<>();

    public static void addDummy(UUID entityId) {
        testDummies.add(entityId);
    }

    public static void removeDummy(UUID entityId) {
        testDummies.remove(entityId);
    }

    public static boolean isTestDummy(UUID entityId) {
        return testDummies.contains(entityId);
    }
}