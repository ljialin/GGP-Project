package org.ggp.base.util.propnet.creationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.util.OpenBitSet;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.DynamicComponent;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.DynamicPropNet;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicProposition;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicTransition;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.ImmutableComponent;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.ImmutablePropNet;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.components.ImmutableAnd;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.components.ImmutableNot;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.components.ImmutableOr;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.components.ImmutableProposition;
import org.ggp.base.util.propnet.architecture.separateExtendedState.immutable.components.ImmutableTransition;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.OptimizationCaller;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.OptimizeAwayConstantValueComponents;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.OptimizeAwayConstants;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.RemoveAnonPropositions;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.RemoveOutputlessComponents;
import org.ggp.base.util.propnet.factory.DynamicPropNetFactory;
import org.ggp.base.util.propnet.state.ImmutableSeparatePropnetState;
import org.ggp.base.util.propnet.utils.PROP_TYPE;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitRole;

import cyclesjohnsonmeyer.de.normalisiert.utils.graphs.ElementaryCyclesSearch;


/**
 * This class takes care of the followings:
 *
 * 1. Create the propNet structure;
 * 2. Optimize the propNet structure (e.g. remove redundant components, shrink
 *    the propNet structure...);
 * 3. Initialize a consistent propNet state (i.e. assign to every proposition a
 * 	  truth value that's consistent with the value of its inputs). Note that this
 * 	  state will be memorized externally and not in each component of the propNet.
 *
 * This class tries to create the propNet. If it fails (i.e. gets interrupted before
 * the propNet has been completely created), the propnet and its external state will
 * be set to null. If it manages to build the propnet in time, it will try to
 * incrementally optimize it until it is interrupted. In this case, when it will be
 * interrupted, the propNet parameter will be set to the last completed optimization
 * of the propnet, so that it can be used and won't be in an inconsistent state.
 * The propnet state will also be initialized accordingly.
 *
 * @author C.Sironi
 *
 */
public class SeparateInternalPropnetManager extends Thread{

	private List<Gdl> description;

	private long timeout;

	/**
	 * Array with all the optimizations that should be performed on the propnet
	 * in the given order.
	 */
	private OptimizationCaller[] optimizations;

	/**
	 * Propnet structure to be used in the optimization phase. Can be modified.
	 */
	private DynamicPropNet dynamicPropNet;

	private long propNetConstructionTime = -1L;

	private long totalInitTime;

	/**
	 * Array that lists the number of components after the creation of the propnet and after performing each optimization.
	 */
	private int[] componentsStats;

	/**
	 * Essential propnet structure to be used in the game playing phase by the state
	 * machine. Cannot be modified and leaves as visible to the state machine only
	 * the strictly necessary components that it needs to reason on the game.
	 */
	private ImmutablePropNet immutablePropnet;

	/**
	 * The initial state of the propnet (i.e. the truth values for each component of
	 * the propnet in the initial state of the game).
	 * This state is built on the immutablePropnet, that is the one that can change its
	 * values. This state, together with the immutable propnet, allows the state machine
	 * to reason on the game.
	 */
	private ImmutableSeparatePropnetState initialPropnetState;

	public SeparateInternalPropnetManager(List<Gdl> description, long timeout) {

		this(description, timeout, null);

	}

	public SeparateInternalPropnetManager(List<Gdl> description, long timeout, OptimizationCaller[] optimizations) {
		this.description = description;
		this.timeout = timeout;

		if(optimizations == null){

			//this.optimizations = new OptimizationCaller[0];

			this.optimizations = new OptimizationCaller[4];

			this.optimizations[0] = new RemoveAnonPropositions();
			this.optimizations[1] = new OptimizeAwayConstants();
			this.optimizations[2] = new OptimizeAwayConstantValueComponents();
			this.optimizations[3] = new RemoveOutputlessComponents();
		}else{
			this.optimizations = optimizations;
		}

		this.componentsStats = new int[this.optimizations.length+1];

		for(int i = 0; i < this.componentsStats.length; i++){
			this.componentsStats[i] = -1;
		}
	}

