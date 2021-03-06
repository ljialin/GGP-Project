package csironi.ggp.course.verifiers;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.ThreadContext;
import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.game.ManualUpdateLocalGameRepository;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.logging.GamerLogger.FORMAT;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.OptimizationCaller;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.OptimizeAwayConstantValueComponents;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.OptimizeAwayConstants;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.RemoveAnonPropositions;
import org.ggp.base.util.propnet.creationManager.optimizationcallers.RemoveOutputlessComponents;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.SeparateInternalPropnetCachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.StateMachineInitializationException;
import org.ggp.base.util.statemachine.implementation.propnet.SelfInitSeparateInternalPropNetStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

//TODO: merge all verifiers together in a single class since their code is similar.

/**
* This class verifies the consistency of the propnet state machine (the one that memorizes externally
* the state of the propnet components and separates the dynamic version of the propnet used during
* propnet optimization from the immutable version used at runtime to reason on the game) wrt the prover
* state machine. The propnet state machine checked by this class is the one that initializes the propnet
* internally instead of only accepting a propnet in the constructor.
*
* It is possible to specify the following combinations of main arguments:
*
* 1. [withCache] [optimizations]
* 2. [withCache] [optimizations] [keyOfGameToTest]
* 3. [withCache] [optimizations] [maximumPropnetInitializationTime] [maximumTestDuration]
* 4. [withCache] [optimizations] [maximumPropnetInitializationTime] [maximumTestDuration] [keyOfGameToTest]
*
* where:
* [withCache] = true if the propnet state machine must be provided with a cache for its results, false
* 				otherwise (DEFAULT: false).
* [optimizations] = the optimizations that the PropNet manager must perform on the PropNet after creation.
*  					  Each optimization corresponds to a number as follows:
*  					  	0 = OptimizeAwayConstants
*  						1 = RemoveAnonPropositions
*  						2 = OptimizeAwayConstantValueComponents
*  						3 = RemoveOutputlessComponents
*  					  The optimizations to be performed must be specified with their corresponding numbers,
*  					  separated by "-", in the order we want the manager to perform them (e.g. the input "0-1-2-3"
*  					  will make the manager perform optimization 0, followed by optimization 1, followed by
*  					  optimization 2, followed by optimization 3). To let the manager perform no optimizations
*  					  give the string "null" as argument. (Default value: "null")
* [maximumPropnetInitializationTime] = time in milliseconds that is available to build and initialize
* 									   the propnet (DEFAULT: 420000ms - 7mins).
* [maximumTestDuration] = duration of each test in millisecond (DEFAULT: 60000ms - 1min).
* [keyOfGameToTest] = key of the game to be tested (DEFAULT: null (i.e. all games)).
*
* If nothing or something inconsistent is specified for any of the parameters, the default value will
* be used.
*
* @author C.Sironi
*
*/
public class SelfInitSeparatePropnetVerifier {

	static{
		System.setProperty("isThreadContextMapInheritable", "true");
	}

