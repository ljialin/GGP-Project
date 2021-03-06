package org.ggp.base.player.gamer.statemachine.MCTS.manager.prover.strategies.selection.evaluators;

import org.ggp.base.player.gamer.statemachine.MCS.manager.MoveStats;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMove;

public interface ProverMoveEvaluator {

	public double computeMoveValue(int allMoveVisits, ExplicitMove theMove, MoveStats theMoveStats);

	public String getEvaluatorParameters();

	public String printEvaluator();

}
