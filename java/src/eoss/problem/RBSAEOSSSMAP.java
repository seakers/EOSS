/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import eoss.problem.evaluation.ArchitectureEvaluatorParams;
import aos.aos.AOSEpsilonMOEA;
import aos.aos.AOSFactory;
import aos.creditassigment.CreditDefFactory;
import aos.creditassigment.ICreditAssignment;
import aos.nextoperator.INextOperator;
import aos.operatorselectors.replacement.EpochTrigger;
import aos.operatorselectors.replacement.OperatorReplacementStrategy;
import aos.operatorselectors.replacement.RemoveNLowest;
import eoss.problem.assignment.InstrumentAssignment;
import eoss.problem.assignment.InstrumentAssignment2;
import eoss.problem.evaluation.RequirementMode;
import eoss.problem.scheduling.MissionScheduling;
import java.io.File;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.OnePointCrossover;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.operator.binary.BitFlip;
import org.moeaframework.util.TypedProperties;
import eoss.search.InnovizationSearch;
import eoss.search.InstrumentedSearch;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
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
import knowledge.constraint.AdaptiveConstraintSelection;
import knowledge.constraint.EpsilonKnoweldgeConstraintComparator;
import knowledge.constraint.PopulationConsistency;
import knowledge.operator.EOSSOperatorCreator;
import knowledge.constraint.KnowledgeStochasticRanking;
import knowledge.operator.RepairDutyCycle;
import knowledge.operator.RepairInstrumentOrbit;
import knowledge.operator.RepairInterference;
import knowledge.operator.RepairMass;
import knowledge.operator.RepairPackingEfficiency;
import knowledge.operator.RepairSynergy;
import mining.label.AbstractPopulationLabeler;
import mining.label.NondominatedSortingLabeler;
import orekit.util.OrekitConfig;
import org.moeaframework.algorithm.EpsilonMOEA;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.Population;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoObjectiveComparator;
import org.moeaframework.core.operator.CompoundVariation;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.integer.IntegerUM;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.xml.sax.SAXException;

/**
 *
 * @author dani
 */
public class RBSAEOSSSMAP {

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

        //PATH
        if (args.length == 0) {
            args = new String[4];
//            args[0] = "C:\\Users\\SEAK2\\Nozomi\\EOSS\\problems\\climateCentric";
//            args[0] = "C:\\Users\\SEAK1\\Nozomi\\EOSS\\problems\\climateCentric";
            args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/climateCentric";
//            args[0] = "/Users/nozomihitomi/Dropbox/EOSS/problems/decadalScheduling";
            args[1] = "2"; //Mode
            args[2] = "1"; //numCPU
            args[3] = "1"; //numRuns
        }

        System.out.println("Path set to " + args[0]);
        System.out.println("Running mode " + args[1]);
        System.out.println("Will get " + args[2] + " resources");
        System.out.println("Will do " + args[3] + " runs");

        String path = args[0];

        //record the current class java file to save the parameters for future reference
        quine(new File(String.join(File.separator, new String[]{System.getProperty("user.dir"),"java","src","eoss","problem","RBSAEOSSSMAP.java"})),
                new File(String.join(File.separator,new String[]{path,"result","RBSAEOSSSMAP.java"})));
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
        int popSize = 10;
        int maxEvals = 20;
        properties.setInt("maxEvaluations", maxEvals);
        properties.setInt("populationSize", popSize);
        double crossoverProbability = 1.0;
        double mutationProbability = 1. / 65.;
        Variation singlecross;
        Variation bitFlip;
        Variation intergerMutation;
        Initialization initialization;

        //setup for epsilon MOEA
        DominanceComparator comparator = new ParetoDominanceComparator();
        double[] epsilonDouble = new double[]{0.001, 0.001};
        final TournamentSelection selection = new TournamentSelection(2, comparator);

        //setup for innovization
        int epochLength = 1000; //for learning rate
        properties.setInt("nOpsToAdd", 4);
        properties.setInt("nOpsToRemove", 4);

        //setup for saving results
        properties.setBoolean("saveQuality", true);
        properties.setBoolean("saveCredits", true);
        properties.setBoolean("saveSelection", true);

        //initialize EOSS database
        EOSSDatabase.getInstance();
        EOSSDatabase.loadBuses(new File(path + File.separator + "config" + File.separator + "candidateBuses.xml"));
        EOSSDatabase.loadInstruments(new File(path + File.separator + "xls" + File.separator + "Instrument Capability Definition.xls"));
        EOSSDatabase.loadOrbits(new File(path + File.separator + "config" + File.separator + "candidateOrbits.xml"));
        EOSSDatabase.loadLaunchVehicles(new File(path + File.separator + "config" + File.separator + "candidateLaunchVehicles.xml"));

