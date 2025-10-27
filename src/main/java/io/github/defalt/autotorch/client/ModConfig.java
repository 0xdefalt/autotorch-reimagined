package io.github.defalt.autotorch.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.*;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "autotorch-reimagined")
public class ModConfig implements ConfigData {

    @Comment("Enable the AutoTorch Reimagined mod.")
    boolean enabled = true;

    @Comment("""
            Enable accurate torch placement directly on block below.
            Note: This may be considered cheaty because it 'fakes' player rotation
            and is basically desyncing the client and server.
            Enable only if you are sure your server doesn't consider it cheaty.
            """
    )
    boolean accuratePlacement = false;

    @Comment("Ignore Sculk Sensor when placing torches.")
    boolean ignoreSculkSensors = false;

    @Comment("The block light level below which the torches are placed.")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 14)
    int lightLevel = 4;

}