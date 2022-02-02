package eoss.problem;

import eoss.problem.assignment.InstrumentAssignment;
import eoss.problem.assignment.InstrumentAssignmentArchitecture;
import eoss.problem.evaluation.RequirementMode;
import knowledge.operator.*;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.BinaryVariable;
import seakers.orekit.util.OrekitConfig;

import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EOSSArchitectureEvaluation {

    /**
     * flag for if EOSSDatabase has been initialized
     */
    private static boolean initEOSSDatabase;

    /**
     * pool of resources
     */
    private static ExecutorService pool;

    /**
     * Executor completion services helps remove completed tasks
     */
    private static CompletionService<Algorithm> ecs;

    public static void main (String[] args) throws IOException {
        Locale.setDefault(Locale.ENGLISH);

        initEOSSDatabase = false;

        //PATH
        String path = "C:\\SEAK Lab\\SEAK Lab Github\\EOSS\\EOSS-AIAA\\problems\\climateCentric";

        //record the current class java file to save the parameters for future reference
        quine(new File(String.join(File.separator, new String[]{System.getProperty("user.dir"),"EOSSAIAA","src","main","java","eoss","problem","EOSSArchitectureEvaluation.java"})),
                new File(String.join(File.separator, new String[]{path, "result", "EOSSArchitectureEvaluation.java"})));
        int numCPU = 1;

        pool = Executors.newFixedThreadPool(numCPU);
        ecs = new ExecutorCompletionService<>(pool);

        //setup for using orekit
        OrekitConfig.init(numCPU,"C:\\SEAK Lab\\SEAK Lab Github\\EOSS\\EOSS-AIAA");

        //initialize problem
        InstrumentAssignment problem = getAssignmentProblem(path, RequirementMode.FUZZYATTRIBUTE);

        // Define bitstring representation of architecture
        String architectureString = "000000000000000000000000000000000000011100000000000101000001";
        InstrumentAssignmentArchitecture arch = new InstrumentAssignmentArchitecture(new int[]{1}, EOSSDatabase.getNumberOfInstruments(), EOSSDatabase.getNumberOfOrbits(), 2);

        //Solution solution = new Solution(60,2);

        for (int j = 1; j < arch.getNumberOfVariables(); ++j) {
            BinaryVariable var = new BinaryVariable(1);
            String decision = architectureString.substring(j-1,j);
            if (decision.equalsIgnoreCase("0")) {
                var.set(0, false);
            } else if (decision.equalsIgnoreCase("1")) {
                var.set(0, true);
            }
            arch.setVariable(j, var);
        }

        //assert solution instanceof InstrumentAssignmentArchitecture;
        //InstrumentAssignmentArchitecture arch = (InstrumentAssignmentArchitecture) solution;
        arch.setMissions();
        problem.evaluateArch(arch);

        System.out.println("Architecture to evaluate: " + architectureString);
        System.out.println("Objectives: " + Arrays.toString(arch.getObjectives()));
        System.out.println("Duty Cycle Violation: " + arch.getAttribute("dcViolationSum"));
        System.out.println("Instrument Orbit Relationships Violation: " + arch.getAttribute("instrumentOrbitAssignmentViolationSum"));
        System.out.println("Interference Violation: " + arch.getAttribute("interferenceViolationSum"));
        System.out.println("Packing Efficiency Violation: " + arch.getAttribute("packingEfficiencyViolationSum"));
        System.out.println("Spacecraft Mass Violation: " + arch.getAttribute("massViolationSum"));
        System.out.println("Synergy Violation: " + arch.getAttribute("synergyViolationSum"));

    }

    public static InstrumentAssignment getAssignmentProblem(String path, RequirementMode mode) {
        if (!initEOSSDatabase) {
            //initialize EOSS database
            EOSSDatabase.getInstance();
            EOSSDatabase.loadBuses(new File(path + File.separator + "config" + File.separator + "candidateBuses.xml"));
            EOSSDatabase.loadInstruments(new File(path + File.separator + "xls" + File.separator + "Instrument Capability Definition.xls"));
            EOSSDatabase.loadOrbits(new File(path + File.separator + "config" + File.separator + "candidateOrbits5.xml"));
            EOSSDatabase.loadLaunchVehicles(new File(path + File.separator + "config" + File.separator + "candidateLaunchVehicles.xml"));
            initEOSSDatabase = !initEOSSDatabase;
        }
        return new InstrumentAssignment(path, mode, new int[]{1}, true);
    }

    /**
     * Creates a copy of this class by reading it and saves it at the specified
     * path so that it can be referenced back when you need to know what
     * settings were used for a run
     *
     * @param infile the java file to save
     * @param outfile the location to save the java file
     * @throws java.io.IOException
     */
    public static void quine(File infile, File outfile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(infile))) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
            } catch (IOException ex) {
                throw ex;
            }
        } catch (IOException ex) {
            throw ex;
        }
    }
}
