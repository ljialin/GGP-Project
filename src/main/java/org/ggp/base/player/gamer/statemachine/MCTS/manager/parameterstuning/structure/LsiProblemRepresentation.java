package org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ggp.base.player.gamer.statemachine.MCS.manager.MoveStats;
import org.ggp.base.player.gamer.statemachine.MCS.manager.hybrid.CompleteMoveStats;
import org.ggp.base.util.logging.GamerLogger;

import csironi.ggp.course.utils.MyPair;

public class LsiProblemRepresentation {

    public enum Phase{
    	GENERATION, EVALUATION, STOP
    }

    /**
     * Phase of this LSI problem.
     */
    private Phase phase;

    /**
     * Index that keeps track of the next combination to return.
     */
    private int currentIndex;

    //******************************* For the generation phase ********************************//

	/**
	 * Combinatorial actions (i.e. combinations of parameters) that must be tested during the
	 * generation phase, paired with the index of the only parameter that was not picked randomly.
	 * This allows to update only the statistics of this parameter value with the outcome of the
	 * simulation that tested the combination. An alternative would be to ignore this parameter
	 * index and always update all values with the outcome of the simulation, even if they were
	 * selected randomly to complete the combination.
	 */
	private List<MyPair<CombinatorialCompactMove,Integer>> combinationsToTest;

	/**
	 * For each parameter, a list of statistics, each of which corresponds to a possible
	 * value that can be assigned to that parameter.
	 */
	private MoveStats[][] paramsStats;

	/**
	 * True if when receiving the reward for a certain combinatorial action we want to use it to update the stats
	 * of all unit actions that form the combinatorial action. False if we want to use it only to update the stats
	 * for the only unit action that wasn't generated randomly when creating the combinatorial action.
	 */
	private boolean updateAll;

    //******************************* For the evaluation phase ********************************//

	/**
	 * Stats of the combinatorial actions (i.e. combinations of parameters) generated during the generation
	 * phase that must be evaluated with sequential halving during the evaluation phase.
	 */
	private List<CompleteMoveStats> generatedCandidatesStats;

	/**
	 * Maximum number of samples that can be taken for each iteration of sequential halving
	 */
	private int maxSamplesPerIteration;

	/**
	 * Number of candidates being considered during the current iteration of the evaluation phase.
	 * Each time we finish evaluating all of them, they will be halved and only the best half of
	 * them will be considered.
	 */
	private int numCandidatesOfCurrentIteration;

	/**
	 * List of indices of the combinations for the evaluation phase in a random order, specifying the order in which the
	 * generated combinations will be evaluated (indices might appear more than once because each sample might need to be
	 * evaluated multiple times).
	 */
	private List<Integer> evalOrder;

	public LsiProblemRepresentation(List<MyPair<CombinatorialCompactMove,Integer>> combinationsToTest, int[] numValuesPerParam, boolean updateAll) {

		this.phase = Phase.GENERATION;

		this.currentIndex = 0;

		this.combinationsToTest = combinationsToTest;

		this.paramsStats = new MoveStats[numValuesPerParam.length][];

		for(int paramIndex = 0; paramIndex < paramsStats.length; paramIndex++){
			this.paramsStats[paramIndex] = new MoveStats[numValuesPerParam[paramIndex]];
			for(int valueIndex = 0; valueIndex < paramsStats[paramIndex].length; valueIndex++){
				this.paramsStats[paramIndex][valueIndex] = new MoveStats();
			}
		}

		this.updateAll = updateAll;

		this.generatedCandidatesStats = null;

		this.maxSamplesPerIteration = 0;

		this.numCandidatesOfCurrentIteration = 0;

		this.evalOrder = null;
	}

	public Phase getPhase(){
		return this.phase;
	}

	public List<MyPair<CombinatorialCompactMove,Integer>> getCombinationsToTest(){
		return this.combinationsToTest;
	}

	public MoveStats[][] getParamsStats(){
		return this.paramsStats;
	}

	public void setGeneratedCandidatesStats(List<CompleteMoveStats> generatedCandidatesStats, int maxSamplesPerIteration){
		this.generatedCandidatesStats = generatedCandidatesStats;
		this.maxSamplesPerIteration = maxSamplesPerIteration;

		this.numCandidatesOfCurrentIteration = this.generatedCandidatesStats.size();
		this.currentIndex = 0;
		if(this.numCandidatesOfCurrentIteration > 1){
			this.computeEvalOrder();
		}else{ // Otherwise we only have one candidate, that is automatically the best
			this.evalOrder = null;
			this.phase = Phase.STOP;
		}
	}