	@Override
	public void run(){

		//try{

		// TODO: use the timeout to decide if it is worth trying another optimization
		// or if there probably is not enough time and we don't want to risk taking too
		// long to interrupt in case the time was not enough.

		GamerLogger.log("PropnetManager", "Starting to build propnet.");

		// 1. Create the propnet.
		long startTime = System.currentTimeMillis();

    	try{
    		this.dynamicPropNet = DynamicPropNetFactory.create(description);
    	}catch(InterruptedException e){
    		GamerLogger.logError("PropnetManager", "Propnet creation interrupted!");
    		GamerLogger.logStackTrace("PropnetManager", e);
    		this.dynamicPropNet = null;
    		this.initialPropnetState = null;
    		this.propNetConstructionTime = -1;
    		this.totalInitTime = System.currentTimeMillis() - startTime;
    		return;
    	}
    	// Compute the time taken to construct the propnet
    	this.propNetConstructionTime = System.currentTimeMillis() - startTime;
		GamerLogger.log("PropnetManager", "[Propnet Creator] Propnet creation done. It took " + this.propNetConstructionTime + "ms.");

		this.componentsStats[0] = this.dynamicPropNet.getSize();


		//System.out.println(this.dynamicPropNet.toString());
		//System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS");

		/*
		System.out.println("Propnet has: " + this.propNet.getSize() + " COMPONENTS, " + this.propNet.getNumPropositions() + " PROPOSITIONS, " + this.propNet.getNumLinks() + " LINKS.");
		System.out.println("Propnet has: " + this.propNet.getNumAnds() + " ANDS, " + this.propNet.getNumOrs() + " ORS, " + this.propNet.getNumNots() + " NOTS.");
		System.out.println("Propnet has: " + this.propNet.getNumBases() + " BASES, " + this.propNet.getNumTransitions() + " TRANSITIONS.");
		System.out.println("Propnet has: " + this.propNet.getNumInputs() + " INPUTS, " + this.propNet.getNumLegals() + " LEGALS.");
		System.out.println("Propnet has: " + this.propNet.getNumGoals() + " GOALS.");
		System.out.println("Propnet has: " + this.propNet.getNumInits() + " INITS, " + this.propNet.getNumTerminals() + " TERMINALS.");
		*/



		/* Check if manager has been interrupted between creation and initialization of the propnet.
		 * In this case the propnet structure has been completely created but there is no time for
		 * initialization of the corresponding state. Use this check or not? If not it means that
		 * whenever the manager gets interrupted, if there is a completed version of the propnet
		 * structure available the corresponding state will for sure be initialized so the propnet
		 * can be used. Note that there is a tradeoff between having the guarantee that whenever a
		 * propnet structure is available we also have the corresponding state and having guarantee
		 * taht the player will not time out while getting ready to play.
		try{
			ConcurrencyUtils.checkForInterruption();
		}catch(InterruptedException e){
			GamerLogger.logError("PropnetManager", "Manager interrupted before ropnet state initialization!");
    		GamerLogger.logStackTrace("PropnetManager", e);
    		this.propNet = null;
    		this.initialPropnetState = null;
    		this.propNetConstructionTime = -1;
    		return;
		}
		*/

		/****** PRE-OPTIMIZATIONS (no more needed, done automatically when initializing the propnet)*******/

		/** 0. FIX INPUTLESS COMPONENT:
		 *  set to true or false the input of input-less components that are not supposed to have no input.
		 *  Required before performing any other optimization.
		 */
		//DynamicPropNetFactory.fixInputlessComponents(this.dynamicPropNet);

		/***************************************** OPTIMIZATIONS ******************************************/

		/**
		 * No need to call each optimization singularly.
		 * The optimizations will be called iterating over the array of optimizations.
		 */
		/** 1. OPTIMIZE AWAY CONSTANT COMPONENTS:
		 * 	remove components that are useless (e.g. always true or false).
		 */
		//DynamicPropNetFactory.optimizeAwayConstants(this.dynamicPropNet);

		/*
		System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS, " + this.dynamicPropNet.getNumPropositions() + " PROPOSITIONS, " + this.dynamicPropNet.getNumLinks() + " LINKS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumAnds() + " ANDS, " + this.dynamicPropNet.getNumOrs() + " ORS, " + this.dynamicPropNet.getNumNots() + " NOTS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumBases() + " BASES, " + this.dynamicPropNet.getNumTransitions() + " TRANSITIONS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInputs() + " INPUTS, " + this.dynamicPropNet.getNumLegals() + " LEGALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumGoals() + " GOALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInits() + " INITS, " + this.dynamicPropNet.getNumTerminals() + " TERMINALS.");
		*/

		//System.out.println(this.dynamicPropNet.toString());
		//System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS");

		/** 2. REMOVE ANONYMOUS PROPOSITIONS:
		 *  find and remove all the propositions that have no particular GDL meaning (i.e. the ones that have
		 *  type OTHER). Before removing them connect their single input to each of their outputs.
		 */
		//DynamicPropNetFactory.removeAnonymousPropositions(this.dynamicPropNet);

		/*
		System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS, " + this.dynamicPropNet.getNumPropositions() + " PROPOSITIONS, " + this.dynamicPropNet.getNumLinks() + " LINKS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumAnds() + " ANDS, " + this.dynamicPropNet.getNumOrs() + " ORS, " + this.dynamicPropNet.getNumNots() + " NOTS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumBases() + " BASES, " + this.dynamicPropNet.getNumTransitions() + " TRANSITIONS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInputs() + " INPUTS, " + this.dynamicPropNet.getNumLegals() + " LEGALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumGoals() + " GOALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInits() + " INITS, " + this.dynamicPropNet.getNumTerminals() + " TERMINALS.");
		*/

		//System.out.println(this.dynamicPropNet.toString());
		//System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS");

		/** 3. REMOVE USELESS COMPONENTS WITH CONSTANT VALUE:
		 *  find all the components in the propnet that will always have the same value in all possible states
		 *  of the game (i.e. always TRUE or always FALSE), then connect them to the TRUE/FALSE constant and
		 *  optimize away the components that result in being useless.
		 */

		//try {
		//	DynamicPropNetFactory.optimizeAwayConstantValueComponents(this.dynamicPropNet);
		//} catch (InterruptedException e) {
		//	GamerLogger.logError("PropnetManager", "Propnet optimization interrupted!");
    	//	GamerLogger.logStackTrace("PropnetManager", e);
    		// Note that here the dynamic propnet is still consistent. The initial propnet state can
    		// be computed on the previous optimization by removing the following "return" statement.
    	//	return;
		//}

		/*
		System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS, " + this.dynamicPropNet.getNumPropositions() + " PROPOSITIONS, " + this.dynamicPropNet.getNumLinks() + " LINKS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumAnds() + " ANDS, " + this.dynamicPropNet.getNumOrs() + " ORS, " + this.dynamicPropNet.getNumNots() + " NOTS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumBases() + " BASES, " + this.dynamicPropNet.getNumTransitions() + " TRANSITIONS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInputs() + " INPUTS, " + this.dynamicPropNet.getNumLegals() + " LEGALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumGoals() + " GOALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInits() + " INITS, " + this.dynamicPropNet.getNumTerminals() + " TERMINALS.");
		*/

		//System.out.println(this.dynamicPropNet.toString());
		//System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS");

		/** 4. REMOVE OUTPUT-LESS COMPONENTS THAT ARE USELESS:
		 *  find all the components in the propnet that have no outputs and if they don't have a special
		 *  meaning for the game, remove them (e.g. there is no reason to keep an AND or an OR gate if it
		 *  has no outputs).
		 */

		//DynamicPropNetFactory.removeOutputlessComponents(this.dynamicPropNet);

		/*
		System.out.println("Propnet has: " + this.dynamicPropNet.getSize() + " COMPONENTS, " + this.dynamicPropNet.getNumPropositions() + " PROPOSITIONS, " + this.dynamicPropNet.getNumLinks() + " LINKS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumAnds() + " ANDS, " + this.dynamicPropNet.getNumOrs() + " ORS, " + this.dynamicPropNet.getNumNots() + " NOTS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumBases() + " BASES, " + this.dynamicPropNet.getNumTransitions() + " TRANSITIONS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInputs() + " INPUTS, " + this.dynamicPropNet.getNumLegals() + " LEGALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumGoals() + " GOALS.");
		System.out.println("Propnet has: " + this.dynamicPropNet.getNumInits() + " INITS, " + this.dynamicPropNet.getNumTerminals() + " TERMINALS.");
		*/

		//System.out.println(this.optimizations);

		// this.optimizations is non null for sure!
		if(this.optimizations.length > 0){

			GamerLogger.log("PropnetManager", "Performing optimizations.");

			int i = 1;

			for(OptimizationCaller caller : this.optimizations){
				try {
					GamerLogger.log("PropnetManager", "Starting optimization " + caller.getClass().getSimpleName() + ".");
					caller.optimize(this.dynamicPropNet);
					GamerLogger.log("PropnetManager", "Ended optimization " + caller.getClass().getSimpleName() + ".");
					this.componentsStats[i] = this.dynamicPropNet.getSize();
					i++;
				} catch (InterruptedException e) {
					GamerLogger.logError("PropnetManager", "Propnet optimization " + caller.getClass().getSimpleName() + " interrupted!");
		    		GamerLogger.logStackTrace("PropnetManager", e);
		    		this.dynamicPropNet = null;
		    		this.initialPropnetState = null;
		    		this.propNetConstructionTime = -1;
		    		this.totalInitTime = System.currentTimeMillis() - startTime;
		    		// TODO: if we fail here do something to save the propnet before this last optimization.
		    		// That propnet is still usable. The initial propnet state can be computed on the previous
		    		// optimization by removing the following "return" statement (and saving somewhere the
		    		// state of the dynamic propnet before calling the last optimization).
		    		return;
				}
			}
		}else{
			GamerLogger.log("PropnetManager", "No optimizations to be performed.");
		}

		GamerLogger.log("PropnetManager", "Propnet has " + this.dynamicPropNet.getSize() + " components.");


		//System.out.println(this.dynamicPropNet);

		/************************ PROPNET EXTERNAL COMPLETE STATE INITIALIZATION **************************/

		GamerLogger.log("PropnetManager", "Starting computation of PropNet state.");

		this.computeSeparatePropAutomata();

		GamerLogger.log("PropnetManager", "Ended computation of PropNet state.");

		this.totalInitTime = System.currentTimeMillis() - startTime;

		//}catch(Exception e){
		//	System.out.println("ECCEZIONE!");
		//	e.printStackTrace();
		//}
	}

