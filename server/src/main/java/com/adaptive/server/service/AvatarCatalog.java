package com.adaptive.server.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side avatar catalog — the source of truth for avatar prices, so purchases can be
 * validated and stars deducted on the server (never trusting a client-sent price).
 *
 * KEEP IN SYNC with the client catalog: client/src/components/ProfileSettings/avatarCatalog.js
 * (same ids + prices; pictureId is set for the avatars that map to a real image).
 */
public final class AvatarCatalog {

    public static final class Avatar {
        public final String id;
        public final int price;       // 0 = free
        public final Long pictureId;  // profile pictureId for real-image avatars, else null

        Avatar(String id, int price, Long pictureId) {
            this.id = id;
            this.price = price;
            this.pictureId = pictureId;
        }
    }

    private static final Map<String, Avatar> BY_ID = new HashMap<>();

    private static void add(String id, int price, Long pictureId) {
        BY_ID.put(id, new Avatar(id, price, pictureId));
    }

    static {
        // free starters (two map to a saved pictureId slot)
        add("a-researcher-f",    0,   1L);
        add("a-explorer-m",      0,   0L);
        add("a-discoverer-n",    0,   null);
        // rare (80–120)
        add("a-engineer-f",      80,  null);
        add("a-engineer-m",      80,  null);
        add("a-theorist-n",      120, null);
        // epic (150–250)
        add("a-astronomer-f",    150, null);
        add("a-captain-m",       150, null);
        add("a-pioneer-n",       200, null);
        add("a-scientist-f",     250, null);
        add("a-mathematician-m", 250, null);
        // legendary (300+)
        add("a-superstar-n",     300, null);
    }

    public static Avatar get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    private AvatarCatalog() {}
}
