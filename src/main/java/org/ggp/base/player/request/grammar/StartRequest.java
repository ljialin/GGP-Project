package org.ggp.base.player.request.grammar;

import java.util.Date;

import org.apache.logging.log4j.ThreadContext;
import org.ggp.base.player.event.PlayerTimeEvent;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.event.GamerNewMatchEvent;
import org.ggp.base.player.gamer.event.GamerUnrecognizedMatchEvent;
import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.match.Match;


public final class StartRequest extends Request
{
	private final Game game;
	private final Gamer gamer;
	private final String matchId;
	private final int playClock;
	private final GdlConstant roleName;
	private final int startClock;

	public StartRequest(Gamer gamer, String matchId, GdlConstant roleName, Game theGame, int startClock, int playClock)
	{
		this.gamer = gamer;
		this.matchId = matchId;
		this.roleName = roleName;
		this.game = theGame;
		this.startClock = startClock;
		this.playClock = playClock;
	}

	@Override
	public String getMatchId() {
		return matchId;
	}

	@Override
	public String process(long receptionTime)
	{
	    // Ensure that we aren't already playing a match. If we are,
	    // ignore the message, saying that we're busy.
        if (gamer.getMatch() != null) {
            GamerLogger.logError("GamePlayer", "Got start message while already busy playing a game: ignoring.");
            gamer.notifyObservers(new GamerUnrecognizedMatchEvent(matchId));
            return "busy";
        }

        // Create the new match, and handle all of the associated logistics
        // in the gamer to indicate that we're starting a new match.
        // TODO: the constructor of the match calls game.getRules() to get the game rules as GDL objects -> EXPENSIVE but has to be done once, but...
		Match match = new Match(matchId, -1, startClock, playClock, game);

		gamer.setMatch(match);
		gamer.setRoleName(roleName);
		gamer.notifyObservers(new GamerNewMatchEvent(match, roleName));

		String oldFolder = ThreadContext.get("LOG_FOLDER");
		ThreadContext.put("LOG_FOLDER", oldFolder + "/" + match.getMatchId() + "-" + roleName);

		GamerLogger.log("GamePlayer", "Started match logging at: " + new Date());
		GamerLogger.log("GamePlayer", "Game rules: " + match.getGame().getRules());
		GamerLogger.log("GamePlayer", "Start clock: " + match.getStartClock());
		GamerLogger.log("GamePlayer", "Play clock: " + match.getPlayClock());

		//GamerLogger.startFileLogging(match, roleName.getValue());

		// Finally, have the gamer begin metagaming.
		try {
			gamer.notifyObservers(new PlayerTimeEvent(gamer.getMatch().getStartClock() * 1000));
			// TODO ...also metaGame calls game.getRules() to get the game description as GDL objects... -> double as EXPENSIVE! How to fix this?
			gamer.metaGame(gamer.getMatch().getStartClock() * 1000 + receptionTime);
		} catch (MetaGamingException e) {
			GamerLogger.logError("GamePlayer", "Something went wrong when metagaming: player unable to play!");
		    GamerLogger.logStackTrace("GamePlayer", e);

		    // Upon encountering an uncaught exception during metagaming,
		    // assume that indicates that we aren't actually able to play
		    // right now, and tell the server that we're busy.
			gamer.setMatch(null);
			gamer.setRoleName(null);

			GamerLogger.log("GamePlayer", "Stopped logging to files at: " + new Date());
		    GamerLogger.log("GamePlayer", "LOG SEALED");

			ThreadContext.put("LOG_FOLDER", oldFolder);
			//GamerLogger.stopFileLogging();

			return "busy";
		}

		return "ready";
	}

	@Override
	public String toString()
	{
		return "start";
	}
}