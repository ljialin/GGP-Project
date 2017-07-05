package org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.evolution;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.GamerSettings;
import org.ggp.base.player.gamer.statemachine.MCS.manager.hybrid.CompleteMoveStats;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.GameDependentParameters;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.SearchManagerComponent;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.SharedReferencesCollector;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.evolution.crossover.CrossoverManager;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.evolution.mutation.MutationManager;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.structure.CombinatorialCompactMove;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.reflection.ProjectSearcher;

/**
 * This class evolves a population of size populationsSize keeping eliteSize individuals and using them
 * to generate (populationsSize-eliteSize) new individuals. Each new individual is generated by crossover
 * of two random elite individuals with probability crossoverProbability. With (1-crossoverProbability)
 * probability a new individual is generated by mutation.
 *
 * @author C.Sironi
 *
 */
public class StandardEvolutionManager extends EvolutionManager {

	/**
	 * When evolving the population, a new individual is created by crossover of two random parents
	 * with crossoverProbability, while it's created as mutation of a single parent with probability
	 * (1-crossoverProbability).
	 */
	private double crossoverProbability;

	/**
	 * Class that takes care of creating 1 individual by crossover of two parents.
	 */
	private CrossoverManager crossoverManager;

	/**
	 * Class that takes care of creating 1 individual by mutation of a parent.
	 */
	private MutationManager mutationManager;

	public StandardEvolutionManager(GameDependentParameters gameDependentParameters, Random random,
			GamerSettings gamerSettings, SharedReferencesCollector sharedReferencesCollector) {
		super(gameDependentParameters, random, gamerSettings, sharedReferencesCollector);

		this.crossoverProbability = gamerSettings.getDoublePropertyValue("EvolutionManager.crossoverProbability");

		try {
			this.crossoverManager = (CrossoverManager) SearchManagerComponent.getConstructorForSearchManagerComponent(SearchManagerComponent.getCorrespondingClass(ProjectSearcher.CROSSOVER_MANAGERS.getConcreteClasses(),
					gamerSettings.getPropertyValue("EvolutionManager.crossoverManagerType"))).newInstance(gameDependentParameters, random, gamerSettings, sharedReferencesCollector);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO: fix this!
			GamerLogger.logError("SearchManagerCreation", "Error when instantiating CrossoverManager " + gamerSettings.getPropertyValue("EvolutionManager.crossoverManagerType") + ".");
			GamerLogger.logStackTrace("SearchManagerCreation", e);
			throw new RuntimeException(e);
		}

		try {
			this.mutationManager = (MutationManager) SearchManagerComponent.getConstructorForSearchManagerComponent(SearchManagerComponent.getCorrespondingClass(ProjectSearcher.MUTATION_MANAGERS.getConcreteClasses(),
					gamerSettings.getPropertyValue("EvolutionManager.mutationManagerType"))).newInstance(gameDependentParameters, random, gamerSettings, sharedReferencesCollector);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO: fix this!
			GamerLogger.logError("SearchManagerCreation", "Error when instantiating MutationManager " + gamerSettings.getPropertyValue("EvolutionManager.mutationManagerType") + ".");
			GamerLogger.logStackTrace("SearchManagerCreation", e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public void setReferences(SharedReferencesCollector sharedReferencesCollector) {
		super.setReferences(sharedReferencesCollector);
		this.crossoverManager.setReferences(sharedReferencesCollector);
		this.mutationManager.setReferences(sharedReferencesCollector);
	}

	@Override
	public void clearComponent() {
		super.clearComponent();
		this.crossoverManager.clearComponent();
		this.mutationManager.clearComponent();
	}

	@Override
	public void setUpComponent() {
		super.setUpComponent();
		this.crossoverManager.setUpComponent();
		this.mutationManager.setUpComponent();
	}

	/**
	 * Returns random initial population with populationSize individuals.
	 */
	@Override
	public CompleteMoveStats[] getInitialPopulation() {

		CompleteMoveStats[] population = new CompleteMoveStats[this.populationsSize];

		List<CombinatorialCompactMove> allLegalCombos = this.parametersManager.getAllLegalParametersCombinations();

		for(int i = 0; i < this.populationsSize; i++){
			population[i] = new CompleteMoveStats(allLegalCombos.get(this.random.nextInt(allLegalCombos.size())));
		}

		return population;
	}

	@Override
	public void evolvePopulation(CompleteMoveStats[] population) {

		Arrays.sort(population,
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
				// Sort from largest to smallest
				if(value1 > value2){
					return -1;
				}else if(value1 < value2){
					return 1;
				}else{
					return 0;
				}
			}

		});

		// For the individuals that we are keeping, reset all statistics.
		for(int i = 0; i < this.eliteSize; i++){
			population[i].resetStats();
		}

		// For other individuals, generate new individual t substitute them and reset also statistics.
		// Keep the first eliteSize best individuals and create new individuals
		// to substitute the ones that are being thrown away.
		for(int i = this.eliteSize; i < population.length; i++){

			if(this.random.nextDouble() < this.crossoverProbability){
				// Create new individual with crossover
				population[i].resetStats(this.crossoverManager.crossover(((CombinatorialCompactMove)population[this.random.nextInt(this.eliteSize)].getTheMove()),
						((CombinatorialCompactMove)population[this.random.nextInt(this.eliteSize)].getTheMove())));
			}else{
				// Create new individual with mutation
				population[i].resetStats(this.mutationManager.mutation(((CombinatorialCompactMove)population[this.random.nextInt(this.eliteSize)].getTheMove())));
			}

		}

	}

	@Override
	public String getComponentParameters(String indentation) {

		String superParams = super.getComponentParameters(indentation);

		String params = indentation + "CROSSOVER_PROBABILITY = " + this.crossoverProbability +
				indentation + "CROSSOVER_MANAGER = " + this.crossoverManager.printComponent(indentation + "  ") +
				indentation + "MUTATION_MANAGER = " + this.mutationManager.printComponent(indentation + "  ");

		if(superParams != null){
			return superParams + params;
		}else{
			return params;
		}

	}

}
