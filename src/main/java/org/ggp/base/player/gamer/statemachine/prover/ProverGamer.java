package org.ggp.base.player.gamer.statemachine.prover;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.abstractsm.AbstractStateMachine;
import org.ggp.base.util.statemachine.abstractsm.ExplicitStateMachine;
import org.ggp.base.util.statemachine.cache.NoSyncRefactoredCachedStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public abstract class ProverGamer extends StateMachineGamer {

	/**
	 * The player must complete the executions of methods with a timeout by the time
	 * [timeout - safetyMargin(ms)] to increase the certainty of answering to the Game
	 * Manager in time.
	 */
	protected long selectMoveSafetyMargin;

	protected boolean cache;

	/**
	 *
	 */
	public ProverGamer() {
		// TODO: change code so that the parameters can be set from outside.
		this.selectMoveSafetyMargin = 10000L;
		this.cache = false;

	}

	@Override
	public AbstractStateMachine getInitialStateMachine() {

		if(cache){
			GamerLogger.log("Gamer", "Returning Prover state machine with cache.");
			return new ExplicitStateMachine(new NoSyncRefactoredCachedStateMachine(this.random, new ProverStateMachine(this.random)));
		}else{
			GamerLogger.log("Gamer", "Returning Prover state machine without cache.");
			return new ExplicitStateMachine(new ProverStateMachine(this.random));
		}

	}

	/* (non-Javadoc)
	 * @see org.ggp.base.player.gamer.Gamer#getName()
	 */
	@Override
	public String getName() {
		/*String type = "";
		if(this.singleGame){
			type = "SingleGame";
		}else{
			type = "Starndard";
		}
		return getClass().getSimpleName() + "-" + type;*/
		return getClass().getSimpleName();
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.player.gamer.Gamer#preview(org.ggp.base.util.game.Game, long)
	 */
	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}
}