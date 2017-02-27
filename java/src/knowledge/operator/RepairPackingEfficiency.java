/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.EOSSDatabase;
import eoss.problem.LaunchVehicle;
import eoss.problem.Mission;
import eoss.spacecraft.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import eoss.spacecraft.SpacecraftDesigner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    
    /**
     * Designs the spacecraft
     */
    private final SpacecraftDesigner scDesigner;


    private final ParallelPRNG pprng;

    public RepairPackingEfficiency(double threshold, int xInstruments, int ySatellites) {
        this.scDesigner = new SpacecraftDesigner();
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
        child.setMissions();
        ArrayList<Mission> missions = new ArrayList();
        for (String name : child.getMissionNames()) {
            missions.add(child.getMission(name));
        }
        HashMap<Collection<Spacecraft>, LaunchVehicle> lvSelection = LaunchVehicle.select(missions);

        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        ArrayList<Mission> candidateMission = new ArrayList();
        for (Mission m : missions) {
            scDesigner.designSpacecraft(m);
            Spacecraft s = m.getSpacecraft().keySet().iterator().next();

            //compute packing efficiency
            for (Collection<Spacecraft> group : lvSelection.keySet()) {
                if (group.contains(s)) {
                    double totalVolume = 0;
                    for (Spacecraft sTemp : group) {
                        double volume = 1.0;
                        for (double d : sTemp.getDimensions()) {
                            volume *= d;
                        }
                        totalVolume += volume;
                    }
                    double packingEfficiency = totalVolume / lvSelection.get(group).getVolume();

                    if (packingEfficiency < threshold && !s.getPayload().isEmpty()) {
                        candidateMission.add(m);
                    }
                }
            }
        }

        for (int i = 0; i < ySatellites; i++) {
            if (i > copy.getMissionNames().size() || i >= candidateMission.size()) {
                break;
            }
            int missionIndex = pprng.nextInt(candidateMission.size());
            Mission m = candidateMission.get(missionIndex);
            for (int j = 0; j < xInstruments; j++) {
                if (copy.getInstrumentsInSpacecraft(m).size() == EOSSDatabase.getNumberOfInstruments()) {
                    //cannot add any more instruments
                    break;
                } else {
                    while (true) {
                        //try adding instruments until spacecraft is full or there is a change
                        if (copy.addInstrumentToSpacecraft(
                                pprng.nextInt(EOSSDatabase.getNumberOfInstruments()), m)) {
                            break;
                        }
                    }
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
