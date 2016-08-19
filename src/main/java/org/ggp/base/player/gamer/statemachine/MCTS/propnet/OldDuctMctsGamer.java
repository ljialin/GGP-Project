package org.ggp.base.player.gamer.statemachine.MCTS.propnet;

import java.util.Random;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.InternalPropnetMCTSManager;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.backpropagation.StandardBackpropagation;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.expansion.RandomExpansion;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.movechoice.MaximumScoreChoice;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.playout.RandomPlayout;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.selection.UCTSelection;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.selection.evaluators.UCTEvaluator;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.treestructure.decoupled.PnDecoupledTreeNodeFactory;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.prover.ProverMCTSManager;
import org.ggp.base.util.statemachine.inernalPropnetStructure.InternalPropnetRole;

public class OldDuctMctsGamer extends UctMctsGamer {

	public OldDuctMctsGamer(){

		this.unexploredMoveDefaultSelectionValue = Double.MAX_VALUE;
	}

	@Override
	public InternalPropnetMCTSManager createPropnetMCTSManager(){

	Random r = new Random();

	InternalPropnetRole myRole = this.thePropnetMachine.roleToInternalRole(this.getRole());
	int numRoles = this.thePropnetMachine.getInternalRoles().length;

	return new InternalPropnetMCTSManager(new UCTSelection(numRoles, myRole, r, this.valueOffset, new UCTEvaluator(this.c, this.unexploredMoveDefaultSelectionValue)),
       		new RandomExpansion(numRoles, myRole, r), new RandomPlayout(this.thePropnetMachine),
       		new StandardBackpropagation(numRoles, myRole),	new MaximumScoreChoice(myRole, r),
       		null, null, new PnDecoupledTreeNodeFactory(this.thePropnetMachine), this.thePropnetMachine,
       		this.gameStepOffset, this.maxSearchDepth, this.logTranspositionTable);
	}

	@Override
	public ProverMCTSManager createProverMCTSManager(){


		// ZZZ!

		return null;


	}

}