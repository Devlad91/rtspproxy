/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   Copyright (C) 2005 - Matteo Merli - matteo.merli@gmail.com            *
 *                                                                         *
 ***************************************************************************/

/*
 * $Id: ProxySession.java 319 2005-12-08 08:21:59Z merlimat $
 * 
 * $URL: http://svn.berlios.de/svnroot/repos/rtspproxy/tags/3.0-ALPHA2/src/main/java/rtspproxy/proxy/ProxySession.java $
 * 
 */

package rtspproxy.proxy;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import rtspproxy.lib.number.UnsignedLong;

/**
 * Manages RTSP sessions with both client and server.
 * 
 * @author Matteo Merli
 */
public class ProxySession {

	private static Logger log = Logger.getLogger(ProxySession.class);

	protected static final String ATTR = ProxySession.class.toString() + "Attr";

	/** Map IDs for RTSP session with servers to ProxySession objects. */
	private static Map<String, ProxySession> serverSessionIds = new ConcurrentHashMap<String, ProxySession>();

	/** Map IDs for RTSP session with clients to ProxySession objects. */
	private static Map<String, ProxySession> clientSessionIds = new ConcurrentHashMap<String, ProxySession>();

	/**
	 * Retrieve the ProxySession associated with the given session ID used by
	 * the client.
	 * 
	 * @param clientSessionId
	 *        a string containing the RTSP session ID
	 * @return the associated ProxySession or null if not found
	 */
	public static ProxySession getByClientSessionID(String clientSessionId) {
		return clientSessionIds.get(clientSessionId);
	}

	/**
	 * Retrieve the ProxySession associated with the given session ID used by
	 * the server.
	 * 
	 * @param serverSessionId
	 *        a string containing the RTSP session ID
	 * @return the associated ProxySession or null if not found
	 */
	public static ProxySession getByServerSessionID(String serverSessionId) {
		return serverSessionIds.get(serverSessionId);
	}

	/**
	 * This is the session ID generated by the proxy and used for the
	 * communication with the client.
	 */
	private String clientSessionId = null;

	/**
	 * This is the session ID assigned by the server. RTSP messages with the
	 * server must use this ID.
	 */
	private String serverSessionId = null;

	/** Tells whether the proxySession has already been closed. */
	private boolean isClosed = false;

	/**
	 * Collection of Track associated with this ProxySession.
	 */
	private Map<String, Track> trackList = new ConcurrentHashMap<String, Track>();

	/**
	 * Construct a new ProxySession. The session ID that will be used when
	 * communicating with the client will be generated.
	 */
	public ProxySession() {
		setClientSessionId(newSessionID());
		log.debug("\n----------\nCreated new proxy session: " + clientSessionId + " \n----------");
	}

	/**
	 * Adds a new Track associated with this ProxySession.
	 * 
	 * @param url
	 *        The URL used as a control reference for the Track
	 * @param serverSsrc
	 *        the SSRC id given by the server or null if not provided
	 * @return a reference to the newly created Track
	 */
	public synchronized Track addTrack(String url, String serverSsrc) {
		Track track = new Track(url);
		if (serverSsrc != null)
			track.setServerSSRC(serverSsrc);
		trackList.put(url, track);
		log.debug("ProxySession: " + clientSessionId + " Added track. TrackList: " + trackList);
		return track;
	}

	/**
	 * @return the RTSP session id used by the client in this session.
	 */
	public String getClientSessionId() {
		return clientSessionId;
	}

	/**
	 * @return the RTSP session id used by the server in this session.
	 */
	public String getServerSessionId() {
		return serverSessionId;
	}

	/**
	 * Sets the RTSP session id for the client.
	 * 
	 * @param clientSessionId
	 *        a string containing the session id
	 */
	public synchronized void setClientSessionId(String clientSessionId) {
		this.clientSessionId = clientSessionId;
		clientSessionIds.put(clientSessionId, this);
	}

	/**
	 * Sets the RTSP session id for the client.
	 * 
	 * @param clientSessionId
	 *        a string containing the session id
	 */
	public synchronized void setServerSessionId(String serverSessionId) {
		this.serverSessionId = serverSessionId;
		serverSessionIds.put(serverSessionId, this);
	}

	/**
	 * Closes the entire proxy session and frees all associated resources.
	 */
	public synchronized void close() {
		if (isClosed)
			return;

		log.debug("TrackList: " + trackList);

		// close all associated tracks
		for (Map.Entry<String, Track> entry : trackList.entrySet()) {
			entry.getValue().close();
		}

		isClosed = true;
		log.debug("Closed proxySession: " + clientSessionId);

		String s = "";
		for (String a : clientSessionIds.keySet()) {
			s += a + " ";
		}
		log.debug("Clients: " + s);
		s = "";
		for (String a : serverSessionIds.keySet()) {
			s += a + " ";
		}
		log.debug("Servers: " + s);

		if (clientSessionId != null)
			clientSessionIds.remove(clientSessionId);
		if (serverSessionId != null)
			serverSessionIds.remove(serverSessionId);
	}

	// ///////////////////
	// Session ID generation

	/** Used for Session IDs generation */
	private static Random random = new Random();

	/**
	 * Creates a unique session ID that is a 64 bit number.
	 * 
	 * @return the session ID string.
	 */
	private static String newSessionID() {
		String id;
		while (true) {
			// Create a 64 bit random number
			synchronized (random) {
				id = new UnsignedLong(random).toString();
			}

			if (clientSessionIds.get(id) == null) {
				// Ok, the id is unique
				return id;
			}
			// try with another id
		}
	}

}
