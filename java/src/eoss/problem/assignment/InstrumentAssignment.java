/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment;

import architecture.problem.SystemArchitectureProblem;
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
import architecture.util.ValueTree;
import eoss.explanation.Explanation;
import eoss.jess.JessInitializer;
import eoss.jess.QueryBuilder;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import eoss.problem.ValueAggregationBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * An assigning problem to optimize the allocation of n instruments to m orbits.
 * Also can choose the number of satellites per orbital plane. Objectives are
 * cost and scientific benefit
 *
 * @author nozomihitomi
 */
public class InstrumentAssignment extends AbstractProblem implements SystemArchitectureProblem {

    private final int[] altnertivesForNumberOfSatellites;

    private Rete r;

    private QueryBuilder qb;

    private final boolean explanation;

    private final boolean withSynergy;

    private ValueTree valueTree;

    /**
     * the slot names to record from the MANIFEST::MISSION facts
     */
    private final String[] auxFacts = new String[]{"ADCS-mass#",
        "avionics-mass#", "delta-V", "delta-V-deorbit", "depth-of-discharge",
        "EPS-mass#", "fraction-sunlight", "moments-of-inertia",
        "payload-data-rate#", "payload-dimensions#", "payload-mass#",
        "payload-peak-power#", "payload-power#", "propellant-ADCS",
        "propellant-injection", "propellant-mass-ADCS", "sat-data-rate-per-orbit#",
        "satellite-BOL-power#", "satellite-dimensions", "satellite-dry-mass",
        "satellite-launch-mass", "satellite-wet-mass", "solar-array-area",
        "solar-array-mass", "structure-mass#", "thermal-mass#"};

    /**
     * Solution database to reuse the computed values;
     */
    private final HashMap<BitSet, double[]> solutionDB;

    /**
     *
     * @param path
     * @param fuzzyMode
     * @param altnertivesForNumberOfSatellites
     * @param normalMode
     * @param explanation determines whether or not to attach the explanations
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public InstrumentAssignment(String path, String fuzzyMode, String normalMode, int[] altnertivesForNumberOfSatellites, boolean explanation, boolean withSynergy) {
        this(path, fuzzyMode, normalMode, altnertivesForNumberOfSatellites, explanation, withSynergy, null);
    }

    /**
     *
     * @param path
     * @param fuzzyMode
     * @param altnertivesForNumberOfSatellites
     * @param normalMode
     * @param explanation determines whether or not to attach the explanations
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public InstrumentAssignment(String path, String fuzzyMode, String normalMode, int[] altnertivesForNumberOfSatellites, boolean explanation, boolean withSynergy, File database) {
        //2 decisions for Choosing and Assigning Patterns
        super(2, 2);
        new InstrumentAssignmentParams(path, fuzzyMode, normalMode);//FUZZY or CRISP;

        this.altnertivesForNumberOfSatellites = altnertivesForNumberOfSatellites;
        renewJess();
        this.explanation = explanation;
        this.withSynergy = withSynergy;
        try {
            this.valueTree = ValueAggregationBuilder.build(new File(path + File.separator + "config" + File.separator + "panels.xml"));
        } catch (IOException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }

        //load database of solution if requested.
        solutionDB = new HashMap<>();
        if (database != null) {
            try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(database))) {
                System.out.println("Loading solution database: " + database.toString());
                solutionDB.putAll((HashMap<BitSet, double[]>) is.readObject());
            } catch (IOException ex) {
                Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void renewJess() {
        this.r = new Rete();
        this.qb = new QueryBuilder(r);
        JessInitializer ji = new JessInitializer();
        ji.initializeJess(r, qb);
    }

    /**
     * Gets the instruments for this problem
     *
     * @return the instruments for this problem
     */
    public Collection<Instrument> getInstruments() {
        return EOSSDatabase.getInstruments();
    }

