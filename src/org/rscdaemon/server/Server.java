package org.rscdaemon.server;

import org.rscdaemon.server.event.DelayedEvent;
import org.rscdaemon.server.event.SingleEvent;
import org.rscdaemon.server.util.*;
import org.rscdaemon.server.model.World;
import org.rscdaemon.server.net.RSCConnectionHandler;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.transport.socket.nio.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import javax.swing.UIManager;

/**
 * The entry point for RSC server.
 */
public class Server {
	/**
	 * World instance
	 */
	private static final World world = World.getWorld();
	/**
	 * The game engine
	 */
	private GameEngine engine;
	/**
	 * The SocketAcceptor
	 */
	private IoAcceptor acceptor;
	/**
	 * Update event - if the server is shutting down
	 */
	private DelayedEvent updateEvent;
	/**
	 * The login server connection
	 */
	/**
	 * Is the server running still?
	 */
	private boolean running;
	
	
	public boolean running() {
		return running;
	}
	
	/**
	 * Shutdown the server in 60 seconds
	 */
	public boolean shutdownForUpdate() {
		if(updateEvent != null) {
			return false;
		}
		updateEvent = new SingleEvent(null, 65000) {
    			public void action() {
    				kill();
    			}
    		};
		world.getDelayedEventHandler().add(updateEvent);
		return true;
	}
	
	/**
	 * MS till the server shuts down
	 */
	public int timeTillShutdown() {
		if(updateEvent == null) {
			return -1;
		}
		return updateEvent.timeTillNextRun();
	}
	
	public void resetOnline() {
		try {
		File files = new File("players/");
		int count = 0;
		for(File f : files.listFiles()) {
			
			if(f.getName().endsWith(".cfg")) {
				count++;
				Properties pr = new Properties();

				FileInputStream fis = new FileInputStream(f);
				pr.load(fis);
				fis.close();
				pr.setProperty("loggedin",  "false");
				FileOutputStream fos = new FileOutputStream(f);
				pr.store(fos, "Character Data.");
				fos.close();
			}
			
		}
		Logger.print(count + " Accounts exist.", 3);
		} catch (Exception e) {
			Logger.print(e.toString(), 1);
		}
	}

	/**
	 * Creates a new server instance, which in turn creates a new
	 * engine and prepares the server socket to accept connections.
	 */
	public Server() {
		resetOnline();
		running = true;
		world.setServer(this);
		try {
			engine = new GameEngine();
			engine.start();
			acceptor = new SocketAcceptor();
			IoAcceptorConfig config = new SocketAcceptorConfig();
			config.setDisconnectOnUnbind(true);
			((SocketSessionConfig)config.getSessionConfig()).setReuseAddress(true);
			acceptor.bind(new InetSocketAddress("localhost", GameVars.portNumber), new RSCConnectionHandler(engine), config);
		}
		catch (Exception e) {
			Logger.error(e);
		}
	}
	
	/**
	 * Returns the game engine for this server
	 */
	public GameEngine getEngine() {
		return engine;
	}
	
	public boolean isInitialized() {
		return engine != null;
	}
	
	/**
	 * Kills the game engine and irc engine
	 */
	public void kill() {
		GUI.resetVars();
		Logger.print("TestServer Shutting Down...", 3);
		running = false;
		engine.emptyWorld();
	}
	
	/**
	 * Unbinds the socket acceptor
	 */
	public void unbind() {
		try {
			acceptor.unbindAll();
			GUI.cout("Socket Closed", 3);
		}
		catch(Exception e) { }
	}

	public static void main(String[] args) throws IOException {
		try {
			
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			//UIManager.setLookAndFeel("com.easynth.lookandfeel.EaSynthLookAndFeel");
		} catch (Exception e) {
				
			}
			GUI.args = args;
		new GUI();
		
	}
}
