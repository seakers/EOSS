/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment;

import architecture.Architecture;
import architecture.pattern.ArchitecturalDecision;
import architecture.pattern.Assigning;
import architecture.pattern.Combining;
import architecture.pattern.DownSelecting;
import architecture.util.ValueTree;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import org.moeaframework.core.Solution;

/**
 * This class creates a solution for the problem consisting of an assigning
 * pattern of instruments to spacecraft and a combining pattern for each
 * spacecraft to determine its orbit. Assigning instruments from the left hand
 * side to spacecraft on the right hand side
 *
 * @author nozomi
 */
//import jess.*;
public class InstrumentAssignmentArchitecture2 extends Architecture {

    private static final long serialVersionUID = 7399750399460662005L;

    /**
     * Tag used for the downselect decisions. Each spacecraft will have a
     * different tag
     */
    private static String downTag = "inst_";

    /**
     * Tag used for the combining decision. Each spacecraft will have a
     * different tag
     */
    private static String combineTag = "spacecraft_";

    /**
     * The missions represented by this architecture
     */
    private final HashMap<String, Mission> missions;

    /**
     * The maximum number of spacecraft allowed
     */
    private final int numberOfSpacecraft;

    /**
     * The maximum number of instruments available
     */
    private final int numberOfInstruments;

    /**
     * The maximum number of orbits available
     */
    private final int numberOfOrbits;

    /**
     * A tree containing the scores for each architecture
     */
    private ValueTree valueTree;

    //Constructors
    /**
     * Creates an empty architecture with each spacecraft carrying zero
     * instruments. Default value for each spacecraft orbit is the zero-index in
     * the list of alternatives.
     *
     * @param numberOfInstruments The number of instruments
     * @param numberOfSpacecraft The number of spacecraft
     * @param numberOfOrbits The number of possible orbits
     * @param numberOfObjectives
     */
    public InstrumentAssignmentArchitecture2(int numberOfInstruments,
            int numberOfSpacecraft, int numberOfOrbits, int numberOfObjectives) {
        super(numberOfObjectives, 0,
                createDecisions(numberOfInstruments, numberOfSpacecraft, numberOfOrbits));
        this.missions = new HashMap<>();
        this.numberOfSpacecraft = numberOfSpacecraft;
        this.numberOfInstruments = numberOfInstruments;
        this.numberOfOrbits = numberOfOrbits;
    }

    /**
     * Create an array of assigning decisions and then one combining decision
     * for each spacecraft
     *
     * @param numberOfInstruments The number of instruments
     * @param numberOfSpacecraft The number of spacecraft
     * @return
     */
    private static ArrayList<ArchitecturalDecision> createDecisions(
            int numberOfInstruments, int numberOfSpacecraft, int numberOfOrbits) {
        ArrayList<ArchitecturalDecision> out = new ArrayList<>();
        for (int i = 0; i < numberOfSpacecraft; i++) {
            out.add(new DownSelecting(numberOfInstruments, downTag + i));
            out.add(new Combining(new int[]{numberOfOrbits}, combineTag + i));
        }
        return out;
    }

    /**
     * makes a copy solution from the input solution
     *
     * @param solution
     */
    private InstrumentAssignmentArchitecture2(Solution solution) {
        super(solution);
        if (!(solution instanceof InstrumentAssignmentArchitecture2)) {
            throw new ClassCastException("Expected solution to be of class InstrumentAssignment2.");
        }
        InstrumentAssignmentArchitecture2 arch = (InstrumentAssignmentArchitecture2) solution;
        this.missions = new HashMap<>();
        this.numberOfSpacecraft = arch.numberOfSpacecraft;
        this.numberOfInstruments = arch.numberOfInstruments;
        this.numberOfOrbits = arch.numberOfOrbits;
    }

    //Getters
    /**
     * Gets the names of the payloads that are assigned within the architecture
     * at least once
     *
     * @return
     */
    public Instrument[] getUniqueInstrumentNames() {
        BitSet inst = getUniqueInstruments();
        Instrument[] out = new Instrument[inst.cardinality()];
        int strInd = 0;
        //loop over the indices that have been set true
        for (int i = inst.nextSetBit(0); i >= 0; i = inst.nextSetBit(i + 1)) {
            out[strInd] = EOSSDatabase.getInstrument(i);
            strInd++;
        }
        return out;
    }

    /**
     * Gets the bitset that represents the instruments that are assigned within
     * the architecture at least once
     *
     * @return
     */
    private BitSet getUniqueInstruments() {
        BitSet inst = new BitSet(numberOfInstruments);
        for (int i = 0; i < numberOfInstruments; i++) {
            for (int j = 0; j < numberOfSpacecraft; j++) {
                boolean val = DownSelecting.getValue(i, this, downTag + j);
                if (val) {
                    inst.set(i);
                    break;
                }
            }
        }
        return inst;
    }

