package org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.strategies.backpropagation;

import org.ggp.base.player.gamer.statemachine.MCTS.manager.hybrid.strategies.selection.evaluators.td.GlobalExtremeValues;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.MCTSNode;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.MCTSJointMove;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.SequDecMCTSJointMove;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.SimulationResult;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.decoupled.DecoupledMCTSMoveStats;
import org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.hybrid.tddecoupled.TDDecoupledMCTSNode;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.abstractsm.AbstractStateMachine;
import org.ggp.base.util.statemachine.structure.MachineState;

public abstract class TDBackpropagation implements BackpropagationStrategy {

	protected AbstractStateMachine theMachine;

	protected int numRoles;

	protected GlobalExtremeValues globalExtremeValues;

	/**
	 * Strategy parameters.
	 */

	protected double qPlayout;

	protected double lambda;

	protected double gamma;

	/**
	 * Simulation parameters that must be reset after each simulation is concluded
	 * and before starting the backpropagation for the next simulation.
	 */

	protected double[] deltaSum;

	protected double[] qNext;


	public TDBackpropagation(AbstractStateMachine theMachine, int numRoles, GlobalExtremeValues globalExtremeValues, double qPlayout, double lambda, double gamma) {

		this.theMachine = theMachine;
		this.numRoles = numRoles;
		this.globalExtremeValues = globalExtremeValues;

		this.qPlayout = qPlayout;
		this.lambda = lambda;
		this.gamma = gamma;

		this.deltaSum = new double[numRoles];
		this.qNext = new double[numRoles];

		this.resetSimulationParameters();

	}

	@Override
	public void update(MCTSNode currentNode, MachineState currentState,
			MCTSJointMove jointMove, SimulationResult simulationResult) {

		if(currentNode instanceof TDDecoupledMCTSNode && jointMove instanceof SequDecMCTSJointMove){
			this.decUpdate((TDDecoupledMCTSNode)currentNode, currentState, (SequDecMCTSJointMove)jointMove, simulationResult);
		}else{
			throw new RuntimeException("TDBackpropagation-update(): no method implemented to manage backpropagation for node type (" + currentNode.getClass().getSimpleName() + ") and joint move type (" + jointMove.getClass().getSimpleName() + ").");
		}

	}

	protected void decUpdate(TDDecoupledMCTSNode currentNode, MachineState currentState, SequDecMCTSJointMove jointMove, SimulationResult simulationResult){

		currentNode.incrementTotVisits();

		DecoupledMCTSMoveStats[][] moves = currentNode.getMoves();
		int[] movesIndices = jointMove.getMovesIndices();

		DecoupledMCTSMoveStats currentMoveStat;

		int[] returnValuesForRoles = this.getReturnValuesForRolesInPlayout(simulationResult); // Here the index is useless so we set it to 0

		double qCurrent;
		double delta;
		double alpha;

		double newScore;

		for(int i = 0; i < this.numRoles; i++){

			currentMoveStat = moves[i][movesIndices[i]];

			if(currentMoveStat.getVisits() == 0){
				qCurrent = 0.0;
			}else{
				qCurrent = currentMoveStat.getScoreSum()/((double)currentMoveStat.getVisits());
			}

			delta = ((double)returnValuesForRoles[i]) + this.gamma * this.qNext[i] - qCurrent;
			this.deltaSum[i] = this.lambda * this.gamma * this.deltaSum[i] + delta;

			currentMoveStat.incrementVisits();
			alpha = 1.0/((double)currentMoveStat.getVisits());

			newScore = (qCurrent + alpha * this.deltaSum[i]);

			/*
			if(newScore < 0.0){
				GamerLogger.logError("MCTSManager", "Computed negative score when backpropagating: " + newScore);

				//newScore = 0.0;
			}
			*/

			currentMoveStat.setScoreSum(newScore*((double)currentMoveStat.getVisits())); // Note that the statistics memorize the total sum of move values, thus we must multiply the new expected value by the number of visits of the move.

			if(newScore > currentNode.getMaxStateActionValueForRole(i)){
				currentNode.setMaxStateActionValueForRole(newScore, i);

				// Note: this check is here because if the new score is lower than the maximum value of
				// the current node then it's also lower than the maximum overall value and no update
				// would be needed.
				if(newScore > this.globalExtremeValues.getGlobalMaxValueForRole(i)){
					this.globalExtremeValues.setGlobalMaxValueForRole(newScore, i);
				}
			}

			if(newScore < currentNode.getMinStateActionValueForRole(i)){
				currentNode.setMinStateActionValueForRole(newScore, i);

				// Note: this check is here because if the new score is higher than the mminimum value of
				// the current node then it's also higher than the minimum overall value and no update
				// would be needed.
				if(newScore < this.globalExtremeValues.getGlobalMinValueForRole(i)){
					this.globalExtremeValues.setGlobalMinValueForRole(newScore, i);
				}
			}

			this.qNext[i] = qCurrent;
		}

	}

	@Override
	public void processPlayoutResult(MCTSNode leafNode,	MachineState leafState,
			SimulationResult simulationResult) {

		int playoutLength = simulationResult.getPlayoutLength();

		if(playoutLength <= 0){ // This method should be called only if the playout was actually performed, thus the length must be at least 1!
			GamerLogger.logError("MCTSManager", "Playout length equals 0 when processing the playout result for TD backpropagation. Probably a wrong combination of strategies has been set or there is something wrong in the code!");
			throw new RuntimeException("Playout length equals 0.");
		}

		int[] returnValuesForRoles;
		double delta;
		//double qCurrent = this.qPlayout; Redundant! Basically qCurrent = qPlayout for the whole backpropagation in the playout part of the simulation

		// Update deltaSum for each non-terminal episode in the playout.
		for(int i = 0; i < playoutLength-1; i++){

			returnValuesForRoles = this.getReturnValuesForRolesInPlayout(simulationResult);

			for(int j = 0; j < this.numRoles; j++){

				delta = ((double)returnValuesForRoles[j]) + this.gamma * this.qNext[j] - this.qPlayout;

				this.deltaSum[j] = this.lambda * this.gamma * this.deltaSum[j] + delta;

				this.qNext[j] = this.qPlayout; // Would be enough to do this only for the 1st iteration of the cycle

			}
		}
	}

	@Override
	public String getStrategyParameters() {
		return "Q_PLAYOUT = " + this.qPlayout + ", LAMBDA = " + this.lambda + ", GAMMA = " + this.gamma + ", DEFAUL_GLOBAL_MIN_VALUE = " + this.globalExtremeValues.getDefaultGlobalMinValue() + ", DEFAUL_GLOBAL_MAX_VALUE = " + this.globalExtremeValues.getDefaultGlobalMaxValue();
	}

	@Override
	public String printStrategy() {
		String params = this.getStrategyParameters();

		if(params != null){
			return "[BACKPROPAGATION_STRATEGY = " + this.getClass().getSimpleName() + ", " + params + "]";
		}else{
			return "[BACKPROPAGATION_STRATEGY = " + this.getClass().getSimpleName() + "]";
		}
	}

	public void resetSimulationParameters(){

		for(int i = 0; i < this.deltaSum.length; i++){
			this.deltaSum[i] = 0.0;
			this.qNext[i] = 0.0;
		}
	}

	public abstract int[] getReturnValuesForRolesInPlayout(SimulationResult simulationResult);

}