	public static void main(String[] args) throws InterruptedException{


		/*********************** Parse main arguments ****************************/


		boolean withCache = false;
		long initializationTime = 420000L;
		long testTime = 60000L;
		String gameToTest = null;
		String optimizationsString = "null";
		OptimizationCaller[] optimizations = null;

		if(args.length != 0 && args.length <= 5){

			withCache = Boolean.parseBoolean(args[0]);

			optimizationsString = args[1];
			try{
				optimizations = parseOptimizations(optimizationsString);
			}catch(IllegalArgumentException e){
				System.out.println("Inconsistent specification of the PropNet optimizations. Using default value!");
				optimizationsString = "null";
		    	optimizations = null;
			}

			if(args.length == 5 || args.length == 3){
				gameToTest = args[args.length-1];
			}
			if(args.length == 5 || args.length == 4){
				try{
					initializationTime = Long.parseLong(args[2]);
				}catch(NumberFormatException nfe){
					System.out.println("Inconsistent propnet maximum building time specification! Using default value.");
					initializationTime = 420000L;
				}
				try{
					testTime = Long.parseLong(args[3]);
				}catch(NumberFormatException nfe){
					System.out.println("Inconsistent test duration specification! Using default value.");
					testTime = 60000L;
				}
			}
		}else if(args.length > 5){
			System.out.println("Inconsistent number of main arguments! Ignoring them.");
		}

		if(gameToTest == null){
			System.out.println("Running tests on ALL games with the following settings:");
		}else{
			System.out.println("Running tests on game " + gameToTest + " with the following settings:");
		}
		if(withCache){
			System.out.println("With cache: yes.");
		}else{
			System.out.println("With cache: no.");
		}
		System.out.println("Propnet building time: " + initializationTime + "ms");
		System.out.println("Running time for each test: " + testTime + "ms");
		System.out.println("Optimizations: " + optimizationsString + ".");
		System.out.println();


		/*********************** Perform all the tests ****************************/


		ProverStateMachine theReference;
		SelfInitSeparateInternalPropNetStateMachine thePropnetMachine;
		StateMachine theSubject;

		String mainLogFolder = System.currentTimeMillis() + ".Verifier";
    	ThreadContext.put("LOG_FOLDER", mainLogFolder);

    	GamerLogger.startFileLogging();

	    GamerLogger.log(FORMAT.CSV_FORMAT, "SelfInitSeparatePropnetVerifierTable", "Game key;PN initialization time (ms);PN construction time (ms);SM initialization time;Rounds;Completed rounds;Test duration (ms);Subject exception;Other exceptions;Pass;");

	    //GameRepository theRepository = GameRepository.getDefaultRepository();

	    GameRepository theRepository = new ManualUpdateLocalGameRepository("/home/csironi/GAMEREPOS/GGPBase-GameRepo-03022016");

	    for(String gameKey : theRepository.getGameKeys()) {
	        if(gameKey.contains("laikLee")) continue;

	        // TODO: change code so that if there is only one game to test we won't run through the whole sequence of keys.
	        if(gameToTest != null && !gameKey.equals(gameToTest)) continue;

	        System.out.println("Detected activation in game " + gameKey + ".");

	        Match fakeMatch = new Match(gameKey + System.currentTimeMillis(), -1, -1, -1,theRepository.getGame(gameKey) );

	        ThreadContext.put("LOG_FOLDER", mainLogFolder + "/logs/" + fakeMatch.getMatchId());

	        GamerLogger.log("Verifier", "Testing on game " + gameKey);

	        List<Gdl> description = theRepository.getGame(gameKey).getRules();

	        theReference = new ProverStateMachine(new Random());

	        Random random = new Random();
			// Create the state machine.
		    thePropnetMachine = new SelfInitSeparateInternalPropNetStateMachine(random, optimizations);

		    if(withCache){
		    	theSubject = new SeparateInternalPropnetCachedStateMachine(random, thePropnetMachine);
		    }else{
		    	theSubject = thePropnetMachine;
		    }

		    theReference.initialize(description, Long.MAX_VALUE);

		    long smInitTime = -1L;
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
	        	theSubject.initialize(description, System.currentTimeMillis() + initializationTime);
		        smInitTime = System.currentTimeMillis() - initStart;
		        System.out.println("Propnet creation succeeded. Checking consistency.");
		        long testStart = System.currentTimeMillis();

		        /***************************************/
			    System.gc();
			    /***************************************/

		        pass = ExtendedStateMachineVerifier.checkMachineConsistency(theReference, theSubject, testTime);
		        testDuration = System.currentTimeMillis() - testStart;
		        rounds = ExtendedStateMachineVerifier.lastRounds;
		        completedRounds = ExtendedStateMachineVerifier.completedRounds;
		        exception = ExtendedStateMachineVerifier.exception;
		        otherExceptions = ExtendedStateMachineVerifier.otherExceptions;
	        }catch(StateMachineInitializationException e){
	      	  	smInitTime = System.currentTimeMillis() - initStart;
	        	GamerLogger.logError("Verifier", "State machine " + theSubject.getName() + " initialization failed, impossible to test this game. Cause: [" + e.getClass().getSimpleName() + "] " + e.getMessage() );
	        	GamerLogger.logStackTrace("Verifier", e);
	        	System.out.println("Skipping test on game " + gameKey + ". State machine initialization failed, no propnet available.");
	        }

	        GamerLogger.log(FORMAT.PLAIN_FORMAT, "Verifier", "");

	        ThreadContext.put("LOG_FOLDER", mainLogFolder);

	        GamerLogger.log(FORMAT.CSV_FORMAT, "SelfInitSeparatePropnetVerifierTable", gameKey + ";" + thePropnetMachine.getTotalInitTime() + ";" + thePropnetMachine.getPropnetConstructionTime() + ";" + smInitTime +  ";"  + rounds +  ";"  + completedRounds + ";"  + testDuration + ";"  + exception + ";"  + otherExceptions + ";" + pass + ";");

	        /***************************************/
	        System.gc();
	        GdlPool.drainPool();
	        /***************************************/
	    }
	}

	private static OptimizationCaller[] parseOptimizations(String opts){

		if(opts.equalsIgnoreCase("null")){
			return null;
		}

		String[] splitOpts = opts.split("-");

		if(splitOpts.length < 1){
			throw new IllegalArgumentException();
		}

		OptimizationCaller[] optimizations = new OptimizationCaller[splitOpts.length];

		for(int i = 0; i < splitOpts.length; i++){
			switch(splitOpts[i]){
				case "0":
					optimizations[i] = new OptimizeAwayConstants();
					break;
				case "1":
					optimizations[i] = new RemoveAnonPropositions();
					break;
				case "2":
					optimizations[i] = new OptimizeAwayConstantValueComponents();
					break;
				case "3":
					optimizations[i] = new RemoveOutputlessComponents();
					break;
				default:
					throw new IllegalArgumentException();
			}
		}

		return optimizations;
	}
}
