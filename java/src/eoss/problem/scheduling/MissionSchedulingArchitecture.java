/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import eoss.problem.Mission;
import architecture.Architecture;
import architecture.pattern.ArchitecturalDecision;
import architecture.pattern.Permuting;
import java.util.Arrays;
import java.util.HashMap;
import org.moeaframework.core.Solution;
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
     * The permutation that defines the schedule
     */
    private final Permuting permuting;
    
    /**
     * The launch dates of each mission
     */
    private HashMap<Mission,AbsoluteDate> launchDates;

    //Constructors
    /**
     * Creates an empty schedule
     *
     * @param numberMissions the number of missions to schedule
     * @param numberOfObjectives the number of objectives to consider in this
     * problem
     */
    public MissionSchedulingArchitecture(int numberMissions, int numberOfObjectives) {
        super(Arrays.asList(new ArchitecturalDecision[]{
            new Permuting(numberMissions)}), numberOfObjectives);

        this.permuting = (Permuting) this.getVariable(0);
    }

    /**
     * makes a copy solution from the input solution
     *
     * @param solution
     */
    public MissionSchedulingArchitecture(Solution solution) {
        super(solution);
        if (solution instanceof MissionSchedulingArchitecture) {
            this.permuting = (Permuting) this.getVariable(0);
        } else {
            throw new IllegalArgumentException("Expected type SchedulingArchitecture class. Found " + solution.getClass().getSimpleName());
        }
    }

    /**
     * Get the number of missions this problem is trying to schedule
     *
     * @return the number of missions this problem is trying to schedule
     */
    public int getNumberOfMissions() {
        return permuting.getLength();
    }

    /**
     * Gets the sequence of the launch order
     *
     * @return the sequence of the launch order
     */
    public int[] getSequence() {
        return permuting.getSequence();
    }
    
    /**
     * Gets the launch dates for each mission
     * @return 
     */
    public HashMap<Mission, AbsoluteDate> getLaunchDates() {
        return launchDates;
    }
    
    /**
     * Sets the launch dates for each mission
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
