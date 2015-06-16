/**
 *
 */
package csironi.ggp.course.evalfunctions;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * @author C.Sironi
 *
 */
public abstract class EvaluationFunction {

	StateMachine stateMachine;

	/**
	 *
	 */
	public EvaluationFunction(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	public abstract int eval(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException;

}