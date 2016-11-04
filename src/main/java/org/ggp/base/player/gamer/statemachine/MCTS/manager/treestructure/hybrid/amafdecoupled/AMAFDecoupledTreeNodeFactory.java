package org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.amafdecoupled;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.MCTSNode;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.decoupled.DecoupledMCTSMoveStats;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.decoupled.DecoupledTreeNodeFactory;
import org.ggp.base.util.statemachine.abstractsm.AbstractStateMachine;

public class AMAFDecoupledTreeNodeFactory extends DecoupledTreeNodeFactory {

	public AMAFDecoupledTreeNodeFactory(AbstractStateMachine theMachine) {
		super(theMachine);
	}

	@Override
	protected MCTSNode createActualNewNode(DecoupledMCTSMoveStats[][] ductMovesStats, int[] goals, boolean terminal) {
		return new AMAFDecoupledMCTSNode(ductMovesStats, goals, terminal);
	}

}