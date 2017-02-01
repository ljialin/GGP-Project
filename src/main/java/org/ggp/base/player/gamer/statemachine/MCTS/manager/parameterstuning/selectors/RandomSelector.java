package org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.selectors;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.GamerSettings;
import org.ggp.base.player.gamer.statemachine.MCS.manager.MoveStats;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.GameDependentParameters;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.SharedReferencesCollector;
import org.ggp.base.util.statemachine.structure.Move;

public class RandomSelector extends TunerSelector{

	public RandomSelector(GameDependentParameters gameDependentParameters,
			Random random, GamerSettings gamerSettings,	SharedReferencesCollector sharedReferencesCollector, String id) {
		super(gameDependentParameters, random, gamerSettings, sharedReferencesCollector, id);
	}

	@Override
	public void setReferences(
			SharedReferencesCollector sharedReferencesCollector) {
		// Do nothing

	}

	/*
	 * TODO: ATTENTION! If you add code here that does something on the state of the tuner remember that this method might
	 * be called multiple times after each game if the player is using the SequentialParametersTuner!!! If you want this method
	 * to be called only once per instance of TunerSelector then change the code in the copy constructor of the ParametersTuner
	 * subclasses in order to deep-copy also the TunerSelectors!
	 */
	@Override
	public void clearComponent() {
		// Do nothing

	}

	/*
	 * TODO: ATTENTION! If you add code here that does something on the state of the tuner remember that this method might
	 * be called multiple times before each game if the player is using the SequentialParametersTuner!!! If you want this method
	 * to be called only once per instance of TunerSelector then change the code in the copy constructor of the ParametersTuner
	 * subclasses in order to deep-copy also the TunerSelectors!
	 */
	@Override
	public void setUpComponent() {
		// Do nothing

	}

	@Override
	public int selectMove(MoveStats[] movesStats, int numUpdates) {
		return this.random.nextInt(movesStats.length);
	}

	@Override
	public Move selectMove(Map<Move, MoveStats> movesStats, int numUpdates) {
		int randomNum = this.random.nextInt(movesStats.size());

		for(Entry<Move,MoveStats> entry : movesStats.entrySet()){
			if(randomNum == 0){
				return entry.getKey();
			}
			randomNum--;
		}

		return null;

	}

	@Override
	public String getComponentParameters(String indentation) {
		return null;
	}

}