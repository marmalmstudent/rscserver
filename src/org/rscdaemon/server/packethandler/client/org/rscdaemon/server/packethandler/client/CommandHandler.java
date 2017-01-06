package org.rscdaemon.server.packethandler.client;

import org.rscdaemon.server.GUI;
import org.rscdaemon.server.packethandler.PacketHandler;
import org.rscdaemon.server.model.*;
import org.rscdaemon.server.net.Packet;
import org.rscdaemon.server.util.DataConversions;
import org.rscdaemon.server.entityhandling.EntityHandler;
import org.rscdaemon.server.util.Logger;
import org.rscdaemon.server.util.Formulae;
import org.rscdaemon.server.event.SingleEvent;
import org.rscdaemon.server.states.CombatState;

import org.apache.mina.common.IoSession;

public class CommandHandler implements PacketHandler {
	/**
	 * World instance
	 */
	public static final World world = World.getWorld();

	public void handlePacket(Packet p, IoSession session) throws Exception {
		Player player = (Player)session.getAttachment();
		if(player.isBusy()) {
			player.resetPath();
			return;
		}
		player.resetAll();
		String s = new String(p.getData()).trim();
		int firstSpace = s.indexOf(" ");
		String cmd = s;
		String[] args = new String[0];
		if(firstSpace != -1) {
			cmd = s.substring(0, firstSpace).trim();
			args = s.substring(firstSpace + 1).trim().split(" ");
		}
		try {
			handleCommand(cmd.toLowerCase(), args, player);
		}
		catch(Exception e) { }
	}

