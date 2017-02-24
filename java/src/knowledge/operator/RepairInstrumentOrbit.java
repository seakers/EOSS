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
import eoss.spacecraft.Spacecraft;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.moeaframework.core.ParallelPRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 * This operator removes instruments if they are not well suited at the orbit
 * they are assigned to
 *
 * @author nozomihitomi
 */
public class RepairInstrumentOrbit implements Variation {

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

    public RepairInstrumentOrbit(int xInstruments, int ySatellites) {
        this.xInstruments = xInstruments;
        this.ySatellites = ySatellites;
        this.pprng = new ParallelPRNG();
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
        HashMap<Mission, ArrayList<Instrument>> instrumentsToRemove = new HashMap();
        for (String name : child.getMissionNames()) {
            ArrayList<Instrument> remove = new ArrayList<>();
            HashMap<Spacecraft, Orbit> missionSpacecraft = child.getMission(name).getSpacecraft();
            for (Spacecraft s : missionSpacecraft.keySet()) {
                remove.addAll(chemistryInPM(s, missionSpacecraft.get(s)));
                remove.addAll(passiveInDD(s, missionSpacecraft.get(s)));
                remove.addAll(slantLowAltitude(s, missionSpacecraft.get(s)));
            }
            instrumentsToRemove.put(copy.getMission(name), remove);
        }
        
        //repair the architecture
        ArrayList<Mission> candidateMissions = new ArrayList<>(instrumentsToRemove.keySet());
        for (int i = 0; i < ySatellites; i++) {
            if (i > copy.getMissionNames().size() || i >= candidateMissions.size()) {
                break;
            }
            int missionIndex = pprng.nextInt(candidateMissions.size());
            Mission m =  candidateMissions.get(missionIndex);
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

    /**
     * This method checks if a given mission carries an atmospheric chemistry
     * instrument in a non-afternoon orbit.
     *
     * @return a collection of instruments hosted by the given spacecraft that
     * take atmospheric chemistry instruments in the given orbit if it is a
     * non-afternoon orbit.
     */
    private Collection<Instrument> chemistryInPM(Spacecraft s, Orbit o) {
        ArrayList<Instrument> out = new ArrayList<>();
        if (!o.getRAAN().equals("PM")) {
            for (Instrument inst : s.getPayload()) {
                String concept = inst.getProperty("Concept");
                if (concept.contains("chemistry")) {
                    out.add(inst);
                }
            }
        }
        return out;
    }

    /**
     * This method checks if a given mission carries an passive, optical
     * instrument in a dawn-dusk orbit
     *
     * @return a collection of instruments hosted by the given spacecraft that
     * are both passive and optical in a dawn dusk orbit
     */
    private Collection<Instrument> passiveInDD(Spacecraft s, Orbit o) {
        ArrayList<Instrument> out = new ArrayList<>();
        if (o.getRAAN().equals("DD")) {
            for (Instrument inst : s.getPayload()) {
                if (inst.getProperty("Illumination").equals("Passive")) {
                    out.add(inst);
                }
            }
        }
        return out;
    }

    /**
     * This method checks if a given mission carries an instrument that views in
     * a non-nadir direction and flies in a low altitude orbit (<= 400km)
     *
     * @return a collection of instruments hosted by the given spacecraft that
     * views in a non-nadir direction and flies in a low altitude orbit (<=
     * 400km)
     */
    private Collection<Instrument> slantLowAltitude(Spacecraft s, Orbit o) {
        ArrayList<Instrument> out = new ArrayList<>();
        if (o.getAltitude() <= 400.) {
            for (Instrument inst : s.getPayload()) {
                if (inst.getProperty("Geometry").equals("slant")) {
                    out.add(inst);
                }
            }
        }
        return out;
    }

    @Override
    public int getArity() {
        return 1;
    }

}
