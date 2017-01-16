/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import eoss.problem.Mission;
import architecture.Architecture;
import architecture.pattern.ArchitecturalDecision;
import architecture.pattern.Permuting;
import java.util.ArrayList;
import java.util.HashMap;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.IntegerVariable;
import org.orekit.time.AbsoluteDate;

/**
 * This class creates a solution for the problem where the goal is to schedule a
 * given number of missions.
 *
 * @author nozomi
 */
//import jess.*;
public class MissionSchedulingArchitecture extends Architecture {

    private static final long serialVersionUID = -4409436112554811170L;

    /**
     * Tag used for the permuting decision
     */
    private static final String permTag = "perm";

    /**
     * The launch dates of each mission
     */
    private HashMap<Mission, AbsoluteDate> launchDates;

    //Constructors
    /**
     * Creates an empty schedule
     *
     * @param numberMissions the number of missions to schedule
     * @param numberOfObjectives the number of objectives to consider in this
     * problem
     */
    public MissionSchedulingArchitecture(int numberMissions, int numberOfObjectives) {
        super(numberOfObjectives, 0, createDecisions(numberMissions));
    }

    private static ArrayList<ArchitecturalDecision> createDecisions(int numberMissions) {
        ArrayList<ArchitecturalDecision> out = new ArrayList<>();
        out.add(new Permuting(numberMissions, permTag));
        return out;
    }

    /**
     * makes a copy solution from the input solution
     *
     * @param solution
     */
    private MissionSchedulingArchitecture(Solution solution) {
        super(solution);
        launchDates = new HashMap<>();
    }

    /**
     * Get the number of missions this problem is trying to schedule
     *
     * @return the number of missions this problem is trying to schedule
     */
    public int getNumberOfMissions() {
        return this.getDecision(permTag).getNumberOfVariables();
    }

    /**
     * Gets the sequence of the launch order
     *
     * @return the sequence of the launch order
     */
    public int[] getSequence() {
        int startIndex = this.getDecisionIndex(permTag);
        int[] out = new int[getNumberOfMissions()];
        for(int i=0; i< getNumberOfMissions(); i++){
            out[i] = ((IntegerVariable)this.getVariable(startIndex + i)).getValue();
        }
        return out;
    }

    /**
     * Gets the launch dates for each mission
     *
     * @return
     */
    public HashMap<Mission, AbsoluteDate> getLaunchDates() {
        return launchDates;
    }

    /**
     * Sets the launch dates for each mission
     *
     * @param launchDates the launch dates for each mission
     */
    public void setLaunchDates(HashMap<Mission, AbsoluteDate> launchDates) {
        this.launchDates = launchDates;
    }

    @Override
    public Solution copy() {
        return new MissionSchedulingArchitecture(this);
    }

}
