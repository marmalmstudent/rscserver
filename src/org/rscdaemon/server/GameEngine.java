package org.rscdaemon.server;

import java.util.TreeMap;

import org.apache.mina.common.IoSession;
import org.rscdaemon.server.event.DelayedEvent;
import org.rscdaemon.server.event.SaveEvent;
import org.rscdaemon.server.model.Player;
import org.rscdaemon.server.model.Shop;
import org.rscdaemon.server.model.World;
import org.rscdaemon.server.net.PacketQueue;
import org.rscdaemon.server.net.RSCPacket;
import org.rscdaemon.server.packethandler.PacketHandler;
import org.rscdaemon.server.packethandler.PacketHandlerDef;
import org.rscdaemon.server.util.Logger;
import org.rscdaemon.server.util.PersistenceManager;

/**
 * The central motor of the game. This class is responsible for the
 * primary operation of the entire game.
 */
public final class GameEngine extends Thread {
	/**
	 * World instance
	 */
	private static final World world = World.getWorld();
	/**
	 * The packet queue to be processed
	 */
	private PacketQueue<RSCPacket> packetQueue;
	/**
	 * Whether the engine's thread is running
	 */
	private static boolean running = true;
	/**
	 * The mapping of packet IDs to their handler
	 */
	private TreeMap<Integer, PacketHandler> packetHandlers = new TreeMap<Integer, PacketHandler>();
	/**
	 * Responsible for updating all connected clients
	 */
	private ClientUpdater clientUpdater = new ClientUpdater();
	/**
	 * Handles delayed events rather than events to be ran every iteration
	 */
	private DelayedEventHandler eventHandler = new DelayedEventHandler();
	/**
	 * When the update loop was last ran, required for throttle
	 */
	private long lastSentClientUpdate = 0;

	/**
	 * Constructs a new game engine with an empty packet queue.
	 */
	public GameEngine() {
		packetQueue = new PacketQueue<RSCPacket>();
		
		loadPacketHandlers();
		for(Shop shop : world.getShops()) {
			shop.initRestock();
		}
	}
		
	/**
	 * The thread execution process.
	 */
	public void run() {
		Logger.print("GameEngine now running", 3);
		Logger.print(GameVars.serverName + " is now Online!", 3);
		GameVars.serverRunning = true;
		running = true;
		
		eventHandler.add(new DelayedEvent(null, GameVars.saveAll * 60000) {
			public void run() {
				SaveEvent.saveAll();			
			}
		});
		while (running) {
			try { Thread.sleep(50); } catch(InterruptedException ie) {}
			processLoginServer();
			processIncomingPackets();
			processEvents();
			processClients();
		}
		if(!running)
		world.getServer().unbind();
		GUI.resetVars();
	}
	
	public void emptyWorld() {
		for(Player p : world.getPlayers()) {
			p.save();
			p.getActionSender().sendLogout();
		}
		//world.getServer().getLoginConnector().getActionSender().saveProfiles();
	}
	
	public static void kill() {
		Logger.print("Terminating GameEngine", 1);
		GameVars.serverRunning = false;
		GUI.resetVars();
		GUI.repaintVars();	
		running = false;
		
	}
	
	public void processLoginServer() {
		//LoginConnector connector = world.getServer().getLoginConnector();
		//if(connector != null) {
		//	connector.processIncomingPackets();
		//	connector.sendQueuedPackets();
		//}
	}
	
	/**
	 * Processes incoming packets.
	 */
	private void processIncomingPackets() {
		for(RSCPacket p : packetQueue.getPackets()) {
			IoSession session = p.getSession();
			Player player = (Player)session.getAttachment();
			player.ping();
			PacketHandler handler = packetHandlers.get(p.getID());
			if (handler != null) {
				try {
					handler.handlePacket(p, session);
				}
				catch(Exception e) {
					Logger.error("Exception with p[" + p.getID() + "] from " + player.getUsername() + " [" + player.getCurrentIP() + "]: " + e.getMessage());
					player.getActionSender().sendLogout();
					player.destroy(false);
				}
			}
			else {
				Logger.error("Unhandled packet from " + player.getCurrentIP() + ": " + p.getID());
			}
		}
	}
	
	private void processEvents() {
		eventHandler.doEvents();
	}
	
	private void processClients() {
		clientUpdater.sendQueuedPackets();
		
		long now = System.currentTimeMillis();
		if(now - lastSentClientUpdate >= 600) {
			lastSentClientUpdate = now;
			clientUpdater.updateClients();
		}
	}

	/**
	 * Returns the current packet queue.
	 *
	 * @return A <code>PacketQueue</code>
	 */
	public PacketQueue<RSCPacket> getPacketQueue() {
		return packetQueue;
	}

	/**
	 * Loads the packet handling classes from the persistence
	 * manager.
	 */
	protected void loadPacketHandlers() {
		PacketHandlerDef[] handlerDefs = (PacketHandlerDef[])PersistenceManager.load("PacketHandlers.xml");
		int count = 0;
		for(PacketHandlerDef handlerDef : handlerDefs) {
			try {
				String className = handlerDef.getClassName();
				Class<?> c = Class.forName(className);
				if (c != null) {
					count++;
					
					PacketHandler handler = (PacketHandler)c.newInstance();
					for(int packetID : handlerDef.getAssociatedPackets()) {
						
						packetHandlers.put(packetID, handler);
					}
					
				}
			}
			catch (Exception e) {
				Logger.error(e);
			}
		}
		Logger.print(count + " Packet Handlers Loaded.", 3);
	}

}