        for (int i = 0; i < numRuns; i++) {

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
            constraintOperatorMap.put(repairInstOrb, "instrumentOrbitAssingmentViolationSum");

            //initialize problem
            Problem problem = getAssignmentProblem2(path, 5, RequirementMode.FUZZYATTRIBUTE);

            //initialize population structure for algorithm
            Population population = new Population();
            KnowledgeStochasticRanking ksr = new KnowledgeStochasticRanking(constraintOperatorMap.size(), constraintOperatorMap.values());
            EpsilonKnoweldgeConstraintComparator epskcc = new EpsilonKnoweldgeConstraintComparator(epsilonDouble, ksr);
            EpsilonBoxDominanceArchive archive = new EpsilonBoxDominanceArchive(epskcc);
            ChainedComparator comp = new ChainedComparator(ksr, new ParetoObjectiveComparator());

            switch (mode) {
                case 1: //Use epsilonMOEA Assignment

                    singlecross = new OnePointCrossover(crossoverProbability);
                    bitFlip = new BitFlip(mutationProbability);
                    intergerMutation = new IntegerUM(mutationProbability);
                    CompoundVariation var = new CompoundVariation(singlecross, bitFlip, intergerMutation);

                    initialization = new RandomInitialization(problem, popSize);
                    Algorithm eMOEA = new EpsilonMOEA(problem, population, archive, selection, var, initialization);
                    ecs.submit(new InstrumentedSearch(eMOEA, properties, path + File.separator + "result", "emoea" + String.valueOf(i)));
                    break;

                case 2://AOS search Assignment
                    try {
                        ICreditAssignment creditAssignment = CreditDefFactory.getInstance().getCreditDef("SIDo", properties, problem);
                        ArrayList<Variation> operators = new ArrayList();

                        //add domain-independent heuristics
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), repairMass, new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), repairDC, new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), repairPE, new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), repairSynergy, new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), repairInter, new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        operators.add(new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), repairInstOrb, new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));

                        properties.setDouble("pmin", 0.03);

                        //all other properties use default parameters
                        INextOperator selector = AOSFactory.getInstance().getHeuristicSelector("AP", properties, operators);

                        /////////
                        selector = new AdaptiveConstraintSelection(ksr, constraintOperatorMap,
                                new CompoundVariation(new OnePointCrossover(crossoverProbability, 2),
                                        new BitFlip(mutationProbability), new IntegerUM(mutationProbability)));
                        creditAssignment = new PopulationConsistency(constraintOperatorMap);

                        initialization = new RandomInitialization(problem, popSize);

                        AOSEpsilonMOEA hemoea = new AOSEpsilonMOEA(problem, population, archive, selection,
                                initialization, selector, creditAssignment);
                        hemoea.setName("constraint_adaptive");
                        ecs.submit(new InstrumentedSearch(hemoea, properties, path + File.separator + "result", hemoea.getName() + String.valueOf(i)));

                    } catch (IOException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case 3://innovization search Assignment
                    String innovizeAssignment = "AIAA_innovize_" + System.nanoTime();
                    try {
                        ICreditAssignment creditAssignment = CreditDefFactory.getInstance().getCreditDef("SIDo", properties, problem);

                        ArrayList<Variation> operators2 = new ArrayList();

                        //add domain-independent heuristics
                        Variation SingleCross = new CompoundVariation(new OnePointCrossover(crossoverProbability, 2), new BitFlip(mutationProbability));
                        operators2.add(SingleCross);

                        //set up OperatorReplacementStrategy
                        EpochTrigger epochTrigger = new EpochTrigger(epochLength);
                        EOSSOperatorCreator eossOpCreator = new EOSSOperatorCreator(crossoverProbability, mutationProbability);
                        ArrayList<Variation> permanentOps = new ArrayList();
                        permanentOps.add(SingleCross);
                        RemoveNLowest operatorRemover = new RemoveNLowest(permanentOps, properties.getInt("nOpsToRemove", 2));
                        OperatorReplacementStrategy ops = new OperatorReplacementStrategy(epochTrigger, operatorRemover, eossOpCreator);

                        properties.setDouble("pmin", 0.03);

                        //all other properties use default parameters
                        INextOperator selector = AOSFactory.getInstance().getHeuristicSelector("AP", properties, operators2);

                        initialization = new RandomInitialization(problem, popSize);

                        AOSEpsilonMOEA hemoea = new AOSEpsilonMOEA(problem, population, archive, selection,
                                initialization, selector, creditAssignment);

                        AbstractPopulationLabeler labeler = new NondominatedSortingLabeler(.25);
                        ecs.submit(new InnovizationSearch(hemoea, properties, labeler, ops, path + File.separator + "result", innovizeAssignment + i));
                    } catch (IOException ex) {
                        Logger.getLogger(RBSAEOSSSMAP.class.getName()).log(Level.SEVERE, null, ex);

                    }
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
        return new InstrumentAssignment(path, mode, ArchitectureEvaluatorParams.altnertivesForNumberOfSatellites, true, new File(path + File.separator + "database" + File.separator + "solutions.dat"));
    }

    public static InstrumentAssignment2 getAssignmentProblem2(String path, int nSpacecraft, RequirementMode mode) {
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
        HashMap<HashMap<Orbit, Double>, HashMap<String, Double>> newDb = new HashMap();
        try {
            Workbook xls = Workbook.getWorkbook(new File("/Users/nozomihitomi/Dropbox/EOSS/problems/decadalScheduling/xls/Mission Analysis Database.xls"));
            Sheet sheet = xls.getSheet("Walker");
            int nlines = sheet.getRows();
            for (int i = 1; i < nlines; i++) {
                HashMap<Orbit, Double> orbMap = new HashMap();
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
                HashMap<String, Double> revTimes = new HashMap();
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