	/**
	 * This method creates the immutable version of the propnet and the corresponding
	 * external initial state (i.e. it creates a propositional automata).
	 * The external state contains the truth values for all the propositions in the
	 * propnet. These values are initialized such that the value of each component
	 * is consistent with the value(s) of its input(s). Moreover, this initial state
	 * for the propnet is set to correspond to the initial state of the game (i.e.
	 * the base propositions that are true by init are set to true, while all other
	 * base propositions are set to false).
	 *
	 * This method can be used once all the transformations on the dynamic propnet have
	 * been completed, to get a static version of the propnet that can be used by the
	 * state machine, or it can be used between propnet transformations to guarantee
	 * that at any time this class can return a consistent, static version of the propnet
	 * that the state machine can use even if this class is still busy optimizing the
	 * dynamic version of the propnet.
	 */
	private void computeSeparatePropAutomata(){

		if(this.dynamicPropNet != null){

			// 1. GET THE IMMUTABLE VERSION OF ALL THE COMPONENTS (AND THE REPRESENTATION OF THE PROPNET AS ADJACENCY LIST)

			// Create an adjacency list representing the structure of the propnet where each node is represented
			// by its index in the list of immutable components

			List<List<Integer>> propnetAdjacencyLists = new ArrayList<List<Integer>>(this.dynamicPropNet.getComponents().size());
			//int[][] propnetAdjacencyLists = new int[this.dynamicPropNet.getComponents().size()][];

			// This method will create the list of immutable components in the propnet and the corresponding adjacency list that
			// will be used as abstract representation of the structure of the propnet graph by the methods that need to operate
			// on the structure of such graph (i.e. detect cycles, compute topological order)

			ImmutableComponent[] immutableComponents = this.dynamicToImmutableComponents(this.dynamicPropNet.getComponents(), propnetAdjacencyLists);
			//ImmutableComponent[] immutableComponents = this.dynamicToImmutableComponents(this.dynamicPropNet.getComponents(), propnetAdjacencyLists);

			// 2. DETECT CYCLES IN PROPNET

			ElementaryCyclesSearch ecs = new ElementaryCyclesSearch(propnetAdjacencyLists);

			/*
			Integer[] nodesNumbers = new Integer[immutableComponents.length];
			for(int i = 0; i < nodesNumbers.length; i++){
				nodesNumbers[i] = new Integer(i);
			}
			ElementaryCyclesSearch ecs = new ElementaryCyclesSearch(propnetAdjacencyLists, nodesNumbers);
			*/

			//long time = System.currentTimeMillis();
			List<List<Integer>> cycles = ecs.getElementaryCycles();
			//System.out.println( (System.currentTimeMillis() - time) + "ms to detect cycles");

			GamerLogger.log("PropnetManager", "Found " + cycles.size() + " cycles in PropNet.");

			// Do here all the processing of the cycles (i.e. consider only cycles with ORs, remove from propnet the cycles without
			// ORs because all nodes in them will have constant value, ecc...)
			// For now we just look for all cycles with ORs in them and put the nodes in such cycles in the set of nodes that must
			// be checked (and reset) when propagating.

			// TODO: look for all ORs in cycles and mark all inputs of such ORs that do not belong to the same cycle as the OR.
			// Optimize the propagation of values in cycles by resetting a given cycle only when those external inputs change
			// their value from 1 to 0

			Set<ImmutableComponent> componentsInCycles = new HashSet<ImmutableComponent>();

			List<ImmutableComponent> immutableComponentsCycle;
			ImmutableComponent currentComponent;
			boolean withOr; // True if the cycle contains an OR, false otherwise

			int cyclesWithOr = 0;

			for(List<Integer> cycle : cycles){
				immutableComponentsCycle = new ArrayList<ImmutableComponent>();
				withOr = false;
				for(Integer i : cycle){
					currentComponent = immutableComponents[i.intValue()];
					withOr = withOr || currentComponent instanceof ImmutableOr;
					immutableComponentsCycle.add(currentComponent);
				}
				if(withOr){
					cyclesWithOr++;
					componentsInCycles.addAll(immutableComponentsCycle);
				}
			}

			GamerLogger.log("PropnetManager", "Found " + cyclesWithOr + " cycles containing OR gates in PropNet.");

			ImmutableComponent[] immutableComponentsInCycles = new ImmutableComponent[componentsInCycles.size()];
			int i = 0;
			for(ImmutableComponent c : componentsInCycles){
				immutableComponentsInCycles[i] = c;
				i++;
			}

			GamerLogger.log("PropnetManager", "Found " + immutableComponentsInCycles.length + " components in cycles to reset.");

			// 2. PREPARE THE ROLES
			ExplicitRole[] roles = new ExplicitRole[this.dynamicPropNet.getRoles().size()];
			i = 0;
			for(ExplicitRole r : this.dynamicPropNet.getRoles()){
				roles[i] = new ExplicitRole(r.getName());
				i++;
			}

			// 3. TAKE CARE OF BASES AND TRANSITIONS
			// Prepare the immutable base propositions and the corresponding truth values
			// and the truth values corresponding to transitions.
			List<DynamicProposition> dynamicBasePropositions = this.dynamicPropNet.getBasePropositions();
			int numBases = dynamicBasePropositions.size();
			ImmutableProposition[] immutableBasePropositions = new ImmutableProposition[numBases];
			OpenBitSet initialState = new OpenBitSet(numBases);
			OpenBitSet nextState = new OpenBitSet(numBases);

			// TODO: this assumes that the propnet contains the exact same number of base propositions and
			// transitions, thus when removing a base proposition always make sure to also remove the
			// corresponding transition.
			i = 0;
			// For each dynamic base proposition...
			for(DynamicProposition dynamicBase : dynamicBasePropositions){
				// ...get the corresponding immutable base,...
				ImmutableProposition base = (ImmutableProposition) immutableComponents[dynamicBase.getStructureIndex()];
				// ...add it to the immutable bases array,...
				immutableBasePropositions[i] = base;
				// ...set its state index to the correct value,...
				base.setStateIndex(i);
				// ...set the state index of the corresponding transition,...
				base.getSingleInput().setStateIndex(i);
				// ...and set the correct truth value for the initial game state:
				// if it's a base proposition true in the initial state, set it to
				// true in the bit array representing the initial state.
				if(((ImmutableTransition) base.getSingleInput()).isDependingOnInit()){
					initialState.set(i);
				}
				i++;
			}

			// 4. TAKE CARE OF INPUTS
			// Prepare the immutable input propositions and the corresponding truth values.
			List<DynamicProposition> dynamicInputPropositions = this.dynamicPropNet.getInputPropositions();
			int numInputs = dynamicInputPropositions.size();
			ImmutableProposition[] immutableInputPropositions = new ImmutableProposition[numInputs];
			OpenBitSet currentJointMove = new OpenBitSet(numInputs);

			i = 0;
			// For each dynamic input proposition...
			for(DynamicProposition dynamicInput : dynamicInputPropositions){
				// ...get the corresponding immutable input and add it to the immutable inputs array,...
				immutableInputPropositions[i] = (ImmutableProposition) immutableComponents[dynamicInput.getStructureIndex()];
				// ...and set its state index to the correct value.
				immutableInputPropositions[i].setStateIndex(i);

				i++;
			}

			// 5. TAKE CARE OF OTHER COMPONENTS:

			// 5A. TERMINAL PROPOSITION
			// Set the correct state index for the immutable terminal proposition.
			// It has been decided that the truth value of the terminal proposition is
			// always the first value in the OtherComponents bit set in the propnet	state.
			immutableComponents[this.dynamicPropNet.getTerminalProposition().getStructureIndex()].setStateIndex(0);

			// 5B. GOALS
			// Set the correct state index for the goal propositions and collect all goal values
			// corresponding to the propositions.

			i = 1;

			Map<ExplicitRole, List<DynamicProposition>> goalsPerRole = this.dynamicPropNet.getGoalsPerRole();

			int[] firstGoalIndices = new int[roles.length+1];
			int[][] goalValues = new int[roles.length][];

			int j;
			for(j = 0; j < roles.length; j++){
				firstGoalIndices[j] = i;
				List<DynamicProposition> goals = goalsPerRole.get(roles[j]);

				int goalsize = goals.size();

				goalValues[j] = new int[goalsize];
				int k = 0;
				for(DynamicProposition roleGoal : goals){
					immutableComponents[roleGoal.getStructureIndex()].setStateIndex(i);
					goalValues[j][k] = this.dynamicPropNet.getGoalValue(roleGoal);
					k++;
					i++;
				}
			}
			firstGoalIndices[j] = i;

			// 5C. LEGALS
			// Set the correct state index for the legal propositions.

			Map<ExplicitRole, List<DynamicProposition>> legalsPerRole = this.dynamicPropNet.getLegalsPerRole();

			int[] firstLegalIndices = new int[roles.length+1];

			for(j = 0; j < roles.length; j++){
				firstLegalIndices[j] = i;
				for(DynamicProposition roleLegal : legalsPerRole.get(roles[j])){
					immutableComponents[roleLegal.getStructureIndex()].setStateIndex(i);
					i++;
				}
			}
			firstLegalIndices[j] = i;

			// 5D. REMAINING COMPONENTS
			// Set the correct state index for the remaining components.

			int l = 0;
			int[] andOrGatesValues = new int[this.dynamicPropNet.getAndOrGatesNumber()];

			for(ImmutableComponent c : immutableComponents){
				// The state is a bit
				if(c instanceof ImmutableProposition){
					if(((ImmutableProposition) c).getPropositionType() == PROP_TYPE.OTHER ||
							((ImmutableProposition) c).getPropositionType() == PROP_TYPE.INIT){
						((ImmutableProposition) c).setStateIndex(i);
						i++;
					}
				}else if(c instanceof ImmutableNot){
					c.setStateIndex(i);
					i++;
				// The state is an integer
				}else if(c instanceof ImmutableAnd){
					andOrGatesValues[l] = Integer.MAX_VALUE - c.getInputs().length + 1;
					c.setStateIndex(l);
					l++;
				}else if(c instanceof ImmutableOr){
					andOrGatesValues[l] = Integer.MAX_VALUE;
					c.setStateIndex(l);
					l++;
				}
			}

			// And create the bit set with the truth value of all other components
			// (except AND and OR gates).
			// This corresponds to the truth values of the following components:
			// TERMINAL-GOALS-LEGALS-OTHER PROPOSITIONS AND NOTS
			OpenBitSet otherComponents = new OpenBitSet(i);

			// Create the immutable propnet and the corresponding initial state

			this.initialPropnetState = new ImmutableSeparatePropnetState(initialState, nextState, currentJointMove, firstGoalIndices, firstLegalIndices, andOrGatesValues, otherComponents);
			this.immutablePropnet = new ImmutablePropNet(immutableComponents, immutableComponentsInCycles, roles, immutableBasePropositions, immutableInputPropositions, goalValues, this.dynamicPropNet.getAlwaysTrueBases());

			for(ImmutableComponent c : immutableComponents){
				c.imposeConsistency(this.initialPropnetState);
			}

		}else{
			this.immutablePropnet = null;
			this.initialPropnetState = null;
		}

	}

