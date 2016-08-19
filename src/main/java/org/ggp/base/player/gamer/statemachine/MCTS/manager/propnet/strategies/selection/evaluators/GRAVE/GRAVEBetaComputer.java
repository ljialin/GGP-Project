package org.ggp.base.player.gamer.statemachine.MCTS.manager.propnet.strategies.selection.evaluators.GRAVE;

import org.ggp.base.player.gamer.statemachine.MCS.manager.MoveStats;

public class GRAVEBetaComputer implements BetaComputer {

	private double bias;

	public GRAVEBetaComputer(double bias) {
		this.bias = bias;
	}

	@Override
	public double computeBeta(MoveStats theMoveStats,
			MoveStats theAmafMoveStats, int nodeVisits) {

		if(theAmafMoveStats == null){
			return -1.0;
		}

		double amafVisits = theAmafMoveStats.getVisits();
		double moveVisits = theMoveStats.getVisits();

		return (amafVisits / (amafVisits + moveVisits + (this.bias * amafVisits * moveVisits)));
	}

	@Override
	public String getBetaComputerParameters() {
		return "BIAS = " + this.bias;
	}

	@Override
	public String printBetaComputer() {
		String params = this.getBetaComputerParameters();

		if(params != null){
			return "(BETA_COMPUTER_TYPE = " + this.getClass().getSimpleName() + ", " + params + ")";
		}else{
			return "(BETA_COMPUTER_TYPE = " + this.getClass().getSimpleName() + ")";
		}
	}

}