package com.aizensmp.spawnpaster;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;

public final class AizenSpawnPaster extends JavaPlugin {
    private static final String SCHEMATIC = "DonutSMP_spawn_by-eliterorr.schem";
    private static final String MARKER = ".aizen-donut-spawn-pasted";

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, this::pasteOnce, 40L);
    }

    private void pasteOnce() {
        World world = Bukkit.getWorlds().stream().findFirst().orElse(null);
        if (world == null) {
            getLogger().warning("No world available; spawn paste skipped.");
            return;
        }

        File marker = new File(world.getWorldFolder(), MARKER);
        if (marker.isFile()) {
            getLogger().info("DonutSMP spawn already pasted in " + world.getName() + ".");
            return;
        }

        File schematic = new File(getDataFolder().getParentFile().getParentFile(),
                "WorldEdit/schematics/" + SCHEMATIC);
        if (!schematic.isFile()) {
            getLogger().warning("Missing schematic: " + schematic.getAbsolutePath());
            return;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        if (format == null) {
            getLogger().warning("WorldEdit cannot read schematic: " + schematic.getName());
            return;
        }

        try (FileInputStream in = new FileInputStream(schematic);
             ClipboardReader reader = format.getReader(in)) {

            Clipboard clipboard = reader.read();
            BlockVector3 target = BlockVector3.at(
                    world.getSpawnLocation().getBlockX(),
                    world.getSpawnLocation().getBlockY(),
                    world.getSpawnLocation().getBlockZ()
            );

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSession(BukkitAdapter.adapt(world))) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(target)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
                editSession.flushSession();
            }

            if (marker.createNewFile()) {
                getLogger().info("DonutSMP spawn pasted successfully at "
                        + world.getName() + " spawn: " + target);
            }
        } catch (Exception e) {
            getLogger().severe("DonutSMP spawn paste failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
