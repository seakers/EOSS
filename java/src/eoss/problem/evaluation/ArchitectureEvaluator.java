/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.evaluation;

import architecture.util.FuzzyValue;
import architecture.util.Interval;
import architecture.util.ValueTree;
import eoss.explanation.Explanation;
import eoss.jess.QueryBuilder;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.LaunchVehicle;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import eoss.problem.assignment.InstrumentAssignment;
import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jess.Fact;
import jess.JessException;
import jess.RU;
import jess.Rete;
import jess.Value;
import jess.ValueVector;

/**
 * Evaluates an architecture based on the missions
 *
 * @author nozomihitomi
 */
public class ArchitectureEvaluator {

    /**
     * Rete object to perform induction
     */
    private Rete r;

    /**
     * query builder
     */
    private QueryBuilder qb;

    /**
     * Flag to save the explanations
     */
    private final boolean explanation;

    /**
     * Flag to set performance evaluation with synergies
     */
    private final boolean withSynergy;

    /**
     * A template of the value tree
     */
    private final ValueTree valueTree;

    private final RequirementMode reqMode;

    public ArchitectureEvaluator(String path, RequirementMode reqMode, boolean explanation, boolean withSynergy, ValueTree valueTree) {
        try {
            ArchitectureEvaluatorParams.getInstance(path);
        } catch (IOException ex) {
            Logger.getLogger(ArchitectureEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ArchitectureEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.reqMode = reqMode;
        this.explanation = explanation;
        this.withSynergy = withSynergy;
        this.valueTree = valueTree;
        this.r = new Rete();
        this.qb = new QueryBuilder(r);
        JessInitializer ji = new JessInitializer();
        ji.initializeJess(reqMode, r, qb);
    }

    /**
     * Option to renew the rete engine to clear our working memory and initial
     * facts
     */
    public void renew() {
        this.r = new Rete();
        this.qb = new QueryBuilder(r);
        JessInitializer ji = new JessInitializer();
        ji.initializeJess(reqMode, r, qb);
    }

    /**
     * Asserts the missions into the working memory
     *
     * @param mission
     */
    private void assertMissions(Collection<Mission> mission) {
        try {
            for (Mission mis : mission) {
                HashMap<Spacecraft, Orbit> spacecraftSet = mis.getSpacecraft();
                for (Spacecraft spacecraft : spacecraftSet.keySet()) {
                    Orbit orbit = spacecraftSet.get(spacecraft);
                    if (spacecraft.getPaylaod().size() > 0) {
                        String payload = "";
                        double payloadMass = 0;
                        double characteristicPower = 0;
                        double dataRate = 0;
                        ArrayList<Double> payloadDimensions = new ArrayList<>();
                        payloadDimensions.add(0, 0.0); //max dimension in x, y, and z
                        payloadDimensions.add(1, 0.0); //nadir-area
                        payloadDimensions.add(2, 0.0); //max z dimension
                        String fliesIn = mis.getName() + ":" + orbit;
                        String call = "(assert (MANIFEST::Mission (Name " + fliesIn + ") ";
                        for (Instrument inst : spacecraft.getPaylaod()) {
                            payload = payload + " " + inst.getName();
                            payloadMass += Double.parseDouble(inst.getProperty("mass#"));
                            characteristicPower += Double.parseDouble(inst.getProperty("characteristic-power#"));
                            dataRate += Double.parseDouble(inst.getProperty("average-data-rate#"));
                            double dx = Double.parseDouble(inst.getProperty("dimension-x#"));
                            double dy = Double.parseDouble(inst.getProperty("dimension-y#"));
                            double dz = Double.parseDouble(inst.getProperty("dimension-z#"));
                            payloadDimensions.set(0, Math.max(payloadDimensions.get(0), Math.max(Math.max(dx, dy), dz)));
                            payloadDimensions.set(1, payloadDimensions.get(1) + dx * dy);
                            payloadDimensions.set(2, Math.max(payloadDimensions.get(2), dz));

                            //manifest the instrument
                            String callManifestInstrument = "(assert (CAPABILITIES::Manifested-instrument ";
                            callManifestInstrument += "(Name " + inst.getName() + ")";
                            Iterator iter = inst.getProperties().iterator();
                            while (iter.hasNext()) {
                                String propertyName = (String) iter.next();
                                callManifestInstrument += "(" + propertyName + " " + inst.getProperty(propertyName) + ")";
                            }
                            callManifestInstrument += "(flies-in " + fliesIn + ")";
                            callManifestInstrument += "(orbit-altitude# " + String.valueOf((int) orbit.getAltitude()) + ")";
                            callManifestInstrument += "(orbit-inclination " + orbit.getInclination() + ")";
                            callManifestInstrument += "))";
                            r.eval(callManifestInstrument);
                        }
                        call += "(instruments " + payload + ")";
                        call += "(launch-date " + mis.getLaunchDate() + ")";
                        call += "(lifetime " + mis.getLifetime() + ")";
                        call += "(select-orbit no) " + orbit.toJessSlots();
                        call += "(payload-mass# " + String.valueOf(payloadMass) + ")";
                        call += "(payload-power# " + String.valueOf(characteristicPower) + ")";
                        call += "(payload-peak-power# " + String.valueOf(characteristicPower) + ")";
                        call += "(payload-data-rate# " + String.valueOf(dataRate) + ")";
                        double perOrbit = (dataRate * 1.2 * orbit.getPeriod()) / (1024 * 8); //(GByte/orbit) 20% overhead
                        call += "(payload-dimensions# " + String.valueOf(payloadDimensions.get(0)) + " " + String.valueOf(payloadDimensions.get(1)) + " " + String.valueOf(payloadDimensions.get(2)) + ")";
                        call += "(sat-data-rate-per-orbit# " + String.valueOf(perOrbit) + ")";
                        call += "(num-of-sats-per-plane# " + String.valueOf(1) + ")))"; //should get rid of this attribute
                        call += "(assert (SYNERGY::cross-registered-instruments "
                                + " (instruments " + payload
                                + ") (degree-of-cross-registration spacecraft) "
                                + " (platform " + orbit + " )))";
                        r.eval(call);
                    }
                }
            }
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Resets the working memory and computes the performance. The working
     * memory is not cleared after computation, so facts can be accessed after
     * evaluation
     *
     * @param missions
     * @return
     */
    public ValueTree performance(Collection<Mission> missions) throws JessException {
        r.eval("(reset)");
        assertMissions(missions);

        ValueTree tree = valueTree.copy();

        r.eval("(bind ?*science-multiplier* 1.0)");
        r.eval("(defadvice before (create$ >= <= < >) (foreach ?xxx $?argv (if (eq ?xxx nil) then (return FALSE))))");
        r.eval("(defadvice before (create$ sqrt + * **) (foreach ?xxx $?argv (if (eq ?xxx nil) then (bind ?xxx 0))))");

        r.setFocus("MANIFEST");
        r.run();

//                ArrayList<String> extraRules = addExtraRules(r);
//                r.setFocus("CHANNELS");
//                r.run();
//                Iterator<String> iter = extraRules.iterator();
//                while (iter.hasNext()) {
//                    r.removeDefrule(iter.next());
//                }
        r.setFocus("FUZZY-CAPABILITY-ATTRIBUTE");
        r.run();

        //checks to see if a manifested-instrument cannot take a measurement or upgrades or downgrades it capabilities depending on LHS of rules in CAPABILITIES-CHECK
        r.setFocus("CAPABILITIES-CHECK");
        r.run();

        //computes values for some slots in CAPABILITIES::Manifested-instrument
        r.setFocus("CAPABILITIES");
        r.run();

        //generates the measurement capabilities (REQUIREMENT::Measurement)
        r.setFocus("CAPABILITIES-GENERATE");
        r.run();

        //This synergy rule call creates new measurement facts that may arise from interactions between instruments 
        if (withSynergy) {
            r.setFocus("SYNERGY");
            r.run();
        }

        //Revisit times
        for (String measurement : ArchitectureEvaluatorParams.measurements) {
            ArrayList<Fact> qResults = qb.makeQuery("REQUIREMENTS::Measurement (Parameter " + measurement + ")");
            if (!qResults.isEmpty()) {
                HashSet<String> orbits = new HashSet<>();
                for (Fact fact : qResults) {
                    String orbStr = fact.getSlotValue("orbit-string").toString();
                    orbits.add(orbStr);
                }
                ArrayList<Integer> key = new ArrayList<>(orbits.size());
                for (String s : orbits) {
                    int orbitIndex = EOSSDatabase.findOrbitIndex(EOSSDatabase.getOrbit(s));
                    key.add(orbitIndex);
                }
                Collections.sort(key);

                HashMap<String, Double> therevtimes = ArchitectureEvaluatorParams.revtimes.get(key);
                if (therevtimes == null) {
                    throw new NullPointerException(String.format("Could not find key %s in revisit time look-up table.", key.toString()));
                }
                //convert revisit times from seconds to hours
                double globalRevtime_H = therevtimes.get("Global") / 3600.;
                double usRevtime_H = therevtimes.get("US") / 3600.;

                String call = "(assert (ASSIMILATION::UPDATE-REV-TIME (parameter " + measurement + ") (avg-revisit-time-global# " + globalRevtime_H + ") "
                        + "(avg-revisit-time-US# " + usRevtime_H + ")))";
                r.eval(call);
            }
        }
        r.setFocus("ASSIMILATION");
        r.run();

        r.setFocus("FUZZY-REQUIREMENT-ATTRIBUTE");
        r.run();

        if (withSynergy) {
            r.setFocus("SYNERGY");
            r.run();

            r.setFocus("SYNERGY-ACROSS-ORBITS");
            r.run();
        }

        switch (reqMode) {
            case CRISPATTRIBUTE:
                r.setFocus("REQUIREMENTS");
                r.run();
                r.setFocus("AGGREGATION");
                r.run();
                break;
            case CRISPCASE:
                r.setFocus("REQUIREMENTS");
                r.run();
                r.setFocus("AGGREGATION");
                r.run();
                break;
            case FUZZYATTRIBUTE:
                r.setFocus("FUZZY-REQUIREMENTS");
                r.run();
                r.setFocus("FUZZY-AGGREGATION");
                r.run();
                break;
            case FUZZYCASE:
                r.setFocus("FUZZY-REQUIREMENTS");
                r.run();
                r.setFocus("FUZZY-AGGREGATION");
                r.run();
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unknown requirements mode %s", reqMode));
        }

        ArrayList<Fact> vals = qb.makeQuery("AGGREGATION::VALUE");
        Fact val = vals.get(0);
        double science = val.getSlotValue("satisfaction").floatValue(r.getGlobalContext());
        ArrayList<Fact> subobj_facts = qb.makeQuery("AGGREGATION::SUBOBJECTIVE");
        for (int n = 0; n < subobj_facts.size(); n++) {
            Fact f = subobj_facts.get(n);
            String subobj = f.getSlotValue("id").stringValue(r.getGlobalContext());
            Double subobj_score = f.getSlotValue("satisfaction").floatValue(r.getGlobalContext());
            if (Double.isNaN(tree.getScore(subobj)) || tree.getScore(subobj) < subobj_score) {
                tree.setScore(subobj, subobj_score);
            }
        }

//            aggregate_performance_score_facts(arch);
//            if (explanation) {
//                Explanation explanations = new Explanation();
//                explanations.put("partials", qb.makeQuery("REASONING::partially-satisfied"));
//                explanations.put("full", qb.makeQuery("REASONING::fully-satisfied"));
//                arch.setAttribute("satisfactionExplantion", explanations);
//            }
        return tree;
    }

    private void aggregate_performance_score_facts(InstrumentAssignmentArchitecture arch) {
        ArrayList subobj_scores = new ArrayList();
        ArrayList obj_scores = new ArrayList();
        ArrayList panel_scores = new ArrayList();
        double science = 0.0;
        FuzzyValue fuzzy_science = null;
        Explanation explanations = new Explanation();
        TreeMap<String, Double> tm = new TreeMap<String, Double>();
        try {
            ArrayList<Fact> vals = qb.makeQuery("AGGREGATION::VALUE");
            Fact val = vals.get(0);
            science = val.getSlotValue("satisfaction").floatValue(r.getGlobalContext());
            if (reqMode.equals(RequirementMode.FUZZYATTRIBUTE)) {
                fuzzy_science = (FuzzyValue) val.getSlotValue("fuzzy-value").javaObjectValue(r.getGlobalContext());
            }
            panel_scores = jessList2ArrayList(val.getSlotValue("sh-scores").listValue(r.getGlobalContext()));

            ArrayList<Fact> subobj_facts = qb.makeQuery("AGGREGATION::SUBOBJECTIVE");
            for (int n = 0; n < subobj_facts.size(); n++) {
                Fact f = subobj_facts.get(n);
                String subobj = f.getSlotValue("id").stringValue(r.getGlobalContext());
                Double subobj_score = f.getSlotValue("satisfaction").floatValue(r.getGlobalContext());
                Double current_subobj_score = tm.get(subobj);
                if (current_subobj_score != null && subobj_score > current_subobj_score || current_subobj_score == null) {
                    tm.put(subobj, subobj_score);
                }
                explanations.put(subobj, qb.makeQuery("AGGREGATION::SUBOBJECTIVE (id " + subobj + ")"));
            }
            for (Iterator<String> name = tm.keySet().iterator(); name.hasNext();) {
                subobj_scores.add(tm.get(name.next()));
            }
            //TO DO: obj_score and subobj_scores.
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (reqMode.equals(RequirementMode.FUZZYATTRIBUTE)) {
            arch.setAttribute("fuzzyScience", fuzzy_science);
        }
        if (explanation) {
            arch.setAttribute("scienceExplanation", explanations);
            arch.setAttribute("measurement", qb.makeQuery("REQUIREMENTS::Measurement"));
        }
    }

    /**
     * Resets the working memory and computes the cost. The working memory is
     * not cleared after computation, so facts can be accessed after evaluation
     *
     * @param missions
     * @return
     * @throws JessException
     */
    public double cost(Collection<Mission> missions) throws JessException {
        designSpacecraft(missions);

        //compute launch cost and set values into facts
        ArrayList<Fact> facts = qb.makeQuery("MANIFEST::Mission");
        for (Mission m : missions) {
            HashMap<Spacecraft, LaunchVehicle> launches = LaunchVehicle.select(m);
            StringBuilder launchStr = new StringBuilder();
            double launchCost = 0;
            for (Spacecraft s : launches.keySet()) {
                LaunchVehicle lv = launches.get(s);
                launchCost += lv.getCost();
                launchStr.append(lv.getName()).append("_");
            }
            launchStr.deleteCharAt(launchStr.length()-1); //delete the last delimiter
            FuzzyValue fcost = new FuzzyValue("Cost", new Interval("delta", launchCost, 10.0), "FY04$M");
            for (int i = 0; i < facts.size(); i++) {
                String name = facts.get(i).getSlotValue("Name").toString().split(":")[0];
                if (name.equalsIgnoreCase(m.getName())) {
                    r.modify(facts.get(i), "launch-cost#", new Value(launchCost, RU.FLOAT));
                    r.modify(facts.get(i), "launch-cost", new Value(fcost));
                    r.modify(facts.get(i), "launch-vehicle", new Value(launchStr.toString(), RU.STRING));
                    facts.remove(i);
                }
            }
        }
        if (!facts.isEmpty()) {
            throw new IllegalStateException("One of the mission facts didn't get assigned any launch vehicles");
        }

        switch (reqMode) {
            case CRISPATTRIBUTE:
                r.setFocus("COST-ESTIMATION");
                r.run();
                break;
            case CRISPCASE:
                r.setFocus("COST-ESTIMATION");
                r.run();
                break;
            case FUZZYATTRIBUTE:
                r.setFocus("FUZZY-COST-ESTIMATION0"); //applies NICM cost model to all instruments without computed costs
                r.run();
                r.setFocus("FUZZY-COST-ESTIMATION");
                r.run();
                break;
            case FUZZYCASE:
                r.setFocus("FUZZY-COST-ESTIMATION0"); //applies NICM cost model to all instruments without computed costs
                r.run();
                r.setFocus("FUZZY-COST-ESTIMATION");
                r.run();
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unknown requirements mode %s", reqMode));
        }

        double cost = 0.0;
        ArrayList<Fact> missionFacts = qb.makeQuery("MANIFEST::Mission");
        for (Fact mission : missionFacts) {
            cost = cost + mission.getSlotValue("lifecycle-cost#").floatValue(r.getGlobalContext());
        }
        return cost;
    }

    public void designSpacecraft(Collection<Mission> missions) throws JessException {
        r.eval("(reset)");

        assertMissions(missions);

        r.eval("(focus MANIFEST)");
        r.run();

        r.eval("(focus PRELIM-MASS-BUDGET)");
        r.run();

        ArrayList<Fact> missionFacts = qb.makeQuery("MANIFEST::Mission");
        Double[] oldmasses = new Double[missionFacts.size()];
        for (int i = 0; i < missionFacts.size(); i++) {
            oldmasses[i] = missionFacts.get(i).getSlotValue("satellite-dry-mass").floatValue(r.getGlobalContext());
        }
        Double[] diffs = new Double[missionFacts.size()];
        double tolerance = 10 * missionFacts.size();
        boolean converged = false;
        while (!converged) {
            r.eval("(focus CLEAN1)");
            r.run();

            r.eval("(focus MASS-BUDGET)");
            r.run();

            r.eval("(focus CLEAN2)");
            r.run();

            r.eval("(focus UPDATE-MASS-BUDGET)");
            r.run();

            Double[] drymasses = new Double[missionFacts.size()];
            double sumdiff = 0.0;
            double summasses = 0.0;
            missionFacts = qb.makeQuery("MANIFEST::Mission");
            for (int i = 0; i < missionFacts.size(); i++) {
                drymasses[i] = missionFacts.get(i).getSlotValue("satellite-dry-mass").floatValue(r.getGlobalContext());
                diffs[i] = Math.abs(drymasses[i] - oldmasses[i]);
                sumdiff = sumdiff + diffs[i];
                summasses = summasses + drymasses[i];
            }
            converged = sumdiff < tolerance || summasses == 0;
            oldmasses = drymasses;
        }

        //record the wetmass into the missions
        for (Mission m : missions) {
            for (int i = 0; i < missionFacts.size(); i++) {
                Fact f = missionFacts.get(i);
                String name = f.getSlotValue("Name").toString().split(":")[0];
                if (name.equalsIgnoreCase(m.getName())) {
                    //TODO need to generatlize to multiple spacecraft case
                    Spacecraft s = m.getSpacecraft().keySet().iterator().next();
                    s.setWetMass(f.getSlotValue("satellite-wet-mass").floatValue(r.getGlobalContext()));
                    String[] dims = f.getSlotValue("satellite-dimensions").toString().split(" ");
                    double[] dbDims = new double[3];
                    for (int j = 0; j < 3; j++) {
                        dbDims[j] = Double.parseDouble(dims[j]);
                    }
                    s.setDimensions(dbDims);
                    missionFacts.remove(i);
                    break;
                }
            }
        }
    }

    private ArrayList jessList2ArrayList(ValueVector vv) {
        ArrayList al = new ArrayList();
        try {
            for (int i = 0; i < vv.size(); i++) {
                al.add(vv.get(i).stringValue(r.getGlobalContext()));
            }
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
            al = null;
        }
        return al;
    }

    /**
     * Query the working memory.
     *
     * @param query
     * @return facts that are found using the query
     */
    public Collection<Fact> makeQuery(String query) {
        return qb.makeQuery(query);
    }
}
