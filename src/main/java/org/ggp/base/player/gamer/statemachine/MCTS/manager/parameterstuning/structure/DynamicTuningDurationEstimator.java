package org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.structure;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.GameDependentParameters;
import org.ggp.base.util.logging.GamerLogger;

/**
 * Used to estimate how many different combinations the tuner will be able to sample during the game
 * (i.e. how many times the tuner will be able to change the evaluated combination). To do this we
 * keep statistics about the number of combinations sampled in the first step of the search and
 * use them to compute how many combinations we will be able to visit per game step and multiply it
 * by the estimated number of game steps.
 *
 * NOTE that we cannot use the parameter stepIterations in the GameDependentParameters class to
 * count the number of times the tuner will be able to change the evaluated combination during
 * the game, because the tuner might be using a batch of iterations to take multiple samples of
 * the same parameter combination. For LSI, when using batches of iterations we must consider each
 * batch as a single sample of the combination in order to properly divide the samples budget among
 * all available combinations in the two phases of LSI.
 *
 * @author c.sironi
 *
 */
public class DynamicTuningDurationEstimator {

	private GameDependentParameters gameDependentParameters;

	/**
	 * When the estimate of the combinations per second that can be visited is computed
	 * in the first step of the game we likely obtain a value that is lower then the average
	 * value of combinations per second over all the game steps. So to better approximate
	 * the average value of combinations per second over all the game steps we multiply the
	 * value computed for the first step with this speedIncreaseFactor.
	 */
	private double speedIncreaseFactor;

	/**
	 * Number of combinations evaluated so far.
	 */
	private int sampledCombos;

	// Parameters that are computed when estimating the total samples //
	//        Some of them are memorized just for log purposes        //
	private double estimatedGameLength;
	private double sampledCombosPerSecond;
	private int estimatedTotalSamples;

	public DynamicTuningDurationEstimator(GameDependentParameters gameDependentParameters, double speedIncreaseFactor) {
		this.gameDependentParameters = gameDependentParameters;
		this.speedIncreaseFactor = speedIncreaseFactor;

		this.sampledCombos = 0;

		this.estimatedGameLength = -1;
		this.sampledCombosPerSecond = -1;
		this.estimatedTotalSamples = -1;
	}

	public void increaseSampledCombos() {
		this.sampledCombos++;
	}

	public void estimateTotalSamples() {

		// First check if the estimate has already been computed.
		// If not, check if the estimate can be computed, otherwise log error and set -1
		if(this.estimatedTotalSamples != -1) {
			GamerLogger.logError("ParametersTuner", "DynamicTuningDurationEstimator - Trying to estimate the total number of samples for the game multiple times. Ignoring request and leaving previously computed estimate.");
		}else if(this.gameDependentParameters.isMetagame()) { // The estimate can be computed consistently only after the metagame.
			GamerLogger.logError("ParametersTuner", "DynamicTuningDurationEstimator - Impossible to estimate the total number of samples for the game during step" +
					this.gameDependentParameters.getGameStep() + " of the search. The estimate will be consistent only if computed after the metagame.");
			this.estimatedGameLength = 0;
			this.sampledCombosPerSecond = 0;
			this.estimatedTotalSamples = 0;
		}else if(this.gameDependentParameters.getStepIterations() <= 0) {
			GamerLogger.logError("ParametersTuner", "DynamicTuningDurationEstimator - Impossible to estimate the total number of samples for the game. " +
					"Found non-positive value for the number of iterations in the metagame: stepIterations=" +
					this.gameDependentParameters.getStepIterations() + ".");
			this.estimatedGameLength = 0;
			this.sampledCombosPerSecond = 0;
			this.estimatedTotalSamples = 0;
		}else if((((double)this.gameDependentParameters.getStepSearchDuration())/1000.0) <= 0) {
			GamerLogger.logError("ParametersTuner", "DynamicTuningDurationEstimator - Impossible to estimate the total number of samples for the game. " +
					"Found non-positive value for the duration in seconds of the metagame search:stepSearchDuration=" +
					(((double)this.gameDependentParameters.getStepSearchDuration())/1000.0) + ".");
			this.estimatedGameLength = 0;
			this.sampledCombosPerSecond = 0;
			this.estimatedTotalSamples = 0;
		}else {
			this.estimatedGameLength = ((double) this.gameDependentParameters.getStepGameLengthSum())/((double)this.gameDependentParameters.getStepIterations());
			this.sampledCombosPerSecond = (((double)this.sampledCombos)/(((double)this.gameDependentParameters.getStepSearchDuration())/1000.0));
			// Round the estimate to the nearest integer
			this.estimatedTotalSamples = (int) Math.round(this.sampledCombosPerSecond * ((double)this.gameDependentParameters.getActualPlayClock()/1000.0) * this.speedIncreaseFactor * this.estimatedGameLength);
		}

	}

	public int getEstimatedTotalSamples() {
		return this.estimatedTotalSamples;
	}

}
