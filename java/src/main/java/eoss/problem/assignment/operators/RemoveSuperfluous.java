/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment.operators;

import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Orbit;
import eoss.problem.evaluation.ArchitectureEvaluatorParams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.moeaframework.core.PRNG;
import rbsa.eoss.Interaction;
import rbsa.eoss.NDSM;
import rbsa.eoss.Nto1pair;

/**
 *
 * @author nozomihitomi
 */
public class RemoveSuperfluous extends AbstractEOSSOperator {

    /**
     * The subset size relates to how many of the top costly superfluous
     * interactions the heuristics should select randomly from
     */
    private final int subsetSize;

    /**
     *
     * @param subsetSize
     */
    public RemoveSuperfluous(int subsetSize) {
        this.subsetSize = subsetSize;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    protected InstrumentAssignmentArchitecture evolve(InstrumentAssignmentArchitecture child) {
        //Find a random non-empty orbit and its payload 
        int randOrbitIndex = getRandomOrbitWithAtLeastNInstruments(child, 1);
        if (randOrbitIndex == -1) {
            return child;
        }
        Orbit randOrbit = EOSSDatabase.getOrbit(randOrbitIndex);

        ArrayList<String> thepayload = new ArrayList<>();
        for (Instrument inst : child.getInstrumentsInOrbit(randOrbit)) {
            thepayload.add(inst.getName());
        }

        //try with 2-lateral synergies
        int extraInstrumentIndex = checkNthOrderSuperfluous(thepayload, randOrbit, 2);

        //try with 3-lateral synergies
//        if (extraInstrumentIndex == -1) {
//            extraInstrumentIndex = checkNthOrderSuperfluous(thepayload, randOrbit, 3);
//        }

        //If a superfluous interaction exists, remove it
        if (extraInstrumentIndex != -1) {
            child.removeInstrumentFromOrbit(extraInstrumentIndex, randOrbitIndex);
        }
        return child;
    }

    /**
     * Checks for opportunities of removing a superfluous instrument that
     * provides redundant capabilities.
     *
     * @param thepayload the payload already assigned to the orbit
     * @param orbit the orbit to examine for opportunity
     * @param order the order is how many instruments should be considered at
     * once.
     * @return the instrument index as found in the EOSSDatabase if there is an
     * superfluous interaction. Else -1;
     */
    private int checkNthOrderSuperfluous(ArrayList<String> thepayload, Orbit orbit, int order) {
        NDSM rdsm = (NDSM) ArchitectureEvaluatorParams.all_dsms.get("RDSM" + order + "@" + orbit.getName());
        TreeSet<Interaction> rtm = rdsm.getAllInteractions("0");
        if (rtm.isEmpty()) {
            return -1;
        }

        //find all superfluous interactions that apply to this spacecraft
        ArrayList<Nto1pair> relevantInteractions = new ArrayList();
        Iterator<Interaction> iter = rtm.iterator();
        while (iter.hasNext()) {
            Interaction key = iter.next();
            ArrayList<String> al = new ArrayList<>();
            al.addAll(Arrays.asList(key.getNtpair().getBase()));
            al.add(key.getNtpair().getAdded());
            if (thepayload.containsAll(al)) {
                relevantInteractions.add(key.getNtpair());
            }
        }
        if(relevantInteractions.isEmpty())
            return -1;

        //now search for the cost of these superfluous interactions
        NDSM edsm = (NDSM) ArchitectureEvaluatorParams.all_dsms.get("EDSM" + order + "@" + orbit.getName());
        TreeSet<Interaction> etm = edsm.getAllInteractions("+");

        //Get added cost of all superfluous interactions
        ArrayList<Interaction> costRTM = new ArrayList<>();
        Iterator<Interaction> iter2 = etm.descendingIterator(); //start with most expensive interactions
        while (iter2.hasNext()) {
            Interaction ntkey = iter2.next();
            if (relevantInteractions.contains(ntkey.getNtpair())) {
                costRTM.add(ntkey);
            }
        }

        //costRTM can have negative values (TODO check why) so it may be empty
        if (!costRTM.isEmpty()) {
            List<Interaction> subset;
            if (costRTM.size() > subsetSize) {
                //set is sorted is in descending order so take the most expensive interactions
                subset = costRTM.subList(0, subsetSize);
            } else {
                subset = costRTM;
            }
            Interaction randCostInteraction = subset.get(PRNG.nextInt(subset.size()));
            return EOSSDatabase.findInstrumentIndex(EOSSDatabase.getInstrument(randCostInteraction.getNtpair().getAdded()));
        } else {
            Nto1pair randpair= relevantInteractions.get(PRNG.nextInt(relevantInteractions.size()));
            return EOSSDatabase.findInstrumentIndex(EOSSDatabase.getInstrument(randpair.getAdded()));
        }
    }

}
