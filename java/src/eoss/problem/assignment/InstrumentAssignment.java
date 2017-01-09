/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.assignment;

import architecture.problem.SystemArchitectureProblem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jess.Fact;
import jess.JessException;
import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;
import architecture.util.ValueTree;
import eoss.problem.evaluation.ArchitectureEvaluator;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Spacecraft;
import eoss.problem.ValueAggregationBuilder;
import eoss.problem.evaluation.RequirementMode;
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

    private final ArchitectureEvaluator eval;

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
     * @param reqMode
     * @param altnertivesForNumberOfSatellites
     * @param explanation determines whether or not to attach the explanations
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public InstrumentAssignment(String path, RequirementMode reqMode,  int[] altnertivesForNumberOfSatellites, boolean explanation, boolean withSynergy) {
        this(path, reqMode, altnertivesForNumberOfSatellites, explanation, withSynergy, null);
    }

    /**
     *
     * @param path
     * @param reqMode
     * @param altnertivesForNumberOfSatellites
     * @param explanation determines whether or not to attach the explanations
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public InstrumentAssignment(String path, RequirementMode reqMode, int[] altnertivesForNumberOfSatellites, boolean explanation, boolean withSynergy, File database) {
        //2 decisions for Choosing and Assigning Patterns
        super(2, 2);

        this.altnertivesForNumberOfSatellites = altnertivesForNumberOfSatellites;
        ValueTree template = null;
        try {
            template = ValueAggregationBuilder.build(new File(path + File.separator + "config" + File.separator + "panels.xml"));
        } catch (IOException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InstrumentAssignment.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.eval = new ArchitectureEvaluator(path, reqMode, explanation, withSynergy, template);

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
            arch.setMissions();
            if (!loadArchitecture(arch)) {
                evaluateArch(arch);
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
            ArrayList<Mission> missions = new ArrayList<>();
            for (String missionName : solution.getMissionNames()) {
                missions.add(solution.getMission(missionName));
            }
            if (missions.isEmpty()) {
                return true;
            } else {
                eval.designSpacecraft(missions);
                getAuxFacts(solution);
                return true;
            }
        } else {
            return false;
        }
    }

    private void evaluateArch(InstrumentAssignmentArchitecture arch) {
        ArrayList<Mission> missions = new ArrayList<>();
        for (String missionName : arch.getMissionNames()) {
            missions.add(arch.getMission(missionName));
        }
        try {
            ValueTree tree = eval.performance(missions); //compute science score
            arch.setObjective(0, -tree.computeScores()); //negative because MOEAFramework assumes minimization problems

            double cost = eval.cost(missions);
            arch.setObjective(1, cost / 33495.939796); //normalize cost to maximum value

            getAuxFacts(arch);
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
        Collection<Fact> missionFacts = eval.makeQuery("MANIFEST::Mission");
        for (Fact fact : missionFacts) {
            String name = fact.getSlotValue("Name").toString();
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


}
