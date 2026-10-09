package com.commhub;

import com.commhub.network.ModNetworking;

import java.util.ArrayList;
import java.util.List;

public class ClientHubState {
    public static List<ChatEntry> publicLog = new ArrayList<>();
    public static List<ChatEntry> privateLog = new ArrayList<>();
    public static List<ModNetworking.PlayerInfo> players = new ArrayList<>();
    public static List<ModNetworking.TradeInfo> trades = new ArrayList<>();
    /** 服务端是否开着「允许接受自己的悬赏」（/commhub selftrade，调试用） */
    public static boolean selfTrade = false;
    public static List<String> warehouses = new ArrayList<>();
    public static List<java.util.UUID> friendIds = new ArrayList<>();
    public static List<java.util.UUID> incomingIds = new ArrayList<>();
    public static int energy = 0;
    public static int fluidAmount = 0;
    public static String fluidName = "";

    private ClientHubState() {
    }

    /** 断开连接时清空，避免跨世界/跨服残留 */
    public static void reset() {
        publicLog = new ArrayList<>();
        privateLog = new ArrayList<>();
        players = new ArrayList<>();
        trades = new ArrayList<>();
        warehouses = new ArrayList<>();
        friendIds = new ArrayList<>();
        incomingIds = new ArrayList<>();
        energy = 0;
        fluidAmount = 0;
        fluidName = "";
    }

    public static void applyChatSync(List<ChatEntry> pub, List<ChatEntry> priv, List<ModNetworking.PlayerInfo> list, List<String> wh, List<java.util.UUID> fr, List<java.util.UUID> inc) {
        publicLog = new ArrayList<>(pub);
        privateLog = new ArrayList<>(priv);
        players = new ArrayList<>(list);
        warehouses = new ArrayList<>(wh);
        friendIds = new ArrayList<>(fr);
        incomingIds = new ArrayList<>(inc);
    }

    public static void addChatPush(ChatEntry entry) {
        List<ChatEntry> target = entry.isPublic() ? publicLog : privateLog;
        target.add(entry);
        while (target.size() > 200) target.remove(0);
    }
}
