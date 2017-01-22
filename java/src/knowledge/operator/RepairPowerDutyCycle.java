/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import eoss.problem.evaluation.ArchitectureEvaluator;
import eoss.problem.evaluation.RequirementMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jess.Fact;
import jess.JessException;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * Checks that the power duty cycle of the spacecraft fall within the
 * acceptable bounds. Multiple changes may occur within a spacecraft to ensure
 * the threshold bound. Random instruments are removed until acceptable bounds
 * are met. User can define whether to change one or multiple spacecraft
 *
 * @author nozomihitomi
 */
public class RepairPowerDutyCycle implements Variation {

    /**
     * The power duty cycle that a spacecraft must be at or higher
     */
    private final double threshold;

    /**
     * The number of modifications that this operators is allowed to make
     */
    private final int numModifications;

    private final ParallelPRNG pprng;

    private final ArchitectureEvaluator eval;

    public RepairPowerDutyCycle(String path, double threshold, int numModifications) {
        this.eval = new ArchitectureEvaluator(path, RequirementMode.FUZZYCASE, false, true, null);
        this.threshold = threshold;
        this.numModifications = numModifications;
        this.pprng = new ParallelPRNG();
    }

    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        child.setMissions();
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        ArrayList<Mission> candidateMission = new ArrayList();
        try {
            for (String name : child.getMissionNames()) {
                Spacecraft s = child.getMission(name).getSpacecraft().keySet().iterator().next();
                
                if (!checkDataRate(child.getMission(name), threshold) && !s.getPaylaod().isEmpty()) {
                    candidateMission.add(child.getMission(name));
                }
            }
            for (int i = 0; i < numModifications; i++) {
                if (i > child.getMissionNames().size()|| i >= candidateMission.size()) {
                    break;
                }
                int missionIndex = pprng.nextInt(candidateMission.size());
                Mission mission = candidateMission.get(missionIndex);
                while (!checkDataRate(mission, threshold)) {
                    ArrayList<Integer> instruments = copy.getInstrumentsInSpacecraft(missionIndex);
                    if(instruments.isEmpty()){
                        break;
                    }else{
                        copy.removeInstrumentFromSpacecraft(instruments.get(pprng.nextInt(instruments.size())), missionIndex);
                    }
                }
                candidateMission.remove(missionIndex);
            }
        } catch (JessException ex) {
            Logger.getLogger(RepairDataDutyCycle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new Solution[]{copy};
    }

    /**
     * Computes the power duty cycle from the beginning of life power required by the satellite
     *
     * @param bolPower  beginning of life power
     * @return
     */
    private double computePowerDutyCycle(double bolPower) {
        return 10000./ bolPower;
    }

    /**
     * Check to see if the power duty cycle of this spacecraft meets the
     * threshold
     *
     * @param arch
     * @param orbit
     * @param threshold
     * @return true if the power duty cycle is at or above the threshold.
     * Otherwise false.
     */
    private boolean checkDataRate(Mission mission, double threshold) throws JessException {
        Collection<Mission> missions = new ArrayList<>();
        missions.add(mission);
        this.eval.designSpacecraft(missions);
        Orbit o = mission.getSpacecraft().values().iterator().next();
        Collection<Fact> missionFacts = eval.makeQuery(String.format("MANIFEST::Mission (Name %s:%s)", mission.getName(), o.getName()));
        Fact fact = missionFacts.iterator().next();
        return computePowerDutyCycle(Double.parseDouble(fact.getSlotValue("satellite-BOL-power#").toString())) >= threshold;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
