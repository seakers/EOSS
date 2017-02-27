/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.spacecraft.Spacecraft;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * This operator adds an instrument from another spacecraft if there is a missed
 * opportunity for a synergistic pair
 *
 * @author nozomihitomi
 */
public class RepairSynergy extends InstrumentSwap {

    /**
     * Names of synergistic instrument pairs
     */
    private final HashMap<String, String[]> mapNames;

    /**
     * synergistic instrument pairs
     */
    private final HashMap<Instrument, Instrument[]> map;

    public RepairSynergy(int nChanges) {
        super(nChanges);

        this.mapNames = new HashMap();
        mapNames.put("ACE_ORCA", new String[]{"DESD_LID", "GACM_VIS", "ACE_POL", "HYSP_TIR", "ACE_LID"});
        mapNames.put("DESD_LID", new String[]{"ACE_ORCA", "ACE_LID", "ACE_POL"});
        mapNames.put("GACM_VIS", new String[]{"ACE_ORCA", "ACE_LID"});
        mapNames.put("HYSP_TIR", new String[]{"ACE_ORCA", "POSTEPS_IRS"});
        mapNames.put("ACE_POL", new String[]{"ACE_ORCA", "DESD_LID"});
        mapNames.put("ACE_LID", new String[]{"ACE_ORCA", "CNES_KaRIN", "DESD_LID", "GACM_VIS"});
        mapNames.put("POSTEPS_IRS", new String[]{"HYSP_TIR"});
        mapNames.put("CNES_KaRIN", new String[]{"ACE_LID"});

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
    protected Collection<Instrument> checkThisSpacecraft(Spacecraft s) {
        ArrayList<Instrument> out = new ArrayList<>();
        //check for potential synergies
        for (Instrument inst : s.getPayload()) {
            if (map.containsKey(inst)) {
                for (Instrument synergyPair : map.get(inst)) {
                    if (!s.getPayload().contains(synergyPair)) {
                        out.add(inst);
                    }
                }
            }
        }
        return out;
    }

    @Override
    protected boolean checkOtherSpacecraft(Spacecraft s, Instrument inst) {
        return s.getPayload().contains(inst);
    }

    @Override
    protected boolean addToCurrentSpacecraft() {
        return true;
    }

}
