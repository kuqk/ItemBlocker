package pl.variant.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCooldown {
    
    private final Map<UUID, Long> cooldowns;
    private final long cooldownTime;
    
    public MessageCooldown(long cooldownMillis) {
        this.cooldowns = new ConcurrentHashMap<>();
        this.cooldownTime = cooldownMillis;
    }
    
    public boolean canSend(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        Long lastMessage = cooldowns.get(playerId);
        
        if (lastMessage == null || currentTime - lastMessage >= cooldownTime) {
            cooldowns.put(playerId, currentTime);
            return true;
        }
        
        return false;
    }
}