	public void handleCommand(String cmd, String[] args, Player player) throws Exception {
		//if(args[0] != null) 
			//args[0] = args[0].replace("_", "");
		if(cmd.equals("stuck")) {
			if(player.getLocation().inModRoom() && !player.isMod()) {
				player.getActionSender().sendMessage("You cannot use ::stuck here");
			}
			else
				if(player.getLocation().inWilderness()){
					player.getActionSender().sendMessage("Cannot use stuck in wilderness");
				}
				else if(System.currentTimeMillis() - player.getLastMoved() < 10000 || System.currentTimeMillis() - player.getCastTimer() < 10000) {
					player.getActionSender().sendMessage("There is a 10 second delay on using ::stuck, please stand still for 10 secs");
				}
				else if(!player.inCombat() && System.currentTimeMillis() - player.getCombatTimer() > 10000) {
					player.setCastTimer();
					player.teleport(220, 440, true);
				}
				else {
					player.getActionSender().sendMessage("You cannot use ::stuck for 10 seconds after combat");
				}
			return;
		}
		if(cmd.equals("online"))
		{
			String msg = "There are currently @cya@" + world.getPlayers().size() + " @whi@players on this server.";

			player.getActionSender().sendMessage("@cya@Server: @whi@" + msg);
			return;
		}
		
		if(!player.isPMod()) {
			return;
		}
		if (cmd.equals("say")) {
			StringBuilder sb = new StringBuilder(100);
			sb.append(player.getUsername()).append(": @gre@");
			for (String s : args) {
				sb.append(s).append(" ");
			}

			String message = sb.toString();
			message = message.substring(0, message.length() - 1);
			for (Player p : world.getPlayers()) {
				p.getActionSender().sendMessage(message);
			}
			return;
		}
		

		
		if(cmd.equals("info")) {
			player.getActionSender().sendMessage("Info is not avaliable.");
			return;
			//if(args.length != 1) {
			//	player.getActionSender().sendMessage("Invalid args. Syntax: INFO name");
				//return;
			//}
		//	
			//Logger.print("::info has been removed due to no LS", 1);
			//loginServer.requestPlayerInfo(player, DataConversions.usernameToHash(args[0]));
		//	return;
		}
		if(cmd.equalsIgnoreCase("ban") || cmd.equalsIgnoreCase("unban")) {
			boolean banned = cmd.equalsIgnoreCase("ban");
			if(args.length != 1) {
				player.getActionSender().sendMessage("Invalid args. Syntax: " + (banned ? "BAN" : "UNBAN") + " name");
				return;
			}
			if(banned) {
				if(Integer.valueOf(GUI.readValue(args[0], "rank")) == 6) {
					player.getActionSender().sendMessage("Target is already banned");
					return;
				} else {
					world.banPlayer(args[0]);
					Logger.mod(player.getUsername() + " has banned " + args[0]);
				}				
			} else {
					if(Integer.valueOf(GUI.readValue(args[0], "rank")) == 6) {
					world.unbanPlayer(args[0]);
					Logger.mod(player.getUsername() + " has unbanned " + args[0]);
					} else {
						player.getActionSender().sendMessage("Target is not banned");
					}
			}
			return;
		} 
	if(cmd.equals("modroom")) {
		if(args.length != 1) {
			player.getActionSender().sendMessage("Invalid Syntax, use ::moodroom <playername");
			return;
		}
		if(GUI.isOnline(args[0])) {
			Player target = world.getPlayer(DataConversions.usernameToHash(args[0]));
			Logger.mod(player.getUsername() + " has teleported " + target.getUsername() + " to the mod room");
			player.teleport(70, 1640, true);
			target.teleport(70, 1640, true);
			return;
		} else {
			player.getActionSender().sendMessage("Player is not online");
			return;
		}
	}
	if(cmd.equals("npc")) {
		if(args.length != 1) {
			player.getActionSender().sendMessage("Invalid args. Syntax: NPC id");
			return;
		}
		int id = Integer.parseInt(args[0]);
		if(EntityHandler.getNpcDef(id) != null) {
			final Npc n = new Npc(id, player.getX(), player.getY(), player.getX() - 2, player.getX() + 2, player.getY() - 2, player.getY() + 2);
			n.setRespawn(false);
			world.registerNpc(n);
			world.getDelayedEventHandler().add(new SingleEvent(null, 60000) {
				public void action() {
					Mob opponent = n.getOpponent();
					if(opponent != null) {
						opponent.resetCombat(CombatState.ERROR);
					}
					n.resetCombat(CombatState.ERROR);
					world.unregisterNpc(n);
					n.remove();
				}
			});
			Logger.mod(player.getUsername() + " spawned a " + n.getDef().getName() + " at " + player.getLocation().toString());
		}
		else {
			player.getActionSender().sendMessage("Invalid id");
		}
		return;
	}
	if(cmd.equals("teleport")) {
		if(args.length != 2) {
			player.getActionSender().sendMessage("Invalid args. Syntax: TELEPORT x y");
			return;
		}
		int x = Integer.parseInt(args[0]);
		int y = Integer.parseInt(args[1]);
		if(world.withinWorld(x, y)) {
			Logger.mod(player.getUsername() + " teleported from " + player.getLocation().toString() + " to (" + x + ", " + y + ")");
			player.teleport(x, y, true);
		}
		else {
			player.getActionSender().sendMessage("Invalid coordinates!");
		}
		return;
	}
	if(cmd.equals("goto") || cmd.equals("summon")) {
		boolean summon = cmd.equals("summon");
		if(args.length != 1) {
			player.getActionSender().sendMessage("Invalid args. Syntax: " + (summon ? "SUMMON" : "GOTO") + " name");
			return;
		}
		long usernameHash = DataConversions.usernameToHash(args[0]);
		Player affectedPlayer = world.getPlayer(usernameHash);
		if(affectedPlayer != null) {
			if(summon) {
				Logger.mod(player.getUsername() + " summoned " + affectedPlayer.getUsername() + " from " + affectedPlayer.getLocation().toString() + " to " + player.getLocation().toString());
				affectedPlayer.teleport(player.getX(), player.getY(), true);
				affectedPlayer.getActionSender().sendMessage("You have been summoned by " + player.getUsername());
			}
			else {
				Logger.mod(player.getUsername() + " went from " + player.getLocation() + " to " + affectedPlayer.getUsername() + " at " + affectedPlayer.getLocation().toString());
				player.teleport(affectedPlayer.getX(), affectedPlayer.getY(), true);
			}
		}
		else {
			player.getActionSender().sendMessage("Invalid player, maybe they aren't currently on this server?");
		}
		return;
	}
	if(cmd.equals("take") || cmd.equals("put")) {
		if(args.length != 1) {
			player.getActionSender().sendMessage("Invalid args. Syntax: TAKE name");
			return;
		}
		Player affectedPlayer = world.getPlayer(DataConversions.usernameToHash(args[0]));
		if(affectedPlayer == null) {
			player.getActionSender().sendMessage("Invalid player, maybe they aren't currently online?");
			return;
		}
		Logger.mod(player.getUsername() + " took " + affectedPlayer.getUsername() + " from " + affectedPlayer.getLocation().toString() + " to admin room");
		affectedPlayer.teleport(78, 1642, true);
		if(cmd.equals("take")) {
			player.teleport(76, 1642, true);
		}
		return;
	}
	if(!player.isAdmin()) {
		return;
	}
	if(cmd.equals("setstat"))
	{
		if(args.length < 2) 
		{
			player.getActionSender().sendMessage("INVALID USE - EXAMPLE setstat attack 99");
			return;
		}

		int stat = Formulae.getStatIndex(args[0]);
		int lvl = Integer.parseInt(args[1]);

		if(lvl < 0 || lvl > 5000)
		{
			player.getActionSender().sendMessage("Invalid " + Formulae.statArray[stat] + " level.");
			return;
		}

		player.setCurStat(stat, lvl);
		player.setMaxStat(stat, lvl);
		player.setExp(stat, Formulae.experienceToLevel(lvl));

		int comb = Formulae.getCombatlevel(player.getMaxStats());
		if(comb != player.getCombatLevel()) 
			player.setCombatLevel(comb);

		player.getActionSender().sendStats();
		player.getActionSender().sendMessage("Your " + Formulae.statArray[stat] + " has been set to level " + lvl);

		//player.checkEquipment();
	}
	if(cmd.equals("npco"))
	{
		if(args.length < 2) 
		{
			player.getActionSender().sendMessage("Invalid args. Syntax: NPC id");
			return;
		}

		int stat = Formulae.getStatIndex(args[0]);
		int lvl = Integer.parseInt(args[1]);

		if(lvl < 0 || lvl > 5000)
		{
			player.getActionSender().sendMessage("Invalid argument...");
			return;
		}

		player.setCurStat(stat, lvl);
		player.setMaxStat(stat, lvl);
		player.setExp(stat, Formulae.experienceToLevel(lvl));

		int comb = Formulae.getCombatlevel(player.getMaxStats());
		if(comb != player.getCombatLevel()) 
			player.setCombatLevel(comb);

		player.getActionSender().sendStats();
		player.getActionSender().sendMessage("Your " + Formulae.statArray[stat] + " has been set to level " + lvl);

		//player.checkEquipment();
	}
	if(cmd.equals("objecto")) {
		if(args.length < 1 || args.length > 2) {
			player.getActionSender().sendMessage("Invalid args. Syntax: OBJECT id [direction]");
			return;
		}
		int id = Integer.parseInt(args[0]);
		if(id < 0) {
			GameObject object = world.getTile(player.getLocation()).getGameObject();
			if(object != null) {
				world.unregisterGameObject(object);
			}
		}
		else if(EntityHandler.getGameObjectDef(id) != null) {
			int dir = args.length == 2 ? Integer.parseInt(args[1]) : 0;
			world.registerGameObject(new GameObject(player.getLocation(), id, dir, 0));
		}
		else {
			player.getActionSender().sendMessage("Invalid id");
		}
		return;
	}
	if(cmd.equals("shutdown")) {
		Logger.mod(player.getUsername() + " shut down the server!");
		world.getServer().kill();
		return;
	}
	if(cmd.equals("update")) {
		String reason = "";
		if(args.length > 0) {
			for(String s : args) {
				reason += (s + " ");
			}
			reason = reason.substring(0, reason.length() - 1);
		}
		if(world.getServer().shutdownForUpdate()) {
			Logger.mod(player.getUsername() + " updated the server: " + reason);
			for(Player p : world.getPlayers()) {
				p.getActionSender().sendAlert("The server will be shutting down in 60 seconds: " + reason, false);
				p.getActionSender().startShutdown(60);
			}
		}
		return;
	}
	if(cmd.equals("appearance")) {
		player.setChangingAppearance(true);
		player.getActionSender().sendAppearanceScreen();
		return;
	}
	if(cmd.equals("skull")) {
		int length = 20;
		try { length = Integer.parseInt(args[0]); } catch(Exception e) { }
		player.addSkull(length * 60000);
		return;
	}
	if(cmd.equals("item")) {
		if(args.length < 1 || args.length > 2) {
			player.getActionSender().sendMessage("Invalid args. Syntax: ITEM id [amount]");
			return;
		}
		int id = Integer.parseInt(args[0]);
		if(EntityHandler.getItemDef(id) != null) {
			int amount = 1;
			if(args.length == 2 && EntityHandler.getItemDef(id).isStackable()) {
				amount = Integer.parseInt(args[1]);
			}
			InvItem item = new InvItem(id, amount);
			player.getInventory().add(item);
			player.getActionSender().sendInventory();
			Logger.mod(player.getUsername() + " spawned themself " + amount + " " + item.getDef().getName() + "(s)");
		}
		else {
			player.getActionSender().sendMessage("Invalid id");
		}
		return;
	}
	if(cmd.equals("object")) {
		if(!player.getLocation().inModRoom()) {
			player.getActionSender().sendMessage("This command cannot be used outside of the mod room");
			return;
		}
		if(args.length < 1 || args.length > 2) {
			player.getActionSender().sendMessage("Invalid args. Syntax: OBJECT id [direction]");
			return;
		}
		int id = Integer.parseInt(args[0]);
		if(id < 0) {
			GameObject object = world.getTile(player.getLocation()).getGameObject();
			if(object != null) {
				world.unregisterGameObject(object);
			}
		}
		else if(EntityHandler.getGameObjectDef(id) != null) {
			int dir = args.length == 2 ? Integer.parseInt(args[1]) : 0;
			world.registerGameObject(new GameObject(player.getLocation(), id, dir, 0));
		}
		else {
			player.getActionSender().sendMessage("Invalid id");
		}
		return;
	}
	if(cmd.equals("door")) {
		if(!player.getLocation().inModRoom()) {
			player.getActionSender().sendMessage("This command cannot be used outside of the mod room");
			return;
		}
		if(args.length < 1 || args.length > 2) {
			player.getActionSender().sendMessage("Invalid args. Syntax: DOOR id [direction]");
			return;
		}
		int id = Integer.parseInt(args[0]);
		if(id < 0) {
			GameObject object = world.getTile(player.getLocation()).getGameObject();
			if(object != null) {
				world.unregisterGameObject(object);
			}
		}
		else if(EntityHandler.getDoorDef(id) != null) {
			int dir = args.length == 2 ? Integer.parseInt(args[1]) : 0;
			world.registerGameObject(new GameObject(player.getLocation(), id, dir, 1));
		}
		else {
			player.getActionSender().sendMessage("Invalid id");
		}
		return;
	}
	if (cmd.equals("dropall")) {
		player.getInventory().getItems().clear();
		player.getActionSender().sendInventory();
	}
}

}