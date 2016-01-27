/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import architecture.Explanation;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.commons.lang3.StringUtils;
import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;
import architecture.util.FuzzyValue;
import architecture.util.Interval;
import eoss.jess.QueryBuilder;
import eoss.jess.Resource;

/**
 * An assigning problem to optimize the allocation of n instruments to m orbits.
 * Also can choose the number of satellites per orbital plane. Objectives are
 * cost and scientific benefit
 *
 * @author nozomihitomi
 */
public class EOSSProblem extends AbstractProblem {

    private final int[] altnertivesForNumberOfSatellites;

    private final Resource resource;

    private final boolean explanation;

    private final boolean withSynergy;

    /**
     *
     * @param altnertivesForNumberOfSatellites
     * @param instruments
     * @param orbits
     * @param buses
     * @param explanation determines whether or not to attach the explanations
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public EOSSProblem(int[] altnertivesForNumberOfSatellites, ArrayList<Instrument> instruments,
            ArrayList<Orbit> orbits, ArrayList<Bus> buses, boolean explanation, boolean withSynergy) {
        //2 decisions for Choosing and Assigning Patterns
        super(2, 2);
        this.altnertivesForNumberOfSatellites = altnertivesForNumberOfSatellites;
        this.resource = new Resource();
        this.explanation = explanation;
        this.withSynergy = withSynergy;
    }

    /**
     * Gets the instruments for this problem
     *
     * @return
     */
    public ArrayList<Instrument> getInstruments() {
        return EOSSDatabase.getInstruments();
    }

    /**
     * Gets the orbits for this problem
     *
     * @return
     */
    public ArrayList<Orbit> getOrbits() {
        return EOSSDatabase.getOrbits();
    }

