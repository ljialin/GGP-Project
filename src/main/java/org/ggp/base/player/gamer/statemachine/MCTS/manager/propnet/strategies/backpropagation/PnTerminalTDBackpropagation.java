package org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.backpropagation;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.propnet.PnSimulationResult;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.InternalPropnetStateMachine;

public class PnTerminalTDBackpropagation extends PnTDBackpropagation {

	/**
	 *  True if for the current simulation the backpropagation hasn't been called once yet
	 *  (i.e. if the node being updated is also the one before the last in the simulated path),
	 *  so the final reward must be considered when computing the value to update.
	 */
	private boolean firstUpdate;

	public PnTerminalTDBackpropagation(InternalPropnetStateMachine theMachine, int numRoles, double qPlayout,
			double lambda, double gamma) {
		super(theMachine, numRoles, qPlayout, lambda, gamma);

		this.firstUpdate = true;
	}

	@Override
	public int[] getReturnValuesForRolesInPlayout(PnSimulationResult simulationResult){

		if(this.firstUpdate){

			this.firstUpdate = false;

			if(simulationResult.getTerminalGoals() == null){
				GamerLogger.logError("MCTSManager", "Found null terminal goals in the simulation result when processing the result for terminal TD backpropagation. Probably a wrong combination of strategies has been set!");
				throw new RuntimeException("Null terminal goals in the simulation result.");
			}

			return simulationResult.getTerminalGoals();

		}else{
			return new int[this.numRoles];
		}

	}

	@Override
	public void resetSimulationParameters(){

		super.resetSimulationParameters();

		this.firstUpdate = true;

	}

}