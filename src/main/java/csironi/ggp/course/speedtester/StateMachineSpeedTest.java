package csironi.ggp.course.speedtester;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * This class computes the nodes visits and the Monte Carlo iterations that a state machine
 * can perform in the given amount of time.
 *
 * @author C.Sironi
 *
 */
public class StateMachineSpeedTest {

	public static int visitedNodes;
	public static int succeededIterations;
	public static int failedIterations;
	public static long exactTimeSpent;


	/**
	 * This method computes the nodes visits and the Monte Carlo iterations that a state machine
	 * can perform in the given amount of time.
	 *
	 * @param theMachine the machine to be tested.
	 * @param timeToSpend the time for which to perform the Monte Carlo simulations.
	 */
	public static void testSpeed(StateMachine theMachine, long timeToSpend){

		visitedNodes = 0;
		succeededIterations = 0;
		failedIterations = 0;
		exactTimeSpent = 0L;

		int[] lastIterationVisitedNodes = new int[1];

		MachineState initialState = theMachine.getInitialState();

		long startTime = System.currentTimeMillis();

		while(System.currentTimeMillis() < startTime + timeToSpend){

			try {
				theMachine.performDepthCharge(initialState, lastIterationVisitedNodes);
				succeededIterations++;
				visitedNodes += lastIterationVisitedNodes[0];
			}catch (TransitionDefinitionException | MoveDefinitionException | StateMachineException e) {
				GamerLogger.logError("SMSpeedTest", "Exception during iteration!");
				GamerLogger.logStackTrace("SMSpeedTest", e);
				failedIterations++;
			}catch (Exception e) { // Keep all other exception separate from the typical exceptions of the state machine (even if now they are all dealt with in the same way)
				GamerLogger.logError("SMSpeedTest", "Exception during iteration!");
				GamerLogger.logStackTrace("SMSpeedTest", e);
				failedIterations++;
			}catch (Error e) {
				GamerLogger.logError("SMSpeedTest", "Error during iteration!");
				GamerLogger.logStackTrace("SMSpeedTest", e);
				failedIterations++;
			}
		}

		exactTimeSpent = System.currentTimeMillis() - startTime;

	}

}