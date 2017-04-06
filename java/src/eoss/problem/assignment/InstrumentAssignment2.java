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

    private final int nSpacecraft;

    private final double dcThreshold = 0.5;

    private final double massThreshold = 3000.0; //[kg]

    private final double packingEffThreshold = 0.4; //[kg]

    /**
     * synergistic instrument pairs
     */
    private final HashMap<Instrument, Instrument[]> synergyMap;

    /**
     * Interfering instrument pairs
     */
    private final HashMap<Instrument, Instrument[]> interferenceMap;

    /**
     *
     * @param path
     * @param nSpacecraft The number of spacecraft to consider
     * @param reqMode
     * @param withSynergy determines whether or not to evaluate the solutions
     * with synergy rules.
     */
    public InstrumentAssignment2(String path, int nSpacecraft, RequirementMode reqMode, boolean withSynergy) {
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

        //synergistic instrument pairs
        HashMap<String, String[]> synergyNameMap = new HashMap();
        synergyNameMap.put("ACE_ORCA", new String[]{"DESD_LID", "GACM_VIS", "ACE_POL", "HYSP_TIR", "ACE_LID"});
        synergyNameMap.put("DESD_LID", new String[]{"ACE_ORCA", "ACE_LID", "ACE_POL"});
        synergyNameMap.put("GACM_VIS", new String[]{"ACE_ORCA", "ACE_LID"});
        synergyNameMap.put("HYSP_TIR", new String[]{"ACE_ORCA", "POSTEPS_IRS"});
        synergyNameMap.put("ACE_POL", new String[]{"ACE_ORCA", "DESD_LID"});
        synergyNameMap.put("ACE_LID", new String[]{"ACE_ORCA", "CNES_KaRIN", "DESD_LID", "GACM_VIS"});
        synergyNameMap.put("POSTEPS_IRS", new String[]{"HYSP_TIR"});
        synergyNameMap.put("CNES_KaRIN", new String[]{"ACE_LID"});

        //get names into instrument objects
        this.synergyMap = new HashMap<>();
        for (String instNameKey : synergyNameMap.keySet()) {
            Instrument[] instArray = new Instrument[synergyNameMap.get(instNameKey).length];
            int count = 0;
            for (String instNameValue : synergyNameMap.get(instNameKey)) {
                instArray[count] = EOSSDatabase.getInstrument(instNameValue);
                count++;
            }
            this.synergyMap.put(EOSSDatabase.getInstrument(instNameKey), instArray);
        }

        HashMap<String, String[]> interferenceNameMap = new HashMap();
        interferenceNameMap.put("ACE_LID", new String[]{"ACE_CPR", "DESD_SAR", "CLAR_ERB", "GACM_SWIR"});
        interferenceNameMap.put("ACE_CPR", new String[]{"ACE_LID", "DESD_SAR", "CNES_KaRIN", "CLAR_ERB", "ACE_POL", "ACE_ORCA", "GACM_SWIR"});
        interferenceNameMap.put("DESD_SAR", new String[]{"ACE_LID", "ACE_CPR"});
        interferenceNameMap.put("CLAR_ERB", new String[]{"ACE_LID", "ACE_CPR"});
        interferenceNameMap.put("CNES_KaRIN", new String[]{"ACE_CPR"});
        interferenceNameMap.put("ACE_POL", new String[]{"ACE_CPR"});
        interferenceNameMap.put("ACE_ORCA", new String[]{"ACE_CPR"});
        interferenceNameMap.put("GACM_SWIR", new String[]{"ACE_LID", "ACE_CPR"});
        //get names into instrument objects
        this.interferenceMap = new HashMap<>();
        for (String instNameKey : interferenceNameMap.keySet()) {
            Instrument[] instArray = new Instrument[interferenceNameMap.get(instNameKey).length];
            int count = 0;
            for (String instNameValue : interferenceNameMap.get(instNameKey)) {
                instArray[count] = EOSSDatabase.getInstrument(instNameValue);
                count++;
            }
            this.interferenceMap.put(EOSSDatabase.getInstrument(instNameKey), instArray);
        }
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
        double instrumentOrbitAssignmentViolationSum = 0;
        double synergyViolationSum = 0;
        double interferenceViolationSum = 0;

        int numSpacecraft = 0;
        for (Mission mission : arch.getMissions()) {
            for (Spacecraft s : mission.getSpacecraft().keySet()) {
                numSpacecraft++;

                Orbit injectionOrbit = mission.getSpacecraft().get(s);

                dcViolationSum += Math.max(0.0, (dcThreshold - Double.parseDouble(s.getProperty("duty cycle"))) / dcThreshold);
                massViolationSum += Math.max(0.0, (s.getWetMass() - massThreshold) / s.getWetMass());

                //compute the packing efficiency
                for (Collection<Spacecraft> group : mission.getLaunchVehicles().keySet()) {
                    if (group.contains(s)) {
                        double totalVolume = 0;
                        double totalMass = 0;
                        for (Spacecraft sTemp : group) {
                            double volume = 1.0;
                            for (double d : sTemp.getDimensions()) {
                                volume *= d;
                            }
                            totalVolume += volume;
                            totalMass += sTemp.getLaunchMass();
                        }
                        double volumeEfficiency = totalVolume / mission.getLaunchVehicles().get(group).getVolume();
                        double massEfficiency = totalMass / mission.getLaunchVehicles().get(group).getMassBudget(injectionOrbit);
                        double packingEfficiency = Math.max(volumeEfficiency, massEfficiency);
                        s.setProperty("packingEfficiency", Double.toString(packingEfficiency));
                        //divide any violation by the size of the launch group to not double count violations
                        packingEfficiencyViolationSum += Math.max(0.0, ((packingEffThreshold - packingEfficiency) / packingEffThreshold) / group.size());
                        
                        if(Double.isNaN(packingEfficiencyViolationSum)){
                            System.out.println("");
                        }
                    }
                }

                //check poor assignment of instrument to orbit
                Orbit o = mission.getSpacecraft().get(s);
                if (!o.getRAAN().equals("PM")) {
                    for (Instrument inst : s.getPayload()) {
                        String concept = inst.getProperty("Concept");
                        if (concept.contains("chemistry")) {
                            instrumentOrbitAssignmentViolationSum++;
                        }
                    }
                }
                if (o.getRAAN().equals("DD")) {
                    for (Instrument inst : s.getPayload()) {
                        if (inst.getProperty("Illumination").equals("Passive")) {
                            instrumentOrbitAssignmentViolationSum++;
                        }
                    }
                }
                if (o.getAltitude() <= 400.) {
                    for (Instrument inst : s.getPayload()) {
                        if (inst.getProperty("Geometry").equals("slant")) {
                            instrumentOrbitAssignmentViolationSum++;
                        }
                    }
                }

                //check other spacecraft for missed opportunities to add synergy or remove interferencess
                for (Instrument inst : s.getPayload()) {
                    //check synergies
                    if (synergyMap.containsKey(inst)) {
                        for (Instrument instPair : synergyMap.get(inst)) {
                            if (!s.getPayload().contains(instPair)) {
                                for (String otherMissionName : arch.getMissionNames()) {
                                    Mission otherMission = arch.getMission(otherMissionName);
                                    for (Spacecraft otherSpacecraft : otherMission.getSpacecraft().keySet()) {
                                        if (otherSpacecraft.getPayload().contains(instPair)) {
                                            synergyViolationSum++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //check interferences
                    if (interferenceMap.containsKey(inst)) {
                        for (Instrument instPair : interferenceMap.get(inst)) {
                            if (s.getPayload().contains(instPair)) {
                                for (String otherMissionName : arch.getMissionNames()) {
                                    Mission otherMission = arch.getMission(otherMissionName);
                                    for (Spacecraft otherSpacecraft : otherMission.getSpacecraft().keySet()) {
                                        if (!otherSpacecraft.getPayload().contains(instPair)) {
                                            interferenceViolationSum++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //normalize the violations
        dcViolationSum /= numSpacecraft;
        massViolationSum /= numSpacecraft;
        packingEfficiencyViolationSum /= numSpacecraft;
        instrumentOrbitAssignmentViolationSum /= (36.0 * numSpacecraft);
        synergyViolationSum /= (10.0 * numSpacecraft);
        interferenceViolationSum /= (10.0 * numSpacecraft);
        
        //fix nans. Can occur if there are no spacecraft (empty architecture)
        if(Double.isNaN(instrumentOrbitAssignmentViolationSum)){
            instrumentOrbitAssignmentViolationSum = 0;
        }
        if(Double.isNaN(synergyViolationSum)){
            synergyViolationSum = 0;
        }
        if(Double.isNaN(interferenceViolationSum)){
            interferenceViolationSum = 0;
        }

        double constraint = (dcViolationSum + massViolationSum
                + packingEfficiencyViolationSum
                + instrumentOrbitAssignmentViolationSum
                + synergyViolationSum + interferenceViolationSum)
                / (6.);
        arch.setAttribute("constraint", constraint);
        arch.setAttribute("dcViolationSum", dcViolationSum);
        arch.setAttribute("massViolationSum", massViolationSum);
        arch.setAttribute("packingEfficiencyViolationSum", packingEfficiencyViolationSum);
        arch.setAttribute("instrumentOrbitAssignmentViolationSum", instrumentOrbitAssignmentViolationSum);
        arch.setAttribute("synergyViolationSum", synergyViolationSum);
        arch.setAttribute("interferenceViolationSum", interferenceViolationSum);
    }

    @Override
    public Solution newSolution() {
        return new InstrumentAssignmentArchitecture2(
                EOSSDatabase.getNumberOfInstruments(), nSpacecraft,
                EOSSDatabase.getNumberOfOrbits(), 2, 4);
    }

}
