package org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.structure.parameters;

import java.util.Arrays;

public class TunableParameter {

	private String name;

	/**
	 * Value of the parameter for the roles not being tuned.
	 * It's also the values used to initialize each value at the beginning of each new game.
	 */
	protected double fixedValue;

	/**
	 * Indicates in which order the parameters should be tuned, if an order is required by the tuning method.
	 * If the tuning method doesn't require a specific order this value might be ignored by it. If the tuning
	 * method requires a specific order but this value is not specified then no guarantee is given on the order
	 * that will be used by the tuner (nor on the correctness of its performance). Also, this value must be
	 * unique for each parameter being tuned and the values must be specified in order starting from 0 up, without
	 * skipping any intermediate value.
	 */
	private int tuningOrderIndex;

	/**
	 * For each role the current value being set.
	 */
	protected double[] currentValues;

	public TunableParameter(String name, double fixedValue, int tuningOrderIndex){

		this.name = name;

		this.fixedValue = fixedValue;

		this.tuningOrderIndex = tuningOrderIndex;

		this.currentValues = null;

	}

	public void clearParameter(){
		this.currentValues = null;
	}

	public void setUpParameter(int numRoles){
		this.currentValues = new double[numRoles];

		for(int i = 0; i < this.currentValues.length; i++){
			this.currentValues[i] = this.fixedValue;
		}
	}

	public double getValuePerRole(int roleIndex){
		return this.currentValues[roleIndex];
	}

	public String getParameters(String indentation) {

		String params = indentation + "NAME = " + this.name +
				indentation + "FIXED_VALUE = " + this.fixedValue +
				indentation + "TUNING_ORDER_INDEX = " + this.tuningOrderIndex;

		if(this.currentValues != null){
			String currentValuesString = "[ ";

			for(int i = 0; i < this.currentValues.length; i++){

				currentValuesString += this.currentValues[i] + " ";

			}

			currentValuesString += "]";

			params += indentation + "current_values = " + currentValuesString;
		}else{
			params += indentation + "current_values = null";
		}

		return params;

	}

	public int getTuningOrderIndex(){
		return this.tuningOrderIndex;
	}

	public String getName(){
		return this.name;
	}

	public double[] getCurrentValues() {
		// return a copy to be sure values won't be modified
		return Arrays.copyOf(currentValues, currentValues.length);
	}

}
