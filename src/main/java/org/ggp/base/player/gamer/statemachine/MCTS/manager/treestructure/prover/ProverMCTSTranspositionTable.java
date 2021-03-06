package org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.prover;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.MctsNode;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMachineState;

public class ProverMCTSTranspositionTable {

	private int currentGameStepStamp;

	private int gameStepOffset;

	/**
	 * The transposition table (implemented with HashMap that uses the internal propnet state as key
	 * and solves collisions with linked lists).
	 */
	private Map<ExplicitMachineState,MctsNode> transpositionTable;

	/**
	 *
	 */
	public ProverMCTSTranspositionTable(int gameStepOffset){
		this.currentGameStepStamp = 0;
		this.transpositionTable = new HashMap<ExplicitMachineState,MctsNode>();
		this.gameStepOffset = gameStepOffset;
	}

	public MctsNode getNode(ExplicitMachineState state){
		MctsNode node = this.transpositionTable.get(state);
		if(node != null){
			//System.out.println("Found");
			node.setGameStepStamp(this.currentGameStepStamp);
		}/*else{
			System.out.println("Not found");
		}*/
		return node;
	}

	public void putNode(ExplicitMachineState state, MctsNode node){
		if(node != null){
			this.transpositionTable.put(state, node);
			node.setGameStepStamp(this.currentGameStepStamp);
		}
	}

	public void clean(int newGameStepStamp){

		//System.out.println("Current TT game step: " + newGameStepStamp);
		//System.out.println("Cleaning TT with game step: " + newGameStepStamp);
		//System.out.println("Current TT size: " + this.transpositionTable.size());

		// Clean the table only if the game-step stamp changed (this is already checked by the caller).
		//if(newGameStepStamp != this.currentGameStepStamp){
			this.currentGameStepStamp = newGameStepStamp;
			// Remove all nodes last accessed earlier than the game step (newGameStepStamp-gameStepOffset)
			Iterator<Entry<ExplicitMachineState,MctsNode>> iterator = this.transpositionTable.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<ExplicitMachineState,MctsNode> entry = iterator.next();
				if(entry.getValue().getGameStepStamp() < (this.currentGameStepStamp-this.gameStepOffset)){
					iterator.remove();
				}
			}

			//System.out.println("TT size after cleaning: " + this.transpositionTable.size());
		//}
	}

	public int getLastGameStep(){
		return this.currentGameStepStamp;
	}

}