    /**
     * Gets the orbits for this problem
     *
     * @return the orbits for this problem
     */
    public Collection<Orbit> getOrbits() {
        return EOSSDatabase.getOrbits();
    }

    @Override
    public void evaluate(Solution sltn) {
        try {
            InstrumentAssignmentArchitecture arch = (InstrumentAssignmentArchitecture) sltn;
            if (!loadArchitecture(arch)) {
                arch.setMissions();
                evaluate(arch);
            }

            System.out.println(String.format("Arch %s Science = %10f; Cost = %10f :: %s",
                    arch.toString(), arch.getObjective(0), arch.getObjective(1), arch.payloadToString()));
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Load the solution from the database if it exists and copies computed
     * values over to give solution.
     *
     * @param solution the solution to evaluate
     * @return true if solution is found in database. Else false;
     */
    private boolean loadArchitecture(InstrumentAssignmentArchitecture solution) throws JessException {
        if (solutionDB.containsKey(solution.getBitString())) {
            double[] objectives = solutionDB.get(solution.getBitString());
            for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
                solution.setObjective(i, objectives[i]);
            }

            //compute the auxilary facts
            r.eval("(reset)");
            ArrayList<Mission> missions = new ArrayList<>();
            for (String missionName : solution.getMissionNames()) {
                missions.add(solution.getMission(missionName));
            }
            if (missions.isEmpty()) {
                return true;
            } else {
                assertMissions(missions);
                designSpacecraft(solution);
                getAuxFacts(solution);
                return true;
            }
        } else {
            return false;
        }
    }

    private void evaluate(InstrumentAssignmentArchitecture arch) {
        ArrayList<Mission> missions = new ArrayList<>();
        for (String missionName : arch.getMissionNames()) {
            missions.add(arch.getMission(missionName));
        }
        try {
            r.eval("(reset)");
            assertMissions(missions);
            ValueTree tree = evaluatePerformance(arch); //compute science score
            arch.setObjective(0, -tree.computeScores()); //negative because MOEAFramework assumes minimization problems

            r.eval("(clear-storage)");
            r.eval("(reset)");
            assertMissions(missions);
            double cost = evaluateCost(arch);
            arch.setObjective(1, cost / 33495.939796); //normalize cost to maximum value

            getAuxFacts(arch);

            r.clearStorage();
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
        solutionDB.put(arch.getBitString(), new double[]{arch.getObjective(0), arch.getObjective(1)});
    }

    /**
     * record auxiliary information
     *
     * @param arch
     * @throws JessException
     */
    private void getAuxFacts(InstrumentAssignmentArchitecture arch) throws JessException {
        ArrayList<Fact> missionFacts = qb.makeQuery("MANIFEST::Mission");
        for (Fact fact : missionFacts) {
            String name = fact.getSlotValue("Name").stringValue(r.getGlobalContext());
            Mission mission = arch.getMission(name);
            //assumes each mission only has one spacecraft
            Spacecraft s = mission.getSpacecraft().keySet().iterator().next();
            for (String slot : auxFacts) {
                s.setProperty(slot, fact.getSlotValue(slot).toString());
            }
        }
    }

    /**
     * Saves the solution database created during the search
     *
     * @param file the file in which to save the database
     * @return true if successfully saved
     */
    public boolean saveSolutionDB(File file) {
        boolean flag = true;
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));) {
            os.writeObject(solutionDB);
            os.close();
        } catch (IOException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
            flag = false;
        }
        return flag;
    }

    @Override
    public Solution newSolution() {
        return new InstrumentAssignmentArchitecture(altnertivesForNumberOfSatellites, EOSSDatabase.getNumberOfInstruments(), EOSSDatabase.getNumberOfOrbits(), 2);
    }

    private double evaluateCost(InstrumentAssignmentArchitecture arch) {
        double cost = 0.0;
        try {
            r.eval("(focus MANIFEST)");
            r.run();

            designSpacecraft(arch);

            r.eval("(focus LV-SELECTION0)");
            r.run();
            r.eval("(focus LV-SELECTION1)");
            r.run();
            r.eval("(focus LV-SELECTION2)");
            r.run();
            r.eval("(focus LV-SELECTION3)");
            r.run();

            if ((InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
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
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cost;
    }

    private void designSpacecraft(InstrumentAssignmentArchitecture arch) {
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
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
                        String call = "(assert (MANIFEST::Mission (Name " + orbit + ") ";
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
                            callManifestInstrument += "(flies-in " + orbit.getName() + ")";
                            callManifestInstrument += "(orbit-altitude# " + String.valueOf((int) orbit.getAltitude()) + ")";
                            callManifestInstrument += "(orbit-inclination " + orbit.getInclination() + ")";
                            callManifestInstrument += "))";
                            r.eval(callManifestInstrument);
                        }
                        call += "(instruments " + payload + ")";
                        call += "(launch-date " + "2015)";
                        call += "(lifetime " + "5)";
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

    private ValueTree evaluatePerformance(InstrumentAssignmentArchitecture arch) {
        ValueTree tree = valueTree.copy();
        try {
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
            for (String measurement : InstrumentAssignmentParams.measurements) {
                Value v = r.eval("(update-fovs " + measurement + " (create$ " + StringUtils.join(EOSSDatabase.getOrbits(), " ") + "))");
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
                    HashMap<String, Double> therevtimes = InstrumentAssignmentParams.revtimes.get(key); //key: 'Global' or 'US', value Double

                    //there were two different maps at one point. one with spaces and the other without spaces and commas
                    if (therevtimes == null) {
                        key = arch.getNumberOfSatellitesPerOrbit() + " x " + StringUtils.join(fovs, "  ");
                        therevtimes = InstrumentAssignmentParams.revtimes.get(key); //key: 'Global' or 'US', value Double
                    }
                    String call = "(assert (ASSIMILATION::UPDATE-REV-TIME (parameter " + measurement + ") (avg-revisit-time-global# " + therevtimes.get("Global") + ") "
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

            if ((InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-REQUIREMENTS");
            } else {
                r.setFocus("REQUIREMENTS");
            }
            r.run();

            if ((InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-AGGREGATION");
            } else {
                r.setFocus("AGGREGATION");
            }
            r.run();

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
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            if (InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES")) {
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
        if (InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES")) {
            arch.setAttribute("fuzzyScience", fuzzy_science);
        }
        if (explanation) {
            arch.setAttribute("scienceExplanation", explanations);
            arch.setAttribute("measurement", qb.makeQuery("REQUIREMENTS::Measurement"));
        }
    }

    private double evaluateCostEONRules(InstrumentAssignmentArchitecture arch) throws JessException {
        r.setFocus("MANIFEST");
        r.run();

        double cost = 0.0;
        Explanation costFacts = new Explanation();
        if ((InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-CASES")) || (InstrumentAssignmentParams.req_mode.equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
            r.setFocus("FUZZY-CUBESAT-COST");
            r.run();

            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            costFacts.put("cost", missions);
            FuzzyValue fzcost = new FuzzyValue("Cost", new Interval("delta", 0, 0), "FY04$M");
            for (Fact mission : missions) {
                cost += mission.getSlotValue("mission-cost#").floatValue(r.getGlobalContext());
                fzcost = fzcost.add((FuzzyValue) mission.getSlotValue("mission-cost").javaObjectValue(r.getGlobalContext()));
            }
            arch.setAttribute("fuzzyCost", fzcost);
            arch.setAttribute("costExplanation", costFacts);
        } else {
            r.setFocus("CUBESAT-COST");
            r.run();
            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            costFacts.put("cost", missions);
            for (Fact mission : missions) {
                cost += mission.getSlotValue("mission-cost#").floatValue(r.getGlobalContext());
            }
            arch.setAttribute("costExplanation", costFacts);
        }
        arch.setObjective(1, cost);
        return cost;
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

}
