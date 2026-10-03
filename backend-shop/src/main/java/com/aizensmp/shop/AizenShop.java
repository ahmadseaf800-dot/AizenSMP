package com.aizensmp.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class AizenShop extends JavaPlugin implements Listener {
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, Integer> shards = new HashMap<>();
    private final Map<String, ShopItem> items = new LinkedHashMap<>();
    private final Map<String, ShopItem> shardItems = new LinkedHashMap<>();
    private final NamespacedKey generatorKey;

    private static final String MAIN = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "AIZEN SHOP" + ChatColor.DARK_GRAY + " ✦";
    private static final String END = ChatColor.DARK_PURPLE + "End";
    private static final String NETHER = ChatColor.RED + "Netherworld";
    private static final String GEAR = ChatColor.YELLOW + "Gear";
    private static final String FOOD = ChatColor.GREEN + "Food";
    private static final String SHARD = ChatColor.AQUA + "Shard Shop";

    public AizenShop() { generatorKey = new NamespacedKey(this, "aizen_generator"); }

    @Override public void onEnable() {
        saveDefaultConfig();
        loadBalances();
        registerItems();
        Objects.requireNonNull(getCommand("shop")).setExecutor((s,c,l,a) -> { if (s instanceof Player p) openMain(p); return true; });
        Objects.requireNonNull(getCommand("balance")).setExecutor((s,c,l,a) -> { if (s instanceof Player p) msg(p, "&eBalance: &6$" + money(balances.getOrDefault(p.getUniqueId(), 0D))); return true; });
        Objects.requireNonNull(getCommand("shards")).setExecutor((s,c,l,a) -> { if (s instanceof Player p) msg(p, "&bShards: &f" + shards.getOrDefault(p.getUniqueId(), 0)); return true; });
        Objects.requireNonNull(getCommand("pay")).setExecutor((s,c,l,a) -> {
            if (!(s instanceof Player p) || a.length != 2) return true;
            Player target = Bukkit.getPlayerExact(a[0]);
            if (target == null) { msg(p, "&cPlayer not found."); return true; }
            double amount;
            try { amount = Double.parseDouble(a[1]); } catch (Exception ex) { msg(p, "&cInvalid amount."); return true; }
            if (amount <= 0) { msg(p, "&cAmount must be positive."); return true; }
            UUID id=p.getUniqueId(); double bal=balances.getOrDefault(id,0D);
            if (bal < amount) { msg(p, "&cInsufficient funds."); return true; }
            balances.put(id, bal-amount);
            balances.put(target.getUniqueId(), balances.getOrDefault(target.getUniqueId(),0D)+amount);
            saveBalances();
            msg(p, "&aPaid &6$"+money(amount)+" &ato "+target.getName()+".");
            msg(target, "&aReceived &6$"+money(amount)+" &afrom "+p.getName()+".");
            return true;
        });
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override public void onDisable() { saveBalances(); }

    private void registerItems() {
        items.clear();
        add(items,"end_chest","Ender Chest",Material.ENDER_CHEST,2500,1);
        add(items,"ender_pearl","Ender Pearl",Material.ENDER_PEARL,75,1);
        add(items,"end_stone","End Stone (16)",Material.END_STONE,128,16);
        add(items,"dragons_breath","Dragon's Breath",Material.DRAGON_BREATH,1000,1);
        add(items,"end_rod","End Rod",Material.END_ROD,100,1);
        add(items,"chorus_fruit","Chorus Fruit",Material.CHORUS_FRUIT,108,1);
        add(items,"popped_chorus","Popped Chorus Fruit",Material.POPPED_CHORUS_FRUIT,24,1);
        add(items,"shulker_shell","Shulker Shell",Material.SHULKER_SHELL,350,1);
        add(items,"shulker_box","Shulker Chest",Material.SHULKER_BOX,800,1);

        add(items,"blaze_rod","Blaze Rod",Material.BLAZE_ROD,150,1);
        add(items,"nether_wart","Netherworld Wart",Material.NETHER_WART,96,1);
        add(items,"glowstone_dust","Glow Stone Dust",Material.GLOWSTONE_DUST,15,1);
        add(items,"magma_cream","Magma Cream",Material.MAGMA_CREAM,96,1);
        add(items,"ghast_tear","Ghast Tear",Material.GHAST_TEAR,350,1);
        add(items,"quartz","Netherworld Quartz",Material.QUARTZ,30,1);
        add(items,"soul_sand","Soul Sand",Material.SOUL_SAND,50,1);
        add(items,"magma_block","Magma Block",Material.MAGMA_BLOCK,35,1);
        add(items,"weeping_obsidian","Weeping Obsidian",Material.CRYING_OBSIDIAN,150,1);

        add(items,"obsidian","Obsidian",Material.OBSIDIAN,100,1);
        add(items,"end_crystal","End Crystal",Material.END_CRYSTAL,350,1);
        add(items,"respawn_anchor","Respawn Point",Material.RESPAWN_ANCHOR,1000,1);
        add(items,"glowstone","Glow Stone",Material.GLOWSTONE,100,1);
        add(items,"totem","Immortality Amulet",Material.TOTEM_OF_UNDYING,1500,1);
        add(items,"ender_pearl_gear","Ender's Pearl",Material.ENDER_PEARL,75,1);
        add(items,"golden_apple_gear","Golden Apple",Material.GOLDEN_APPLE,250,1);
        add(items,"enchanted_bottle","Enchanted Bottle",Material.EXPERIENCE_BOTTLE,100,1);
        add(items,"slow_arrow","Slow-Falling Arrow",Material.TIPPED_ARROW,500,1);

        add(items,"potato","Potatoes",Material.POTATO,96,1);
        add(items,"sweet_berries","Sweet Berries",Material.SWEET_BERRIES,50,1);
        add(items,"melon_slice","Watermelon Slice",Material.MELON_SLICE,36,1);
        add(items,"carrot","Carrot",Material.CARROT,96,1);
        add(items,"apple","Apple",Material.APPLE,25,1);
        add(items,"cooked_chicken","Cooked Chicken",Material.COOKED_CHICKEN,48,1);
        add(items,"steak","Steak",Material.COOKED_BEEF,35,1);
        add(items,"golden_carrot","Golden Carrot",Material.GOLDEN_CARROT,120,1);
        add(items,"golden_apple_food","Golden Apple",Material.GOLDEN_APPLE,250,1);

        shardItems.clear();
        addShard("pig_generator","Pig Generator",Material.SPAWNER,1500,"PIG");
        addShard("cow_generator","Cow Generator",Material.SPAWNER,1500,"COW");
        addShard("zombie_generator","Zombie Generator",Material.SPAWNER,1500,"ZOMBIE");
        addShard("spider_generator","Spider Generator",Material.SPAWNER,1500,"SPIDER");
        addShard("skeleton_generator","Skeleton Generator",Material.SPAWNER,1500,"SKELETON");
        addShard("reptile_generator","Reptile Generator",Material.SPAWNER,1500,"CREEPER");
        addShard("zombie_pig_generator","Zombie Pig Generator",Material.SPAWNER,1500,"ZOMBIFIED_PIGLIN");
        addShard("flame_generator","Flame Generator",Material.SPAWNER,1500,"BLAZE");
        addShard("iron_golem_generator","Iron Golem Generator",Material.SPAWNER,1500,"IRON_GOLEM");
        addShard("regular_key","Regular Key",Material.TRIPWIRE_HOOK,200,"");
        addShard("crimson_key","Crimson Key",Material.TRIPWIRE_HOOK,2500,"");
        addShard("prime_key","Prime Key",Material.TRIPWIRE_HOOK,2500,"");
    }

    private void add(Map<String,ShopItem> map,String id,String name,Material mat,double price,int amount){map.put(id,new ShopItem(id,name,mat,price,amount,false,""));}
    private void addShard(String id,String name,Material mat,int price,String entity){shardItems.put(id,new ShopItem(id,name,mat,price,1,true,entity));}

    private void openMain(Player p) {
        Inventory inv=Bukkit.createInventory(null,27,MAIN);
        button(inv,10,"End",Material.END_STONE,END);
        button(inv,12,"Netherworld",Material.NETHERRACK,NETHER);
        button(inv,14,"Gear",Material.NETHERITE_SWORD,GEAR);
        button(inv,16,"Food",Material.COOKED_BEEF,FOOD);
        button(inv,22,"Shard Shop",Material.AMETHYST_SHARD,SHARD);
        p.openInventory(inv);
    }

    private void button(Inventory inv,int slot,String name,Material mat,String title){ItemStack i=new ItemStack(mat); ItemMeta m=i.getItemMeta(); m.setDisplayName(ChatColor.GOLD+name); m.setLore(List.of(ChatColor.GRAY+"Open "+title)); i.setItemMeta(m); inv.setItem(slot,i);}

    private void openCategory(Player p,String category) {
        Inventory inv=Bukkit.createInventory(null,54,category);
        int index=0;
        for(ShopItem s: items.values()) if(categoryFor(s).equals(category)) inv.setItem(slotFor(index++), display(s));
        p.openInventory(inv);
    }

    private int slotFor(int i){int[] slots={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43}; return slots[Math.min(i,slots.length-1)];}
    private String categoryFor(ShopItem s){
        if(Set.of("end_chest","ender_pearl","end_stone","dragons_breath","end_rod","chorus_fruit","popped_chorus","shulker_shell","shulker_box").contains(s.id)) return END;
        if(Set.of("blaze_rod","nether_wart","glowstone_dust","magma_cream","ghast_tear","quartz","soul_sand","magma_block","weeping_obsidian").contains(s.id)) return NETHER;
        if(Set.of("obsidian","end_crystal","respawn_anchor","glowstone","totem","ender_pearl_gear","golden_apple_gear","enchanted_bottle","slow_arrow").contains(s.id)) return GEAR;
        return FOOD;
    }

    private void openShardShop(Player p){
        Inventory inv=Bukkit.createInventory(null,54,SHARD);
        int i=0; for(ShopItem s: shardItems.values()) inv.setItem(slotFor(i++),display(s));
        p.openInventory(inv);
    }

    private ItemStack display(ShopItem s){
        ItemStack i=new ItemStack(s.material,s.amount); ItemMeta m=i.getItemMeta();
        m.setDisplayName(ChatColor.GOLD+s.name);
        String price=s.shard?"&b"+s.price+" Shards":"&6$"+money(s.price);
        m.setLore(List.of(ChatColor.GRAY+"Price: "+ChatColor.translateAlternateColorCodes('&',price),ChatColor.YELLOW+"Click to buy"));
        i.setItemMeta(m); return i;
    }

    @EventHandler public void onClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)) return;
        String title=String.valueOf(e.getView().getTitle());
        if(title.equals(MAIN)||title.equals(END)||title.equals(NETHER)||title.equals(GEAR)||title.equals(FOOD)||title.equals(SHARD)){
            e.setCancelled(true);
            if(e.getClickedInventory()!=e.getView().getTopInventory()) return;
            ItemStack clicked=e.getCurrentItem(); if(clicked==null||clicked.getType()==Material.AIR)return;
            if(title.equals(MAIN)){
                if(e.getSlot()==10)openCategory(p,END); else if(e.getSlot()==12)openCategory(p,NETHER); else if(e.getSlot()==14)openCategory(p,GEAR); else if(e.getSlot()==16)openCategory(p,FOOD); else if(e.getSlot()==22)openShardShop(p);
                return;
            }
            ShopItem found=findByDisplay(clicked,title); if(found!=null)buy(p,found);
        }
    }

    @EventHandler public void onDrag(InventoryDragEvent e){
        String t=String.valueOf(e.getView().getTitle());
        if(t.equals(MAIN)||t.equals(END)||t.equals(NETHER)||t.equals(GEAR)||t.equals(FOOD)||t.equals(SHARD))e.setCancelled(true);
    }

    private ShopItem findByDisplay(ItemStack clicked,String title){
        ItemMeta m=clicked.getItemMeta(); if(m==null||!m.hasDisplayName())return null;
        String plain=ChatColor.stripColor(m.getDisplayName());
        Map<String,ShopItem> map=title.equals(SHARD)?shardItems:items;
        for(ShopItem s:map.values()) if(s.name.equals(plain)) return s;
        return null;
    }

    private void buy(Player p,ShopItem s){
        UUID id=p.getUniqueId();
        if(s.shard){
            int bal=shards.getOrDefault(id,0);
            if(bal<s.price){msg(p,"&cYou need &b"+s.price+" Shards&c.");return;}
            shards.put(id,bal-(int)s.price);
            ItemStack item=new ItemStack(s.material,s.amount); ItemMeta meta=item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD+s.name);
            meta.getPersistentDataContainer().set(generatorKey,PersistentDataType.STRING,s.entity);
            item.setItemMeta(meta); p.getInventory().addItem(item); saveBalances();
            msg(p,"&aPurchased &f"+s.name+"&a for &b"+(int)s.price+" Shards&a."); return;
        }
        double bal=balances.getOrDefault(id,0D);
        if(bal<s.price){msg(p,"&cYou need &6$"+money(s.price)+"&c.");return;}
        balances.put(id,bal-s.price); p.getInventory().addItem(new ItemStack(s.material,s.amount)); saveBalances();
        msg(p,"&aPurchased &f"+s.name+"&a for &6$"+money(s.price)+"&a.");
    }

    @EventHandler public void onPlace(BlockPlaceEvent e){
        ItemStack item=e.getItemInHand(); ItemMeta m=item.getItemMeta(); if(m==null)return;
        String type=m.getPersistentDataContainer().get(generatorKey,PersistentDataType.STRING);
        if(type==null||e.getBlockPlaced().getType()!=Material.SPAWNER)return;
        try { EntityType t=EntityType.valueOf(type); CreatureSpawner sp=(CreatureSpawner)e.getBlockPlaced().getState(); sp.setSpawnedType(t); sp.update(true,false); } catch(Exception ignored){}
    }

    @EventHandler public void onJoin(PlayerJoinEvent e){
        UUID id=e.getPlayer().getUniqueId();
        balances.putIfAbsent(id,getConfig().getDouble("balances."+id,0D));
        shards.putIfAbsent(id,getConfig().getInt("shards."+id,0));
    }

    private void loadBalances(){
        if(getConfig().getConfigurationSection("balances")!=null)
            for(String k:getConfig().getConfigurationSection("balances").getKeys(false)) try{balances.put(UUID.fromString(k),getConfig().getDouble("balances."+k));}catch(Exception ignored){}
        if(getConfig().getConfigurationSection("shards")!=null)
            for(String k:getConfig().getConfigurationSection("shards").getKeys(false)) try{shards.put(UUID.fromString(k),getConfig().getInt("shards."+k));}catch(Exception ignored){}
    }

    private void saveBalances(){
        for(UUID id:new HashSet<>(balances.keySet())) getConfig().set("balances."+id,balances.get(id));
        for(UUID id:new HashSet<>(shards.keySet())) getConfig().set("shards."+id,shards.get(id));
        saveConfig();
    }

    private String money(double d){return String.format(Locale.US,"%.2f",d).replaceAll("\\.?0+$","");}
    private void msg(Player p,String s){p.sendMessage(ChatColor.translateAlternateColorCodes('&',"&8[&6AizenShop&8] &r"+s));}
    private record ShopItem(String id,String name,Material material,double price,int amount,boolean shard,String entity){}
}
