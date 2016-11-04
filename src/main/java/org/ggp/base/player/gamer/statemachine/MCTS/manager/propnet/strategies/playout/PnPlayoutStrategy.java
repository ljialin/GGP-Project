package org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.playout;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.strategies.Strategy;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.propnet.PnSimulationResult;
import org.ggp.base.util.statemachine.structure.compact.CompactMachineState;

public interface PnPlayoutStrategy extends Strategy{

	public PnSimulationResult playout(CompactMachineState state, int maxDepth);
}