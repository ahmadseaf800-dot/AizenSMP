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
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class AizenShop extends JavaPlugin implements Listener {
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, Integer> shards = new HashMap<>();
    private final Map<String, ShopItem> items = new LinkedHashMap<>();
    private final Map<String, ShopItem> shardItems = new LinkedHashMap<>();
    private final Map<Material, Double> sellPrices = new EnumMap<>(Material.class);
    private final Map<UUID, ItemStack[]> enderChests = new HashMap<>();
    private final Map<UUID, PurchaseState> pendingPurchases = new HashMap<>();
    private final NamespacedKey generatorKey;
    private static final String BUY_PREFIX = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "Buy: ";
    private static final int MAX_PURCHASE = 64;

    private static final String SELL = ChatColor.DARK_GRAY + "✦ " + ChatColor.GREEN + "SELL" + ChatColor.DARK_GRAY + " ✦";
    private static final String EC = ChatColor.DARK_GRAY + "✦ " + ChatColor.AQUA + "ENDER CHEST" + ChatColor.DARK_GRAY + " ✦";

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
        loadEnderChests();
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
        Objects.requireNonNull(getCommand("sell")).setExecutor((s,c,l,a) -> {
            if (s instanceof Player p) openSell(p);
            return true;
        });
        Objects.requireNonNull(getCommand("enderchest")).setExecutor((s,c,l,a) -> {
            if (s instanceof Player p) openEnderChest(p);
            return true;
        });
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override public void onDisable() { saveBalances(); saveEnderChests(); }

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

        // Fixed sell prices. Shop items sell for 40% of their unit purchase value.
        sellPrices.clear();
        for (ShopItem s : items.values()) {
            sellPrices.put(s.material, Math.max(1D, (s.price / Math.max(1, s.amount)) * 0.40D));
        }
        // Useful farm/build materials that are also sellable even if not bought in /shop.
        fixedSell(Material.COBBLESTONE, 2); fixedSell(Material.STONE, 3);
        fixedSell(Material.DIRT, 1); fixedSell(Material.SAND, 2); fixedSell(Material.GRAVEL, 2);
        fixedSell(Material.OAK_LOG, 8); fixedSell(Material.SPRUCE_LOG, 8); fixedSell(Material.BIRCH_LOG, 8);
        fixedSell(Material.DIAMOND, 350); fixedSell(Material.DIAMOND_BLOCK, 3150);
        fixedSell(Material.EMERALD, 120); fixedSell(Material.EMERALD_BLOCK, 1080);
        fixedSell(Material.IRON_INGOT, 35); fixedSell(Material.GOLD_INGOT, 55);
        fixedSell(Material.NETHERITE_SCRAP, 500); fixedSell(Material.NETHERITE_INGOT, 2200);
        fixedSell(Material.REDSTONE, 8); fixedSell(Material.LAPIS_LAZULI, 6);
        fixedSell(Material.COAL, 10); fixedSell(Material.COPPER_INGOT, 12);
        fixedSell(Material.NETHERITE_BLOCK, 19800); fixedSell(Material.OBSIDIAN, 40);
        fixedSell(Material.EXPERIENCE_BOTTLE, 35); fixedSell(Material.SPAWNER, 250);
    }

    private void fixedSell(Material material, double price) { sellPrices.put(material, price); }

    private void add(Map<String,ShopItem> map,String id,String name,Material mat,double price,int amount){map.put(id,new ShopItem(id,name,mat,price,amount,false,""));}
    private void addShard(String id,String name,Material mat,int price,String entity){shardItems.put(id,new ShopItem(id,name,mat,price,1,true,entity));}

    private void openSell(Player p) {
        Inventory inv=Bukkit.createInventory(null,54,SELL);
        ItemStack sellButton=new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sm=sellButton.getItemMeta();
        sm.setDisplayName(ChatColor.GREEN+"Sell Items");
        sm.setLore(List.of(ChatColor.GRAY+"Put any items in the slots above.",ChatColor.GRAY+"Click to sell everything for its fixed price."));
        sellButton.setItemMeta(sm);
        inv.setItem(49,sellButton);

        ItemStack info=new ItemStack(Material.PAPER);
        ItemMeta im=info.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW+"Sell Prices");
        im.setLore(List.of(ChatColor.GRAY+"Every item has a fixed sell price.",ChatColor.GRAY+"Unknown items sell for $10 each."));
        info.setItemMeta(im);
        inv.setItem(45,info);
        p.openInventory(inv);
    }

    private void openEnderChest(Player p) {
        ItemStack[] saved=enderChests.computeIfAbsent(p.getUniqueId(), k -> new ItemStack[54]);
        Inventory inv=Bukkit.createInventory(null,54,EC);
        for(int i=0;i<54;i++) if(saved[i]!=null) inv.setItem(i,saved[i].clone());
        p.openInventory(inv);
    }

    private void saveEnderChest(Player p, Inventory inv) {
        ItemStack[] data=new ItemStack[54];
        for(int i=0;i<54;i++) {
            ItemStack item=inv.getItem(i);
            if(item!=null && item.getType()!=Material.AIR) data[i]=item.clone();
        }
        enderChests.put(p.getUniqueId(),data);
    }

    private void sellContents(Player p, Inventory inv) {
        double total=0D;
        int soldStacks=0;
        for(int i=0;i<45;i++) {
            ItemStack item=inv.getItem(i);
            if(item==null || item.getType()==Material.AIR) continue;
            double unit=sellPrices.getOrDefault(item.getType(),10D);
            total += unit * item.getAmount();
            soldStacks++;
            inv.setItem(i,null);
        }
        if(total<=0) {
            msg(p,"&cضع أغراضًا في خانات البيع أولاً.");
            return;
        }
        UUID id=p.getUniqueId();
        balances.put(id,balances.getOrDefault(id,0D)+total);
        saveBalances();
        msg(p,"&aتم بيع &f"+soldStacks+" &aخانات مقابل &6$"+money(total)+"&a.");
    }

    private String serializeEnderChest(ItemStack[] items) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(BukkitObjectOutputStream out=new BukkitObjectOutputStream(bytes)) {
                out.writeInt(items.length);
                for(ItemStack item:items) out.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch(Exception ex) { getLogger().warning("Could not save an ender chest: "+ex.getMessage()); return ""; }
    }

    private ItemStack[] deserializeEnderChest(String encoded) {
        ItemStack[] items=new ItemStack[54];
        if(encoded==null || encoded.isBlank()) return items;
        try {
            byte[] bytes=Base64.getDecoder().decode(encoded);
            try(BukkitObjectInputStream in=new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                int len=Math.min(54,in.readInt());
                for(int i=0;i<len;i++) items[i]=(ItemStack)in.readObject();
            }
        } catch(Exception ex) { getLogger().warning("Could not load an ender chest: "+ex.getMessage()); }
        return items;
    }

    private void loadEnderChests() {
        if(getConfig().getConfigurationSection("enderchests")==null) return;
        for(String key:getConfig().getConfigurationSection("enderchests").getKeys(false)) {
            try { enderChests.put(UUID.fromString(key),deserializeEnderChest(getConfig().getString("enderchests."+key,""))); }
            catch(Exception ignored) {}
        }
    }

    private void saveEnderChests() {
        for(Map.Entry<UUID,ItemStack[]> entry:enderChests.entrySet())
            getConfig().set("enderchests."+entry.getKey(),serializeEnderChest(entry.getValue()));
        saveConfig();
    }

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

        if(title.startsWith(BUY_PREFIX)) {
            e.setCancelled(true);
            if(e.getClickedInventory()!=e.getView().getTopInventory()) return;
            PurchaseState state=pendingPurchases.get(p.getUniqueId());
            if(state==null) return;
            int amount=state.amount();
            if(e.getSlot()==20) amount=Math.max(1,amount-16);
            else if(e.getSlot()==21) amount=Math.max(1,amount-1);
            else if(e.getSlot()==22) amount=1;
            else if(e.getSlot()==24) amount=Math.min(MAX_PURCHASE,amount+1);
            else if(e.getSlot()==25) amount=Math.min(MAX_PURCHASE,amount+16);
            else if(e.getSlot()==26) amount=MAX_PURCHASE;
            else if(e.getSlot()==31) { finishPurchase(p); return; }
            else if(e.getSlot()==35) {
                String category=state.category();
                pendingPurchases.remove(p.getUniqueId());
                returnToShop(p,category);
                return;
            } else return;
            pendingPurchases.put(p.getUniqueId(),new PurchaseState(state.item(),amount,state.category()));
            renderPurchase(p);
            return;
        }

        if(title.equals(SELL)) {
            if(e.getClickedInventory()!=e.getView().getTopInventory()) return;
            if(e.getSlot()==49) { e.setCancelled(true); sellContents(p,e.getView().getTopInventory()); }
            else if(e.getSlot()>=45) e.setCancelled(true);
            return;
        }

        if(title.equals(EC)) return;

        if(title.equals(MAIN)||title.equals(END)||title.equals(NETHER)||title.equals(GEAR)||title.equals(FOOD)||title.equals(SHARD)){
            e.setCancelled(true);
            if(e.getClickedInventory()!=e.getView().getTopInventory()) return;
            ItemStack clicked=e.getCurrentItem(); if(clicked==null||clicked.getType()==Material.AIR)return;
            if(title.equals(MAIN)){
                if(e.getSlot()==10)openCategory(p,END); else if(e.getSlot()==12)openCategory(p,NETHER); else if(e.getSlot()==14)openCategory(p,GEAR); else if(e.getSlot()==16)openCategory(p,FOOD); else if(e.getSlot()==22)openShardShop(p);
                return;
            }
            ShopItem found=findByDisplay(clicked,title); if(found!=null)openPurchase(p,found,title);
        }
    }

    @EventHandler public void onDrag(InventoryDragEvent e){
        String t=String.valueOf(e.getView().getTitle());
        if(t.startsWith(BUY_PREFIX)) {
            e.setCancelled(true);
            return;
        }
        if(t.equals(MAIN)||t.equals(END)||t.equals(NETHER)||t.equals(GEAR)||t.equals(FOOD)||t.equals(SHARD)) {
            e.setCancelled(true);
        } else if(t.equals(SELL)) {
            for(int slot:e.getRawSlots()) if(slot>=45) { e.setCancelled(true); break; }
        }
    }

    @EventHandler public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if(!(e.getPlayer() instanceof Player p)) return;
        String title=String.valueOf(e.getView().getTitle());
        if(title.equals(EC)) saveEnderChest(p,e.getInventory());
        if(title.startsWith(BUY_PREFIX)) {
            pendingPurchases.remove(p.getUniqueId());
            return;
        }
        if(title.equals(SELL)) {
            // Return unsold items to the player when /sell is closed.
            for(int i=0;i<45;i++) {
                ItemStack item=e.getInventory().getItem(i);
                if(item!=null && item.getType()!=Material.AIR) {
                    Map<Integer,ItemStack> left=p.getInventory().addItem(item.clone());
                    left.values().forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(),rest));
                }
            }
        }
    }

    private ShopItem findByDisplay(ItemStack clicked,String title){
        ItemMeta m=clicked.getItemMeta(); if(m==null||!m.hasDisplayName())return null;
        String plain=ChatColor.stripColor(m.getDisplayName());
        Map<String,ShopItem> map=title.equals(SHARD)?shardItems:items;
        for(ShopItem s:map.values()) if(s.name.equals(plain)) return s;
        return null;
    }

    private void openPurchase(Player p, ShopItem s, String category) {
        pendingPurchases.put(p.getUniqueId(), new PurchaseState(s, 1, category));
        renderPurchase(p);
    }

    private void renderPurchase(Player p) {
        PurchaseState state=pendingPurchases.get(p.getUniqueId());
        if(state==null) return;
        ShopItem s=state.item();
        Inventory inv=Bukkit.createInventory(null,45,BUY_PREFIX+ChatColor.stripColor(s.name)+ChatColor.DARK_GRAY+" ✦");
        ItemStack preview=new ItemStack(s.material,Math.min(state.amount(),s.material.getMaxStackSize()));
        ItemMeta pm=preview.getItemMeta();
        pm.setDisplayName(ChatColor.GOLD+s.name);
        pm.setLore(List.of(ChatColor.GRAY+"Quantity: "+ChatColor.WHITE+state.amount(),
                ChatColor.GRAY+"Total: "+totalText(s,state.amount())));
        preview.setItemMeta(pm);
        inv.setItem(13,preview);

        glassButton(inv,20,Material.RED_STAINED_GLASS_PANE,"-16","&cDecrease by 16");
        glassButton(inv,21,Material.RED_STAINED_GLASS_PANE,"-1","&cDecrease by 1");
        glassButton(inv,22,Material.RED_STAINED_GLASS_PANE,"1","&cSet minimum: 1");
        glassButton(inv,24,Material.GREEN_STAINED_GLASS_PANE,"+1","&aIncrease by 1");
        glassButton(inv,25,Material.GREEN_STAINED_GLASS_PANE,"+16","&aIncrease by 16");
        glassButton(inv,26,Material.GREEN_STAINED_GLASS_PANE,"64","&aSet to 64");

        ItemStack confirm=new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta cm=confirm.getItemMeta();
        cm.setDisplayName(ChatColor.GREEN+"Confirm Purchase");
        cm.setLore(List.of(ChatColor.GRAY+"Buy "+state.amount()+" x "+s.name,ChatColor.GRAY+"Total: "+totalText(s,state.amount())));
        confirm.setItemMeta(cm);
        inv.setItem(31,confirm);

        ItemStack cancel=new ItemStack(Material.BARRIER);
        ItemMeta xm=cancel.getItemMeta();
        xm.setDisplayName(ChatColor.RED+"Cancel");
        xm.setLore(List.of(ChatColor.GRAY+"Return to the shop."));
        cancel.setItemMeta(xm);
        inv.setItem(35,cancel);
        p.openInventory(inv);
    }

    private void glassButton(Inventory inv,int slot,Material mat,String name,String lore) {
        ItemStack i=new ItemStack(mat);
        ItemMeta m=i.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&',lore)+" "+name);
        i.setItemMeta(m);
        inv.setItem(slot,i);
    }

    private String totalText(ShopItem s,int amount) {
        double total=s.price*amount;
        return s.shard ? ChatColor.AQUA+money(total)+" Shards" : ChatColor.GOLD+"$"+money(total);
    }

    private void finishPurchase(Player p) {
        PurchaseState state=pendingPurchases.get(p.getUniqueId());
        if(state==null) return;
        ShopItem s=state.item();
        int amount=Math.max(1,Math.min(MAX_PURCHASE,state.amount()));
        double total=s.price*amount;
        UUID id=p.getUniqueId();

        if(s.shard) {
            int bal=shards.getOrDefault(id,0);
            if(bal<total){msg(p,"&cYou need &b"+money(total)+" Shards&c.");return;}
            shards.put(id,bal-(int)total);
        } else {
            double bal=balances.getOrDefault(id,0D);
            if(bal<total){msg(p,"&cYou need &6$"+money(total)+"&c.");return;}
            balances.put(id,bal-total);
        }

        ItemStack item=new ItemStack(s.material,amount);
        if(s.shard) {
            ItemMeta meta=item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD+s.name);
            meta.getPersistentDataContainer().set(generatorKey,PersistentDataType.STRING,s.entity);
            item.setItemMeta(meta);
        }
        Map<Integer,ItemStack> left=p.getInventory().addItem(item);
        left.values().forEach(rest->p.getWorld().dropItemNaturally(p.getLocation(),rest));
        saveBalances();
        msg(p,"&aPurchased &f"+amount+" x "+s.name+"&a for "+totalText(s,amount)+"&a.");
        String category=state.category();
        pendingPurchases.remove(id);
        returnToShop(p,category);
    }

    private void returnToShop(Player p,String category) {
        if(SHARD.equals(category)) openShardShop(p);
        else if(MAIN.equals(category)) openMain(p);
        else openCategory(p,category);
    }

    private record PurchaseState(ShopItem item,int amount,String category) {}

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
