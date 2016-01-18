package org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicAnd;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicConstant;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicNot;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicOr;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicProposition;
import org.ggp.base.util.propnet.architecture.separateExtendedState.dynamic.components.DynamicTransition;
import org.ggp.base.util.propnet.utils.PROP_TYPE;
import org.ggp.base.util.statemachine.Role;

public class DynamicPropNet {

	/********************************** Parameters **********************************/

	/** References to every component in the PropNet. */
	private final Set<DynamicComponent> components;

	/** References to every Proposition in the PropNet. */
	private final Set<DynamicProposition> propositions;

	/** A helper list of all of the roles. */
	private final List<Role> roles;

	/** Reference to the single TRUE constant in the propnet */
	private DynamicConstant trueConstant;

	/** Reference to the single FALSE constant in the prpnet */
	private DynamicConstant falseConstant;

	/** A reference to the single, unique, InitProposition. */
	//private DynamicProposition initProposition;

	/** A reference to the single, unique, TerminalProposition. */
	private DynamicProposition terminalProposition;

	/**
	 * References to every BaseProposition in the PropNet.
	 * Corresponds to the current state in the ExternalPropnetState class.
	 * This list and the current and next state in the ExternalPropnetState class all
	 * have the elements in the same order.
	 */
	private final List<DynamicProposition> basePropositions;

	/**
	 * References to every InputProposition in the PropNet, ordered by role.
	 * Roles are in the same order as in the roles list.
	 * The order is the same as the values of the currentJointMove in the ExternalPropnetState class.
	 */
	private List<DynamicProposition> inputPropositions;

	/**
	 * References to every LegalProposition in the PropNet, ordered by role.
	 * Roles are in the same order as in the roles list.
	 * The order is the same as the values of the inputPropositions list.
	 */
	private List<DynamicProposition> legalPropositions;

	/**
	 * References to every input proposition in the PropNet, grouped by role.
	 * The order of the input propositions for every role is the same as the order of the legal
	 * propositions for the same role.
	 */
	private final Map<Role, List<DynamicProposition>> inputsPerRole;

	/**
	 * References to every legal proposition in the PropNet, grouped by role.
	 * The order of the legal propositions for every role is the same as the order of the input
	 * propositions for the same role.
	 */
	private final Map<Role, List<DynamicProposition>> legalsPerRole;

	/** References to every GoalProposition in the PropNet, indexed by role. */
	private final Map<Role, List<DynamicProposition>> goalsPerRole;

	/**
	 * Number of AND and OR gates in the propnet.
	 */
	private int andOrGatesNumber;

	/**
	 * This set keeps track of the GDL propositions corresponding to base propositions that have
	 * been removed because always true. These are needed when converting an internal representation
	 * of a game state to the standard representation of GGP-Base.
	 */
	private Set<GdlSentence> alwaysTrueBases;

