package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.forwardInterrupting.ForwardInterruptingPropNet;
import org.ggp.base.util.propnet.architecture.forwardInterrupting.components.ForwardInterruptingProposition;
import org.ggp.base.util.propnet.architecture.forwardInterrupting.components.ForwardInterruptingTransition;
import org.ggp.base.util.propnet.factory.FwdInterrPropNetCreator;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineInitializationException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMachineState;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMove;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitRole;

import com.google.common.collect.ImmutableList;


public class CheckFwdInterrPropnetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private ForwardInterruptingPropNet propNet;
    /** The player roles */
    private ImmutableList<ExplicitRole> roles;
    /** The initial state */
    private ExplicitMachineState initialState;

    /** The maximum time (in milliseconds) that this state machine can spend to create the propnet */
    private long maxPropnetCreationTime;

    /**
	 * Total time (in milliseconds) taken to construct the propnet.
	 * If it is negative it means that the propnet didn't build in time.
	 */
	private long constructionTime = -1L;

	/**
	 * Constructor that sets the maximum time (in milliseconds) that this state machine can spend to
	 * create the propnet to the default value of 5 minutes.
	 */
	public CheckFwdInterrPropnetStateMachine(Random random){
    	this(random, 300000L);
    }

	/**
	 * Constructor that sets the maximum time (in milliseconds) that this state machine can spend to
	 * create the propnet to the given time value.
	 *
	 * @param maxPropnetCreationTime the maximum time (in milliseconds) that this state machine can spend to
	 * create the propnet.
	 */
    public CheckFwdInterrPropnetStateMachine(Random random, long maxPropnetCreationTime){

    	super(random);

    	this.maxPropnetCreationTime = maxPropnetCreationTime;
    }

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     *
     * This state machine doesn't care about the timeout for initialization,
     * just makes sure to build the propnet in the given amount of time.
     *
     */
    /*
     * (non-Javadoc)
     * @see org.ggp.base.util.statemachine.StateMachine#initialize(java.util.List)
     */
    @Override
    public void initialize(List<Gdl> description, long timeout) throws StateMachineInitializationException {

    	FwdInterrPropNetCreator creator = new FwdInterrPropNetCreator(description);
    	// Try to create the propnet, if it takes too long stop the creation.
    	creator.start();
    	try{
    		creator.join(maxPropnetCreationTime);
    	}catch (InterruptedException e) {
    		GamerLogger.logError("StateMachine", "[Propnet] Propnet creation interrupted! Terminating initialization!");
    		GamerLogger.logStackTrace("StateMachine", e);
    		Thread.currentThread().interrupt();
			throw new StateMachineInitializationException("Propnet creation interrupted by external action.", e);
		}

    	// After 'maxPropnetCreationTime' milliseconds, if the creator thread is still alive it means
    	// that it is still busy creating the propnet and thus must be interrupted.
    	if(creator.isAlive()){
    		creator.interrupt();
    		try{
    			// Wait for the creator to actually stop running (needed only to have no problems with logging:
    			// if both the state machine thread and the propnet creator thread try to write on the same log
    			// file at the same time there might be an exception. Moreover, the time order of the logs might
    			// not be respected in the file).
    			creator.join();
    		}catch(InterruptedException e){
    			GamerLogger.logError("StateMachine", "[Propnet] Propnet creation interrupted! Terminating initialization!");
        		GamerLogger.logStackTrace("StateMachine", e);
        		Thread.currentThread().interrupt();
    			throw new StateMachineInitializationException("Propnet creation interrupted by external action.", e);
    		}
    		// Set again the propnet to null just to be sure.
    		// The propnet set to null after initializing the state machine means that the propnet
    		// couldn't be created.
    		this.propNet = null;
    		// Throw an exception to signal that something went wrong with propnet creation, more precisely
    		// the propnet was not constructed in time.
    		throw new StateMachineInitializationException("Propnet creation interrupted, taking too long! " + this.maxPropnetCreationTime + "ms are not enough to build the propnet for the game.");
    	}else{
    		// If the creator is not alive, it might be because...
    		this.propNet = creator.getPropNet();

    		// ...it finished creating the propnet, and thus we can use it to finish initializing
    		// the state machine...
    		if(this.propNet != null){
	    		this.roles = ImmutableList.copyOf(this.propNet.getRoles());
		        // If it exists, set init proposition to true without propagating, so that when making
		        // the propnet consistent its value will be propagated so that the next state
		        // will correspond to the initial state.
	    		// REMARK: if there is not TRUE proposition in the initial state, the INIT proposition
	    		// will not exist.
	    		ForwardInterruptingProposition init = this.propNet.getInitProposition();
	    		if(init != null){
	    			this.propNet.getInitProposition().setValue(true);
	    		}

		        // No need to set all other inputs to false because they already are.
		        // Impose consistency on the propnet.
		        this.propNet.imposeConsistency();
		        // The initial state can be computed by only setting the truth value of the INIT
		        // proposition to TRUE, and then computing the resulting next state.
		        // Given that the INIT proposition has already been set to TRUE (without propagation)
		        // before imposing consistency and all other input propositions are set to FALSE at
		        // this moment, it is possible to use the computeNextState() method to compute the
		        // initial state.
		        // If there is no init proposition we can just set the initial state to the state with
		        // empty content.
		        if(init != null){
		        	this.initialState = this.computeInitialState();
		        }else{
		        	this.initialState = new ExplicitMachineState(new HashSet<GdlSentence>());
		        }
    		}else{
    			//...or it encountered an OutOfMemory error or some other error or Exception,
    			// and thus we have no propnet for this state machine.
    			throw new StateMachineInitializationException("Propnet creation interrupted, an error or exception occurred during creation!");
    		}

    	}
    	this.constructionTime = creator.getConstructionTime();
    }

    /**
     * This method returns the next machine state. This state contains the set of all the base propositions
     * that will become true in the next state, given the current state of the propnet, with both
     * base and input proposition marked and their value propagated.
     *
     * !REMARK: this method computes the next state but doesn't advance the propnet state. The propnet
     * will still be in the current state.
	 *
     * @return the next state.
     */
    private ExplicitMachineState computeInitialState(){

    	//AGGIUNTA
    	//System.out.println("COMPUTING INIT STATE");
    	//FINE AGGIUNTA

    	Set<GdlSentence> contents = new HashSet<GdlSentence>();

    	// Add to the initial machine state all the base propositions that are connected to a true transition
    	// whose value also depends on the value of the INIT proposition.
		for (ForwardInterruptingProposition p : this.propNet.getBasePropositions().values()){
			// Get the transition (We can be sure that when getting the single input of a base proposition we get a
			// transition, right?)
			ForwardInterruptingTransition transition = ((ForwardInterruptingTransition) p.getSingleInput());
			if (transition.getValue() && transition.isDependingOnInit()){
				contents.add(p.getName());
			}
		}

		return new ExplicitMachineState(contents);
    }

    /**
     * This method returns the next machine state. This state contains the set of all the base propositions
     * that will become true in the next state, given the current state of the propnet, with both
     * base and input proposition marked and their value propagated.
     *
     * !REMARK: this method computes the next state but doesn't advance the propnet state. The propnet
     * will still be in the current state.
	 *
     * @return the next state.
     */
    private ExplicitMachineState computeNextState(){

    	//AGGIUNTA
    	//System.out.println("COMPUTING NEXT STATE");
    	//FINE AGGIUNTA

    	Set<GdlSentence> contents = new HashSet<GdlSentence>();

    	// For all the base propositions that are true, add the corresponding proposition to the
    	// next machine state.
		for (ForwardInterruptingProposition p : this.propNet.getBasePropositions().values()){
			if (p.getSingleInput().getValue()){
				contents.add(p.getName());
			}
		}

		return new ExplicitMachineState(contents);
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(ExplicitMachineState state) {
		this.markBases(state);
		return this.propNet.getTerminalProposition().getValue();
	}

	@Override
	public List<Double> getAllGoalsForOneRole(ExplicitMachineState state, ExplicitRole role) {
		// Mark base propositions according to state.
		this.markBases(state);

		// Get all goal propositions for the given role.
		Set<ForwardInterruptingProposition> goalPropsForRole = this.propNet.getGoalPropositions().get(role);

		List<Double> trueGoals = new ArrayList<Double>();

		// Check all the goal propositions that are true for the role. If there is more than one throw an exception.
		for(ForwardInterruptingProposition goalProp : goalPropsForRole){
			if(goalProp.getValue()){
				trueGoals.add(this.getGoalValue(goalProp));
			}
		}

		/*
		if(trueGoals.size() > 1){
			GamerLogger.logError("StateMachine", "[Propnet] Got more than one true goal in state " + state + " for role " + role + ".");
		}

		// If there is no true goal proposition for the role in this state throw an exception.
		if(trueGoals.isEmpty()){
			GamerLogger.logError("StateMachine", "[Propnet] Got no true goal in state " + state + " for role " + role + ".");
		}
		*/

		// Return the single goal for the given role in the given state.
		return trueGoals;
	}

	/**
	 * Returns the initial state. If the initial state has not been computed yet because
	 * this state machine has not been initialized, NULL will be returned.
	 */
	@Override
	public ExplicitMachineState getExplicitInitialState() {
		return this.initialState;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<ExplicitMove> getExplicitLegalMoves(ExplicitMachineState state, ExplicitRole role)
	throws MoveDefinitionException {
		// Mark base propositions according to state.
		this.markBases(state);

		// Retrieve all legal propositions for the given role.
		Set<ForwardInterruptingProposition> legalPropsForRole = this.propNet.getLegalPropositions().get(role);

		// Create the list of legal moves.
		List<ExplicitMove> legalMovesForRole = new ArrayList<ExplicitMove>();
		for(ForwardInterruptingProposition legalProp : legalPropsForRole){
			if(legalProp.getValue()){
				legalMovesForRole.add(getMoveFromProposition(legalProp));
			}
		}

		// If there are no legal moves for the role in this state throw an exception.
		if(legalMovesForRole.size() == 0){
			throw new MoveDefinitionException(state, role);
		}

		return legalMovesForRole;
	}

	/**
	 * Computes the next state given a state and the list of moves.
	 */
	@Override
	public ExplicitMachineState getExplicitNextState(ExplicitMachineState state, List<ExplicitMove> moves)
	throws TransitionDefinitionException {
		// Mark base propositions according to state.
		this.markBases(state);

		// Mark input propositions according to the moves.
		this.markInputs(moves);

		// Compute next state for each base proposition from the corresponding transition.
		return this.computeNextState();
	}

	/* Already implemented for you */
	@Override
	public List<ExplicitRole> getExplicitRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves the moves to be translated into 'does' propositions.
	 * @return a list with the 'does' propositions corresponding to the given joint move.
	 */
	private List<GdlSentence> toDoes(List<ExplicitMove> moves){

		//AGGIUNTA
    	//System.out.println("TO DOES");
    	//System.out.println("MOVES");
    	//System.out.println(moves);
    	//FINE AGGIUNTA

		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<ExplicitRole, Integer> roleIndices = getRoleIndices();

		//AGGIUNTA
		//System.out.println("ROLES");
    	//System.out.println(this.roles);

    	//System.out.println("ROLE INDICES");
    	//System.out.println(roleIndices);
    	//FINE AGGIUNTA

		// TODO: both the roles and the moves should be already in the correct order so no need
		// to retrieve the roles indices!!
		for (int i = 0; i < roles.size(); i++)
		{

			//AGGIUNTA
	    	//System.out.println("i=" + i);
	    	//FINE AGGIUNTA

			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}

		//AGGIUNTA
		//System.out.println("MOVES");
		//System.out.println(moves);
		//System.out.println("DOESES");
		//System.out.println(doeses);
		//FINE AGGIUNTA

		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move.
	 *
	 * @param p the proposition to be transformed into a move.
	 * @return the legal move corresponding to the given proposition.
	 */
	public static ExplicitMove getMoveFromProposition(ForwardInterruptingProposition p){
		return new ExplicitMove(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private double getGoalValue(ForwardInterruptingProposition goalProposition){
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Double.parseDouble(constant.toString());
	}

    /**
     * Marks the base propositions with the correct value so that the propnet state resembles the
     * given machine state. This method works only if the GdlPool is being used.
     *
     * @param state the machine state to be set in the propnet.
     */
	private void markBases(ExplicitMachineState state){

		//AGGIUNTA
    	//System.out.println("MARKING BASES");
    	//FINE AGGIUNTA

		Set<GdlSentence> contents = state.getContents();
		for(ForwardInterruptingProposition base : this.propNet.getBasePropositions().values()){
			base.setAndPropagateValue(contents.contains(base.getName()));
		}
	}

	/**
     * Marks the input propositions with the correct values so that the proposition corresponding to
     * a performed move are set to TRUE and all others to FALSE. This method works only if the
     * GdlPool is being used.
     *
     * !REMARK: also the INIT proposition can be considered as a special case of INPUT proposition,
     * thus when marking the input propositions to compute a subsequent state different from the
     * initial one, make sure that the INIT proposition is also set to FALSE.
     *
     * @param moves the moves to be set as performed in the propnet.
     */
	private void markInputs(List<ExplicitMove> moves){

		//AGGIUNTA
    	//System.out.println("MARKING INPUTS");
    	//FINE AGGIUNTA

		// Transform the moves into 'does' propositions (the correct order should be kept).
		List<GdlSentence> movesToDoes = this.toDoes(moves);

		for(ForwardInterruptingProposition input : this.propNet.getInputPropositions().values()){
			input.setAndPropagateValue(movesToDoes.contains(input.getName()));
		}

		// Set to false also the INIT proposition, if it exists.
		// REMARK: since at the moment the initial state is computed only once at the beginning,
		// the setting of the INIT proposition to FALSE could be done only once after computing the
		// initial state.

		ForwardInterruptingProposition init = this.propNet.getInitProposition();
		if(init != null){
			this.propNet.getInitProposition().setAndPropagateValue(false);
		}
	}

	/**
	 * Set method for the 'maxPropnetCreationTime' parameter.
	 *
	 * @param maxPropnetCreationTime The maximum time (in milliseconds) that this state machine can
	 * spend to create the propnet.
	 */
	public void setMaxPropnetCreationTime(long maxPropnetCreationTime){
		this.maxPropnetCreationTime = maxPropnetCreationTime;
	}

	/**
	 * Get method for the 'maxPropnetCreationTime' parameter.
	 *
	 * @return The maximum time (in milliseconds) that this state machine can spend to create the propnet.
	 */
	public long getMaxPropnetCreationTime(){
		return this.maxPropnetCreationTime;
	}

	/**
	 * Get method for the 'propNet' parameter.
	 *
	 * @return The proposition network.
	 */
	public ForwardInterruptingPropNet getPropNet(){
		return this.propNet;
	}

	/**
	 * Get method for the propnet construction time.
	 *
	 * @return the actual construction time of the propnet, -1 if it has not been created in time.
	 */
	public long getPropnetConstructionTime(){
		return this.constructionTime;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		// No need to do anything

	}
}