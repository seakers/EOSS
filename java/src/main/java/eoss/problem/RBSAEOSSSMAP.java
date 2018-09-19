/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import seakers.aos.aos.AOSMOEA;
import seakers.aos.creditassignment.setimprovement.SetImprovementDominance;
import seakers.aos.operator.AOSVariation;
import seakers.aos.operator.AOSVariationSI;
import seakers.aos.operatorselectors.AdaptivePursuit;
import seakers.aos.operatorselectors.OperatorSelector;
import eoss.problem.evaluation.ArchitectureEvaluatorParams;
import seakers.aos.operatorselectors.replacement.CompoundTrigger;
import seakers.aos.operatorselectors.replacement.EpochTrigger;
import seakers.aos.operatorselectors.replacement.InitialTrigger;
import seakers.aos.operatorselectors.replacement.OperatorReplacementStrategy;
import seakers.aos.operatorselectors.replacement.RemoveNLowest;
import seakers.aos.operatorselectors.replacement.ReplacementTrigger;
import eoss.problem.assignment.InstrumentAssignment;
import eoss.problem.assignment.InstrumentAssignment2;
import eoss.problem.evaluation.RequirementMode;
import eoss.problem.scheduling.MissionScheduling;
import java.io.File;

import knowledge.constraint.*;
import org.moeaframework.core.*;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.util.TypedProperties;
import eoss.search.KDOSearch;
import eoss.search.InstrumentedSearch;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import jess.JessException;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import knowledge.operator.EOSSOperatorCreator;
import knowledge.operator.RepairDutyCycle;
import knowledge.operator.RepairInstrumentOrbit;
import knowledge.operator.RepairInterference;
import knowledge.operator.RepairMass;
import knowledge.operator.RepairPackingEfficiency;
import knowledge.operator.RepairSynergy;
import mining.label.AbstractPopulationLabeler;
import mining.label.NondominatedSortingLabeler;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.ParetoObjectiveComparator;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.RandomInitialization;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.xml.sax.SAXException;
import seakers.architecture.operators.IntegerUM;
import seak.orekit.util.OrekitConfig;

/**
 *
 * @author dani
 */
public class RBSAEOSSSMAP {

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

