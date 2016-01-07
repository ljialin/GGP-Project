package org.ggp.base.player.gamer.statemachine.MCTS.manager.strategies.playout;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.InternalPropnetStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.inernalPropnetStructure.InternalPropnetMachineState;
import org.ggp.base.util.statemachine.inernalPropnetStructure.InternalPropnetRole;

public class RandomPlayout implements PlayoutStrategy {

	private InternalPropnetStateMachine theMachine;

	public RandomPlayout(InternalPropnetStateMachine theMachine){
		this.theMachine = theMachine;
	}

	@Override
	public int[] playout(InternalPropnetMachineState state, InternalPropnetRole myRole, int[] playoutVisitedNodes, int maxDepth){

		int[] goals;

		InternalPropnetMachineState lastState;

		try {
			lastState = this.theMachine.performLimitedDepthCharge(state, playoutVisitedNodes, maxDepth);
		} catch (TransitionDefinitionException | MoveDefinitionException | StateMachineException e) {
			GamerLogger.logError("MCTSManager", "A random playout failed during MCTS. Returning default loss goals.");
			GamerLogger.logStackTrace("MCTSManager", e);
			return this.theMachine.getLossGoals(myRole);
		}

		// Now try to get the goals of the state.
		try{
			goals = this.theMachine.getGoals(lastState);
		}catch(GoalDefinitionException e){
			// If this fails in a terminal state we consider the playout as a loss.
			if(this.theMachine.isTerminal(lastState)){
				GamerLogger.logError("MCTSManager", "A random playout failed to get terminal goals during MCTS. Returning default loss goals.");
				GamerLogger.logStackTrace("MCTSManager", e);
				return this.theMachine.getLossGoals(myRole);
			}else{// Otherwise we consider it a tie (the state is not terminal so we cannot know if it's good or bad)
				GamerLogger.logError("MCTSManager", "Random playout interrupted before reaching a treminal state. Returning default tie goals.");
				GamerLogger.logStackTrace("MCTSManager", e);
				return this.theMachine.getTieGoals();
			}
		}

		return goals;
	}

}