    /**
     * Gets the number of unique instruments that are assigned
     *
     * @return
     */
    public int getNinstruments() {
        return getUniqueInstruments().cardinality();
    }
    
    /**
     * Gets the index of the orbit of the specified spacecraft
     * @param spacecraftIndex the index of the desired spacecraft
     * @return the index of the orbit of the specified spacecraft
     */
    public int getOrbitIndex(int spacecraftIndex){
        return Combining.getValue(0, this, combineTag + spacecraftIndex);
    }
    
    /**
     * Gets the orbit of the specified spacecraft
     * @param spacecraftIndex the index of the desired spacecraft
     * @return the orbit of the specified spacecraft
     */
    public Orbit getOrbit(int spacecraftIndex){
        int val = Combining.getValue(0, this, combineTag + spacecraftIndex);
        return EOSSDatabase.getOrbit(val);
    }

    /**
     * Gets the indices spacecraft that has at least one instrument
     *
     * @return the indices spacecraft that has at least one instrument
     */
    public int[] getSpacecraft() {
        BitSet sc = new BitSet(this.numberOfOrbits);
        for (int i = 0; i < numberOfSpacecraft; i++) {
            //check if spacecraft has an instrument
            for (int j = 0; j < numberOfInstruments; j++) {
                boolean downVal = DownSelecting.getValue(j, this, downTag + i);
                if (downVal) {
                    sc.set(i);
                    break;
                }
            }
        }
        int[] out = new int[sc.cardinality()];
        int count = 0;
        for (int i = sc.nextSetBit(0); i >= 0; i = sc.nextSetBit(i + 1)) {
            out[count] = i;
            count++;
        }
        return out;
    }
    
    /**
     * Gets the indices of the instruments assigned to a specified orbit
     *
     * @param scIndex the index of the spacecraft
     * @return the indices of the instruments as they are in the EOSSDatabase
     */
    public ArrayList<Integer> getInstrumentsInSpacecraft(int scIndex) {
        ArrayList<Integer> payloads = new ArrayList<>();
        //loop over the instruments
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
            if (DownSelecting.getValue(i, this, downTag + scIndex)) {
                payloads.add(i);
            }
        }
        return payloads;
    }
    
    /**
     * removes the instrument from the orbit
     *
     * @param instrumentIndex the index of the instrument as it is in the
     * EOSSDatabase
     * @param scIndex the index of the spacecraft
     * @return true if removing the instrument to orbit changes the architecture
     * decision
     */
    public boolean removeInstrumentFromSpacecraft(int instrumentIndex, int scIndex){
        return DownSelecting.set(instrumentIndex, false, this, downTag + scIndex);
    }

    public String payloadToString() {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<this.numberOfSpacecraft; i++) {
            sb.append("sat_").append(i).append(":").append(this.getOrbit(i).getName()).append(":{");
            ArrayList<Integer> payload = this.getInstrumentsInSpacecraft(i);
            for(Integer val : payload){
                sb.append(EOSSDatabase.getInstrument(val)).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append("} ");
        }
        return sb.toString();
    }

    /**
     * Gets all the mission names
     *
     * @return all the mission names
     */
    public Collection<String> getMissionNames() {
        return missions.keySet();
    }

    /**
     * Gets the mission by the mission name
     *
     * @param name
     * @return
     */
    public Mission getMission(String name) {
        return missions.get(name);
    }

    /**
     * Sets the mission field represented by this architecture. Resets any
     * missions fields that are computed (e.g. mass, power).
     */
    public void setMissions() {
        missions.clear();
        for (int spacecraftIndex : getSpacecraft()) {
            HashMap<Spacecraft, Orbit> map = new HashMap<>(1);
            ArrayList<Instrument> payload = new ArrayList<>();
            for(Integer instIndex : getInstrumentsInSpacecraft(spacecraftIndex)){
                payload.add(EOSSDatabase.getInstrument(instIndex));
            }
            map.put(new Spacecraft(payload),getOrbit(spacecraftIndex));
            Mission miss = new Mission.Builder(combineTag + spacecraftIndex, map).build();
            missions.put(miss.getName(), miss);
        }
    }

    public void setValueTree(ValueTree valueTree) {
        this.valueTree = valueTree;
    }

    public ValueTree getValueTree() {
        return valueTree;
    }

    @Override
    public Solution copy() {
        return new InstrumentAssignmentArchitecture2(this);
    }
}
