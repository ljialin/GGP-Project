package org.ggp.base.player.gamer.statemachine.MCTS.manager.treestructure.SUCT;

import org.ggp.base.player.gamer.statemachine.MCS.manager.MoveStats;

public class SUCTMCTSMoveStats extends MoveStats {

	private double uct;

	/**
	 * Reference to the list of SUCTMovesStats for the next role.
	 * This list contains the statistics for all the next role's
	 * moves given that the current role played this move.
	 */
	private SUCTMCTSMoveStats[] nextRoleMovesStats;

	/**
	 *  Keeps track of the number of leaves in the moves statistics tree that are descendants
	 *  of this move statistics node and haven't been visited at least once yet.
	 */
	private int unvisitedSubleaves;

	public SUCTMCTSMoveStats(SUCTMCTSMoveStats[] nextRoleMovesStats) {
		super();
		this.uct = 0.0;
		this.nextRoleMovesStats = nextRoleMovesStats;
		if(this.nextRoleMovesStats == null){
			this.unvisitedSubleaves = 1;
		}else{
			this.unvisitedSubleaves = this.nextRoleMovesStats[0].getUnvisitedSubleaves() * this.nextRoleMovesStats.length;
			// This works because each of the next moves has the same amount of leaves in its descendants
			// and they are all not visited yet.
		}
	}

	/**
	 * Getter method.
	 *
	 * @return the current UCT value of the move.
	 */
	public double getUct() {
		return this.uct;
	}

	/**
	 * Setter method.
	 *
	 * @param uct the UCT value to be set
	 */
	public void setUct(double uct) {
		this.uct = uct;
	}

	/**
	 * Getter method.
	 *
	 * @return move statistics for the next role given this move.
	 */
	public SUCTMCTSMoveStats[] getNextRoleMovesStats(){
		return this.nextRoleMovesStats;
	}

	public int getUnvisitedSubleaves(){
		return this.unvisitedSubleaves;
	}

	public void decreaseUnvisitedSubLeaves(){
		if(this.unvisitedSubleaves > 0)
			this.unvisitedSubleaves--;
	}

	@Override
	public String toString(){
		return super.toString() + ", UCT(" + this.uct + ")";
	}

}