    @Override
    public void evaluate(Solution sltn) {
        EOSSArchitecture arch = (EOSSArchitecture) sltn;

        Rete r = resource.getRete();
        QueryBuilder qb = resource.getQueryBuilder();

        try {
            evaluatePerformance(r, arch, qb); //compute science score
            r.reset();

            assertMissions(r, arch);
//            evaluateCostEONRules(r, arch, qb); //compute cost
            evaluateCost(r, arch, qb);

            System.out.println("Arch " + arch.toString() + ": Science = " + arch.getObjective(0) + "; Cost = " + arch.getObjective(1) + " :: " + arch.payloadToString());
        } catch (JessException ex) {
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Solution newSolution() {
        return new EOSSArchitecture(altnertivesForNumberOfSatellites, EOSSDatabase.getInstruments().size(), EOSSDatabase.getOrbits().size(), 2);
    }

    private void aggregate_performance_score_facts(EOSSArchitecture arch, Rete r, QueryBuilder qb) {
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
            if (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES")) {
                fuzzy_science = (FuzzyValue) val.getSlotValue("fuzzy-value").javaObjectValue(r.getGlobalContext());
            }
            panel_scores = jessList2ArrayList(val.getSlotValue("sh-scores").listValue(r.getGlobalContext()), r);

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
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
        }
        arch.setObjective(0, -science); //negative because MOEAFramework assumes minimization problems
        if (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES")) {
            arch.setFuzzyObjective(0, fuzzy_science);
        }
        if (explanation) {
            arch.setExplanation(0, explanations);
            arch.setCapabilities(qb.makeQuery("REQUIREMENTS::Measurement"));
        }
    }

    private void aggregate_performance_score(EOSSArchitecture arch, Rete r) {
        ArrayList subobj_scores = new ArrayList();
        ArrayList obj_scores = new ArrayList();
        ArrayList panel_scores = new ArrayList();
        //Subobjective scores
        for (int p = 0; p < Params.npanels; p++) {
            int nob = Params.num_objectives_per_panel.get(p);
            ArrayList subobj_scores_p = new ArrayList(nob);
            for (int o = 0; o < nob; o++) {
                ArrayList subobj_p = (ArrayList) Params.subobjectives.get(p);
                ArrayList subobj_o = (ArrayList) subobj_p.get(o);
                int nsubob = subobj_o.size();
                ArrayList subobj_scores_o = new ArrayList(nsubob);
                for (int so = 0; so < nsubob; so++) {
                    String var_name = "?*subobj-" + subobj_o.get(so) + "*";
                    try {
                        subobj_scores_o.add(r.eval(var_name).floatValue(r.getGlobalContext()));
                    } catch (JessException ex) {
                        Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                subobj_scores_p.add(subobj_scores_o);
            }
            subobj_scores.add(subobj_scores_p);
        }

        //Objective scores
        for (int p = 0; p < Params.npanels; p++) {
            int nob = Params.num_objectives_per_panel.get(p);
            ArrayList obj_scores_p = new ArrayList(nob);
            for (int o = 0; o < nob; o++) {
                ArrayList subobj_weights_p = (ArrayList) Params.subobj_weights.get(p);
                ArrayList subobj_weights_o = (ArrayList) subobj_weights_p.get(o);
                ArrayList subobj_scores_p = (ArrayList) subobj_scores.get(p);
                ArrayList subobj_scores_o = (ArrayList) subobj_scores_p.get(o);
                obj_scores_p.add(innerProduct(subobj_weights_o, subobj_scores_o));
            }
            obj_scores.add(obj_scores_p);
        }
        //Stakeholder and final score
        for (int p = 0; p < Params.npanels; p++) {
            panel_scores.add(innerProduct((ArrayList) Params.obj_weights.get(p), (ArrayList) obj_scores.get(p)));
        }
        double science = innerProduct(Params.panel_weights, panel_scores);
        arch.setObjective(numberOfVariables, -science); //negative because MOEAFramework assumes minimization problems
    }

    private double evaluateCostEONRules(Rete r, EOSSArchitecture arch, QueryBuilder qb) throws JessException {
        r.setFocus("MANIFEST");
        r.run();

        double cost = 0.0;
        Explanation costFacts = new Explanation();
        if ((Params.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
            r.setFocus("FUZZY-CUBESAT-COST");
            r.run();

            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            costFacts.put("cost", missions);
            FuzzyValue fzcost = new FuzzyValue("Cost", new Interval("delta", 0, 0), "FY04$M");
            for (Fact mission : missions) {
                cost += mission.getSlotValue("mission-cost#").floatValue(r.getGlobalContext());
                fzcost = fzcost.add((FuzzyValue) mission.getSlotValue("mission-cost").javaObjectValue(r.getGlobalContext()));
            }
            arch.setFuzzyObjective(0, fzcost);
            arch.setExplanation(0, costFacts);
        } else {
            r.setFocus("CUBESAT-COST");
            r.run();
            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            costFacts.put("cost", missions);
            for (Fact mission : missions) {
                cost += mission.getSlotValue("mission-cost#").floatValue(r.getGlobalContext());
            }
            arch.setExplanation(0, costFacts);
        }
        arch.setObjective(1, cost);
        return cost;
    }

    private double evaluateCost(Rete r, EOSSArchitecture arch, QueryBuilder qb) {
        double cost = 0.0;
        try {
            //
            r.eval("(focus MANIFEST)");
            r.run();

            designSpacecraft(r, arch, qb);
            r.eval("(focus SAT-CONFIGURATION)");
            r.run();

            r.eval("(focus LV-SELECTION0)");
            r.run();
            r.eval("(focus LV-SELECTION1)");
            r.run();
            r.eval("(focus LV-SELECTION2)");
            r.run();
            r.eval("(focus LV-SELECTION3)");
            r.run();

            if ((Params.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-COST-ESTIMATION0"); //applies NICM cost model to all instruments without computed costs
                r.run();
                r.setFocus("FUZZY-COST-ESTIMATION");
            } else {
                r.setFocus("COST-ESTIMATION");
            }
            r.run();

            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            for (Fact mission : missions) {
                cost = cost + mission.getSlotValue("lifecycle-cost#").floatValue(r.getGlobalContext());
            }

            arch.setObjective(1, cost);
            Explanation explanations = new Explanation();
            explanations.put("cost", missions);
            arch.setExplanation(1, explanations);
        } catch (JessException ex) {
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println("Arch " + arch.toBitString() + ": Science = " + res.getScience() + "; Cost = " + res.getCost());
        return cost;
    }

    private void designSpacecraft(Rete r, EOSSArchitecture arch, QueryBuilder qb) {
        try {
            r.eval("(focus PRELIM-MASS-BUDGET)");
            r.run();

            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            Double[] oldmasses = new Double[missions.size()];
            for (int i = 0; i < missions.size(); i++) {
                oldmasses[i] = missions.get(i).getSlotValue("satellite-dry-mass").floatValue(r.getGlobalContext());
            }
            Double[] diffs = new Double[missions.size()];
            double tolerance = 10 * missions.size();
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

                Double[] drymasses = new Double[missions.size()];
                double sumdiff = 0.0;
                double summasses = 0.0;
                for (int i = 0; i < missions.size(); i++) {
                    drymasses[i] = missions.get(i).getSlotValue("satellite-dry-mass").floatValue(r.getGlobalContext());
                    diffs[i] = Math.abs(drymasses[i] - oldmasses[i]);
                    sumdiff = sumdiff + diffs[i];
                    summasses = summasses + drymasses[i];
                }
                converged = sumdiff < tolerance || summasses == 0;
                oldmasses = drymasses;

            }
        } catch (JessException ex) {
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void assertMissions(Rete r, EOSSArchitecture arch) {
        try {
            for (int i = 0; i < EOSSDatabase.getOrbits().size(); i++) {
                Orbit orbit = EOSSDatabase.getOrbits().get(i);
                ArrayList<Instrument> instruments = arch.getInstrumentsInOrbit(orbit);
                if (instruments.size() > 0) {
                    String payload = "";
                    double payloadMass = 0;
                    double characteristicPower = 0;
                    double dataRate = 0;
                    ArrayList<Double> payloadDimensions = new ArrayList<>();
                    payloadDimensions.add(0, 0.0); //max dimension in x, y, and z
                    payloadDimensions.add(1, 0.0); //nadir-area
                    payloadDimensions.add(2, 0.0); //max z dimension
                    String call = "(assert (MANIFEST::Mission (Name " + orbit + ") ";
                    for (Instrument inst : instruments) {
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
                        Iterator iter = inst.getProperties().iterator();
                        while (iter.hasNext()) {
                            String propertyName = (String) iter.next();
                            callManifestInstrument += "(" + propertyName + " " + inst.getProperty(propertyName) + ")";
                        }
                        callManifestInstrument += "(flies-in " + orbit.getName() + ")";
                        callManifestInstrument += "(orbit-altitude# " + String.valueOf((int) orbit.getAltitude()) + ")";
                        callManifestInstrument += "(orbit-inclination " + orbit.getInclination() + ")";
                        callManifestInstrument += "))";
                        r.eval(callManifestInstrument);
                    }
                    call += "(instruments " + payload + ") (launch-date 2015) (lifetime 5) (select-orbit no) " + orbit.toJessSlots();
                    call += "(payload-mass# " + String.valueOf(payloadMass) + ")";
                    call += "(payload-power# " + String.valueOf(characteristicPower) + ")";
                    call += "(payload-peak-power# " + String.valueOf(characteristicPower) + ")";
                    call += "(payload-data-rate# " + String.valueOf(dataRate) + ")";
                    double perOrbit = (dataRate * 1.2 * orbit.getPeriod()) / (1024 * 8); //(GByte/orbit) 20% overhead
                    call += "(payload-dimensions# " + String.valueOf(payloadDimensions.get(0)) + " " + String.valueOf(payloadDimensions.get(1)) + " " + String.valueOf(payloadDimensions.get(2)) + ")";
                    call += "(sat-data-rate-per-orbit# " + String.valueOf(perOrbit) + ")";
                    call += "(num-of-sats-per-plane# " + String.valueOf(arch.getNumberOfSatellitesPerOrbit()) + ")))";
                    call += "(assert (SYNERGY::cross-registered-instruments "
                            + " (instruments " + payload
                            + ") (degree-of-cross-registration spacecraft) "
                            + " (platform " + orbit + " )))";
                    r.eval(call);
                }
            }
        } catch (JessException ex) {
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void evaluatePerformance(Rete r, EOSSArchitecture arch, QueryBuilder qb) {
        try {
            r.reset();

            r.eval("(bind ?*science-multiplier* 1.0)");
            r.eval("(defadvice before (create$ >= <= < >) (foreach ?xxx $?argv (if (eq ?xxx nil) then (return FALSE))))");
            r.eval("(defadvice before (create$ sqrt + * **) (foreach ?xxx $?argv (if (eq ?xxx nil) then (bind ?xxx 0))))");

            Explanation explanations = new Explanation();

            assertMissions(r, arch);

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
            Iterator parameters = Params.measurements_to_instruments.keySet().iterator();
            while (parameters.hasNext()) {
                String param = (String) parameters.next();
                Value v = r.eval("(update-fovs " + param + " (create$ " + StringUtils.join(EOSSDatabase.getOrbits(), " ") + "))");
                if (RU.getTypeName(v.type()).equalsIgnoreCase("LIST")) {
                    ValueVector thefovs = v.listValue(r.getGlobalContext());
                    ArrayList<String> fovs = new ArrayList<>(thefovs.size());
                    for (int i = 0; i < thefovs.size(); i++) {
                        int tmp = thefovs.get(i).intValue(r.getGlobalContext());
                        fovs.add(i, String.valueOf(tmp));
                    }
                    String key = arch.getNumberOfSatellitesPerOrbit() + "x" + StringUtils.join(fovs, ",");
                    //if(!key.equals("5 x -1  -1  -1  -1  50")){
                    //System.out.println(param);
                    //key = "5 x -1  -1  -1  -1  50";
                    //}
                    HashMap therevtimes = (HashMap) Params.revtimes.get(key); //key: 'Global' or 'US', value Double

                    if (therevtimes == null) {
                        key = arch.getNumberOfSatellitesPerOrbit() + " x " + StringUtils.join(fovs, "  ");
                        therevtimes = (HashMap) Params.revtimes.get(key); //key: 'Global' or 'US', value Double
                    }
                    String call = "(assert (ASSIMILATION::UPDATE-REV-TIME (parameter " + param + ") (avg-revisit-time-global# " + therevtimes.get("Global") + ") "
                            + "(avg-revisit-time-US# " + therevtimes.get("US") + ")))";
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

            if ((Params.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-REQUIREMENTS");
            } else {
                r.setFocus("REQUIREMENTS");
            }
            r.run();

            if ((Params.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-AGGREGATION");
            } else {
                r.setFocus("AGGREGATION");
            }
            r.run();

            if ((Params.req_mode.equalsIgnoreCase("CRISP-ATTRIBUTES")) || (Params.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                aggregate_performance_score_facts(arch, r, qb);
            } else if ((Params.req_mode.equalsIgnoreCase("CRISP-CASES")) || (Params.req_mode.equalsIgnoreCase("FUZZY-CASES"))) {
                aggregate_performance_score(arch, r);
            }

            if (Params.run_mode.equalsIgnoreCase("DEBUG")) {
                explanations.put("partials", qb.makeQuery("REASONING::partially-satisfied"));
                explanations.put("full", qb.makeQuery("REASONING::fully-satisfied"));
                arch.setExplanation(0, explanations);
            }
        } catch (JessException ex) {
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    /**
//     * TODO had to put the rules here instead of clp file because the rules were
//     * firing when they shouldn't have been...
//     */
//    private ArrayList<String> addExtraRules(Rete r) {
//        ArrayList<String> out = new ArrayList();
//        try {
//            String rule1name = "CHANNELS::compute-EON-vertical-spatial-resolution1";
//            String call1 = "(defrule " + rule1name
//                    + " ?s <- (SYNERGIES::cross-registered-instruments (platform ?plat) (total-num-channels 0)) "
//                    + "?c <- (accumulate (bind ?countss 0) "
//                    + "(bind ?countss (+ ?countss ?num)) "
//                    + "?countss "
//                    + "(CAPABILITIES::Manifested-instrument (orbit-string ?plat)(num-of-mmwave-band-channels ?num) )) "
//                    + "=> "
//                    + "(modify ?s (total-num-channels ?c)))";
//
//            String rule2name = "CHANNELS::compute-EON-vertical-spatial-resolution2";
//            String call2 = "(defrule " + rule2name
//                    + " ?EON <- (CAPABILITIES::Manifested-instrument  (Vertical-Spatial-Resolution# nil) (orbit-string ?orbs)) "
//                    + "(SYNERGIES::cross-registered-instruments (total-num-channels ?c&~nil)(platform ?orbs)) "
//                    + "=> "
//                    + "(modify ?EON (Vertical-Spatial-Resolution# (compute-vertical-spatial-resolution-EON ?c))))";
//            r.eval(call1);
//            r.eval(call2);
//
//            out.add(rule1name);
//            out.add(rule2name);
//        } catch (JessException ex) {
//            System.err.println("ExtraRules are wrong...");
//            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return out;
//    }
    private ArrayList jessList2ArrayList(ValueVector vv, Rete r) {
        ArrayList al = new ArrayList();
        try {
            for (int i = 0; i < vv.size(); i++) {
                al.add(vv.get(i).stringValue(r.getGlobalContext()));
            }
        } catch (JessException ex) {
            Logger.getLogger(EOSSProblem.class.getName()).log(Level.SEVERE, null, ex);
            al = null;
        }
        return al;
    }

    /**
     * Takes two vectors (as an arraylist) and multiplies the elements
     *
     * @param a
     * @param b
     * @return
     * @throws Exception
     */
    private ArrayList<Double> elementMult(ArrayList<Double> a, ArrayList<Double> b) {
        int n = a.size();
        int n2 = b.size();
        if (n != n2) {
            throw new IllegalArgumentException("dotSum: Arrays of different sizes");
        }
        ArrayList c = new ArrayList(n);
        for (int i = 0; i < n; i++) {
            Double t = a.get(i) * b.get(i);
            c.add(t);
        }
        return c;
    }

    /**
     * Takes the inner product or dot product of two vectors
     *
     * @param a
     * @param b
     * @return a scalar value equal to the inner product of two vectors
     */
    private double innerProduct(ArrayList a, ArrayList b) {
        ArrayList<Double> vector = elementMult(a, b);
        int n = vector.size();
        double res = 0.0;
        for (Double val : vector) {
            res += val;
        }
        return res;
    }

}
