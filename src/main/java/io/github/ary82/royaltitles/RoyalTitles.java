package io.github.ary82.royaltitles;

import io.github.ary82.royaltitles.net.Net;
import net.minecraftforge.fml.common.Mod;

@Mod(RoyalTitles.MODID)
public class RoyalTitles {
    public static final String MODID = "royal_titles";

    public RoyalTitles() {
        Net.register();
    }
}
