package org.ggp.base.util.propnet.state;

import org.apache.lucene.util.OpenBitSet;

public class ExternalPropnetState {

	/** Currently set values of the BASE propositions. */
	private OpenBitSet currentState;

	/** Currently set values of the TRANSITIONS. */
	private OpenBitSet nextState;

	/** Currently set values for the INPUTS.
	 * One input for each role will be set to true.
	 */
	private OpenBitSet currentJointMove;

	/**
	 *  List containing for each role the index that its first goal proposition
	 *  has in the otherComponnets array. This array contains one element more
	 *  that gives the first index of a non-goal proposition in the otherComponents
	 *  arrray.
	 */
	private int[] firstGoalIndices;

	/**
	 *  List containing for each role the index that its first legal proposition
	 *  has in the otherComponnets array. This array contains one element more
	 *  that gives the first index of a non-legal proposition in the otherComponents
	 *  arrray.
	 */
	private int[] firstLegalIndices;

	/** Currently set values for the GOALS, grouped by role. */
	private OpenBitSet[] goals;

	/** Currently set values for the LEGAL propositions, grouped by role */
	private OpenBitSet[] legals;

	/** Currently set values of the AND and OR gates.
	 *
	 * Each integer in the array corresponds to a gate in the propnet.
	 * It keeps track of the number of true inputs of the gate so that the sign bit
	 * of the integer also represents the truth value of the gate.
	 * It's initial value is set so that the integer will overflow when the truth value
	 * of the gate becomes true, so that it will correspond to the sign bit of the integer
	 * being set to 1.
	 */
	private int[] andOrGatesValues;

	/** Currently set values of all the components not yet included in the previous parameters.
	 *
	 * Note that the first bit corresponds to the terminal state.
	 * After that there will be the values of the legal propositions, ordered by role, then the
	 * ones of the goal propositions, ordered by role, then the ones of all other propositions
	 * and in the end the ones of the NOT components.
	 */
	private OpenBitSet otherComponents;


	public OpenBitSet getCurrentState(){
		return this.currentState;
	}

	public OpenBitSet getNextState(){
		return this.nextState;
	}

	public OpenBitSet getCurrentJointMove(){
		return this.currentJointMove;
	}

	public int[] getFirstGoalIndices(){
		return this.firstGoalIndices;
	}

	public int[] getFirstLegalIndices(){
		return this.firstLegalIndices;
	}

	/*
	public OpenBitSet[] getGoals(){
		return this.goals;
	}

	public OpenBitSet getGoals(int role){
		return this.goals[role];
	}

	public OpenBitSet[] getLegals(){
		return this.legals;
	}

	public OpenBitSet getLegals(int role){
		return this.legals[role];
	}

	*/

	public boolean isTerminal(){
		return this.otherComponents.fastGet(0);
	}

	public OpenBitSet getOtherComponents(){
		return this.otherComponents;
	}



}
