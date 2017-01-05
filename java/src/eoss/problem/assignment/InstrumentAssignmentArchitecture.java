/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment;

import architecture.Architecture;
import architecture.pattern.Combining;
import architecture.pattern.Assigning;
import architecture.pattern.ArchitecturalDecision;
import architecture.util.ValueTree;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.moeaframework.core.Solution;

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

    private Assigning assignment;

    private Combining combine;

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
        super(Arrays.asList(new ArchitecturalDecision[]{
            new Combining(new int[]{altnertivesForNumberOfSatellites.length}),
            new Assigning(numberOfInstruments, numberOfOrbits)}), numberOfObjectives);

        this.combine = (Combining) this.getVariable(0);
        this.assignment = (Assigning) this.getVariable(1);
        this.altnertivesForNumberOfSatellites = altnertivesForNumberOfSatellites;
        this.missions = new HashMap<>();
    }

    /**
     * makes a copy solution from the input solution
     *
     * @param solution
     */
    private InstrumentAssignmentArchitecture(Solution solution) {
        super(solution);
        this.combine = (Combining) this.getVariable(0);
        this.assignment = (Assigning) this.getVariable(1);
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
        return altnertivesForNumberOfSatellites[combine.getValue(0)] * getNorbits();
    }

    /**
     * Returns the number of satellites per orbit.
     *
     * @return the number of satellites per orbit.
     */
    public int getNumberOfSatellitesPerOrbit() {
        return altnertivesForNumberOfSatellites[combine.getValue(0)];
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
        BitSet bs = assignment.getBitSet();
        BitSet inst = new BitSet(assignment.getNumberOfLHS());
        for (int i = 0; i < assignment.getNumberOfLHS(); i++) {
            BitSet inst_i = bs.get(i * assignment.getNumberOfRHS(), i * assignment.getNumberOfRHS() + assignment.getNumberOfRHS());
            if (inst_i.cardinality() > 0) {
                inst.set(i);
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
        orbs.flip(0, orbs.length());
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
        BitSet bs = assignment.getBitSet();
        BitSet orbs = new BitSet(assignment.getNumberOfRHS());
        for (int o = 0; o < assignment.getNumberOfLHS(); o++) {
            orbs.or(bs.get(o * assignment.getNumberOfRHS(), o * assignment.getNumberOfRHS() + assignment.getNumberOfRHS()));
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
     * TODO fix this up to be more efficient. Use information in the BITSET
     * Checks to see if the specified instrument has been assigned in this
     * architecture
     *
     * @param instr
     * @return
     */
    public boolean containsInst(Instrument instr) {
        ArrayList<Instrument> validInstruments = new ArrayList<>();
        validInstruments.addAll(EOSSDatabase.getInstruments());
        if (!validInstruments.contains(instr)) {
            return false;
        }
        for (int o = 0; o < assignment.getNumberOfRHS(); o++) {
            Orbit orb = EOSSDatabase.getOrbit(o);
            ArrayList<Instrument> payls = this.getInstrumentsInOrbit(orb);
            if (payls != null) {
                ArrayList<Instrument> paylds = new ArrayList();
                paylds.addAll(payls);
                if (paylds.contains(instr)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @return
     */
    public BitSet getBitString() {
        return assignment.getBitSet();
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
            if (assignment.isConnected(i, orbIndex)) {
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
        boolean out = !assignment.isConnected(instrumentIndex, orbIndex);
        assignment.connect(instrumentIndex, orbIndex);
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
        boolean out = assignment.isConnected(instrumentIndex, orbIndex);
        assignment.disconnect(instrumentIndex, orbIndex);
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
     * Sets the mission field represented by this architecture. Resets any
     * missions fields that are computed (e.g. mass, power).
     */
    public void setMissions() {
        missions.clear();
        for (Orbit orb : getOccupiedOrbits()) {
            HashMap<Spacecraft, Orbit> map = new HashMap<>(1);
            map.put(new Spacecraft(getInstrumentsInOrbit(orb)), orb);
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.assignment);
        hash = 37 * hash + Objects.hashCode(this.combine);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InstrumentAssignmentArchitecture other = (InstrumentAssignmentArchitecture) obj;
        if (!Objects.equals(this.assignment, other.assignment)) {
            return false;
        }
        if (!Objects.equals(this.combine, other.combine)) {
            return false;
        }
        return true;
    }

}
