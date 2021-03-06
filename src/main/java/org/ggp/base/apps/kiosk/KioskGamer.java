package org.ggp.base.apps.kiosk;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JPanel;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.server.event.ServerCompletedMatchEvent;
import org.ggp.base.server.event.ServerNewGameStateEvent;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.statemachine.abstractsm.AbstractStateMachine;
import org.ggp.base.util.statemachine.abstractsm.ExplicitStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMachineState;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitMove;
import org.ggp.base.util.statemachine.structure.explicit.ExplicitRole;


public class KioskGamer extends StateMachineGamer implements Observer {
    private BlockingQueue<ExplicitMove> theQueue = new ArrayBlockingQueue<ExplicitMove>(25);

    private GameGUI theGUI;
    private JPanel theGUIPanel;
    public KioskGamer(JPanel theGUIPanel) {
        this.theGUIPanel = theGUIPanel;
        theGUIPanel.setLayout(new BorderLayout());
    }

    private GameCanvas theCanvas = null;
    public void setCanvas(GameCanvas theCanvas) {
        this.theCanvas = theCanvas;
    }

    @Override
    public void stateMachineMetaGame(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {
        if(theCanvas == null)
            throw new IllegalStateException("KioskGamer did not receive a canvas.");

        theCanvas.setStateMachine(this.getStateMachine().getActualStateMachine());

        theGUI = new GameGUI(theCanvas);
        theGUI.setRole(this.getStateMachine().convertToExplicitRole(getRole()));
        theGUI.setBackground(theGUIPanel.getBackground());
        theGUI.updateGameState(getStateMachine().convertToExplicitMachineState(getStateMachine().getInitialState()));
        theGUI.addObserver(this);

        theGUIPanel.removeAll();
        theGUIPanel.add("Center", theGUI);
        theGUIPanel.repaint();

        theGUIPanel.setVisible(false);
        theGUIPanel.setVisible(true);
        theGUIPanel.validate();
        theGUIPanel.repaint();
    }

    @Override
    public ExplicitMove stateMachineSelectMove(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException,
            GoalDefinitionException {
    	theGUI.beginPlay();
        theQueue.clear();
        theGUI.updateGameState(this.getStateMachine().convertToExplicitMachineState(getCurrentState()));
        try {
            return theQueue.take();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AbstractStateMachine getInitialStateMachine() {
        return new ExplicitStateMachine(new ProverStateMachine(this.random));
    }

    @Override
    public String getName() {
        return "GraphicalHumanGamer";
    }

    private ExplicitMachineState stateFromServer;

    @Override
    public void observe(Event event) {
        if(event instanceof MoveSelectedEvent) {
            ExplicitMove theMove = ((MoveSelectedEvent)event).getMove();
            if(theQueue.size() < 2) {
                theQueue.add(theMove);
            }
        } else if(event instanceof ServerNewGameStateEvent) {
            stateFromServer = ((ServerNewGameStateEvent)event).getState();
        } else if(event instanceof ServerCompletedMatchEvent) {
            theGUI.updateGameState(stateFromServer);

            List<ExplicitRole> theRoles = getStateMachine().convertToExplicitRoles(getStateMachine().getRoles());
            List<Double> theGoals = ((ServerCompletedMatchEvent)event).getGoals();

            StringBuilder finalMessage = new StringBuilder();
            finalMessage.append("Goals: ");
            for(int i = 0; i < theRoles.size(); i++) {
                finalMessage.append(theRoles.get(i));
                finalMessage.append(" = ");
                finalMessage.append(theGoals.get(i));
                if(i < theRoles.size()-1) {
                    finalMessage.append(", ");
                }
            }

            theGUI.showFinalMessage(finalMessage.toString());
        }
    }

	@Override
	public void stateMachineStop() {
		// Do nothing
	}

	@Override
	public void stateMachineAbort() {
		// Add an "ABORT" move to the queue so that we don't wait indefinitely
		// for a human to submit a move for the aborted match; instead we should
		// finish it up as quickly as possible so we can display the next match
		// when it arrives.
		theQueue.add(new ExplicitMove(GdlPool.getConstant("ABORT")));
		if (theGUI != null) {
			theGUI.showFinalMessage("Aborted");
		}
	}

	@Override
	public boolean isComputerPlayer() {
		return false;
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		;
	}

}