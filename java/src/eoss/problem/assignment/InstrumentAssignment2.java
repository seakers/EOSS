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
import eoss.spacecraft.Spacecraft;
import eoss.problem.ValueAggregationBuilder;
import eoss.problem.evaluation.RequirementMode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * An assigning problem to optimize the allocation of n instruments to m
 * spacecraft. The orbit of each spacecraft is a combining decision. Objectives
 * are cost and scientific benefit
 *
 * @author nozomihitomi
 */
public class InstrumentAssignment2 extends AbstractProblem implements SystemArchitectureProblem {

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

    private final int nSpacecraft;

    private final double dcThreshold = 0.5;

    private final double massThreshold = 3000.0; //[kg]

    private final double packingEffThreshold = 0.4; //[kg]

    /**
     *
     * @param path
     * @param nSpacecraft The number of spacecraft to consider
     * @param reqMode
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public InstrumentAssignment2(String path, int nSpacecraft, RequirementMode reqMode, boolean withSynergy){
        //nInstruments*nSpacecraft for the assigning problem, nSpacecraft for the combining problem
        super(EOSSDatabase.getNumberOfInstruments() * nSpacecraft + nSpacecraft, 2, 4);

        this.nSpacecraft = nSpacecraft;

        ValueTree template = null;
        try {
            template = ValueAggregationBuilder.build(new File(path + File.separator + "config" + File.separator + "panels.xml"));
        } catch (IOException ex) {
            Logger.getLogger(InstrumentAssignment2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InstrumentAssignment2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InstrumentAssignment2.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.eval = new ArchitectureEvaluator(path, reqMode, withSynergy, template);
    }

    @Override
    public void evaluate(Solution sltn) {
        InstrumentAssignmentArchitecture2 arch = (InstrumentAssignmentArchitecture2) sltn;
        arch.setMissions();
        evaluateArch(arch);
        System.out.println(String.format("Arch %s Science = %10f; Cost = %10f :: %s",
                arch.toString(), arch.getObjective(0), arch.getObjective(1), arch.payloadToString()));
    }

    private void evaluateArch(InstrumentAssignmentArchitecture2 arch) {
        ArrayList<Mission> missions = new ArrayList<>();
        for (String missionName : arch.getMissionNames()) {
            missions.add(arch.getMission(missionName));
        }
        try {
            ValueTree tree = eval.performance(missions); //compute science score
            arch.setObjective(0, -tree.computeScores()); //negative because MOEAFramework assumes minimization problems

            double cost = eval.cost(missions);
            arch.setObjective(1, cost);

            getAuxFacts(arch);
        } catch (JessException ex) {
            Logger.getLogger(InstrumentAssignment2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * record auxiliary information and check constraints
     *
     * @param arch
     * @throws JessException
     */
    private void getAuxFacts(InstrumentAssignmentArchitecture2 arch) throws JessException {
        double dcViolationSum = 0;
        double massViolationSum = 0;
        double packingEfficiencyViolationSum = 0;
        int instrumentOrbitAssingmentViolationSum = 0;
        int synergyViolationSum = 0;
        int interferenceViolationSum = 0;

        Collection<Fact> missionFacts = eval.makeQuery("MANIFEST::Mission");
        for (Fact fact : missionFacts) {
            String name = fact.getSlotValue("Name").toString().split(":")[0];
            Mission mission = arch.getMission(name);
            //assumes each mission only has one spacecraft
            Spacecraft s = mission.getSpacecraft().keySet().iterator().next();
            for (String slot : auxFacts) {
                s.setProperty(slot, fact.getSlotValue(slot).toString());
            }

            dcViolationSum += Math.max(0.0, (dcThreshold - Double.parseDouble(s.getProperty("duty cycle")))/dcThreshold);

            massViolationSum += Math.max(0.0, (s.getWetMass() - massThreshold)/s.getWetMass());

            //compute the packing efficiency
            for (Collection<Spacecraft> group : mission.getLaunchVehicles().keySet()) {
                if (group.contains(s)) {
                    double totalVolume = 0;
                    for (Spacecraft sTemp : group) {
                        double volume = 1.0;
                        for (double d : sTemp.getDimensions()) {
                            volume *= d;
                        }
                        totalVolume += volume;
                    }
                    double packingEfficiency = totalVolume / mission.getLaunchVehicles().get(group).getVolume();
                    s.setProperty("packingEfficiency", Double.toString(packingEfficiency));
                    //divide any violation by the size of the launch group to not double count violations
                    packingEfficiencyViolationSum += Math.max(0.0, ((packingEffThreshold - packingEfficiency)/packingEffThreshold) / group.size());
                }
            }

            //check poor assignment of instrument to orbit
            Orbit o = mission.getSpacecraft().get(s);
            if (!o.getRAAN().equals("PM")) {
                for (Instrument inst : s.getPayload()) {
                    String concept = inst.getProperty("Concept");
                    if (concept.contains("chemistry")) {
                        instrumentOrbitAssingmentViolationSum++;
                    }
                }
            }
            if (o.getRAAN().equals("DD")) {
                for (Instrument inst : s.getPayload()) {
                    if (inst.getProperty("Illumination").equals("Passive")) {
                        instrumentOrbitAssingmentViolationSum++;
                    }
                }
            }
            if (o.getAltitude() <= 400.) {
                for (Instrument inst : s.getPayload()) {
                    if (inst.getProperty("Geometry").equals("slant")) {
                        instrumentOrbitAssingmentViolationSum++;
                    }
                }
            }

            //synergy and interference violation
            HashMap<String, Instrument> instrumentSet = new HashMap<>();
            for (Instrument inst : s.getPayload()) {
                instrumentSet.put(inst.getName(), inst);
            }

            HashMap<String, String[]> synergyMap = new HashMap();
            synergyMap.put("ACE_ORCA", new String[]{"DESD_LID", "GACM_VIS", "ACE_POL", "HYSP_TIR", "ACE_LID"});
            synergyMap.put("DESD_LID", new String[]{"ACE_ORCA", "ACE_LID", "ACE_POL"});
            synergyMap.put("GACM_VIS", new String[]{"ACE_ORCA", "ACE_LID"});
            synergyMap.put("HYSP_TIR", new String[]{"ACE_ORCA", "POSTEPS_IRS"});
            synergyMap.put("ACE_POL", new String[]{"ACE_ORCA", "DESD_LID"});
            synergyMap.put("ACE_LID", new String[]{"ACE_ORCA", "CNES_KaRIN", "DESD_LID", "GACM_VIS"});
            synergyMap.put("POSTEPS_IRS", new String[]{"HYSP_TIR"});
            synergyMap.put("CNES_KaRIN", new String[]{"ACE_LID"});

            HashMap<String, String[]> interferenceMap = new HashMap();
            interferenceMap.put("ACE_LID", new String[]{"ACE_CPR", "DESD_SAR", "CLAR_ERB", "GACM_SWIR"});
            interferenceMap.put("ACE_CPR", new String[]{"ACE_LID", "DESD_SAR", "CNES_KaRIN", "CLAR_ERB", "ACE_POL", "ACE_ORCA", "GACM_SWIR"});
            interferenceMap.put("DESD_SAR", new String[]{"ACE_LID", "ACE_CPR"});
            interferenceMap.put("CLAR_ERB", new String[]{"ACE_LID", "ACE_CPR"});
            interferenceMap.put("CNES_KaRIN", new String[]{"ACE_CPR"});
            interferenceMap.put("ACE_POL", new String[]{"ACE_CPR"});
            interferenceMap.put("ACE_ORCA", new String[]{"ACE_CPR"});
            interferenceMap.put("GACM_SWIR", new String[]{"ACE_LID", "ACE_CPR"});

            for (String instName : instrumentSet.keySet()) {
                if (synergyMap.containsKey(instName)) {
                    for (String instPairName : synergyMap.get(instName)) {
                        if (!instrumentSet.containsKey(instPairName)) {
                            synergyViolationSum++;
                        }
                    }
                }
                if (interferenceMap.containsKey(instName)) {
                    for (String instPairName : interferenceMap.get(instName)) {
                        if (!instrumentSet.containsKey(instPairName)) {
                            interferenceViolationSum++;
                        }
                    }
                }
            }

            double constraint = (1./5.)*(dcViolationSum + 
                    massViolationSum +
                    packingEfficiencyViolationSum + 
                    instrumentOrbitAssingmentViolationSum/36. +
                    synergyViolationSum/10. +
                    interferenceViolationSum/10.);
            arch.setAttribute("constraint", constraint);
            arch.setAttribute("dcViolationSum", (double) dcViolationSum);
            arch.setAttribute("massViolationSum", (double) massViolationSum);
            arch.setAttribute("packingEfficiencyViolationSum", (double) packingEfficiencyViolationSum);
            arch.setAttribute("instrumentOrbitAssingmentViolationSum", (double) instrumentOrbitAssingmentViolationSum);
            arch.setAttribute("synergyViolationSum", (double) synergyViolationSum);
            arch.setAttribute("interferenceViolationSum", (double) interferenceViolationSum);

        }
    }
    
    @Override
    public Solution newSolution() {
        return new InstrumentAssignmentArchitecture2(
                EOSSDatabase.getNumberOfInstruments(), nSpacecraft,
                EOSSDatabase.getNumberOfOrbits(), 2, 4);
    }

}
