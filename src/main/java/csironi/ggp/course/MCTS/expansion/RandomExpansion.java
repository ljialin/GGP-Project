/**
 *
 */
package csironi.ggp.course.MCTS.expansion;

import java.util.Random;

import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import csironi.ggp.course.MCTS.MCTNode;

/**
 * @author C.Sironi
 *
 */
public class RandomExpansion implements ExpansionStrategy {

	Random random;

	/**
	 * Contructor.
	 */
	public RandomExpansion() {
		this.random = new Random();
	}

	/* (non-Javadoc)
	 * @see csironi.ggp.course.MCTS.expansion.ExpansionStrategy#expand(csironi.ggp.course.MCTS.MCTNode)
	 */
	@Override
	public MCTNode expand(MCTNode nodeToExpand) throws MoveDefinitionException, TransitionDefinitionException {

		// Visit for the first time one random unvisited child of the node nodeToExpand.
		return nodeToExpand.childFirstVisit(this.random.nextInt(nodeToExpand.getUnvisitedChildrenNumber()));

	}

}
