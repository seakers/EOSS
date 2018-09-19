/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Orbit;
import eoss.spacecraft.Spacecraft;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This operator removes an instrument and places it into another satellite if
 * two instruments are known to significantly increase engineering costs
 *
 * @author nozomihitomi
 */
public class RepairInterference extends AbstractInstrumentMove {

    /**
     * synergistic instrument pairs
     */
    private final HashMap<String, String[]> mapNames;

    /**
     * synergistic instrument pairs
     */
    private final HashMap<Instrument, Instrument[]> map;

    public RepairInterference(int nChanges) {
        super(nChanges);

        this.mapNames = new HashMap<>();
        mapNames.put("ACE_LID", new String[]{"ACE_CPR", "DESD_SAR", "CLAR_ERB", "GACM_SWIR"});
        mapNames.put("ACE_CPR", new String[]{"ACE_LID", "DESD_SAR", "CNES_KaRIN", "CLAR_ERB", "ACE_POL", "ACE_ORCA", "GACM_SWIR"});
        mapNames.put("DESD_SAR", new String[]{"ACE_LID", "ACE_CPR"});
        mapNames.put("CLAR_ERB", new String[]{"ACE_LID", "ACE_CPR"});
        mapNames.put("CNES_KaRIN", new String[]{"ACE_CPR"});
        mapNames.put("ACE_POL", new String[]{"ACE_CPR"});
        mapNames.put("ACE_ORCA", new String[]{"ACE_CPR"});
        mapNames.put("GACM_SWIR", new String[]{"ACE_LID", "ACE_CPR"});

        //get names into instrument objects
        this.map = new HashMap<>();
        for (String instNameKey : mapNames.keySet()) {
            Instrument[] instArray = new Instrument[mapNames.get(instNameKey).length];
            int count = 0;
            for (String instNameValue : mapNames.get(instNameKey)) {
                instArray[count] = EOSSDatabase.getInstrument(instNameValue);
                count++;
            }
            map.put(EOSSDatabase.getInstrument(instNameKey), instArray);
        }
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected Collection<Instrument> checkThisSpacecraft(Spacecraft s, Orbit o) {
         HashSet<Instrument> out = new HashSet<>();
        //check for potential synergies
        for (Instrument inst : s.getPayload()) {
            if (map.containsKey(inst)) {
                for (Instrument interferingPair : map.get(inst)) {
                    if (s.getPayload().contains(interferingPair)) {
                        out.add(interferingPair);
                    }
                }
            }
        }
        return out;
    }

    /**
     * {@inheritDoc} The other spacecraft is a candidate for receiving an
     * instrument if it doesn't already have it in its payload
     *
     */
    @Override
    protected boolean checkOtherSpacecraft(Spacecraft s, Orbit o, Instrument inst) {
        return !(s.getPayload().contains(inst));
    }

    @Override
    protected boolean addToCurrentSpacecraft() {
        return false;
    }

}
