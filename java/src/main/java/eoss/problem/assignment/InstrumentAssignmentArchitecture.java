/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment;

import seakers.architecture.Architecture;
import seakers.architecture.pattern.ArchitecturalDecision;
import seakers.architecture.pattern.Combining;
import seakers.architecture.pattern.Assigning;
import architecture.util.ValueTree;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.spacecraft.Spacecraft;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.moeaframework.core.Solution;
import seakers.architecture.util.IntegerVariable;

/**
 * This class creates a solution for the problem consisting of an assigning
 * pattern of instruments to orbits and a combining pattern for the number of
 * satellites per orbit. Assigning instruments from the left hand side to orbits
 * on the right hand side
 *
 * @author nozomi
 */
//import jess.*;
public class InstrumentAssignmentArchitecture extends Architecture {

    private static final long serialVersionUID = 8776271523867355732L;

    /**
     * Tag used for the assigning decision
     */
    private static final String assignTag = "inst";

    /**
     * Tag used for the combining decision
     */
    private static final String combineTag = "nSat";

    /**
     * The available options of the number of satellites
     */
    private final int[] altnertivesForNumberOfSatellites;

    /**
     * The missions represented by this architecture
     */
    private final HashMap<String, Mission> missions;

    /**
     * A tree containing the scores for each architecture
     */
    private ValueTree valueTree;

    //Constructors
    /**
     * Creates an empty architecture with a default number of satellites.
     * Default value is the first value in the array given as
     * alternativesForNumberOfSatellites
     *
     * @param numberOfInstruments
     * @param numberOfOrbits
     * @param altnertivesForNumberOfSatellites
     * @param numberOfObjectives
     */
    public InstrumentAssignmentArchitecture(int[] altnertivesForNumberOfSatellites,
            int numberOfInstruments, int numberOfOrbits, int numberOfObjectives) {
        super(numberOfObjectives, 0,
                createDecisions(altnertivesForNumberOfSatellites, numberOfInstruments, numberOfOrbits));
        this.altnertivesForNumberOfSatellites = altnertivesForNumberOfSatellites;
        this.missions = new HashMap<>();
    }

    private static ArrayList<ArchitecturalDecision> createDecisions(
            int[] altnertivesForNumberOfSatellites,
            int numberOfInstruments, int numberOfOrbits) {
        ArrayList<ArchitecturalDecision> out = new ArrayList<>();
        out.add(new Combining(new int[]{altnertivesForNumberOfSatellites.length}, combineTag));
        out.add(new Assigning(numberOfInstruments, numberOfOrbits, assignTag));
        return out;
    }

    /**
     * makes a copy solution from the input solution
     *
     * @param solution
     */
    private InstrumentAssignmentArchitecture(Solution solution) {
        super(solution);
        this.altnertivesForNumberOfSatellites = ((InstrumentAssignmentArchitecture) solution).altnertivesForNumberOfSatellites;
        this.missions = new HashMap<>();
    }

    //Getters
    /**
     * Returns the total number of satellites in the architecture (number of
     * satellites x number of orbits with at least one instrument assigned).
     *
     * @return the total number of satellites in the architecture
     */
    public int getTotalNumberOfSatellites() {
        return getNumberOfSatellitesPerOrbit() * getNorbits();
    }

    /**
     * Returns the number of satellites per orbit.
     *
     * @return the number of satellites per orbit.
     */
    public int getNumberOfSatellitesPerOrbit() {
        int index = this.getDecisionIndex(combineTag);
        IntegerVariable var = (IntegerVariable) this.getVariable(index);
        return this.altnertivesForNumberOfSatellites[var.getValue()];
    }

    public int[] getAltnertivesForNumberOfSatellites() {
        return altnertivesForNumberOfSatellites;
    }

