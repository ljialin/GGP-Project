package org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid;

import java.util.List;

import org.ggp.base.util.statemachine.structure.Move;

public class SeqDecMctsJointMove extends MctsJointMove {

	/**
	 * Index that each single move has in the list of legal moves for its role.
	 */
	private int[] movesIndices;

	/**
	 * @param jointMove
	 * @param movesIndices
	 */
	public SeqDecMctsJointMove(List<Move> jointMove, int[] movesIndices) {
		super(jointMove);
		this.movesIndices = movesIndices;
	}

	public int[] getMovesIndices() {
		return movesIndices;
	}


	@Override
	public String toString(){
		String s = "JointMove[";
		for(int i = 0; i < this.movesIndices.length; i++){
			s += "(I=" + movesIndices[i] + ", M=" + this.jointMove.get(i) +")";
		}

		return s;
	}

}
