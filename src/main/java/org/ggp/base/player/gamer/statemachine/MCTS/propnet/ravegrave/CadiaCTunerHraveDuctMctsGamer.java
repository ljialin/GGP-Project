package org.ggp.base.player.gamer.statemachine.MCTS.propnet.ravegrave;


public class CadiaCTunerHraveDuctMctsGamer extends CadiaCTunerRaveDuctMctsGamer {

	public CadiaCTunerHraveDuctMctsGamer() {
		super();

		this.cadiaBetaComputer = true;
		this.k = 50;

		this.minAMAFVisits = Integer.MAX_VALUE;
	}

}
