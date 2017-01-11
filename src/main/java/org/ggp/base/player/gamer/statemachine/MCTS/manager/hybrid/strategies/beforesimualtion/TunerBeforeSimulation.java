package org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.strategies.beforesimualtion;

import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.GamerSettings;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.combinatorialtuning.CombinatorialTuner;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.combinatorialtuning.UcbCombinatorialTuner;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.evolution.OnlineTunableComponent;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.GameDependentParameters;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.SharedReferencesCollector;

public class TunerBeforeSimulation extends BeforeSimulationStrategy {

	private boolean tuneAllRoles;

	/**
	 * Each selected configuration of parameters will be evaluated with a batch of simulations and not
	 * only one simulation. This expresses the size of such batch (i.e. after evaluating batchSize times
	 * a configuration of parameters, a new one is selected with the combinatorial tuner).
	 */
	private int batchSize;

	/**
	 * Counts the number of simulations performed in the current interval.
	 * When this number reaches batchSize then the parameters are updated with a new
	 * configuration that will be evaluated next.
	 */
	private int simCount;

	private CombinatorialTuner combinatorialTuner;

	/**
	 * List of the components that we are tuning.
	 */
	private List<OnlineTunableComponent> tunableComponents;

	public TunerBeforeSimulation(GameDependentParameters gameDependentParameters, Random random,
			GamerSettings gamerSettings, SharedReferencesCollector sharedReferencesCollector) {

		super(gameDependentParameters, random, gamerSettings, sharedReferencesCollector);

		this.tuneAllRoles = gamerSettings.getBooleanPropertyValue("BeforeSimulationStrategy.tuneAllRoles");

		this.batchSize = gamerSettings.getIntPropertyValue("BeforeSimulationStrategy.batchSize");

		this.simCount = 0;

		double tunerC = gamerSettings.getDoublePropertyValue("BeforeSimulationStrategy.tunerC");
		double tunerValueOffset = gamerSettings.getDoublePropertyValue("BeforeSimulationStrategy.tunerValueOffset");
		double tunerFpu = gamerSettings.getDoublePropertyValue("BeforeSimulationStrategy.tunerFpu");

		this.combinatorialTuner = new UcbCombinatorialTuner(random, tunerC, tunerValueOffset, tunerFpu);

		sharedReferencesCollector.setCombinatorialTuner(combinatorialTuner);

		// Here the combinatorial tuner has no classes lengths.
		// They will be initialized when setting references to the tunable components.

	}

	@Override
	public void setReferences(SharedReferencesCollector sharedReferencesCollector) {
		this.tunableComponents = sharedReferencesCollector.getTheComponentsToTune();

		int[] classesLength = new int[this.tunableComponents.size()];

		int i = 0;
		for(OnlineTunableComponent c : this.tunableComponents){
			classesLength[i] = c.getPossibleValues().length;
			i++;
		}

		this.combinatorialTuner.setClassesLength(classesLength);

	}

	@Override
	public void clearComponent(){

		this.simCount = 0;

		// It's not the job of this class to clear the tunable component because the component
		// is for sure either another strategy or part of another strategy. A class must be
		// responsible of clearing only the objects that it was responsible for creating.
		this.combinatorialTuner.clear();
	}

	@Override
	public void setUpComponent(){

		if(this.tuneAllRoles){
			this.combinatorialTuner.setUp(this.gameDependentParameters.getNumRoles());
		}else{
			this.combinatorialTuner.setUp(1);
		}

		this.simCount = 0;

	}

	@Override
	public void beforeSimulationActions() {

		if(this.simCount % this.batchSize == 0){
			int[][] nextCombinations = this.combinatorialTuner.selectNextCombinations();

			int i = 0;
			for(OnlineTunableComponent c : this.tunableComponents){

				//System.out.print(c.getClass().getSimpleName() + ": [ ");

				int[] newValuesIndices = new int[nextCombinations.length]; // nextCombinations.length equals the number of roles for which we are tuning

				for(int j = 0; j < nextCombinations.length; j++){
					newValuesIndices[j] = nextCombinations[j][i];
					//System.out.print(newValuesIndices[j] + " ");
				}

				//System.out.println("]");

				c.setNewValuesFromIndices(newValuesIndices);

				i++;
			}
		}

		this.simCount++;

	}

	@Override
	public String getComponentParameters(String indentation) {

		String params = indentation + "TUNE_ALL_ROLES = " + this.tuneAllRoles + indentation + "COMBINATORIAL_TUNER = " + this.combinatorialTuner.printCombinatorialTuner(indentation + "  ");

		if(this.tunableComponents != null){

			String tunableComponentsString = "[ ";

			for(OnlineTunableComponent c : this.tunableComponents){

				tunableComponentsString += c.printOnlineTunableComponent(indentation + "  ");

			}

			tunableComponentsString += "\n]";

			params += indentation + "ONLINE_TUNABLE_COMPONENTS = " + tunableComponentsString;
		}else{
			params += indentation + "ONLINE_TUNABLE_COMPONENTS = null";
		}

		return params;

	}

}
