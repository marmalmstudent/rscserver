package org.rscdaemon.server.npchandler;

import org.rscdaemon.server.model.Player;
import org.rscdaemon.server.model.Npc;
import org.rscdaemon.server.model.World;
import org.rscdaemon.server.model.ChatMessage;
import org.rscdaemon.server.event.ShortEvent;

public class Bankers implements NpcHandler {
	/**
	 * World instance
	 */
	public static final World world = World.getWorld();

	public void handleNpc(final Npc npc, Player player) throws Exception {
		player.setBusy(true);
      		player.informOfChatMessage(new ChatMessage(player, "I'd like to access my bank account please", npc));
      		world.getDelayedEventHandler().add(new ShortEvent(player) {
      			public void action() {
      				owner.informOfNpcMessage(new ChatMessage(npc, "Certainly " + (owner.isMale() ? "sir" : "miss"), owner));
      				world.getDelayedEventHandler().add(new ShortEvent(owner) {
      					public void action() {
      						owner.setBusy(false);
      						owner.setAccessingBank(true);
      						owner.getActionSender().showBank();
      					}
      				});
      				npc.unblock();
      			}
      		});
      		npc.blockedBy(player);
	}
	
}