	/**
	 * Creates a new PropNet from a list of Components.
	 *
	 * TODO: Also checks if a gate or a proposition has no inputs and if so connects it to the FALSE component
	 * (TRUE component if it is a NOT).
	 * Note: if a BASE proposition has no input it won't be detected as base proposition, so can be
	 * treated as any other proposition and connected to FALSE.
	 * Note: an INPUT proposition must be excluded from this check as it is normal for it not to have any
	 * input.
	 *
	 * ATTENTION: the algorithm that detects an input/legal propositions and puts it in the corresponding map
	 * if and only if it has its legal/input counterpart works only if for each different move there is at
	 * most only one input proposition and at most only one legal proposition. (Note that with move we intend
	 * the list of GdlTerms that come after the keyword 'does' in an input proposition or after the keyword
	 * 'legal' in a legal proposition: e.g. (xplayer (mark 1 1)).
	 *
	 * @param components
	 *            A list of Components.
	 */
	public DynamicPropNet(List<Role> roles, Set<DynamicComponent> components, DynamicConstant trueConstant, DynamicConstant falseConstant){

		//System.out.println();
		//System.out.println(trueConstant.getComponentType());
		//System.out.println(falseConstant.getComponentType());

		//System.out.println(trueConstant.getValue());
		//System.out.println(falseConstant.getValue());

		//for(DynamicComponent c : trueConstant.getOutputs()){
		//	System.out.println(c.getComponentType());
		//}

		//long start = System.currentTimeMillis();

	    this.roles = roles;

	    //for(Role r : roles){
	    //	System.out.println(r);
	    //}
		this.components = components;
		this.falseConstant = falseConstant;
		this.trueConstant = trueConstant;
		this.components.add(trueConstant);
		this.components.add(falseConstant);

		this.propositions = new HashSet<DynamicProposition>();
		this.basePropositions = new ArrayList<DynamicProposition>();

		this.inputsPerRole = new HashMap<Role, List<DynamicProposition>>();
		this.legalsPerRole = new HashMap<Role, List<DynamicProposition>>();
		//Map<Role, List<DynamicProposition>> inputsPerRole = new HashMap<Role, List<DynamicProposition>>();
		//Map<Role, List<DynamicProposition>> legalsPerRole = new HashMap<Role, List<DynamicProposition>>();

		this.andOrGatesNumber = 0;

		this.alwaysTrueBases = new HashSet<GdlSentence>();

		//Map<Role, Map<List<GdlTerm>, Integer>> moveIndices = new HashMap<Role, Map<List<GdlTerm>, Integer>>();
		//Map<Role, Integer> currentIndices = new HashMap<Role, Integer>();

		/* ALG3 - START */
		Map<Role, Map<List<GdlTerm>, DynamicProposition>> possibleInputs = new HashMap<Role, Map<List<GdlTerm>, DynamicProposition>>();
		Map<Role, Map<List<GdlTerm>, DynamicProposition>> possibleLegals = new HashMap<Role, Map<List<GdlTerm>, DynamicProposition>>();
		/* ALG3 - END */


		this.goalsPerRole = new HashMap<Role, List<DynamicProposition>>();

		for(Role r : this.roles){
			this.inputsPerRole.put(r, new ArrayList<DynamicProposition>());
			this.legalsPerRole.put(r, new ArrayList<DynamicProposition>());
			//inputsPerRole.put(r, new ArrayList<DynamicProposition>());
			//legalsPerRole.put(r, new ArrayList<DynamicProposition>());

			//moveIndices.put(r, new HashMap<List<GdlTerm>, Integer>());
			//currentIndices.put(r, new Integer(0));

			/* ALG3 - START */
			possibleInputs.put(r, new HashMap<List<GdlTerm>, DynamicProposition>());
			possibleLegals.put(r, new HashMap<List<GdlTerm>, DynamicProposition>());
			/* ALG3 - END */

			this.goalsPerRole.put(r, new ArrayList<DynamicProposition>());
		}

		for(DynamicComponent c : this.components){

			if(c instanceof DynamicConstant){
				if(((DynamicConstant) c).getValue()){
					if(this.trueConstant != c){
						throw new RuntimeException("Found more than only one TRUE constant in the propnet!");
					}
				}else{
					if(this.falseConstant != c){
						throw new RuntimeException("Found more than only one FALSE constant in the propnet!");
					}
				}
			}else if(c instanceof DynamicProposition){

				DynamicProposition p = (DynamicProposition) c;
				this.propositions.add(p);
				// So that if the following code doesn't detect it as a special type
				// of proposition, its type won't be null.
				p.setPropositionType(PROP_TYPE.OTHER);

				// Check if it's a base.
			    if(p.getInputs().size() == 1 && p.getSingleInput() instanceof DynamicTransition){
			    	this.basePropositions.add(p);
			    	// Set that the type of this proposition is BASE
			    	p.setPropositionType(PROP_TYPE.BASE);
			    // Check if it's an input or legal
				}else if(p.getName() instanceof GdlRelation){
					GdlRelation relation = (GdlRelation) p.getName();
					if(relation.getName().getValue().equals("does")){ // Possible input

						/* ALG3 - START */
						// Get the GDL move corresponding to this proposition
						List<GdlTerm> gdlMove = p.getName().getBody();
						// Get the role performing the move
						GdlConstant name = (GdlConstant) relation.get(0);
						Role r = new Role(name);

						// Get the map of possible inputs for the role
						Map<List<GdlTerm>, DynamicProposition> possibleInputsPerRole = possibleInputs.get(r);

						// If we have no map, r is not a relevant role, so we just classify the proposition as an OTHER proposition...
						if(possibleInputsPerRole != null){
							//...otherwise we put this proposition in the map of possible inputs.
							possibleInputsPerRole.put(gdlMove, p);
						}
						/* ALG3 - END */

					}else if(relation.getName().getValue().equals("legal")){ // Possible legal move

						/* ALG3 - START */
						// Get the GDL move corresponding to this proposition
						List<GdlTerm> gdlMove = p.getName().getBody();
						// Get the role performing the move
						GdlConstant name = (GdlConstant) relation.get(0);
						Role r = new Role(name);

						// Get the map of possible legals for the role
						Map<List<GdlTerm>, DynamicProposition> possibleLegalsPerRole = possibleLegals.get(r);

						// If we have no map, r is not a relevant role, so we just classify the proposition as an OTHER proposition...
						if(possibleLegalsPerRole != null){
							//...otherwise we put this proposition in the map of possible legals.
							possibleLegals.get(r).put(gdlMove, p);
						}
						/* ALG3 - END */

					}else if(relation.getName().getValue().equals("goal")){
						GdlConstant name = (GdlConstant) relation.get(0);
						//System.out.println(name);
						Role r = new Role(name);
						if(this.roles.contains(r)){
							this.goalsPerRole.get(r).add(p);
							p.setPropositionType(PROP_TYPE.GOAL);
						}
					}
				}else if(p.getName() instanceof GdlProposition){

					GdlConstant constant = ((GdlProposition) p.getName()).getName();

					if(constant.getValue().equals("terminal")){
						if(this.terminalProposition == null){
							this.terminalProposition = p;
							p.setPropositionType(PROP_TYPE.TERMINAL);
						}else{
							throw new RuntimeException("Found more than only one TERMINAL proposition in the propnet!");
						}
					}/*else if (constant.getValue().toUpperCase().equals("INIT")){
						if(this.initProposition == null){
							this.initProposition = p;
							p.setPropositionType(PROP_TYPE.INIT);
						}else{
							throw new RuntimeException("Found more than only one INIT proposition in the propnet!");
						}
					}*/ // Since I don't use the init proposition, commenting this part of code means that the init
					// proposition will be treated as any OTHER proposition, and thus connected to FALSE as any OTHER
					// input-less proposition. Uncomment this to keep it! If you want to keep it also pay attention when
					// using some propnet optimization methods as they might remove it anyway.
				}

			    //System.out.println("["+c.getComponentType()+"]");

			}else if(c instanceof DynamicTransition){

			}else if(c instanceof DynamicNot){

			}else if(c instanceof DynamicAnd){
				this.andOrGatesNumber++;
			}else if(c instanceof DynamicOr){
				this.andOrGatesNumber++;
			}else{
				throw new RuntimeException("Unhandled component type " + c.getClass());
			}
		}

		//System.out.println("Iteration over components: " + (System.currentTimeMillis() - start) + "ms");
		//start = System.currentTimeMillis();

		// If a constant is null, create and add it to the propnet
		// Note that the constant must also be added to the components. However it will
		// not be associated with an index when the propnet state will be created, its
		// index will always be set to -1 since the propnet state will not include its
		// value. The constant will store its fixed value internally.
		/*if(this.trueConstant == null){
			this.trueConstant = new DynamicConstant(true);
			this.components.add(this.trueConstant);
		}
		if(this.falseConstant == null){
			this.falseConstant = new DynamicConstant(false);
			this.components.add(this.falseConstant);
		}*/

		//System.out.println("Check constants: " + (System.currentTimeMillis() - start) + "ms");
		//start = System.currentTimeMillis();

		/*
		 * This part of code will remove from the map of INPUTs the ones that have no LEGAL counterpart
		 * and remove from the map of LEGALs the ones that have no INPUT counterpart.
		 * Having no counterpart means that the corresponding proposition is null, thus also the null
		 * elements in the map will be removed.
		 * After being removed, the propositions will be considered as OTHER propositions.
		 */
		/*for(Role r : this.roles){
			List<DynamicProposition> roleInputs = this.inputsPerRole.get(r);
			List<DynamicProposition> roleLegals = this.legalsPerRole.get(r);

			int i = roleInputs.indexOf(null);

			while(i != -1){
				roleInputs.remove(i);
				// The reset of the type to OTHER is used to tell the proposition not to consider
				// itself as a legal proposition anymore. No sense in still considering it a legal
				// proposition since there is no input counterpart.
			    roleLegals.remove(i).setPropositionType(PROP_TYPE.OTHER);

				i = roleInputs.indexOf(null);
			}

			i = roleLegals.indexOf(null);

			while(i != -1){
				// The reset of the type to OTHER is used to tell the proposition not to consider
				// itself as an input proposition anymore. No sense in still considering it an input
				// proposition since there is no legal counterpart.
				roleInputs.remove(i).setPropositionType(PROP_TYPE.OTHER);
				roleLegals.remove(i);

				i = roleLegals.indexOf(null);
			}
		}

		System.out.println("Fix legal inputs: " + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();

		*/


		this.inputPropositions = new ArrayList<DynamicProposition>();
		this.legalPropositions = new ArrayList<DynamicProposition>();

		//int x = 0;
		for(Role r : this.roles){

			/* ALG3 - START */
			Map<List<GdlTerm>,DynamicProposition> possibleLegalsPerRole = possibleLegals.get(r);
			Map<List<GdlTerm>,DynamicProposition> possibleInputsPerRole = possibleInputs.get(r);
			for(Entry<List<GdlTerm>,DynamicProposition> legalEntry : possibleLegalsPerRole.entrySet()){
				DynamicProposition input = possibleInputsPerRole.remove(legalEntry.getKey());

				// If the legal has no corresponding input, we create the input proposition
				// and add it to the propnet (note that it will have no input nor output so it won't
				// influence the game, however we must create it because the corresponding legal might
				// become true at a certain moment in the game and the player could choose to play the
				// corresponding move. Also note that it's highly likely that the corresponding legal
				// will never become true, however we cannot assume this here, but it will be detected
				// later when the propnet will be optimized by the removeConstantValueComponents() method
				// in the DynamicPropnetFactory).
				if(input == null){
					input = new DynamicProposition(GdlPool.getRelation(GdlPool.getConstant("does"), legalEntry.getValue().getName().getBody()));

					/*System.out.println("INLEGAL:");

					for(DynamicComponent i: legalEntry.getValue().getInputs()){
						System.out.println("	- " + i.getComponentType());
						for(DynamicComponent ii: i.getInputs()){
							System.out.println("		- " + ii.getComponentType());
						}
					}
					System.out.println("LEGAL = " + legalEntry.getValue().getName());

					for(DynamicComponent o: legalEntry.getValue().getOutputs()){
						System.out.println("	- " + o.getComponentType());
						for(DynamicComponent oo: o.getOutputs()){
							System.out.println("	- " + oo.getComponentType());
						}
					}


					System.out.println("INPUT = " + input.getName()); */

					this.components.add(input);
				}

				// Now we put both the legal and input in the correct
				// list for the role in the same position
				this.inputsPerRole.get(r).add(input);
				input.setPropositionType(PROP_TYPE.INPUT);
				this.legalsPerRole.get(r).add(legalEntry.getValue());
				legalEntry.getValue().setPropositionType(PROP_TYPE.LEGAL);

			}

			for(DynamicProposition p : this.inputsPerRole.get(r)){
				this.inputPropositions.add(p);
			}
			for(DynamicProposition p : this.legalsPerRole.get(r)){
				this.legalPropositions.add(p);
			}
			/*
			for(DynamicProposition p : inputsPerRole.get(r)){
				this.inputPropositions.add(p);
			}
			for(DynamicProposition p : legalsPerRole.get(r)){
				this.legalPropositions.add(p);
			}
			*/
		}
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    public int getGoalValue(DynamicProposition goalProposition){
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}


	/************************************** Getters and setters ***************************************/

	/**
	 * Getter method.
	 *
	 * @return References to every Component in the PropNet.
	 */
	public Set<DynamicComponent> getComponents(){
		return components;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every Proposition in the PropNet.
	 */
	public Set<DynamicProposition> getPropositions(){
		return propositions;
	}

	/**
	 * Getter method.
	 *
	 * @return ordered list of roles.
	 */
	public List<Role> getRoles(){
	    return roles;
	}

	/**
	 * Getter method.
	 *
	 * @return the single TRUE constant in the propNet.
	 */
	public DynamicConstant getTrueConstant(){
		return this.trueConstant;
	}

	/**
	 * Getter method.
	 *
	 * @return the single FALSE constant in the propNet.
	 */
	public DynamicConstant getFalseConstant(){
		return this.falseConstant;
	}

	/**
	 * Getter method.
	 *
	 * @return A reference to the single, unique, InitProposition.
	 */
	/*
	public DynamicProposition getInitProposition(){
		return initProposition;
	}
	*/

	/**
	 * Getter method.
	 *
	 * @return A reference to the single, unique, TerminalProposition.
	 */
	public DynamicProposition getTerminalProposition(){
		return terminalProposition;
	}

	/**
	 * Getter method.
	 *
	 * @return references to every BaseProposition in the PropNet in the correct order.
	 */
	public List<DynamicProposition> getBasePropositions(){
		return this.basePropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every InputProposition in the PropNet, grouped by role
	 * and ordered.
	 */
	public List<DynamicProposition> getInputPropositions(){
		return this.inputPropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every InputProposition in the PropNet, grouped by role
	 * and ordered.
	 */
	public List<DynamicProposition> getLegalPropositions(){
		return this.legalPropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every InputProposition in the PropNet, indexed by
	 *         player name and ordered.
	 */
	public Map<Role, List<DynamicProposition>> getInputsPerRole(){
		return this.inputsPerRole;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every LegalProposition in the PropNet, indexed by
	 *         player name and ordered.
	 */
	public Map<Role, List<DynamicProposition>> getLegalsPerRole(){
		return this.legalsPerRole;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every GoalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, List<DynamicProposition>> getGoalsPerRole(){
		return this.goalsPerRole;
	}

	/**
	 * Getter method.
	 *
	 * @return The number of AND and OR gates in the PropNet.
	 */
	public int getAndOrGatesNumber(){
		return this.andOrGatesNumber;
	}

	/**
	 * Getter method.
	 *
	 * @return a copy of the list with all the Gdl base propositions that are true in
	 * every state and thus have been removed from the propnet.
	 */
	public Set<GdlSentence> getAlwaysTrueBases(){
		return new HashSet<GdlSentence>(this.alwaysTrueBases);
	}

	/**
	 * Setter method.
	 *
	 * @param alwaysTrueBase the GDL representation of a base proposition that has been
	 * detected as always having a TRUE value and so has been removed as base proposition
	 * from the propnet.
	 */
	public void addAlwaysTrueBase(GdlSentence alwaysTrueBase){
		this.alwaysTrueBases.add(alwaysTrueBase);
	}
	/** References to every LegalProposition in the PropNet, indexed by role. */
//	private final Map<Role, Set<DynamicProposition>> legalPropositions;


	/** A helper mapping between input/legal propositions. */
//	private final Map<DynamicProposition, DynamicProposition> legalInputMap;


/*
	public void addComponent(DynamicComponent c)
	{
		components.add(c);
		if (c instanceof DynamicProposition) propositions.add((DynamicProposition)c);
	}
*/
	/**
	 * Creates a new PropNet from a list of Components, along with indices over
	 * those components.
	 *
	 * @param components
	 *            A list of Components.
	 */
	/*public DynamicPropNet(List<Role> roles, Set<DynamicComponent> components)
	{

	    this.roles = roles;
		this.components = components;
		this.propositions = recordPropositions();
		this.basePropositions = recordBasePropositions();
		this.inputPropositions = recordInputPropositions();
		this.legalPropositions = recordLegalPropositions();
		this.goalPropositions = recordGoalPropositions();
		this.initProposition = recordInitProposition();
		this.terminalProposition = recordTerminalProposition();
		this.legalInputMap = makeLegalInputMap();
	}*/


/*
	public Map<DynamicProposition, DynamicProposition> getLegalInputMap()
	{
		return legalInputMap;
	}
*/
	/*
	private Map<DynamicProposition, DynamicProposition> makeLegalInputMap() {
		Map<DynamicProposition, DynamicProposition> legalInputMap = new HashMap<DynamicProposition, DynamicProposition>();
		// Create a mapping from Body->Input.
		Map<List<GdlTerm>, DynamicProposition> inputPropsByBody = new HashMap<List<GdlTerm>, DynamicProposition>();
		for(DynamicProposition inputProp : inputPropositions.values()) {
			List<GdlTerm> inputPropBody = (inputProp.getName()).getBody();
			inputPropsByBody.put(inputPropBody, inputProp);
		}
		// Use that mapping to map Input->Legal and Legal->Input
		// based on having the same Body proposition.
		for(Set<DynamicProposition> legalProps : legalPropositions.values()) {
			for(DynamicProposition legalProp : legalProps) {
				List<GdlTerm> legalPropBody = (legalProp.getName()).getBody();
				if (inputPropsByBody.containsKey(legalPropBody)) {
					DynamicProposition inputProp = inputPropsByBody.get(legalPropBody);
    				legalInputMap.put(inputProp, legalProp);
    				legalInputMap.put(legalProp, inputProp);
				}
			}
		}
		return legalInputMap;
	}

	*/

	/**
	 * Getter method.
	 *
	 * @return References to every LegalProposition in the PropNet, indexed by
	 *         player name.
	 */
	/*public Map<Role, Set<DynamicProposition>> getLegalPropositions()
	{
		return legalPropositions;
	}
*/

	/**
	 * Returns a representation of the PropNet in .dot format.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("digraph propNet\n{\n");
		for (DynamicComponent component : components){
			sb.append("\t" + component.toString() + "\n");
		}
		sb.append("}");

		return sb.toString();
	}

	/**
     * Outputs the propnet in .dot format to a particular file.
     * This can be viewed with tools like Graphviz and ZGRViewer.
     *
     * @param filename the name of the file to output to
     */
    public void renderToFile(String filename){
        try{
            File f = new File(filename);
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter fout = new OutputStreamWriter(fos, "UTF-8");
            fout.write(toString());
            fout.close();
            fos.close();
        }catch(Exception e){
            GamerLogger.logStackTrace("StateMachine", e);
        }
    }

	/**
	 * Builds an index over the BasePropositions in the PropNet.
	 *
	 * This is done by going over every single-input proposition in the network,
	 * and seeing whether or not its input is a transition, which would mean that
	 * by definition the proposition is a base proposition.
	 *
	 * @return An index over the BasePropositions in the PropNet.
	 */
	/*private Map<GdlSentence, DynamicProposition> recordBasePropositions()
	{
		Map<GdlSentence, DynamicProposition> basePropositions = new HashMap<GdlSentence, DynamicProposition>();
		for (DynamicProposition proposition : propositions) {
		    // Skip all propositions without exactly one input.
		    if (proposition.getInputs().size() != 1)
		        continue;

			DynamicComponent component = proposition.getSingleInput();
			if (component instanceof DynamicTransition) {
				basePropositions.put(proposition.getName(), proposition);
			}
		}

		return basePropositions;
	}*/

	/**
	 * Builds an index over the GoalPropositions in the PropNet.
	 *
	 * This is done by going over every function proposition in the network
     * where the name of the function is "goal", and extracting the name of the
     * role associated with that goal proposition, and then using those role
     * names as keys that map to the goal propositions in the index.
	 *
	 * @return An index over the GoalPropositions in the PropNet.
	 */
	/*private Map<Role, Set<DynamicProposition>> recordGoalPropositions()
	{
		Map<Role, Set<DynamicProposition>> goalPropositions = new HashMap<Role, Set<DynamicProposition>>();
		for (DynamicProposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlRelations.
		    if (!(proposition.getName() instanceof GdlRelation))
		        continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (!relation.getName().getValue().equals("goal"))
			    continue;

			Role theRole = new Role((GdlConstant) relation.get(0));
			if (!goalPropositions.containsKey(theRole)) {
				goalPropositions.put(theRole, new HashSet<DynamicProposition>());
			}
			goalPropositions.get(theRole).add(proposition);
		}

		return goalPropositions;
	}*/

	/**
	 * Returns a reference to the single, unique, InitProposition.
	 *
	 * @return A reference to the single, unique, InitProposition.
	 */
/*	private DynamicProposition recordInitProposition()
	{
		for (DynamicProposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlPropositions.
			if (!(proposition.getName() instanceof GdlProposition))
			    continue;

			GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
			if (constant.getValue().toUpperCase().equals("INIT")) {
				return proposition;
			}
		}
		return null;
	}
*/
	/**
	 * Builds an index over the InputPropositions in the PropNet.
	 *
	 * @return An index over the InputPropositions in the PropNet.
	 */
/*	private Map<GdlSentence, DynamicProposition> recordInputPropositions()
	{
		Map<GdlSentence, DynamicProposition> inputPropositions = new HashMap<GdlSentence, DynamicProposition>();
		for (DynamicProposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlFunctions.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("does")) {
				inputPropositions.put(proposition.getName(), proposition);
			}
		}

		return inputPropositions;
	}
*/
	/**
	 * Builds an index over the LegalPropositions in the PropNet.
	 *
	 * @return An index over the LegalPropositions in the PropNet.
	 */
/*	private Map<Role, Set<DynamicProposition>> recordLegalPropositions()
	{
		Map<Role, Set<DynamicProposition>> legalPropositions = new HashMap<Role, Set<DynamicProposition>>();
		for (DynamicProposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlRelations.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("legal")) {
				GdlConstant name = (GdlConstant) relation.get(0);
				Role r = new Role(name);
				if (!legalPropositions.containsKey(r)) {
					legalPropositions.put(r, new HashSet<DynamicProposition>());
				}
				legalPropositions.get(r).add(proposition);
			}
		}

		return legalPropositions;
	}
*/
	/**
	 * Builds an index over the Propositions in the PropNet.
	 *
	 * @return An index over Propositions in the PropNet.
	 */
/*	private Set<DynamicProposition> recordPropositions()
	{
		Set<DynamicProposition> propositions = new HashSet<DynamicProposition>();
		for (DynamicComponent component : components)
		{
			if (component instanceof DynamicProposition) {
				propositions.add((DynamicProposition) component);
			}
		}
		return propositions;
	}
*/
	/**
	 * Records a reference to the single, unique, TerminalProposition.
	 *
	 * @return A reference to the single, unqiue, TerminalProposition.
	 */
/*	private DynamicProposition recordTerminalProposition()
	{
		for ( DynamicProposition proposition : propositions )
		{
			if ( proposition.getName() instanceof GdlProposition )
			{
				GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
				if ( constant.getValue().equals("terminal") )
				{
					return proposition;
				}
			}
		}

		return null;
	}
*/

    /**
     * Computes the size of the propNet.
     *
     * @return  the size of the propNet, i.e. the number of components in it.
     */
	public int getSize(){
		return this.components.size();
	}

    /**
     * Computes the number of propositions in the propNet.
     *
     * @return  the number of propositions in the propNet.
     */
	public int getNumPropositions(){
		return this.propositions.size();
	}

	/**
	 * Computes the number of AND gates in the propNet.
	 *
	 * @return the number of AND gates in the propNet.
	 */
	public int getNumAnds(){
		int andCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicAnd)
				andCount++;
		}
		return andCount;
	}

	/**
	 * Computes the number of OR gates in the propNet.
	 *
	 * @return the number of OR gates in the propNet.
	 */
	public int getNumOrs(){
		int orCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicOr)
				orCount++;
		}
		return orCount;
	}

	/**
	 * Computes the number of NOT components in the propNet.
	 *
	 * @return the number of NOT components in the propNet.
	 */
	public int getNumNots(){
		int notCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicNot)
				notCount++;
		}
		return notCount;
	}

	/**
	 * Computes the number of links in the propNet.
	 *
	 * @return the number of links in the propNet.
	 */
	public int getNumLinks(){
		int linkCount = 0;
		for(DynamicComponent c : this.components){
			linkCount += c.getOutputs().size();
		}
		return linkCount;
	}

	/**
	 * Computes the number of BASE propositions in the propNet.
	 *
	 * @return the number of BASE propositions in the propNet.
	 */
	public int getNumBases(){
		int basesCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.BASE)
				basesCount++;
		}
		return basesCount;
	}

	/**
	 * Computes the number of INPUT propositions in the propNet.
	 *
	 * @return the number of INPUT propositions in the propNet.
	 */
	public int getNumInputs(){
		int inputsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.INPUT)
				inputsCount++;
		}
		return inputsCount;
	}

	/**
	 * Computes the number of GOAL propositions in the propNet.
	 *
	 * @return the number of GOAL propositions in the propNet.
	 */
	public int getNumGoals(){
		int goalsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.GOAL)
				goalsCount++;
		}
		return goalsCount;
	}

	/**
	 * Computes the number of LEGAL propositions in the propNet.
	 *
	 * @return the number of LEGAL propositions in the propNet.
	 */
	public int getNumLegals(){
		int legalsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.LEGAL)
				legalsCount++;
		}
		return legalsCount;
	}

	/**
	 * Computes the number of OTHER propositions in the propNet.
	 *
	 * @return the number of OTHER propositions in the propNet.
	 */
	public int getNumOthers(){
		int othersCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.OTHER)
				othersCount++;
		}
		return othersCount;
	}

	/**
	 * Computes the number of INIT (not "init") propositions in the propNet.
	 *
	 * @return the number of INIT (not "init") propositions in the propNet.
	 */
	public int getNumInits(){
		int initsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.INIT)
				initsCount++;
		}
		return initsCount;
	}

	/**
	 * Computes the number of TERMINAL propositions in the propNet.
	 *
	 * @return the number of TERMINAL propositions in the propNet.
	 */
	public int getNumTerminals(){
		int terminalsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicProposition && ((DynamicProposition)c).getPropositionType() == PROP_TYPE.TERMINAL)
				terminalsCount++;
		}
		return terminalsCount;
	}

	/**
	 * Computes the number of TRANSITIONS in the propNet.
	 *
	 * @return the number of TRANSITIONS in the propNet.
	 */
	public int getNumTransitions(){
		int transitionsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicTransition)
				transitionsCount++;
		}
		return transitionsCount;
	}

	/**
	 * Computes the number of CONSTANTS in the propNet.
	 *
	 * @return the number of CONSTANTS in the propNet.
	 */
	public int getNumConstants(){
		int constantsCount = 0;
		for(DynamicComponent c : this.components){
			if(c instanceof DynamicConstant)
				constantsCount++;
		}
		return constantsCount;
	}


	/**
	 * Removes a component from the propnet. Be very careful when using
	 * this method, as it is not thread-safe. It is highly recommended
	 * that this method only be used in an optimization period between
	 * the propnet's creation and its initial use, during which it
	 * should only be accessed by a single thread.
	 *
	 * ATTENTION: use this method only before initializing the propnet
	 * state and setting the indices of each component in the state. If
	 * you use it after that, the propNet state will not correspond
	 * anymore to the propnet structure. Whenever you call this method
	 * you will have to recompute the propnet state and set the indices.
	 *
	 * ATTENTION: ANY(!) component can be removed from the propnet. Pay
	 * attention not to remove a component that is fundamental for the
	 * the correct functioning of your implementation of the propnet
	 * state machine (i.e. don't remove the terminal proposition if it
	 * is the one that gets checked at for every state to know if the
	 * state is terminal, or don't remove the INIT proposition if your
	 * implementation of the state machine uses it to determine which
	 * base propositions are true in the initial state).
	 */
	public void removeComponent(DynamicComponent c) {

		boolean found = components.remove(c);

		//Go through all the collections it could appear in
		if(c instanceof DynamicProposition) {
			DynamicProposition p = (DynamicProposition) c;

			Role r;

			switch(p.getPropositionType()){
			case BASE:
				this.basePropositions.remove(p);
				break;
			case INPUT:
				this.inputPropositions.remove(p);
				// Find the role for this input
				r = new Role((GdlConstant) ((GdlRelation) p.getName()).get(0));
				this.inputsPerRole.get(r).remove(p);
				break;
			case LEGAL:
				this.legalPropositions.remove(p);
				// Find the role for this legal
				r = new Role((GdlConstant) ((GdlRelation) p.getName()).get(0));
				this.legalsPerRole.get(r).remove(p);
				break;
			case GOAL:
				// Find the role for this goal
				r = new Role((GdlConstant) ((GdlRelation) p.getName()).get(0));
				this.goalsPerRole.get(r).remove(p);
				break;
			case TERMINAL:
				if(this.terminalProposition == p){
					this.terminalProposition = null;
				}
				break;
			case INIT:
				/*if(this.initProposition == p){
					this.initProposition = null;
				}*/
				break;
			default:
				break;
			}

			this.propositions.remove(p);
		}else if(c instanceof DynamicConstant){
			if(this.trueConstant == c){
				this.trueConstant = null;
			}else if(this.falseConstant == c){
				this.falseConstant = null;
			}
		}else if(c instanceof DynamicAnd || c instanceof DynamicOr){
			if(found){
				this.andOrGatesNumber--;
			}
		}

		//Remove all the local links to the component
		for(DynamicComponent parent : c.getInputs())
			parent.removeOutput(c);
		for(DynamicComponent child : c.getOutputs())
			child.removeInput(c);
		//These are actually unnecessary...
		//c.removeAllInputs();
		//c.removeAllOutputs();
	}

	/**
	 * This method transforms the given proposition into an OTHER proposition.
	 * This means that it will change its type to OTHER and, if needed, remove
	 * it from the collections containing that type of proposition (if any exists).
	 *
	 * @param p
	 */
	public void convertToOther(DynamicProposition p){
		Role r;

		switch(p.getPropositionType()){
		case BASE:
			this.basePropositions.remove(p);
			break;
		case INPUT:
			this.inputPropositions.remove(p);
			// Find the role for this input
			r = new Role((GdlConstant) ((GdlRelation) p.getName()).get(0));
			this.inputsPerRole.get(r).remove(p);
			break;
		/*case LEGAL:
			this.legalPropositions.remove(p);
			// Find the role for this legal
			r = new Role((GdlConstant) ((GdlRelation) p.getName()).get(0));
			this.legalsPerRole.get(r).remove(p);
			break;
		case GOAL:
			// Find the role for this goal
			r = new Role((GdlConstant) ((GdlRelation) p.getName()).get(0));
			this.goalsPerRole.get(r).remove(p);
			break;
		case TERMINAL:
			if(this.terminalProposition == p){
				this.terminalProposition = null;
			}
			break;
		case INIT:
			//if(this.initProposition == p){
			//	this.initProposition = null;
			//}
			break;
			*/
		default:
			throw new RuntimeException("Trying to convert to an OTHER proposition a " + p.getPropositionType().toString() + " proposition. This type cannot be converted, if necessary, extend the code to do so!");
		}

		p.setPropositionType(PROP_TYPE.OTHER);
	}
}