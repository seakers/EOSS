/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knowledge.operator;

import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.assignment.InstrumentAssignmentArchitecture2;
import eoss.spacecraft.Spacecraft;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;

/**
 *
 * @author nozomihitomi
 */
public abstract class InstrumentSwap implements Variation {

    /**
     * The number of changes to make to the architecture
     */
    private final int nChanges;

    /**
     * The list of possible swaps that can improve the architecture
     */
    private final ArrayList<SwapMove> moves;

    /**
     *
     * @param nChanges
     */
    public InstrumentSwap(int nChanges) {
        this.nChanges = nChanges;
        this.moves = new ArrayList<>();
    }

    @Override
    public Solution[] evolve(Solution[] sltns) {
        InstrumentAssignmentArchitecture2 child = (InstrumentAssignmentArchitecture2) sltns[0];
        child.setMissions();
        InstrumentAssignmentArchitecture2 copy = (InstrumentAssignmentArchitecture2) child.copy();
        for (String name : copy.getMissionNames()) {
            Mission mi = copy.getMission(name);
            for (Spacecraft si : mi.getSpacecraft().keySet()) {
                for (Instrument inst : checkThisSpacecraft(si)) {
                    //search for a satellite in the architecture with missing instrument
                    for (String namej : copy.getMissionNames()) {
                        Mission mj = copy.getMission(namej);
                        for (Spacecraft sj : mj.getSpacecraft().keySet()) {
                            if (!si.equals(sj) && checkOtherSpacecraft(sj, inst)) {
                                if (addToCurrentSpacecraft()) {
                                    moves.add(new SwapMove(mi, mj, inst));
                                } else {
                                    moves.add(new SwapMove(mj, mi, inst));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (moves.isEmpty()) {
            return new Solution[]{copy};
        }

        //repair the architecture
        Collections.shuffle(moves);
        for (int i = 0;
                i < nChanges;
                i++) {
            SwapMove swap = moves.get(i);
            int instInd = EOSSDatabase.findInstrumentIndex(swap.getInst());
            copy.addInstrumentToSpacecraft(instInd, swap.getAdd());
            copy.removeInstrumentFromSpacecraft(instInd, swap.getRemove());
        }
        return new Solution[]{copy
        };
    }

    /**
     * Checks the given spacecraft for instruments that can be added or removed
     * (must be either or) to improve the spacecraft
     *
     * @param s
     * @return the list of instruments to be added or removed from the
     * spacecraft that will improve the performance/cost of the spacecraft or
     * architecture
     */
    protected abstract Collection<Instrument> checkThisSpacecraft(Spacecraft s);

    /**
     * Checks the other spacecraft if the given instrument is present or absent
     *
     * @param s the other spacecraft
     * @param inst the instrument to swap
     * @return true if the other spacecraft is a candidate for an instrument
     * swap. else false.
     */
    protected abstract boolean checkOtherSpacecraft(Spacecraft s, Instrument inst);

    /**
     * When comparing the current spacecraft with other spacecraft, this method
     * returns true if instruments should be added to the current spacecraft and
     * returns false if instruments should be removed from the current
     * spacecraft
     *
     * @return
     */
    protected abstract boolean addToCurrentSpacecraft();

    /**
     * Class that stores information on which instrument to swap between two
     * spacecraft
     */
    private class SwapMove {

        private final Instrument inst;

        private final Mission mAdd;

        private final Mission mRemove;

        /**
         * Creates a new synergy pair where the instrument shall be added to the
         * "add" spacecraft and removed from the "remove" spacecraft
         *
         * @param inst Instrument to add and remove
         * @param add spacecraft to add instrument
         * @param remove spacecraft to remove instrument
         */
        public SwapMove(Mission mAdd, Mission mRemove, Instrument inst) {
            this.inst = inst;
            this.mAdd = mAdd;
            this.mRemove = mRemove;
        }

        /**
         * Gets the instrument to remove and add
         *
         * @return
         */
        public Instrument getInst() {
            return inst;
        }

        /**
         * Gets the spacecraft from which the instrument shall be added
         *
         * @return the spacecraft from which the instrument shall be added
         */
        public Mission getAdd() {
            return mAdd;
        }

        /**
         * Gets the spacecraft from which the instrument shall be removed
         *
         * @return the spacecraft from which the instrument shall be removed
         */
        public Mission getRemove() {
            return mRemove;
        }

    }

    @Override
    public int getArity() {
        return 1;
    }

}