    /**
     * Gets the names of the payloads that are assigned within the architecture
     * at least once
     *
     * @return
     */
    public Instrument[] getPayloads() {
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
        Assigning dec = (Assigning) this.getDecision(assignTag);
        BitSet inst = new BitSet(dec.getNumberOfLHS());
        for (int i = 0; i < dec.getNumberOfLHS(); i++) {
            for (int j = 0; j < dec.getNumberOfRHS(); j++) {
                if (Assigning.isConnected(i, j, this, assignTag)) {
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
     * Gets the names of the orbits that have at least one instrument assigned
     * to it
     *
     * @return
     */
    public Orbit[] getOccupiedOrbits() {
        BitSet orbs = getUniqueOrbits();
        Orbit[] out = new Orbit[orbs.cardinality()];
        int strInd = 0;
        //loop over the indices that have been set true
        for (int i = orbs.nextSetBit(0); i >= 0; i = orbs.nextSetBit(i + 1)) {
            out[strInd] = EOSSDatabase.getOrbit(i);
            strInd++;
        }
        return out;
    }

    /**
     * Gets the names of the orbits that have no instruments assigned to it
     *
     * @return
     */
    public Orbit[] getEmptyOrbits() {
        BitSet orbs = getUniqueOrbits();
        int norbits = ((Assigning) this.getDecision(assignTag)).getNumberOfRHS();
        orbs.flip(0, norbits);
        Orbit[] out = new Orbit[orbs.cardinality()];
        int strInd = 0;
        //loop over the indices that have been set true
        for (int i = orbs.nextSetBit(0); i >= 0; i = orbs.nextSetBit(i + 1)) {
                out[strInd] = EOSSDatabase.getOrbit(i);
            strInd++;
        }
        return out;
    }

    /**
     * Gets the bitset that represents the orbits that have at least one
     * instrument assigned to it
     *
     * @return the bitset that represents the orbits that have at least one
     * instrument assigned to it
     */
    private BitSet getUniqueOrbits() {
        Assigning dec = (Assigning) this.getDecision(assignTag);
        BitSet orbs = new BitSet(dec.getNumberOfRHS());
        for (int j = 0; j < dec.getNumberOfRHS(); j++) {
            for (int i = 0; i < dec.getNumberOfLHS(); i++) {
                if (Assigning.isConnected(i, j, this, assignTag)) {
                    orbs.set(j);
                    break;
                }
            }
        }
        return orbs;
    }

    /**
     * Gets the number of unique orbits that are assigned
     *
     * @return the number of unique orbits that are assigned
     */
    public int getNorbits() {
        return getUniqueOrbits().cardinality();
    }

    /**
     * Gets the names of the instruments assigned to a specified orbit
     *
     * @param orb
     * @return
     */
    public ArrayList<Instrument> getInstrumentsInOrbit(Orbit orb) {
        ArrayList<Instrument> payloads = new ArrayList<>();
        int orbIndex;
        for (orbIndex = 0; orbIndex < EOSSDatabase.getNumberOfOrbits(); orbIndex++) {
            if (orb.equals(EOSSDatabase.getOrbit(orbIndex))) {
                break;
            }
        }

        //loop over the instruments
        ArrayList<Integer> paylaodIndex = getInstrumentsInOrbit(orbIndex);
        for (Integer index : paylaodIndex) {
            payloads.add(EOSSDatabase.getInstrument(index));
        }
        return payloads;
    }

    /**
     * Gets the indices of the instruments assigned to a specified orbit
     *
     * @param orbIndex the index of the orbit as it is in the EOSSDatabase
     * @return the indices of the instruments as they are in the EOSSDatabase
     */
    public ArrayList<Integer> getInstrumentsInOrbit(int orbIndex) {
        ArrayList<Integer> payloads = new ArrayList<>();
        //loop over the instruments
        for (int i = 0; i < EOSSDatabase.getNumberOfInstruments(); i++) {
            if (Assigning.isConnected(i, orbIndex, this, assignTag)) {
                payloads.add(i);
            }
        }
        return payloads;
    }

    /**
     * adds the instrument to the orbit
     *
     * @param instrumentIndex the index of the instrument as it is in the
     * EOSSDatabase
     * @param orbIndex the index of the orbit as it is in the EOSSDatabase
     * @return true if adding the instrument to orbit changes the architecture
     * decision
     */
    public boolean addInstrumentToOrbit(int instrumentIndex, int orbIndex) {
        boolean out = !Assigning.isConnected(instrumentIndex, orbIndex, this, assignTag);
        Assigning.connect(instrumentIndex, orbIndex, this, assignTag);
        return out;
    }

    /**
     * adds the instrument to the orbit
     *
     * @param instrument the instrument to add
     * @param orbit the orbit to add the instrument to
     * @return true if adding the instrument to orbit changes the architecture
     * decision
     */
    public boolean addInstrumentToOrbit(Instrument instrument, Orbit orbit) {
        int instrumentIndex = EOSSDatabase.findInstrumentIndex(instrument);
        int orbIndex = EOSSDatabase.findOrbitIndex(orbit);
        return addInstrumentToOrbit(instrumentIndex, orbIndex);
    }

    /**
     * removes the instrument from the orbit
     *
     * @param instrumentIndex the index of the instrument as it is in the
     * EOSSDatabase
     * @param orbIndex the index of the orbit as it is in the EOSSDatabase
     * @return true if removing the instrument to orbit changes the architecture
     * decision
     */
    public boolean removeInstrumentFromOrbit(int instrumentIndex, int orbIndex) {
        boolean out = Assigning.isConnected(instrumentIndex, orbIndex, this, assignTag);
        Assigning.disconnect(instrumentIndex, orbIndex, this, assignTag);
        return out;
    }

    public String payloadToString() {
        String str = String.valueOf(this.getNumberOfSatellitesPerOrbit()) + " x ";
        for (Orbit orb : EOSSDatabase.getOrbits()) {
            ArrayList<Instrument> payls = this.getInstrumentsInOrbit(orb);
            str = str + " " + orb + "{" + StringUtils.join(payls, ",") + "}";
        }
        return str;
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
     * Gets the missions within this architecture
     * @return the missions within this architecture
     */
    public Collection<Mission> getMissions(){
        return missions.values();
    }
    
    /**
     * Sets the mission field represented by this architecture. Resets any
     * missions fields that are computed (e.g. mass, power).
     */
    public void setMissions() {
        missions.clear();
        for (Orbit orb : getOccupiedOrbits()) {
            HashMap<Spacecraft, Orbit> map = new HashMap<>(1);
            map.put(new Spacecraft(orb.getName() + "_0", getInstrumentsInOrbit(orb)), orb);
            Mission miss = new Mission.Builder(orb.getName(), map).build();
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
        return new InstrumentAssignmentArchitecture(this);
    }
}
