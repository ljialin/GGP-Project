package csironi.ggp.course.verifiers;

import java.util.List;
import java.util.Random;

import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.logging.GamerLogger.FORMAT;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.exceptions.StateMachineInitializationException;
import org.ggp.base.util.statemachine.implementation.propnet.CheckFwdInterrPropnetStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

//TODO: merge all verifiers together in a single class since their code is similar.

/**
 * This class verifies the consistency of the propnet state machine (the one that checks internally during
 * initialization that the building time of the propnet doesn't exceed a given threshold) wrt the prover
 * state machine.
 *
 * It is possible to specify the following combinations of main arguments:
 *
 * 1. [keyOfGameToTest]
 * 2. [maximumPropnetBuildingTime] [maximumTestDuration]
 * 3. [maximumPropnetBuildingTime] [maximumTestDuration] [keyOfGameToTest]
 *
 * where:
 * [maximumPropnetBuildingTime] = time in milliseconds that the propnet state machine has available to
 * 								  build the propnet (DEFAULT: 300000ms - 5mins).
 * [maximumTestDuration] = duration of each test in millisecond (DEFAULT: 60000ms - 1min).
 * [keyOfGameToTest] = key of the game to be tested (DEFAULT: null (i.e. all games)).
 *
 * If nothing or something inconsistent is specified for any of the parameters, the default value will
 * be used.
 *
 * @author C.Sironi
 *
 */
public class FirstPropnetVerifier {

	public static void main(String[] args) throws InterruptedException{


		/*********************** Parse main arguments ****************************/


		long buildingTime = 300000L;
		long testTime = 60000L;
		String gameToTest = null;

		if (args.length != 0 && args.length <= 3){
			if(args.length == 3 || args.length == 1){
				gameToTest = args[args.length-1];
			}
			if(args.length == 2 || args.length == 3){
				try{
					buildingTime = Long.parseLong(args[0]);
				}catch(NumberFormatException nfe){
					System.out.println("Inconsistent propnet maximum building time specification! Using default value.");
					buildingTime = 300000L;
				}
				try{
					testTime = Long.parseLong(args[1]);
				}catch(NumberFormatException nfe){
					System.out.println("Inconsistent test duration specification! Using default value.");
					testTime = 60000L;
				}
			}
		}else if(args.length > 3){
			System.out.println("Inconsistent number of main arguments! Ignoring them.");
		}

		if(gameToTest == null){
			System.out.println("Running tests on ALL games with the following time settings:");
		}else{
			System.out.println("Running tests on game " + gameToTest + " with the following time settings:");
		}
		System.out.println("Propnet building time: " + buildingTime + "ms");
		System.out.println("Running time for each test: " + testTime + "ms");
		System.out.println();


		/*********************** Perform all the tests ****************************/


        ProverStateMachine theReference;
        CheckFwdInterrPropnetStateMachine thePropnetMachine;

        GamerLogger.setSpilloverLogfile("FirstPropnetVerifierTable.csv");
        GamerLogger.log(FORMAT.CSV_FORMAT, "FirstPropnetVerifierTable", "Game key;Initialization time (ms);Construction time (ms);Rounds;Completed rounds;Test duration (ms);Subject exception;Other exceptions;Pass;");

        GameRepository theRepository = GameRepository.getDefaultRepository();
        for(String gameKey : theRepository.getGameKeys()) {
            if(gameKey.contains("laikLee")) continue;

            // TODO: change code so that if there is only one game to test we won't run through the whole sequence of keys.
            if(gameToTest != null && !gameKey.equals(gameToTest)) continue;

            System.out.println("Detected activation in game " + gameKey + ".");

            Match fakeMatch = new Match(gameKey + System.currentTimeMillis(), -1, -1, -1,theRepository.getGame(gameKey) );

            GamerLogger.startFileLogging(fakeMatch, "FirstPropnetVerifier");

            GamerLogger.log("Verifier", "Testing on game " + gameKey);

            List<Gdl> description = theRepository.getGame(gameKey).getRules();

            theReference = new ProverStateMachine(new Random());

            // Create propnet state machine giving it buildingTime milliseconds to build the propnet
            thePropnetMachine = new CheckFwdInterrPropnetStateMachine(new Random(), buildingTime);

            theReference.initialize(description, Long.MAX_VALUE);

            long initializationTime;
            int rounds = -1;
            int completedRounds = -1;
            long testDuration = -1L;
            boolean pass = false;
            String exception = "-";
            int otherExceptions = -1;

            long initStart = System.currentTimeMillis();

            // Try to initialize the propnet state machine.
            // If initialization fails, skip the test.
            try{
            	thePropnetMachine.initialize(description, Long.MAX_VALUE);
            	initializationTime = System.currentTimeMillis() - initStart;
            	System.out.println("Propnet creation succeeded. Checking consistency.");
            	long testStart = System.currentTimeMillis();
                pass = ExtendedStateMachineVerifier.checkMachineConsistency(theReference, thePropnetMachine, testTime);
                testDuration = System.currentTimeMillis() - testStart;
                rounds = ExtendedStateMachineVerifier.lastRounds;
                completedRounds = ExtendedStateMachineVerifier.completedRounds;
                exception = ExtendedStateMachineVerifier.exception;
                otherExceptions = ExtendedStateMachineVerifier.otherExceptions;
            }catch(StateMachineInitializationException e){
            	initializationTime = System.currentTimeMillis() - initStart;
            	GamerLogger.logError("Verifier", "State machine " + thePropnetMachine.getName() + " initialization failed, impossible to test this game. Cause: [" + e.getClass().getSimpleName() + "] " + e.getMessage() );
            	GamerLogger.logStackTrace("Verifier", e);
            	System.out.println("Skipping test on game " + gameKey + ". State machine initialization failed, no propnet available.");
            }

            GamerLogger.log(FORMAT.PLAIN_FORMAT, "Verifier", "");

            GamerLogger.stopFileLogging();

            GamerLogger.log(FORMAT.CSV_FORMAT, "FirstPropnetVerifierTable", gameKey + ";" + initializationTime + ";" + thePropnetMachine.getPropnetConstructionTime() + ";" + rounds +  ";"  + completedRounds + ";"  + testDuration + ";"  + exception + ";"  + otherExceptions + ";" + pass + ";");
        }
	}
}
