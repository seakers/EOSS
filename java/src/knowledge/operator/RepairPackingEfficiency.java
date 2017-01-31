/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.Mission;
import eoss.problem.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import java.util.ArrayList;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * Checks that the data rate duty cycle of the spacecraft fall within the
 * acceptable bounds. If not one or more random instruments are removed to try
 * to alleviate the situation. User can define whether to change one or multiple
 * spacecraft.
 *
 * @author nozomihitomi
 */
public class RepairPackingEfficiency implements Variation {
    
    /**
     * The duty cycle that a spacecraft must be at or higher
     */
    private final double threshold;

    /**
     * The number of instruments to remove from each satellite that does not
     * meet the threshold
     */
    private final int xInstruments;

    /**
     * The number of satellites to modify
     */
    private final int ySatellites;

    private final ParallelPRNG pprng;

    public RepairPackingEfficiency(double threshold, int xInstruments, int ySatellites) {
        this.xInstruments = xInstruments;
        this.ySatellites = ySatellites;
        this.pprng = new ParallelPRNG();
        this.threshold = threshold;
    }

    /**
     * removes x number of instruments from the payload of y number of satellite
     * that does not meet the data rate duty cycle threshold
     *
     * @param sltns
     * @return
     */
    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        ArrayList<Mission> candidateMission = new ArrayList();
        for (String name : child.getMissionNames()) {
            Spacecraft s = child.getMission(name).getSpacecraft().keySet().iterator().next();

            if (Double.parseDouble(s.getProperty("packingEfficiency")) < threshold
                    && !s.getPaylaod().isEmpty()) {
                candidateMission.add(child.getMission(name));
            }
        }
        for (int i = 0; i < ySatellites; i++) {
            if (i > copy.getMissionNames().size() || i >= candidateMission.size()) {
                break;
            }
            int missionIndex = pprng.nextInt(candidateMission.size());
            Mission m =  candidateMission.get(missionIndex);
            for (int j = 0; j < xInstruments; j++) {
                ArrayList<Integer> instruments = copy.getInstrumentsInSpacecraft(m);
                if (instruments.isEmpty()) {
                    break;
                } else {
                    copy.removeInstrumentFromSpacecraft(instruments.get(pprng.nextInt(instruments.size())), m);
                }
            }
            candidateMission.remove(missionIndex);
        }
        return new Solution[]{copy};
    }

    @Override
    public int getArity() {
        return 1;
    }

}
