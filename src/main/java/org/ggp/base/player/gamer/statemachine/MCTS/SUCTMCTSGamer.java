package org.ggp.base.player.gamer.statemachine.MCTS;

import java.util.Random;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.InternalPropnetMCTSManager;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.backpropagation.StandardBackpropagation;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.expansion.RandomExpansion;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.movechoice.MaximumScoreChoice;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.playout.RandomPlayout;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.selection.UCTSelection;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.treestructure.sequential.PnSequentialTreeNodeFactory;
import org.ggp.base.util.statemachine.inernalPropnetStructure.InternalPropnetRole;

public class SUCTMCTSGamer extends UCTMCTSGamer{

	/**
	 *
	 */
	public SUCTMCTSGamer() {
		super();
	}

	@Override
	public InternalPropnetMCTSManager createMCTSManager(){

		Random r = new Random();

		InternalPropnetRole myRole = this.thePropnetMachine.roleToInternalRole(this.getRole());
		int numRoles = this.thePropnetMachine.getInternalRoles().length;

		return new InternalPropnetMCTSManager(new UCTSelection(numRoles, myRole, r, this.valueOffset, this.c, this.unexploredMoveDefaultSelectionValue),
	       		new RandomExpansion(numRoles, myRole, r), new RandomPlayout(this.thePropnetMachine),
	       		new StandardBackpropagation(numRoles, myRole),	new MaximumScoreChoice(myRole, r),
	       		null, null, new PnSequentialTreeNodeFactory(this.thePropnetMachine, myRole),
	       		this.thePropnetMachine,	this.gameStepOffset, this.maxSearchDepth);
	}

}
