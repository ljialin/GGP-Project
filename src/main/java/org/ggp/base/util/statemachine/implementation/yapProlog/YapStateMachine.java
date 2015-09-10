/**
 *
 */
package org.ggp.base.util.statemachine.implementation.yapProlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.yapProlog.transform.YapEngineSupport;
import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;

import com.declarativa.interprolog.YAPSubprocessEngine;
import com.google.common.collect.ImmutableList;

/**
 * This class implements a state machine based on YAP prolog. For this state
 * machine to be able to work correctly, YAP prolog needs to be installed and the
 * Interprolog.jar library needs to be imported (a version that includes the
 * YapSubprocessEngine class, removed from most recent versions).
 *
 * This state machine represents game states in YAP prolog and uses YAP prolog to
 * reason on the game rules, translated from the GDL description into prolog syntax.
 *
 * ATTENTION: if the initialization method fails do not query this state machine
 * as its answers will not be consistent. (TODO: add a way to check if state
 * machine is an inconsistent state and thus cannot answer queries).
 *
 * ATTENTION: if the initialization method succeeds YAP prolog will be running, so
 * when you stop using this state machine, remember to shut it down. In case the
 * state machine is being used by a StateMachineGamer, it will already take care
 * of calling the shutdown method whenever the match it is playing is aborted or
 * stopped.
 *
 * On the contrary, if any other method fails throwing the StateMachineException,
 * it is possible to keep asking queries to the state machine since each method
 * that fails also makes sure to leave the state machine in a consistent state.
 *
 * ! A LOT OF ATTENTION WHEN MODIFYING THE CODE OF THIS CLASS: all the methods
 * that can be interrupted because they call deterministicGoal on Yap Prolog
 * must not modify any instance variable of this class because if a method is
 * interrupted then the caller will probably call the restart() method, but if
 * the method takes a while before stopping it might still be able to modify
 * some instance variable that has already been reset by the restart() method!
 * If you really wanna do that, then access to variables must be synchronized
 * (i.e. a method that modifies a variable locks it till it's done), but this
 * will probably impact performance!
 *
 * @author C.Sironi
 *
 */
public class YapStateMachine extends StateMachine {

	/**
	 * Initial state of the current game.
	 */
	private MachineState initialState;

	/**
	 * Ordered list of roles in the current game.
	 */
	private ImmutableList<Role> roles;


	/////////////////////////////////////////////////////////////////////////////////


	// The YapEngineSupport which handles the translations and the mapping
	private YapEngineSupport support;



	// Second file with all the predefined Prolog functions
	// => using the Internal DataBase

	/*
	 * NEEDED?????
	 */
	/*
	private String functionsFileIdbPath;
	private File functionsIdbFile;
	*/

	// The state in the Prolog side
	// -> to avoid running "computeState(MachineState)" when it's useless
	/**
	 * This variable keeps track of the game state that currently is registered on
	 * YAP Prolog side.
	 * If null, it means that the game state on YAP Prolog side is unknown.
	 *
	 * This variable can be used to avoid running "computeState(MachineState)" on YAP
	 * Prolog side when the game state registered on it is already the one we need.
	 */
	private MachineState currentYapState;

	// The list of the roles as strings
	private List<String> fakeRoles;


	////////////////////////////////////////////////////////////////////////////////


	/**
	 * Constructor that sets the command that this state machine must use to run YAP,
	 * the paths of the files that YAP prolog must use and the maximum time to wait
	 * for YAP to answer to a query to the default values.
	 */
	public YapStateMachine() {
		this("/home/csironi/CadiaplayerInstallation/Yap/bin/yap", "/home/csironi/YAPplayer/prologFiles/description.pl", "/home/csironi/YAPplayer/prologFiles/prologFunctions.pl", 50L);
	}

