/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import java.util.ArrayList;
import java.util.HashMap;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * This operator removes an instrument if two instruments are known to
 * significantly increase engineering costs
 *
 * @author nozomihitomi
 */
public class RepairInterference implements Variation {

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
     * synergistic instrument pairs
     */
    private final HashMap<String, String[]> map;

    private final ParallelPRNG pprng;

    public RepairInterference(int xInstruments, int ySatellites) {
        this.xInstruments = xInstruments;
        this.ySatellites = ySatellites;
        this.pprng = new ParallelPRNG();

        this.map = new HashMap();
        map.put("ACE_LID", new String[]{"ACE_CPR", "DESD_SAR", "CLAR_ERB", "GACM_SWIR"});
        map.put("ACE_CPR", new String[]{"ACE_LID", "DESD_SAR", "CNES_KaRIN", "CLAR_ERB", "ACE_POL", "ACE_ORCA", "GACM_SWIR"});
        map.put("DESD_SAR", new String[]{"ACE_LID", "ACE_CPR"});
        map.put("CLAR_ERB", new String[]{"ACE_LID", "ACE_CPR"});
        map.put("CNES_KaRIN", new String[]{"ACE_CPR"});
        map.put("ACE_POL", new String[]{"ACE_CPR"});
        map.put("ACE_ORCA", new String[]{"ACE_CPR"});
        map.put("GACM_SWIR", new String[]{"ACE_LID", "ACE_CPR"});
    }

    /**
     * removes x number of instruments from the payload of y number of satellite
     * that are not supposed to be in a certain orbit
     *
     * @param sltns
     * @return
     */
    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        HashMap<Mission, ArrayList<Instrument>> instrumentsToRemove = new HashMap();
        for (String name : child.getMissionNames()) {
            ArrayList<Instrument> instrumentsRemove = new ArrayList<>();
            HashMap<Spacecraft, Orbit> missionSpacecraft = child.getMission(name).getSpacecraft();
            for (Spacecraft s : missionSpacecraft.keySet()) {
                HashMap<String, Instrument> instrumentSet = new HashMap<>();
                for (Instrument inst : s.getPaylaod()) {
                    instrumentSet.put(inst.getName(), inst);
                }

                for (String instName : instrumentSet.keySet()) {
                    if (map.containsKey(instName)) {
                        for (String instPairName : map.get(instName)) {
                            if (instrumentSet.containsKey(instPairName)) {
                                instrumentsRemove.add(EOSSDatabase.getInstrument(instPairName));
                            }
                        }
                    }
                }
            }
            instrumentsToRemove.put(copy.getMission(name), instrumentsRemove);
        }

        //repair the architecture
        ArrayList<Mission> candidateMissions = new ArrayList<>(instrumentsToRemove.keySet());
        for (int i = 0; i < ySatellites; i++) {
            if (i > copy.getMissionNames().size() || i >= candidateMissions.size()) {
                break;
            }
            int missionIndex = pprng.nextInt(candidateMissions.size());
            Mission m = candidateMissions.get(missionIndex);
            for (int j = 0; j < xInstruments; j++) {
                ArrayList<Instrument> instruments = instrumentsToRemove.get(m);
                if (instruments.isEmpty()) {
                    break;
                } else {
                    int instIndex = pprng.nextInt(instruments.size());
                    Instrument inst = instruments.get(instIndex);
                    copy.removeInstrumentFromSpacecraft(EOSSDatabase.findInstrumentIndex(inst), m);
                    instruments.remove(instIndex);
                }
            }
            candidateMissions.remove(missionIndex);
        }
        return new Solution[]{copy};
    }

    @Override
    public int getArity() {
        return 1;
    }

}
