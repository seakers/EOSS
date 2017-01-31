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
 * This operator adds an instrument if there is a missed opportunity for a
 * synergistic pair
 *
 * @author nozomihitomi
 */
public class RepairSynergy implements Variation {

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

    public RepairSynergy(int xInstruments, int ySatellites) {
        this.xInstruments = xInstruments;
        this.ySatellites = ySatellites;
        this.pprng = new ParallelPRNG();

        this.map = new HashMap();
        map.put("ACE_ORCA", new String[]{"DESD_LID", "GACM_VIS", "ACE_POL", "HYSP_TIR", "ACE_LID"});
        map.put("DESD_LID", new String[]{"ACE_ORCA", "ACE_LID", "ACE_POL"});
        map.put("GACM_VIS", new String[]{"ACE_ORCA", "ACE_LID"});
        map.put("HYSP_TIR", new String[]{"ACE_ORCA", "POSTEPS_IRS"});
        map.put("ACE_POL", new String[]{"ACE_ORCA", "DESD_LID"});
        map.put("ACE_LID", new String[]{"ACE_ORCA", "CNES_KaRIN", "DESD_LID", "GACM_VIS"});
        map.put("POSTEPS_IRS", new String[]{"HYSP_TIR"});
        map.put("CNES_KaRIN", new String[]{"ACE_LID"});
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
        child.setMissions();
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        HashMap<Mission, ArrayList<Instrument>> instrumentsToAdd = new HashMap();
        for (String name : child.getMissionNames()) {
            ArrayList<Instrument> instrumentsAdd = new ArrayList<>();
            HashMap<Spacecraft, Orbit> missionSpacecraft = child.getMission(name).getSpacecraft();
            for (Spacecraft s : missionSpacecraft.keySet()) {
                HashMap<String, Instrument> instrumentSet = new HashMap<>();
                for (Instrument inst : s.getPaylaod()) {
                    instrumentSet.put(inst.getName(), inst);
                }

                for (String instName : instrumentSet.keySet()) {
                    if (map.containsKey(instName)) {
                        for (String instPairName : map.get(instName)) {
                            if (!instrumentSet.containsKey(instPairName)) {
                                instrumentsAdd.add(EOSSDatabase.getInstrument(instPairName));
                            }
                        }
                    }
                }
            }
            instrumentsToAdd.put(copy.getMission(name), instrumentsAdd);
        }

        //repair the architecture
        ArrayList<Mission> candidateMissions = new ArrayList<>(instrumentsToAdd.keySet());
        for (int i = 0; i < ySatellites; i++) {
            if (i > copy.getMissionNames().size() || i >= candidateMissions.size()) {
                break;
            }
            int missionIndex = pprng.nextInt(candidateMissions.size());
            Mission m = candidateMissions.get(missionIndex);
            for (int j = 0; j < xInstruments; j++) {
                ArrayList<Instrument> instruments = instrumentsToAdd.get(m);
                if (instruments.isEmpty()) {
                    break;
                } else {
                    int instIndex = pprng.nextInt(instruments.size());
                    Instrument inst = instruments.get(instIndex);
                    copy.addInstrumentToSpacecraft(EOSSDatabase.findInstrumentIndex(inst), m);
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
