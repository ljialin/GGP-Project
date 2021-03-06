package org.ggp.base.player.gamer.statemachine.MCTS.manager.parameterstuning.structure.parameters;


/**
 * TODO: should this become a generic class?
 * @author C.Sironi
 *
 */
public class IntTunableParameter /*extends TunableParameter*/ {

	/**
	 * Value of the parameter for the roles not being tuned.
	 * It's also the values used to initialize each value at the beginning of each new game.
	 */
	private int fixedValue;

	/**
	 * If we are tuning this parameter for at least one role, these are all the possible values
	 * that the parameter can assume.
	 */
	private int[] possibleValues;

	/**
	 * For each role the current value being set.
	 */
	private int[] currentValues;

	public IntTunableParameter(String name, int fixedValue) {

		this(name, fixedValue, null, null, -1);

	}

	public IntTunableParameter(String name, int fixedValue, int[] possibleValues, double[] possibleValuesPenalty, int tuningOrderIndex) {

		//super(name, possibleValuesPenalty, tuningOrderIndex);

		this.fixedValue = fixedValue;

		this.possibleValues = possibleValues;

		this.currentValues = null;

	}

	public void clearParameter(){
		this.currentValues = null;
	}

	public void setUpParameter(int numRoles){
		this.currentValues = new int[numRoles];

		for(int i = 0; i < this.currentValues.length; i++){
			this.currentValues[i] = this.fixedValue;
		}
	}

	public int getValuePerRole(int roleIndex){
		return this.currentValues[roleIndex];
	}


	public int getNumPossibleValues(){

		if(this.possibleValues == null){
			return 0;
		}else{
			return this.possibleValues.length;
		}
	}


	public void setMyRoleNewValue(int myRoleIndex, int newValueIndex) {
		// We are tuning only the parameter of myRole
		this.currentValues[myRoleIndex] = this.possibleValues[newValueIndex];
	}


	public void setAllRolesNewValues(int[] newValuesIndices) {
		// We are tuning for all roles
		for(int i = 0; i < newValuesIndices.length; i++){
			this.currentValues[i] = this.possibleValues[newValuesIndices[i]];
		}
	}


	public String getParameters(String indentation) {

		//String superParams = super.getParameters(indentation);

		String params = indentation + "FIXED_VALUE = " + this.fixedValue;

		if(this.possibleValues != null){
			String possibleValuesString = "[ ";

			for(int i = 0; i < this.possibleValues.length; i++){
				if(this.possibleValues[i] == Integer.MAX_VALUE){
					possibleValuesString += "inf ";
				}else{
					possibleValuesString += this.possibleValues[i] + " ";
				}



			}

			possibleValuesString += "]";

			params += indentation + "POSSIBLE_VALUES = " + possibleValuesString;
		}else{
			params += indentation + "POSSIBLE_VALUES = null";
		}

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

		//if(superParams != null){
			//return superParams + params;
		//}else{
			return params;
		//}

	}

	public String[] getPossibleValues() {
		String[] values = new String[this.possibleValues.length];
		for(int i = 0; i < this.possibleValues.length; i++){
			if(this.possibleValues[i] == Integer.MAX_VALUE){
				values[i] = "inf";
			}else{
				values[i] = ""+this.possibleValues[i];
			}
		}
		return values;
	}

	public double getPossibleValue(int valueIndex) {
		return this.possibleValues[valueIndex];
	}

	public int[] getCurrentValuesIndices() {
		int[] currentValuesIndices = new int[this.currentValues.length];
		for(int roleIndex = 0; roleIndex < this.currentValues.length; roleIndex++){
			currentValuesIndices[roleIndex] = 0;
			while(currentValuesIndices[roleIndex] < this.possibleValues.length && this.possibleValues[currentValuesIndices[roleIndex]] != this.currentValues[roleIndex]){
				currentValuesIndices[roleIndex]++;
			}
		}
		return currentValuesIndices;
	}

}
