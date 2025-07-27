package Perdume.rpg.system; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import Perdume.rpg.raid.RaidInstance;
import Perdume.rpg.util.ChatUtil;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

public class RaidErrorHandler {

    public static void handle(RaidInstance raidInstance, Exception e) {
        String errorCode = UUID.randomUUID().toString().substring(0, 8);

        // --- [핵심] 1. 서버 콘솔에 상세 로그 출력 ---
        Rpg.log.severe("!!!!!!!!!! CRITICAL RAID ERROR !!!!!!!!!");
        Rpg.log.severe("Raid: " + raidInstance.getRaidWorld().getName());
        Rpg.log.severe("Error Code: " + errorCode + " (자세한 내용은 error-logs 폴더의 " + errorCode + ".log 파일을 확인하세요)");
        e.printStackTrace();
        Rpg.log.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        // --- [핵심] 2. 별도의 로그 파일에 '사건 보고서' 작성 ---
        logErrorToFile(errorCode, raidInstance, e);

        // --- 3. 플레이어에게 알림 ---
        raidInstance.broadcastMessage("§4[치명적 오류] §c레이드 진행 중 심각한 오류가 발생하여, 레이드를 강제 종료합니다.");
        for (Player player : raidInstance.getOnlinePlayers()) {
            ChatUtil.sendClickableMessage(player,
                    "§c관리자에게 다음 오류 코드를 전달해주세요: §e",
                    errorCode,
                    "§a클릭하여 오류 코드를 복사합니다."
            );
        }

        // 4. 레이드 강제 종료
        raidInstance.end(false);
    }

    /**
     * [신규] 오류 정보를 별도의 .log 파일로 저장합니다.
     */
    private static void logErrorToFile(String errorCode, RaidInstance raidInstance, Exception e) {
        Rpg plugin = Rpg.getInstance();
        File logsFolder = new File(plugin.getDataFolder(), "error-logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }

        File logFile = new File(logsFolder, errorCode + ".log");
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            writer.println("=====================================");
            writer.println("              RAID ERROR LOG");
            writer.println("=====================================");
            writer.println("Time: " + sdf.format(new Date()));
            writer.println("Error Code: " + errorCode);
            writer.println("Raid World: " + raidInstance.getRaidWorld().getName());
            
            String playerNames = raidInstance.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .collect(Collectors.joining(", "));
            writer.println("Players in Raid: [" + playerNames + "]");
            writer.println("-------------------------------------");
            writer.println("Stack Trace:");
            e.printStackTrace(writer);
            writer.println("=====================================");

        } catch (IOException ex) {
            Rpg.log.severe("오류 로그 파일 작성에 실패했습니다!");
            ex.printStackTrace();
        }
    }
}