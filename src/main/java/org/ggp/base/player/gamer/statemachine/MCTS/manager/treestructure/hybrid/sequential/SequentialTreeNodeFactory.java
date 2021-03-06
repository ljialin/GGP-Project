package org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.sequential;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.GamerSettings;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.GameDependentParameters;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.SharedReferencesCollector;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.MctsNode;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.TreeNodeFactory;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineException;
import org.ggp.base.util.statemachine.structure.MachineState;
import org.ggp.base.util.statemachine.structure.Move;
import org.ggp.base.util.statemachine.structure.Role;

public class SequentialTreeNodeFactory extends TreeNodeFactory {

	public SequentialTreeNodeFactory(GameDependentParameters gameDependentParameters, Random random,
			GamerSettings gamerConfiguration, SharedReferencesCollector sharedReferencesCollector) {
		super(gameDependentParameters, random, gamerConfiguration, sharedReferencesCollector);
	}

	@Override
	public void setReferences(SharedReferencesCollector sharedReferencesCollector) {
		// No need for any reference
	}

	@Override
	public void clearComponent() {
		// Do nothing

	}

	@Override
	public void setUpComponent() {
		// Do nothing

	}

	@Override
	public MctsNode createNewNode(MachineState state) {
		//System.out.println("Creating new node.");

		double goals[] = null;
		boolean terminal = false;
		List<List<Move>> allLegalMoves = null;

		SequentialMctsMoveStats[] suctMovesStats = null;
		int unvisitedLeavesCount = 0;

		try {
			terminal = this.gameDependentParameters.getTheMachine().isTerminal(state);
		} catch (StateMachineException e) {
			GamerLogger.logError("MctsManager", "Failed to compute state terminality when creating new tree node. Considering node terminal.");
			GamerLogger.logStackTrace("MctsManager", e);
			terminal = true;
		}

		// Terminal state:
		if(terminal){

			goals = this.gameDependentParameters.getTheMachine().getSafeGoalsAvgForAllRoles(state);

		}else{// Non-terminal state:

			try {
				allLegalMoves = this.gameDependentParameters.getTheMachine().getAllLegalMovesForAllRoles(state);

				suctMovesStats = this.createSuctMctsMoves(allLegalMoves);
				unvisitedLeavesCount = suctMovesStats[0].getUnvisitedSubleaves() * suctMovesStats.length;
				//this.printSUCTMovesTree(suctMovesStats, "");
			} catch (MoveDefinitionException | StateMachineException e) {
				// Error when computing moves.
				GamerLogger.logError("MctsManager", "Failed to retrieve the legal moves while adding non-terminal SUCT state to the tree.");
				GamerLogger.logStackTrace("MctsManager", e);
				// If for at least one player the legal moves cannot be computed we consider this node
				// "pseudo-terminal" (i.e. the corresponding state is not terminal but we cannot explore
				// any of the next states, so we treat it as terminal during the MCT search). This means
				// that we will need the goal values in this node and they will not change for all the rest
				// of the search, so we compute them and memorize them.

				// Compute the goals for each player. We are in a non terminal state so the goal might not be defined.
				// We use the state machine method that will return default goal values for the player for which goal
				// values cannot be computed in this state.

				//System.out.println("Null moves in non terminal state!");

				allLegalMoves = null;
				goals = this.gameDependentParameters.getTheMachine().getSafeGoalsAvgForAllRoles(state);
				terminal = true;
				unvisitedLeavesCount = 0;
			}
			// If the legal moves can be computed for every player, there is no need to compute the goals.
		}

		return new SequentialMctsNode(allLegalMoves, suctMovesStats, goals, terminal, unvisitedLeavesCount, this.gameDependentParameters.getNumRoles());

	}

	private SequentialMctsMoveStats[] createSuctMctsMoves(List<List<Move>> allLegalMoves){

		List<Role> roles = this.gameDependentParameters.getTheMachine().getRoles();

		// Get legal moves for all players.
		/*try {
			for(int i = 0; i < roles.length; i++){
				legalMoves.add(this.theMachine.getInternalLegalMoves(state, roles[i]));
			}
		}catch (MoveDefinitionException e) {
			// If for at least one player the legal moves cannot be computed, we return null.
			GamerLogger.logError("MctsManager", "Failed to retrieve the legal moves while adding non-terminal SUCT state to the tree.");
			GamerLogger.logStackTrace("MctsManager", e);

			return null;
		} */

		// For all the moves of my role (i.e. the role actually performing the search)
		// create the SUCT move containing the move statistics.

		List<Move> myLegalMoves = allLegalMoves.get(this.gameDependentParameters.getMyRoleIndex());
		SequentialMctsMoveStats[] moves = new SequentialMctsMoveStats[myLegalMoves.size()];
		for(int i = 0; i < myLegalMoves.size(); i++){
			moves[i] = new SequentialMctsMoveStats(createSuctMctsMoves((this.gameDependentParameters.getMyRoleIndex()+1)%(roles.size()), roles.size(), allLegalMoves));
		}

		return moves;
	}

	private SequentialMctsMoveStats[] createSuctMctsMoves(int roleIndex, int numRoles, List<List<Move>> allLegalMoves){

		if(roleIndex == this.gameDependentParameters.getMyRoleIndex()){
			return null;
		}

		List<Move> roleLegalMoves = allLegalMoves.get(roleIndex);
		SequentialMctsMoveStats[] moves = new SequentialMctsMoveStats[roleLegalMoves.size()];
		for(int i = 0; i < roleLegalMoves.size(); i++){
			moves[i] = new SequentialMctsMoveStats(createSuctMctsMoves((roleIndex+1)%(numRoles), numRoles, allLegalMoves));
		}

		return moves;
	}

	@Override
	public String getComponentParameters(String indentation) {
		return null;
	}

}
