/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import seakers.aos.operator.CheckParents;
import eoss.problem.Mission;
import eoss.spacecraft.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import eoss.spacecraft.SpacecraftDesigner;
import java.util.ArrayList;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * Checks that the dry mass of the spacecraft fall within the acceptable bounds.
 * If not one or more random instruments are removed to try to alleviate
 * the situation. User can define whether to change one or multiple spacecraft
 *
 * @author nozomihitomi
 */
public class RepairMass implements Variation, CheckParents {

    /**
     * The dry mass of the spacecraft must be at or lower
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
    
    /**
     * Designs the spacecraft
     */
    private final SpacecraftDesigner scDesigner;

    public RepairMass(double threshold, int xInstruments, int ySatellites) {
        this.scDesigner = new SpacecraftDesigner();
        this.threshold = threshold;
        this.xInstruments = xInstruments;
        this.ySatellites = ySatellites;
    }

    /**
     * removes x number of instruments from the payload of y number of satellite
     * that exceeds the mass threshold
     *
     * @param sltns
     * @return
     */
    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        copy.setMissions();
        
        ArrayList<Mission> candidateMission = new ArrayList<>();
        for (Mission m : copy.getMissions()) {
            scDesigner.designSpacecraft(m);
            Spacecraft s = m.getSpacecraft().keySet().iterator().next();
            if (s.getWetMass() > threshold && !s.getPayload().isEmpty()) {
                candidateMission.add(m);
            }
        }
        for (int i = 0; i < ySatellites; i++) {
            if (i > copy.getMissionNames().size() || i >= candidateMission.size()) {
                break;
            }
            int missionIndex = PRNG.nextInt(candidateMission.size());
            Mission m =  candidateMission.get(missionIndex);
            for (int j = 0; j < xInstruments; j++) {
                ArrayList<Integer> instruments = copy.getInstrumentsInSpacecraft(m);
                if (instruments.isEmpty()) {
                    break;
                } else {
                    copy.removeInstrumentFromSpacecraft(instruments.get(PRNG.nextInt(instruments.size())), m);
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
    
    private boolean checkSpacecraft(Spacecraft s) {
        return s.getWetMass() > threshold && !s.getPayload().isEmpty();
    }

    @Override
    public boolean check(Solution[] sltns) {
        for (Solution sol : sltns) {
            InstrumentAssignmentArchitecture2 arch = (InstrumentAssignmentArchitecture2) sol;
            for (Mission m : arch.getMissions()) {
                Spacecraft s = m.getSpacecraft().keySet().iterator().next();
                if (checkSpacecraft(s)) {
                    return true;
                }
            }
        }
        return false;
    }


}
