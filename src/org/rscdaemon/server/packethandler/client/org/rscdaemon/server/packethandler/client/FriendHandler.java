package org.rscdaemon.server.packethandler.client;

import org.rscdaemon.server.packethandler.PacketHandler;
import org.rscdaemon.server.util.DataConversions;
import org.rscdaemon.server.model.Player;
import org.rscdaemon.server.model.World;
import org.rscdaemon.server.net.Packet;
import org.rscdaemon.server.net.RSCPacket;

import org.apache.mina.common.IoSession;

public class FriendHandler implements PacketHandler {
	/**
	 * World instance
	 */
	public static final World world = World.getWorld();


	public void handlePacket(Packet p, IoSession session) throws Exception {
		Player player = (Player)session.getAttachment();
		int pID = ((RSCPacket)p).getID();

		player.getUsernameHash();
		long f = p.readLong();
		boolean isOnline = world.getPlayers().contains(world.getPlayer(f));

		String friend = DataConversions.hashToUsername(f);

		switch(pID) {
		case 168: // Add friend
			if(player.friendCount() >= 50) {
				player.getActionSender().sendMessage("Your friend list is too full");
				return;
			}
			if(isOnline) {
				player.getActionSender().sendFriendUpdate(f, org.rscdaemon.server.util.Config.SERVER_NUM);
			} else {
				player.getActionSender().sendFriendUpdate(f, 0);
			}		
			player.addFriend(friend);

			break;
		case 52: // Remove friend

			player.removeFriend(friend);
			break;
		case 25: // Add ignore
			if(player.ignoreCount() >= 50) {
				player.getActionSender().sendMessage("Your ignore list is too full");
				return;
			}		
			player.addIgnore(friend);
			break;
		case 108: // Remove ignore

			player.removeIgnore(friend);
			break;
		case 254: // Send PM
			if(player.getFriendList().contains(friend) && !player.getIgnoreList().contains(friend) && isOnline) {
				Player pe = world.getPlayer(f);
				
				byte[] remaining = p.getRemainingData();
				//System.out.println(DataConversions.byteToString(remaining, 0, remaining.length));			
				pe.getActionSender().sendPrivateMessage(player.getUsernameHash(), remaining);
			}	 else {
				player.getActionSender().sendMessage("the target is not online.");
			}
			break;
		}
	}

}
