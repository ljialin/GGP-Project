/**
 *
 */
package csironi.ggp.course.MCTS.selection;

import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.StateMachineException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import csironi.ggp.course.MCTS.MCTNode;
import csironi.ggp.course.MCTS.expansion.OldExpansionStrategy;
import csironi.ggp.course.MCTS.playout.OldPlayoutStrategy;

/**
 * @author C.Sironi
 *
 */
public class OldUCTSelection implements OldSelectionStrategy {

	OldExpansionStrategy expansionStrategy;
	OldPlayoutStrategy playoutStrategy;

	Random random;

	double c;

	/**
	 *
	 */
	public OldUCTSelection(OldExpansionStrategy expansionStrategy, OldPlayoutStrategy playoutStrategy, double c) {
		this.expansionStrategy = expansionStrategy;
		this.playoutStrategy = playoutStrategy;
		this.random = new Random();
		this.c = c;
	}

	/* (non-Javadoc)
	 * @see csironi.ggp.course.MCTS.selection.SelectionStrategy#select(csironi.ggp.course.MCTS.MCTNode, org.ggp.base.util.statemachine.Role)
	 */
	@Override
	public List<Double> select(MCTNode node) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, StateMachineException {

		List<Double> goals;

		// If the node has no children it means it is a leaf node of the MCTS tree
		if(node.hasNoChildren()){
			return node.getTerminalGoals();
		}

		MCTNode selectedChild;

		if(node.hasUnvisitedChildren()){
			selectedChild = this.expansionStrategy.expand(node);
			goals = this.playoutStrategy.playout(selectedChild);
		}else{

			selectedChild = getBestUCTChild(node);
			goals = select(selectedChild);
		}

		selectedChild.update(goals);

		return goals;
	}

	private MCTNode getBestUCTChild(MCTNode parent){

		// Number of visits of the parent
		double np = (double) parent.getVisits();

		List<MCTNode> visitedChildren = parent.getVisitedChildren();
		MCTNode bestChild = null;
		double maxUCTValue = -1;

		for(MCTNode child: visitedChildren){
			double UCTValue = getUCTValue(child, np);
			if(UCTValue > maxUCTValue){
				maxUCTValue = UCTValue;
				bestChild = child;
			}
		}

		return bestChild;

	}

	private double getUCTValue(MCTNode node, double np){

		double ni = (double) node.getVisits();
		double avgScore = ((double) node.getScoreSum() / ni) / 100.0;
		return avgScore + this.c * Math.sqrt(Math.log(np)/ni);

	}

}
