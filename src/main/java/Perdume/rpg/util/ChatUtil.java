package Perdume.rpg.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ChatUtil {

    /**
     * 플레이어에게 클릭하면 내용이 복사되는 메시지를 보냅니다.
     * @param player 대상 플레이어
     * @param prefix 메시지 앞부분 (예: "§c오류 코드: ")
     * @param clickableText 클릭할 수 있는 텍스트 (실제 오류 코드)
     * @param hoverText 마우스를 올렸을 때 보일 안내 문구
     */
    public static void sendClickableMessage(Player player, String prefix, String clickableText, String hoverText) {
        TextComponent message = new TextComponent(prefix);

        TextComponent clickablePart = new TextComponent(clickableText);
        // [핵심] 클릭 시, clickableText의 내용을 채팅창에 복사하도록 설정
        clickablePart.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, clickableText));
        // [핵심] 마우스를 올렸을 때, hoverText 안내 문구가 보이도록 설정
        clickablePart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));

        message.addExtra(clickablePart);
        player.spigot().sendMessage(message);
    }
}