	public List<CompleteMoveStats> getGeneratedCandidatesStats(){
		return this.generatedCandidatesStats;
	}

	public int[] getNextCombination(){

		switch(this.phase){
		case GENERATION:
			return this.combinationsToTest.get(this.currentIndex).getFirst().getIndices();
		case EVALUATION:
			return ((CombinatorialCompactMove) this.generatedCandidatesStats.get(this.evalOrder.get(this.currentIndex)).getTheMove()).getIndices();
		case STOP:
			// If we stopped it's because the best move has been found and is the first one in the list of candidates
			return ((CombinatorialCompactMove) this.generatedCandidatesStats.get(0).getTheMove()).getIndices();
		default:
			GamerLogger.logError("ParametersTuner", "LsiParametersTuner - Unrecognized phase of LSI when trying to get next combination to test: " + this.phase + "!");
			throw new RuntimeException("LsiParametersTuner - Unrecognized phase of LSI when trying to get next combination to test: " + this.phase + "!");
		}

	}

	public void updateStatsOfCombination(int reward){

		switch(this.phase){
		case GENERATION:
			MyPair<CombinatorialCompactMove,Integer> theTestedCombo = this.combinationsToTest.get(this.currentIndex);

			int[] theIndices = theTestedCombo.getFirst().getIndices();

			if(updateAll){
				for(int pramIndex = 0; pramIndex < theIndices.length; pramIndex++){
					this.paramsStats[pramIndex][theIndices[pramIndex]].incrementScoreSum(reward);
					this.paramsStats[pramIndex][theIndices[pramIndex]].incrementVisits();
				}
			}else{
				int paramIndex = theTestedCombo.getSecond().intValue();
				this.paramsStats[paramIndex][theIndices[paramIndex]].incrementScoreSum(reward);
				this.paramsStats[paramIndex][theIndices[paramIndex]].incrementVisits();
			}

			this.currentIndex++;

			if(this.currentIndex == this.combinationsToTest.size()){// All random combinations have been tested
				this.phase = Phase.EVALUATION;
				this.currentIndex = 0;
				this.numCandidatesOfCurrentIteration = 0;
			}
			break;
		case EVALUATION:

			this.generatedCandidatesStats.get(this.evalOrder.get(this.currentIndex)).incrementScoreSum(reward);
			this.generatedCandidatesStats.get(this.evalOrder.get(this.currentIndex)).incrementVisits();

			this.currentIndex++;

			if(this.currentIndex == this.evalOrder.size()){ // All candidates have been tested for the given amount of times
				// We must half the candidates and recompute the order.
				Collections.sort(this.generatedCandidatesStats.subList(0,this.numCandidatesOfCurrentIteration),
						new Comparator<CompleteMoveStats>(){

							@Override
							public int compare(CompleteMoveStats o1, CompleteMoveStats o2) {

								double value1;
								if(o1.getVisits() == 0){
									value1 = 0;
								}else{
									value1 = o1.getScoreSum()/o1.getVisits();
								}
								double value2;
								if(o2.getVisits() == 0){
									value2 = 0;
								}else{
									value2 = o2.getScoreSum()/o2.getVisits();
								}
								if(value1 > value2){
									return 1;
								}else if(value1 < value2){
									return -1;
								}else{
									return 0;
								}
							}

						});

				this.numCandidatesOfCurrentIteration = (int) Math.ceil((double)this.numCandidatesOfCurrentIteration/2.0);
				this.currentIndex = 0;

				if(this.numCandidatesOfCurrentIteration > 1){
					this.computeEvalOrder();
				}else{ // Otherwise we only have one candidate, that is automatically the best
					this.evalOrder = null;
					this.phase = Phase.STOP;
				}

			}
		case STOP:
			break;
		}
	}

	public void computeEvalOrder(){

		int samplesPerCombo = Math.floorDiv(this.maxSamplesPerIteration, this.numCandidatesOfCurrentIteration);

		if(samplesPerCombo < 1){
			samplesPerCombo = 1; // Get at least one sample
		}

		// Prepare random order of testing for the best elements
		this.evalOrder = new ArrayList<Integer>();

		for(int comboIndex = 0; comboIndex < this.numCandidatesOfCurrentIteration; comboIndex++){
			for(int repetition = 0; repetition < samplesPerCombo; repetition++){
				this.evalOrder.add(new Integer(comboIndex));
			}
		}

		Collections.shuffle(evalOrder);

	}

}