	/**
	 * Constructor that sets the command that this state machine must use to run YAP
	 * and the paths of the files that YAP must use and the maximum time to wait
	 * for YAP to answer to a query to the given values.
	 */
	public YapStateMachine(String yapCommand, String descriptionFilePath, String functionsFilePath, long waitingTime) {
		this.yapCommand = yapCommand;
		this.descriptionFilePath = descriptionFilePath;
		this.functionsFilePath = functionsFilePath;
		this.waitingTime = waitingTime;
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#initialize(java.util.List)
	 */
	@Override
	public void initialize(List<Gdl> description) throws StateMachineException{
		try{
			// Create the bridge between Java and YAP Prolog, trying to start the YAP Prolog program.
			this.yapProver = new YAPSubprocessEngine(this.yapCommand);

			this.executor = Executors.newSingleThreadExecutor();

			////NEEDED???????
			//this.functionsFile = new File(this.functionsFilePath);
			//this.functionsIdbFile = new File(this.functionsFileIdbPath);

			this.support = new YapEngineSupport();

			flushAndWrite(support.toProlog(description));


			/*
			 * NEEDED?
			 */
			/*
			if(IDB) engine.consultAbsolute(fileFunctionsIdb);
			else */ this.yapProver.consultAbsolute(new File(functionsFilePath));

			randomizeProlog();

			// If creation succeeded, compute initial state and roles.
			this.initialState = computeInitialState();
			this.roles = computeRoles();

		}catch(RuntimeException re){
			throw re;
		}catch(Exception e){
			// Log the exception
			GamerLogger.logError("StateMachine", "[YAP] Exception during state machine initialization. Shutting down.");
			GamerLogger.logStackTrace("StateMachine", e);

			// Reset all the variables of the state machine to null to leave the state machine in a consistent
			// state, since initialization failed.
			this.roles = null;
			this.fakeRoles = null;
			this.currentYapState = null;
			this.initialState = null;
			// Shutdown Yap Prolog and remove the reference to it, as it is now unusable.
			this.yapProver.shutdown();
			this.yapProver = null;
			// Shutdown the executor
			this.executor.shutdownNow();
			this.executor = null;

			// Throw an exception.
			throw new StateMachineException("State machine initialization failure.", e);
		}
	}

	private MachineState computeInitialState() throws StateMachineException
	{
		Object[] bindings = null;

		// If a positive waiting time has been set, give a timeout to the query.
		if(this.waitingTime > 0){

			// Create the list of tasks to be executed by the executor (just one task).
			Callable<Object[]> task = new Callable<Object[]>(){
										@Override
										public Object[] call(){
											return yapProver.deterministicGoal("initialize_state(List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
										}
									};
			try {
				// Try to query Yap Prolog and wait for an answer till timeout has been reached.
				bindings = this.executor.invokeAny(Arrays.asList(task),this.waitingTime, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException
					| TimeoutException e) {
				// If something went erong or timeout has been reached, then throw an exception.
				GamerLogger.logError("StateMachine", "[YAP] Computation of initial state on Yap Prolog side failed.");
				GamerLogger.logStackTrace("StateMachine", e);
				throw new StateMachineException("Computation of initial state on Yap Prolog side failed.", e);
			}
		// If no positive waiting time has been set just wait indefinitely.
		}else{
			bindings = yapProver.deterministicGoal("initialize_state(List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
		}

		// If bindings is null => something went wrong on Yap Prolog side during the computation.
		// Note that this should never happen, but an extra check won't hurt.
		if(bindings == null){
			// State computation failed on Yap prolog side.
			this.currentYapState = null;
			GamerLogger.logError("StateMachine", "[YAP] Computation of initial state on Yap Prolog side failed.");
			throw new StateMachineException("Computation of initial state on Yap Prolog side failed.");
		}

		// Compute the machine state using the Yap Prolog answer (note that it could be an empty array of strings in case
		// no propositions are true in the initial state. In this case the content of the machine state will be an empty HashSet)
		this.currentYapState = new MachineState(support.askToState((String[]) bindings[0]));

		return currentYapState.clone();
	}

	private ImmutableList<Role> computeRoles() throws StateMachineException	{

		Object[] bindings = null;

		// If a positive waiting time has been set, give a timeout to the query.
		if(this.waitingTime > 0){

			// Create the list of tasks to be executed by the executor (just one task).
			Callable<Object[]> task = new Callable<Object[]>(){
										@Override
										public Object[] call(){
											return yapProver.deterministicGoal("get_roles(List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
										}
									};
			try {
				// Try to query Yap Prolog and wait for an answer till timeout has been reached.
				bindings = this.executor.invokeAny(Arrays.asList(task),this.waitingTime, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException
					| TimeoutException e) {
				// If something went wrong or timeout has been reached, then throw an exception.
				GamerLogger.logError("StateMachine", "[YAP] Computation of game roles on Yap Prolog side failed.");
				GamerLogger.logStackTrace("StateMachine", e);
				throw new StateMachineException("Computation of game roles on Yap Prolog side failed.", e);
			}
		// If no positive waiting time has been set just wait indefinitely.
		}else{
			bindings = yapProver.deterministicGoal("get_roles(List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
		}

		if(bindings == null){
			GamerLogger.logError("StateMachine", "[YAP] Got no results for the computation of the game roles, while expecting at least one role.");
			throw new StateMachineException("Got no results for the computation of the game roles, while expecting at least one role.");
		}

		List<Role> tmpRoles = new ArrayList<Role>();

		try{
			tmpRoles = support.askToRoles((String[]) bindings[0]);

			this.fakeRoles = support.getFakeRoles(tmpRoles);

		}catch(SymbolFormatException e){
			GamerLogger.logError("StateMachine", "[YAP] Got exception while parsing the game roles.");
			GamerLogger.logStackTrace("StateMachine", e);
			this.fakeRoles = null;
			throw new StateMachineException("Impossible to parse th game roles.", e);
		}
		return ImmutableList.copyOf(tmpRoles);
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#getGoal(org.ggp.base.util.statemachine.MachineState, org.ggp.base.util.statemachine.Role)
	 */
	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException, StateMachineException {

		updateYapState(state);

		Object[] bindings = null;

		// If a positive waiting time has been set, give a timeout to the query.
		if(this.waitingTime > 0){

			// Create the list of tasks to be executed by the executor (just one task).
			Callable<Object[]> task = new Callable<Object[]>(){
										@Override
										public Object[] call(){
											return yapProver.deterministicGoal("get_goal("+support.getFakeRole(role)+", List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
										}
									};
			try {
				// Try to query Yap Prolog and wait for an answer till timeout has been reached.
				bindings = this.executor.invokeAny(Arrays.asList(task),this.waitingTime, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException
					| TimeoutException e) {
				// If something went wrong or timeout has been reached, then throw an exception.
				GamerLogger.logError("StateMachine", "[YAP] Computation of game roles on Yap Prolog side failed.");
				GamerLogger.logStackTrace("StateMachine", e);
				throw new StateMachineException("Computation of game roles on Yap Prolog side failed.", e);
			}
		// If no positive waiting time has been set just wait indefinitely.
		}else{
			bindings = yapProver.deterministicGoal("get_roles(List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
		}



		int goal;
		Object[] bindings = yapProver.deterministicGoal("get_goal("+support.getFakeRole(role)+", List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");

		if(bindings == null){
			GamerLogger.logError("StateMachine", "[YAP] Got no goal when expecting one.");
			throw new GoalDefinitionException(state, role);
		}

		String[] goals = (String[]) bindings[0];

		if(goals.length != 1){
			GamerLogger.logError("StateMachine", "[YAP] Got goal results of size: " + goals.length + " when expecting size one.");
			throw new GoalDefinitionException(state, role);
		}

		try{
			goal = Integer.parseInt(goals[0]);
		}catch(NumberFormatException ex){
			GamerLogger.logError("StateMachine", "[YAP] Got goal results that is not a number.");
			GamerLogger.logStackTrace("StateMachine", ex);
			throw new GoalDefinitionException(state, role, ex);
		}

		return goal;
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#isTerminal(org.ggp.base.util.statemachine.MachineState)
	 */
	@Override
	public boolean isTerminal(MachineState state) throws StateMachineException {

		updateYapState(state);

		return yapProver.deterministicGoal("is_terminal");
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#getRoles()
	 */
	@Override
	public List<Role> getRoles() {
		return this.roles;
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#getInitialState()
	 */
	@Override
	public MachineState getInitialState() {
		return this.initialState;
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#getLegalMoves(org.ggp.base.util.statemachine.MachineState, org.ggp.base.util.statemachine.Role)
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException, StateMachineException {

		// TODO: should I catch the state machine exception before re-throwing it?
		updateYapState(state);

		// TODO: deterministicGoal will throw a runtime exception if interrupted. Better to catch it here
		// and throw StateMachineException or let it pass and catch it in the method that interrupted this call?
		Object[] bindings = yapProver.deterministicGoal("get_legal_moves("+support.getFakeRole(role)+", List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");

		if(bindings == null){
			GamerLogger.logError("StateMachine", "[YAP] Got no legal moves when expecting at least one.");
			throw new MoveDefinitionException(state, role);
		}

		String[] yapMoves = (String[]) bindings[0];

		// Extra check, but this should never happen.
		if(yapMoves.length < 1){
			GamerLogger.logError("StateMachine", "[YAP] Got no legal moves when expecting at least one.");
			throw new MoveDefinitionException(state, role);
		}

		List<Move> moves = support.askToMoves(yapMoves);

		return moves;
	}

	/* (non-Javadoc)
	 * @see org.ggp.base.util.statemachine.StateMachine#getNextState(org.ggp.base.util.statemachine.MachineState, java.util.List)
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException, StateMachineException {

		updateYapState(state);

		// Get the next state and assert it on Yap Prolog side.
		Object[] bindings = yapProver.deterministicGoal("get_next_state("+fakeRoles+", "+support.getFakeMoves(moves)+", List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");

		if(bindings == null){
			GamerLogger.logError("StateMachine", "[YAP] Computation of next state on Yap Prolog side failed.");
			throw new StateMachineException("Computation of next state on Yap Prolog side failed for moves " + moves + " in state " + state + ".");
		}

		// Compute the machine state using the Yap Prolog answer (note that it could be an empty array of strings in case no
		// propositions are true in the computed next state. In this case the content of the machine state will be an empty HashSet)
		this.currentYapState = new MachineState(support.askToState((String[]) bindings[0]));

		return currentYapState.clone();
	}

	////////////////////////////////////////////////////////////////////////
	/**
	 *	Flush the description file and write the game description in it
	 * @param string: the description of the game
	 * @throws IOException
	 */
	private void flushAndWrite(String string) throws IOException{

		BufferedWriter out = null;
		try{
			out = new BufferedWriter(new FileWriter(this.descriptionFilePath));
			out.write(string);
		}finally{
			if(out != null){
				out.close();
			}
		}
	}


	/**
	 * Change the Prolog random number generator
	 * using the Java random number generator
	 */
	private void randomizeProlog()
	{
		int i = (int)Math.min(Math.random()*(30268), 30268)+1;
		int j = (int)Math.min(Math.random()*(30307), 30307)+1;
		int k = (int)Math.min(Math.random()*(30323), 30323)+1;
		this.yapProver.realCommand("setrand(rand("+i+", "+j+", "+k+"))");
	}


	/**
	 * Compute the given MachineState in the Prolog side
	 * @throws StateMachineException
	 */
	private void updateYapState(MachineState state) throws StateMachineException{

		// TODO: better to perform this check on yap side?
		// Don't modify any class variable in a method that can be interrupted cause if it keeps
		// running for a while before realizing that it has been interrupted it might modify again
		// the currentYapState of this state machine after it has been reset to null by someone
		// calling the restart method.
		if(currentYapState==null||!currentYapState.equals(state)){

			boolean success = yapProver.deterministicGoal("update_state("+support.getFakeMachineState(state.getContents())+")");

			// This should never happen, but you never know...
			if(!success){
				// State computation failed on Yap prolog side.
				this.currentYapState = null;
				GamerLogger.logError("StateMachine", "[YAP] Computation of current state on YAP Prolog side failed!");
				throw new StateMachineException("Computation on YAP Prolog side failed for state: " + state);
			}

			this.currentYapState = state;
		}
	}



	/**
	 * This method shuts down the Yap Prover program and the executor.
	 * To be called when this state machine won't be used anymore to make sure that
	 * all running tasks and also the external Yap Prolog program will be stopped.
	 */
	@Override
	public void shutdown(){
		this.yapProver.shutdown();
		this.executor.shutdownNow();
	}

	/**
	 * Restarts Yap Prolog and the executor.
	 * To be used after interrupting a query that is taking too long to be answerd by
	 * Yap Prolog.
	 */
	public void restart(){

		// Shutdown Yap Prolog and the executor to stop all running tasks (if any).
		this.shutdown();

		// Try to restart the YAP Prolog program.
		this.yapProver = new YAPSubprocessEngine(this.yapCommand);

		// Create a new ExecutorService
		this.executor = Executors.newSingleThreadExecutor();

		// Consult the functions file (and consequently the game description file).
		this.yapProver.consultAbsolute(new File(functionsFilePath));

		randomizeProlog();

		// Reset to null the current game state on Yap Prolog side, since after restarting
		// Yap Prolog no state has been asserted yet.
		this.currentYapState = null;

	}

	private Object[] getBindingsFromQuery(String goal, String answer){

		Object[] bindings = null;

		// If a positive waiting time has been set, give a timeout to the query.
		if(this.waitingTime > 0){

			// Create the list of tasks to be executed by the executor (just one task).
			Callable<Object[]> task = new CallableQuery(goal,answer);
			try {
				// Try to query Yap Prolog and wait for an answer till timeout has been reached.
				bindings = this.executor.invokeAny(Arrays.asList(task),this.waitingTime, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException
					| TimeoutException e) {
				// If something went erong or timeout has been reached, then throw an exception.
				GamerLogger.logError("StateMachine", "[YAP] Computation of initial state on Yap Prolog side failed.");
				GamerLogger.logStackTrace("StateMachine", e);
				throw new StateMachineException("Computation of initial state on Yap Prolog side failed.", e);
			}
		// If no positive waiting time has been set just wait indefinitely.
		}else{
			bindings = yapProver.deterministicGoal("initialize_state(List), processList(List, LL), ipObjectTemplate('ArrayOfString',AS,_,[LL],_)", "[AS]");
		}

	}

	public class CallableQuery implements Callable<Object[]>{

		private String goal;
		private String answer;

		public CallableQuery(String goal, String answer){
			this.goal = goal;
			this.answer = answer;
		}

		@Override
		public Object[] call() throws Exception {
			return yapProver.deterministicGoal(goal,answer);
		}

	}

}