	private ImmutableComponent[] dynamicToImmutableComponents(Set<DynamicComponent> dynamicComponents, List<List<Integer>> propnetAdjacencyLists){

		ImmutableComponent[] immutableComponents = new ImmutableComponent[dynamicComponents.size()];

		// Create immutable components
		int structureIndex = 0;
		for(DynamicComponent c : dynamicComponents){
			immutableComponents[structureIndex] = c.getImmutableClone();
			c.setStructureIndex(structureIndex);
			structureIndex++;
			propnetAdjacencyLists.add(new ArrayList<Integer>());
		}

		// Add all links
		for(DynamicComponent c : dynamicComponents){
			// Set input links
			int numInputs = c.getInputs().size();
			ImmutableComponent[] immutableInputs = new ImmutableComponent[numInputs];
			int index = 0;
			for(DynamicComponent i : c.getInputs()){
				immutableInputs[index] = immutableComponents[i.getStructureIndex()];
				index++;
			}

			immutableComponents[c.getStructureIndex()].setInputs(immutableInputs);

			// Set output links
			int numOutputs = c.getOutputs().size();
			ImmutableComponent[] immutableOutputs = new ImmutableComponent[numOutputs];
			index = 0;

			//System.out.println("Connecting outputs of " + c.getComponentType());

			List<Integer> componentAdjacencyList = propnetAdjacencyLists.get(c.getStructureIndex());

			if(c instanceof DynamicTransition){

				for(DynamicComponent i : c.getOutputs()){

					//System.out.println("Output: " + i.getComponentType());

					immutableOutputs[index] = immutableComponents[i.getStructureIndex()];
					index++;
				}
			}else{

				for(DynamicComponent i : c.getOutputs()){

					//System.out.println("Output: " + i.getComponentType());

					componentAdjacencyList.add(new Integer(i.getStructureIndex()));

					immutableOutputs[index] = immutableComponents[i.getStructureIndex()];
					index++;
				}
			}

			immutableComponents[c.getStructureIndex()].setOutputs(immutableOutputs);
		}

		return immutableComponents;
	}

/*
	private ImmutableComponent[] dynamicToImmutableComponents(Set<DynamicComponent> dynamicComponents, int[][] propnetAdjacencyLists){

		ImmutableComponent[] immutableComponents = new ImmutableComponent[dynamicComponents.size()];

		// Create immutable components
		int structureIndex = 0;
		for(DynamicComponent c : dynamicComponents){
			immutableComponents[structureIndex] = c.getImmutableClone();
			c.setStructureIndex(structureIndex);
			structureIndex++;
			//!!!propnetAdjacencyLists.add(new ArrayList<Integer>());
		}

		// Add all links
		for(DynamicComponent c : dynamicComponents){
			// Set input links
			int numInputs = c.getInputs().size();
			ImmutableComponent[] immutableInputs = new ImmutableComponent[numInputs];
			int index = 0;
			for(DynamicComponent i : c.getInputs()){
				immutableInputs[index] = immutableComponents[i.getStructureIndex()];
				index++;
			}

			immutableComponents[c.getStructureIndex()].setInputs(immutableInputs);

			// Set output links
			int numOutputs = c.getOutputs().size();
			ImmutableComponent[] immutableOutputs = new ImmutableComponent[numOutputs];
			index = 0;

			//System.out.println("Connecting outputs of " + c.getComponentType());

			int[] componentAdjacencyList;

			if(c instanceof DynamicTransition){
				componentAdjacencyList = new int[0];

				for(DynamicComponent i : c.getOutputs()){

					//System.out.println("Output: " + i.getComponentType());

					immutableOutputs[index] = immutableComponents[i.getStructureIndex()];
					index++;
				}
			}else{
				componentAdjacencyList = new int[c.getOutputs().size()];

				for(DynamicComponent i : c.getOutputs()){

					//System.out.println("Output: " + i.getComponentType());

					componentAdjacencyList[index] = i.getStructureIndex();

					immutableOutputs[index] = immutableComponents[i.getStructureIndex()];
					index++;
				}
			}

			immutableComponents[c.getStructureIndex()].setOutputs(immutableOutputs);
			propnetAdjacencyLists[c.getStructureIndex()] = componentAdjacencyList;
		}

		return immutableComponents;
	}
*/

	/*
	private Set<ImmutableComponent> findNodesInCycles(ImmutableComponent[] immutableComponents){

		boolean added

		Set<ImmutableComponent> nodesInCycles = new HashSet<ImmutableComponent>();

		for(int i = 0; i < immutableComponents.length; i++){
			if()
		}


	}/*

	/**
	 * Getter method.
	 *
	 * @return the object representing the structure of the propnet.
	 */
	public ImmutablePropNet getImmutablePropnet(){
		return this.immutablePropnet;
	}

	public ImmutableSeparatePropnetState getInitialPropnetState(){
		if(this.initialPropnetState == null){
			return null;
		}
		return this.initialPropnetState.clone();
	}

	public long getPropnetConstructionTime(){
		return this.propNetConstructionTime;
	}

	public long getTotalInitTime(){
		return this.totalInitTime;
	}

	public DynamicPropNet getDynamicPropnet(){
		return this.dynamicPropNet;
	}

	public int[] getComponentsStats(){
		return this.componentsStats;
	}

	public OptimizationCaller[] getOptimizations() {
		return this.optimizations;
	}

}
