package org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.strategies.expansion;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.strategies.Strategy;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.MCTSNode;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.MCTSJointMove;

public interface ExpansionStrategy extends Strategy {

	public boolean expansionRequired(MCTSNode node);

	public MCTSJointMove expand(MCTSNode node);

}