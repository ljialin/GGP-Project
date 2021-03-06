package org.ggp.base.validator;

import java.util.List;
import java.util.Random;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineException;
import org.ggp.base.util.statemachine.exceptions.StateMachineInitializationException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMachineState;

import com.google.common.collect.ImmutableList;

public final class SimulationValidator implements GameValidator
{
	private final int maxDepth;
	private final int numSimulations;

	public SimulationValidator(int maxDepth, int numSimulations)
	{
		this.maxDepth = maxDepth;
		this.numSimulations = numSimulations;
	}

	@Override
	public List<ValidatorWarning> checkValidity(Game theGame) throws ValidatorException {
		for (int i = 0; i < numSimulations; i++) {
			StateMachine stateMachine = new ProverStateMachine(new Random());
			try {
				stateMachine.initialize(theGame.getRules(), Long.MAX_VALUE);
			} catch (StateMachineInitializationException sme) {
				throw new ValidatorException("Ran into a state machine initialization exception: " + sme);
			}

			ExplicitMachineState state = stateMachine.getExplicitInitialState();
			try {
				for (int depth = 0; !stateMachine.isTerminal(state); depth++) {
					if (depth == maxDepth) {
						throw new ValidatorException("Hit max depth while simulating: " + maxDepth);
					}
					try {
						state = stateMachine.getRandomNextState(state);
					} catch (MoveDefinitionException mde) {
						throw new ValidatorException("Could not find legal moves while simulating: " + mde);
					} catch (TransitionDefinitionException tde) {
						throw new ValidatorException("Could not find transition definition while simulating: " + tde);
					}
				}
			} catch (StateMachineException sme) {
				throw new ValidatorException("Ran into a state machine exception while simulating: " + sme);
			}

			try {
				stateMachine.getGoals(state);
			} catch (GoalDefinitionException gde) {
				throw new ValidatorException("Could not find goals while simulating: " + gde);
			} catch (StateMachineException sme) {
				throw new ValidatorException("Ran into a state machine exception while simulating: " + sme);
			}
		}
		return ImmutableList.of();
	}
}
