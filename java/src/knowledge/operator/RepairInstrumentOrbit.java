/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.Instrument;
import eoss.problem.Orbit;
import eoss.spacecraft.Spacecraft;
import java.util.Collection;
import java.util.HashSet;

/**
 * This operator moves an instruments from one spacecraft to another if the
 * instruments are not well suited at the orbit they are assigned to
 *
 * @author nozomihitomi
 */
public class RepairInstrumentOrbit extends AbstractInstrumentMove {

    public RepairInstrumentOrbit(int nChange) {
        super(nChange);
    }

    @Override
    protected Collection<Instrument> checkThisSpacecraft(Spacecraft s, Orbit o) {
        HashSet<Instrument> remove = new HashSet<>();
        for (Instrument inst : s.getPayload()) {
            if(chemistryInPM(inst, o) || passiveInDD(inst, o) || slantLowAltitude(inst, o)){
                remove.add(inst);
            }
        }
        return remove;
    }

    @Override
    protected boolean checkOtherSpacecraft(Spacecraft s, Orbit o, Instrument inst) {
        return chemistryInPM(inst, o) || passiveInDD(inst, o) || slantLowAltitude(inst, o);
    }

    @Override
    protected boolean addToCurrentSpacecraft() {
        //incompatible instruments should be removed from the antecedent spacecraft
        return false;
    }

    /**
     * This method checks if a given instrument is an atmospheric chemistry
     * instrument in a non-afternoon orbit.
     *
     * @return true if the instruments take atmospheric chemistry instruments
     * and the given orbit is a non-afternoon orbit.
     */
    private boolean chemistryInPM(Instrument inst, Orbit o) {
        if (!o.getRAAN().equals("PM")) {
            String concept = inst.getProperty("Concept");
            if (concept.contains("chemistry")) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method checks if a given mission carries an passive, optical
     * instrument in a dawn-dusk orbit
     *
     * @return true if instrument is passive and optical and orbit is a dawn
     * dusk orbit
     */
    private boolean passiveInDD(Instrument inst, Orbit o) {
        return o.getRAAN().equals("DD") && inst.getProperty("Illumination").equals("Passive");
    }

    /**
     * This method checks if a given mission carries an instrument that views in
     * a non-nadir direction and flies in a low altitude orbit (<= 400km)
     *
     * @return ture if instrument views in a non-nadir direction and flies in a
     * low altitude orbit (<= 400km)
     */
    private boolean slantLowAltitude(Instrument inst, Orbit o) {
        return o.getAltitude() <= 400. && inst.getProperty("Geometry").equals("slant");
    }

}
