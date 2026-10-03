package com.aizen.smp;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DashboardBridge implements Listener {
 private final JavaPlugin plugin; private final String url; private final String token;
 public DashboardBridge(JavaPlugin plugin){this.plugin=plugin;this.url=plugin.getConfig().getString("dashboard.url","").trim();this.token=plugin.getConfig().getString("dashboard.token","").trim();}
 public void start(){if(url.isEmpty()){plugin.getLogger().warning("Dashboard API URL is empty; dashboard bridge is disabled.");return;}Bukkit.getScheduler().runTaskTimer(plugin,this::snapshotStats,20L,200L);Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,this::pollCommands,40L,40L);plugin.getLogger().info("Dashboard bridge enabled.");}
 @EventHandler public void onJoin(PlayerJoinEvent e){sendEvent("join",e.getPlayer().getName(),"Connection","Online","");snapshotStats();}
 @EventHandler public void onQuit(PlayerQuitEvent e){sendEvent("quit",e.getPlayer().getName(),"Connection","Offline","");snapshotStats();}
 @EventHandler public void onKick(PlayerKickEvent e){sendEvent("kick",e.getPlayer().getName(),"Server Kick","Kicked",e.getReason()==null?"No reason provided":e.getReason());snapshotStats();}
 @EventHandler public void onPlayerCommand(PlayerCommandPreprocessEvent e){inspectBanCommand(e.getMessage());}
 @EventHandler public void onServerCommand(ServerCommandEvent e){inspectBanCommand("/"+e.getCommand());}

 private void inspectBanCommand(String command){String raw=command==null?"":command.trim();if(raw.startsWith("/"))raw=raw.substring(1);String lower=raw.toLowerCase(Locale.ROOT);if(!(lower.startsWith("ban ")||lower.startsWith("minecraft:ban ")||lower.startsWith("tempban ")||lower.startsWith("ban-ip ")))return;String[] p=raw.split("\\s+");if(p.length<2)return;String player=p[1];String requested=p.length>=3?join(p,2):"Permanent";Bukkit.getScheduler().runTaskLater(plugin,()->{BanEntry entry=Bukkit.getBanList(BanList.Type.NAME).getBanEntry(player);String reason=requested,duration="Permanent";if(entry!=null){if(entry.getReason()!=null&&!entry.getReason().isBlank())reason=entry.getReason();if(entry.getExpiration()!=null)duration=entry.getExpiration().toString();}sendEvent("ban",player,"Ban","Banned",reason,duration);},1L);}
 private String join(String[] a,int s){StringBuilder b=new StringBuilder();for(int i=s;i<a.length;i++){if(b.length()>0)b.append(' ');b.append(a[i]);}return b.toString();}

 private void snapshotStats(){if(url.isEmpty())return;List<Player> ps=new ArrayList<>(Bukkit.getOnlinePlayers());StringBuilder players=new StringBuilder("["),admins=new StringBuilder("[");boolean fp=true,fa=true;for(Player p:ps){if(!fp)players.append(",");fp=false;String role=getRole(p);players.append("{\"player\":\"").append(escape(p.getName())).append("\",\"status\":\"Online\",\"violations\":0,\"op\":").append(p.isOp()).append(",\"role\":\"").append(escape(role)).append("\"}");if(p.isOp()||isStaff(p)){if(!fa)admins.append(",");fa=false;admins.append("{\"player\":\"").append(escape(p.getName())).append("\",\"op\":").append(p.isOp()).append(",\"role\":\"").append(escape(role)).append("\",\"permissions\":").append(permissionJson(p)).append("}");}}players.append("]");admins.append("]");post("{\"type\":\"stats\",\"online\":"+ps.size()+",\"players\":"+players+",\"admins\":"+admins+",\"time\":\""+escape(Instant.now().toString())+"\"}");}
 private boolean isStaff(Player p){return p.isOp()||p.hasPermission("aizen.owner")||p.hasPermission("aizen.admin")||p.hasPermission("aizen.moderator")||p.hasPermission("aizen.staff")||p.hasPermission("luckperms.group.owner")||p.hasPermission("luckperms.group.admin")||p.hasPermission("luckperms.group.moderator")||p.hasPermission("luckperms.group.staff")||p.hasPermission("essentials.ban");}
 private String getRole(Player p){if(p.isOp())return "OP";if(p.hasPermission("aizen.owner")||p.hasPermission("luckperms.group.owner"))return "Owner";if(p.hasPermission("aizen.admin")||p.hasPermission("luckperms.group.admin"))return "Admin";if(p.hasPermission("aizen.developer")||p.hasPermission("luckperms.group.developer"))return "Developer";if(p.hasPermission("aizen.moderator")||p.hasPermission("luckperms.group.moderator"))return "Moderator";if(p.hasPermission("aizen.support")||p.hasPermission("luckperms.group.support"))return "Support";if(p.hasPermission("aizen.staff")||p.hasPermission("luckperms.group.staff"))return "Staff";return "Player";}
 private String permissionJson(Player p){StringBuilder s=new StringBuilder("[");boolean f=true;for(org.bukkit.permissions.PermissionAttachmentInfo x:p.getEffectivePermissions()){if(!x.getValue())continue;if(!f)s.append(",");f=false;s.append("\"").append(escape(x.getPermission())).append("\"");}return s.append("]").toString();}
 public void sendEvent(String type,String player,String detection,String action,String reason){sendEvent(type,player,detection,action,reason,"");}
 public void sendEvent(String type,String player,String detection,String action,String reason,String duration){if(url.isEmpty())return;post("{\"type\":\""+escape(type)+"\",\"player\":\""+escape(player)+"\",\"detection\":\""+escape(detection)+"\",\"action\":\""+escape(action)+"\",\"reason\":\""+escape(reason)+"\",\"duration\":\""+escape(duration)+"\",\"time\":\""+Instant.now()+"\"}");}

 private void pollCommands(){if(url.isEmpty()||token.isEmpty())return;try{String json=get(url+"/api/commands");Matcher m=Pattern.compile("\\{\\s*\\"id\\":\\"([^\\"]+)\\".*?\\"command\\":\\"([^\\"]*)\\".*?\\}").matcher(json);while(m.find()){String id=unescape(m.group(1)),cmd=unescape(m.group(2));Bukkit.getScheduler().runTask(plugin,()->executeDashboardCommand(id,cmd));}}catch(Exception e){plugin.getLogger().warning("Dashboard command poll failed: "+e.getMessage());}}
 private void executeDashboardCommand(String id,String command){String clean=command.trim();if(clean.startsWith("/"))clean=clean.substring(1);boolean ok=false;String result="";try{if(clean.equalsIgnoreCase("restart")){ok=Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"restart");result="Restart command dispatched";}else{ok=Bukkit.dispatchCommand(Bukkit.getConsoleSender(),clean);result=ok?"Command dispatched":"Command returned false";}}catch(Exception e){result=e.getMessage()==null?e.toString():e.getMessage();}sendCommandResult(id,ok,result);}
 private void sendCommandResult(String id,boolean ok,String result){String json="{\"id\":\""+escape(id)+"\",\"ok\":"+ok+",\"result\":\""+escape(result)+"\"}";postTo("/api/command-result",json);}
 private String get(String endpoint)throws Exception{HttpURLConnection c=(HttpURLConnection)URI.create(endpoint).toURL().openConnection();c.setRequestMethod("GET");c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestProperty("Authorization","Bearer "+token);try(InputStream in=c.getInputStream()){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}finally{c.disconnect();}}
 private void post(String json){postTo("/api/event",json);}
 private void postTo(String endpoint,String json){Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try{HttpURLConnection c=(HttpURLConnection)URI.create(url+endpoint).toURL().openConnection();c.setRequestMethod("POST");c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");if(!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);try(OutputStream out=c.getOutputStream()){out.write(json.getBytes(StandardCharsets.UTF_8));}c.getResponseCode();c.disconnect();}catch(Exception ignored){}});}
 private String unescape(String v){return v.replace("\\\"","\"").replace("\\\\","\\");}
 private String escape(String v){return v==null?"":v.replace("\\","\\\\").replace("\"","\\\"");}
}