/**
 *
 */
package org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.SlowSUCT;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.MCTSJointMove;
import org.ggp.base.util.statemachine.inernalPropnetStructure.InternalPropnetMove;

/**
 * @author C.Sironi
 *
 */
public class SlowSUCTMCTSJointMove extends MCTSJointMove {

	/**
	 * Reference to the leaf move that allows to reconstruct the joint move.
	 */
	private SlowSUCTMCTSMoveStats leafMove;

	/**
	 * @param jointMove
	 */
	public SlowSUCTMCTSJointMove(List<InternalPropnetMove> jointMove, SlowSUCTMCTSMoveStats leafMove) {
		super(jointMove);
		this.leafMove = leafMove;
	}

	public SlowSUCTMCTSMoveStats getLeafMove(){
		return this.leafMove;
	}

}