    /**
     * First argument is the path to the project folder. Second argument is the
     * mode. Third argument is the number of ArchitecturalEvaluators to
     * initialize.
     *
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ENGLISH);

        initEOSSDatabase = false;

        //PATH
        if (args.length == 0) {
            args = new String[4];
//            args[0] = "C:\\Users\\SEAK2\\Nozomi\\EOSS\\problems\\climateCentric";
//            args[0] = "C:\\Users\\SEAK1\\Nozomi\\EOSS\\problems\\climateCentric";
            args[0] = "./problems/climateCentric";
//            args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/decadalScheduling";
            args[1] = "1"; //Mode
            args[2] = "1"; //numCPU
            args[3] = "1"; //numRuns
        }

        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");
        System.out.println("Will do " + args[3] + " runs");

        String path = args[0];

//        getAssignmentProblem(path, RequirementMode.FUZZYCASE);
//        DrivingFeaturesGenerator dfg = new DrivingFeaturesGenerator(61);
//        dfg.getDrivingFeatures("/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/AIAA_innovize_10643440470652193_0_labels.csv", "/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric/result/AIAA_innovize_10643440470652193_0_labels.res", 4);
//        System.exit(0);
        //record the current class java file to save the parameters for future reference
        quine(new File(String.join(File.separator, new String[]{System.getProperty("user.dir"), "java", "src", "main", "java", "eoss", "problem", "RBSAEOSSSMAP.java"})),
                new File(String.join(File.separator, new String[]{path, "result", "RBSAEOSSSMAP.java"})));
        int mode = Integer.parseInt(args[1]);
        int numCPU = Integer.parseInt(args[2]);
        int numRuns = Integer.parseInt(args[3]);

        pool = Executors.newFixedThreadPool(numCPU);
        ecs = new ExecutorCompletionService<>(pool);

        //setup for using orekit
        OrekitConfig.init();

        //parameters and operators for search
        TypedProperties properties = new TypedProperties();
        //search paramaters set here
        int popSize = 20;
        int maxEvals = 5000;
        properties.setInt("maxEvaluations", maxEvals);
        properties.setInt("populationSize", popSize);
        double crossoverProbability = 1.0;
        properties.setDouble("crossoverProbability", crossoverProbability);
        double mutationProbability = 1. / 60.;
        properties.setDouble("mutationProbability", mutationProbability);
        Variation singlecross;
        Variation bitFlip;
        Variation intergerMutation;
        Initialization initialization;

        //setup for epsilon MOEA
        double[] epsilonDouble = new double[]{0.001, 1};

        //setup for innovization
        int epochLength = 1000; //for learning rate
        int triggerOffset = 100;
        properties.setInt("nOpsToAdd", 4);
        properties.setInt("nOpsToRemove", 4);

        //setup for saving results
        properties.setBoolean("saveQuality", true);
        properties.setBoolean("saveCredits", true);
        properties.setBoolean("saveSelection", true);

        for (int i = 0; i < numRuns; i++) {

            //initialize problem
//            Problem problem = getAssignmentProblem2(path, 5, RequirementMode.FUZZYATTRIBUTE);
            Problem problem = getAssignmentProblem(path, RequirementMode.FUZZYATTRIBUTE);

            //Random knowledge operator
            Variation repairMass = new RepairMass(3000.0, 1, 1);
            Variation repairDC = new RepairDutyCycle(0.5, 1, 1);
            Variation repairPE = new RepairPackingEfficiency(0.4, 1, 1);
            Variation repairSynergy = new RepairSynergy(1);
            Variation repairInter = new RepairInterference(1);
            Variation repairInstOrb = new RepairInstrumentOrbit(1);

//            Variation[] operators = new Variation[]{
//                repairMass, repairDC, repairPE,
//                repairSynergy, repairInter, repairInstOrb};
//            RandomKnowledgeOperator rko = new RandomKnowledgeOperator(6, operators);
            HashMap<Variation, String> constraintOperatorMap = new HashMap<>();
            constraintOperatorMap.put(repairMass, "massViolationSum");
            constraintOperatorMap.put(repairDC, "dcViolationSum");
            constraintOperatorMap.put(repairPE, "packingEfficiencyViolationSum");
            constraintOperatorMap.put(repairSynergy, "synergyViolationSum");
            constraintOperatorMap.put(repairInter, "interferenceViolationSum");
            constraintOperatorMap.put(repairInstOrb, "instrumentOrbitAssignmentViolationSum");

            HashSet<String> constraints = new HashSet<>(constraintOperatorMap.values());

            initialization = new RandomInitialization(problem, popSize);
//            initialization = new NInstrumentInitializer(5, 3, (InstrumentAssignment2) problem, popSize);

            //initialize population structure for algorithm
            Population population = new Population();
            KnowledgeStochasticRanking ksr = new KnowledgeStochasticRanking(constraintOperatorMap.size(), constraintOperatorMap.values());
            DisjunctiveNormalForm dnf = new DisjunctiveNormalForm(constraints);
            EpsilonKnoweldgeConstraintComparator epskcc = new EpsilonKnoweldgeConstraintComparator(epsilonDouble, dnf);
            EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epsilonDouble);
            ChainedComparator comp = new ChainedComparator(new ParetoObjectiveComparator());
            TournamentSelection selection = new TournamentSelection(2, comp);

            switch (mode) {
                case 1: //Use epsilonMOEA Assignment

                    singlecross = new OnePointCrossover(crossoverProbability);
                    bitFlip = new BitFlip(mutationProbability);
                    intergerMutation = new IntegerUM(mutationProbability);
                    CompoundVariation var = new CompoundVariation(singlecross, bitFlip, intergerMutation);

                    Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, var, initialization);
                    ecs.submit(new InstrumentedSearch(eMOEA, properties, path + File.separator + "result", "emoea" + String.valueOf(i)));
                    break;

                case 2: //AOS search Assignment
                    ArrayList<Variation> operators = new ArrayList<>();
                    //add domain-independent heuristics
                    operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability), new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                    operators.add(repairMass);
                    operators.add(repairDC);
                    operators.add(repairPE);
                    operators.add(repairSynergy);
                    operators.add(repairInter);
                    operators.add(repairInstOrb);
                    properties.setDouble("pmin", 0.03);
                    //create operator selector
                    OperatorSelector operatorSelector = new AdaptivePursuit(operators, 0.8, 0.8, 0.03);
                    //create credit assignment
                    SetImprovementDominance creditAssignment = new SetImprovementDominance(archive, 1, 0);
                    //create AOS
                    EpsilonMOEA emoea = new EpsilonMOEA(problem, population, archive,
                            selection, null, initialization, comp);
                    AOSVariation aosStrategy = new AOSVariationSI(operatorSelector, creditAssignment, popSize);
                    AOSMOEA aos = new AOSMOEA(emoea, aosStrategy, true);
                    ////////
//                        constraintOperatorMap.put(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), new BitFlip(mutationProbability), new IntegerUM(mutationProbability)), "none");
//                        selector = new AdaptiveConstraintSelector(ksr, constraintOperatorMap,0.8, 0.8, 0.03);
                    //selector for combined
//                        ConsistencyTracker consistencyPopulation = new ConsistencyTracker(population, constraints);
//                        selector = new AOSConstraintConsistency(consistencyPopulation, constraintOperatorMap, operators, 0.8, 0.8, 0.03);
                    /////////
//                        selector = new AdaptiveConstraintHandler(ksr, constraintOperatorMap,
//                                new CompoundVariation(new OnePointCrossover(crossoverProbability, 2),
//                                        new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
//                        creditAssignment = new PopulationConsistency(constraintOperatorMap);
//                        AOSEpsilonMOEA hemoea = new AOSEpsilonMOEA(problem, population, archive, selection,
//                                initialization, selector, creditAssignment, comp);
                    
                    aos.setName("constraint_adaptive");
                    ecs.submit(new InstrumentedSearch(aos, properties, path + File.separator + "result", aos.getName() + String.valueOf(i)));
                    break;
                case 3://innovization search Assignment
                    String innovizeAssignment = "AIAA_innovize_" + System.nanoTime();
                    ArrayList<Variation> operators3 = new ArrayList<>();
                    //kdo mode set to operator or repair
                    properties.setString("kdomode", "operator");
                    //add domain-independent heuristics
                    Variation SingleCross = new CompoundVariation(new OnePointCrossover(crossoverProbability), new BitFlip(mutationProbability));
                    operators3.add(SingleCross);
                    //set up OperatorReplacementStrategy
                    EpochTrigger epochTrigger = new EpochTrigger(epochLength, triggerOffset);
                    InitialTrigger initTrigger = new InitialTrigger(triggerOffset);
                    CompoundTrigger compTrigger = new CompoundTrigger(Arrays.asList(new ReplacementTrigger[]{epochTrigger, initTrigger}));
                    EOSSOperatorCreator eossOpCreator = new EOSSOperatorCreator();
                    ArrayList<Variation> permanentOps = new ArrayList<>();
                    permanentOps.add(SingleCross);
                    RemoveNLowest operatorRemover = new RemoveNLowest(permanentOps, properties.getInt("nOpsToRemove", 2));
                    OperatorReplacementStrategy ops = new OperatorReplacementStrategy(compTrigger, operatorRemover, eossOpCreator);
                    properties.setDouble("pmin", 0.03);
                    //create operator selector
                    OperatorSelector operatorSelector3 = new AdaptivePursuit(operators3, 0.8, 0.8, 0.03);
                    //create credit assignment
                    SetImprovementDominance creditAssignment3 = new SetImprovementDominance(archive, 1, 0);
                    //create AOS
                    EpsilonMOEA emoea3 = new EpsilonMOEA(problem, population, archive,
                            selection, null, initialization, comp);
                    AOSVariation aosStrategy3 = new AOSVariationSI(operatorSelector3, creditAssignment3, popSize);
                    AOSMOEA aos3 = new AOSMOEA(emoea3, aosStrategy3, true);
                    AbstractPopulationLabeler labeler = new NondominatedSortingLabeler(.25);
                    ecs.submit(new KDOSearch(aos3, properties, labeler, ops, path + File.separator + "result", innovizeAssignment + i));
                    break;

                default:
                    throw new IllegalArgumentException(String.format("%d is an invalid option", mode));
            }
        }

        for (int i = 0; i < numRuns; i++) {
            try {
                Algorithm alg = ecs.take().get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        pool.shutdown();
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

    public static InstrumentAssignment2 getAssignmentProblem2(String path, int nSpacecraft, RequirementMode mode) {
        if (!initEOSSDatabase) {
            //initialize EOSS database
            EOSSDatabase.getInstance();
            EOSSDatabase.loadBuses(new File(path + File.separator + "config" + File.separator + "candidateBuses.xml"));
            EOSSDatabase.loadInstruments(new File(path + File.separator + "xls" + File.separator + "Instrument Capability Definition.xls"));
            EOSSDatabase.loadOrbits(new File(path + File.separator + "config" + File.separator + "candidateOrbits12.xml"));
            EOSSDatabase.loadLaunchVehicles(new File(path + File.separator + "config" + File.separator + "candidateLaunchVehicles.xml"));
            initEOSSDatabase = !initEOSSDatabase;
        }
        return new InstrumentAssignment2(path, nSpacecraft, mode, true);
    }

    public static MissionScheduling getSchedulingProblem(String path, RequirementMode mode) {
        try {
            TimeScale utc = TimeScalesFactory.getUTC();
            return new MissionScheduling(path, mode,
                    new AbsoluteDate(2010, 1, 1, utc),
                    new AbsoluteDate(2050, 1, 1, utc), 1.);
        } catch (OrekitException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BiffException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JessException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static void convertXlsToMap() {
        HashMap<HashMap<Orbit, Double>, HashMap<String, Double>> newDb = new HashMap<>();
        try {
            Workbook xls = Workbook.getWorkbook(new File("/Users/nozomihitomi/Dropbox/EOSS/problems/decadalScheduling/xls/Mission Analysis Database.xls"));
            Sheet sheet = xls.getSheet("Walker");
            int nlines = sheet.getRows();
            for (int i = 1; i < nlines; i++) {
                HashMap<Orbit, Double> orbMap = new HashMap<>();
                Cell[] row = sheet.getRow(i);
                if (Integer.parseInt(row[0].getContents()) == 1) {
                    double sa = Double.parseDouble(row[2].getContents()) * 1000. + 6378100.0;
                    String inclination = row[2].getContents();
                    Orbit.OrbitType type;
                    if (inclination.equals("12345")) {
                        inclination = "SSO";
                        type = Orbit.OrbitType.SSO;
                    } else {
                        type = Orbit.OrbitType.LEO;
                    }
                    Orbit orb = new Orbit(String.valueOf(i), type, sa, inclination, "N/A", 0, 0, 0);
                    double fov = Double.parseDouble(row[4].getContents());
                    orbMap.put(orb, fov);
                }
                HashMap<String, Double> revTimes = new HashMap<>();
                revTimes.put("avg_revisit_time", Double.parseDouble(row[5].getContents()));
                revTimes.put("avg_revisit_time_tropics", Double.parseDouble(row[6].getContents()));
                revTimes.put("avg_revisit_time_NH", Double.parseDouble(row[7].getContents()));
                revTimes.put("avg_revisit_time_SH", Double.parseDouble(row[8].getContents()));
                revTimes.put("avg_revisit_time_cold regions", Double.parseDouble(row[9].getContents()));
                revTimes.put("avg_revisit_time_US", Double.parseDouble(row[10].getContents()));
                newDb.put(orbMap, revTimes);
            }

            try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("newRevtimes"));) {
                os.writeObject(newDb);
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(ArchitectureEvaluatorParams.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BiffException ex) {
            Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
        }
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
