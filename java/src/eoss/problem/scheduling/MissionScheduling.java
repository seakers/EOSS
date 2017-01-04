///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package eoss.problem.scheduling;
//
//import architecture.io.ResultIO;
//import architecture.problem.SystemArchitectureProblem;
//import architecture.util.ValueTree;
//import eoss.problem.EOSSDatabase;
//import eoss.problem.Instrument;
//import eoss.problem.Measurement;
//import eoss.problem.Mission;
//import eoss.problem.Orbit;
//import eoss.problem.Panel;
//import eoss.problem.Spacecraft;
//import eoss.problem.assignment.InstrumentAssignmentArchitecture;
//import eoss.problem.assignment.InstrumentAssignment;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//import jxl.Cell;
//import jxl.Sheet;
//import jxl.Workbook;
//import jxl.read.biff.BiffException;
//import org.hipparchus.linear.RealMatrix;
//import org.hipparchus.linear.RealVector;
//import org.moeaframework.core.Population;
//
//import org.moeaframework.core.Solution;
//import org.moeaframework.problem.AbstractProblem;
//import org.orekit.errors.OrekitException;
//import org.orekit.time.AbsoluteDate;
//import org.orekit.time.TimeScalesFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import org.xml.sax.SAXException;
//
///**
// *
// * @author nozomihitomi
// */
//public class MissionScheduling extends AbstractProblem implements SystemArchitectureProblem {
//
//    /**
//     * The budget of a series of dates
//     */
//    private final ArrayList<Budget> budgets;
//
//    /**
//     * The missions to schedule
//     */
//    private final HashMap<Integer, Mission> missionMap;
//
//    /**
//     * The start date of the analysis
//     */
//    private final AbsoluteDate startDate;
//
//    /**
//     * The end date of the analysis
//     */
//    private final AbsoluteDate endDate;
//
//    /**
//     * The time step in years to take evaluate each continuity metric
//     */
//    private final double timeStep;
//
//    /**
//     * The discount rates for each panel
//     */
//    private final HashSet<Panel> panels;
//
//    /**
//     * The data continuity matrix for each mission
//     */
//    private final HashMap<Mission, DataContinutityMatrix> missionDataContinuity;
//
//    /**
//     * The pre-existing data continuity matrix
//     */
//    private final DataContinutityMatrix precursors;
//
//    /**
//     * The weights for importance of gaps in each measurement
//     */
//    private final RealVector measurementWeights;
//
//    /**
//     * The weights for relative importance of gaps in the future with respect to
//     * gaps now
//     */
//    private final RealVector continuityWeights;
//
//    /**
//     * Creates a mission scheduling problem from the path. This method will
//     * search for data files within the directory given by the path
//     *
//     * @param path the path to the directory containing the necessary info files
//     * @param starDate the start date of the analysis
//     * @param endDate the end date of the analysis
//     * @param timeStep
//     * @throws java.text.ParseException
//     * @throws org.orekit.errors.OrekitException
//     * @throws jxl.read.biff.BiffException
//     * @throws javax.xml.parsers.ParserConfigurationException
//     * @throws java.io.IOException
//     * @throws org.xml.sax.SAXException
//     */
//    public MissionScheduling(String path, AbsoluteDate starDate, AbsoluteDate endDate, double timeStep) throws ParseException, OrekitException, BiffException, IOException, SAXException, ParserConfigurationException {
//        //2 decisions for Choosing and Assigning Patterns
//        super(2, 2);
//        this.startDate = starDate;
//        this.endDate = endDate;
//        this.timeStep = timeStep;
//
//        ArrayList<Mission> missions = null;
//
//        //load missions that are to be scheduled
//        missions = loadMissions(new File(path + File.separator + "config" + File.separator + "missions.xml"));
//        //load the panels that have stakes in these missions
//        this.panels = new HashSet<>(loadPanels(new File(path + File.separator + "config" + File.separator + "panels.xml")));
//        //load the weights on the measurements invovled in these missions
//        HashMap<Measurement, Double> measurementWts = loadMeasurementWeights(new File(path + File.separator + "xls" + File.separator + "Data Continuity Requirements.xls"));
//        HashMap<AbsoluteDate, Double> continuityWts = loadContinuityWeights(new File(path + File.separator + "xls" + File.separator + "Data Continuity Requirements.xls"));
//        
//        
//        HashSet<String> existingMissionNames = loadExistingMissions(new File(path + File.separator + "xls" + File.separator + "Data Continuity Requirements.xls"));
//        
//        
//        this.precursors = loadPrecursor(path);
//        this.budgets = loadBudget(new File(path + File.separator + "config" + File.separator + "budget.txt"));
//
//        //load scores for each mission
//        Collection<Mission> scoredMissions = getSingleMissionScores(path, missions);
//        this.missionMap = new HashMap<>(scoredMissions.size());
//        int i = 0;
//        for (Mission m : scoredMissions) {
//            this.missionMap.put(i, m);
//        }
//
//    }
//
//    public MissionScheduling(ArrayList<Budget> budgets,
//            DataContinutityMatrix precursors,
//            HashMap<Mission, DataContinutityMatrix> missionDataContinuity,
//            HashSet<Panel> panels,
//            RealVector continuityWeights, RealVector measurementWeights) {
//        super(1, 2, 0); //1 permutation variable, 2 objectives, 0 constraints
//        this.budgets = budgets;
//        this.precursors = precursors;
//        this.missionDataContinuity = missionDataContinuity;
//        this.panels = panels;
//        this.continuityWeights = continuityWeights;
//        this.measurementWeights = measurementWeights;
//        this.startDate = precursors.getStartDate();
//        this.missionMap = new HashMap<>();
//        int i = 0;
//        for (Mission m : missionDataContinuity.keySet()) {
//            missionMap.put(i, m);
//            i++;
//        }
//    }
//
//    @Override
//    public void evaluate(Solution sltn) {
//        MissionSchedulingArchitecture arch = (MissionSchedulingArchitecture) sltn;
//        arch.setLaunchDates(computeLaunchDates(arch));
//        sltn.setObjective(0, computeDiscountedValue(arch));
//        sltn.setObjective(1, computeDataContinuityScore(arch));
//    }
//
//    /**
//     * Loads the importance of the measurements from the given excel file
//     *
//     * @param file the excel file that contains the importance of each
//     * measurement
//     * @return all the measurements and
//     * @throws BiffException
//     * @throws IOException
//     */
//    private HashMap<Measurement, Double> loadMeasurementWeights(File file) throws BiffException, IOException {
//        System.out.println("Loading measurement weights...");
//        Workbook xls = Workbook.getWorkbook(file);
//        Sheet meas = xls.getSheet("Measurement Importance");
//        HashMap<Measurement, Double> out = new HashMap<>(meas.getRows());
//
//        for (int i = 1; i < meas.getRows(); i++) {
//            Cell[] row = meas.getRow(i);
//            String measurementName = row[0].getContents();
//            double importance = Double.parseDouble(row[1].getContents());
//
//            if (out.put(new Measurement.Builder(measurementName).build(), importance) != null) {
//                throw new IllegalArgumentException(String.format("Measurement %s already exists.", measurementName));
//            }
//        }
//        xls.close();
//        return out;
//    }
//
//    /**
//     * Loads the continuity weights
//     *
//     * @param file the excel file that contains the importance of each
//     * measurement
//     * @return all the measurements and
//     * @throws BiffException
//     * @throws IOException
//     */
//    private HashMap<AbsoluteDate, Double> loadContinuityWeights(File file) throws BiffException, IOException, OrekitException {
//        System.out.println("Loading continuity weights...");
//        Workbook xls = Workbook.getWorkbook(file);
//        Sheet times = xls.getSheet("Discounting Scheme");
//        HashMap<AbsoluteDate, Double> out = new HashMap<>(times.getRows());
//
//        for (int i = 1; i < times.getRows(); i++) {
//            Cell[] row = times.getRow(i);
//            int year = Integer.parseInt(row[0].getContents());
//            int month = Integer.parseInt(row[1].getContents());
//            AbsoluteDate date = new AbsoluteDate(year, month, 0, TimeScalesFactory.getUTC());
//            double importance = Double.parseDouble(row[2].getContents());
//
//            if (out.put(date, importance) != null) {
//                throw new IllegalArgumentException(String.format("Date %s already exists.", date));
//            }
//        }
//        xls.close();
//        return out;
//    }
//
//    /**
//     * Loads the names of the existing missions to consider
//     *
//     * @param file the excel file that contains the importance of each
//     * measurement
//     * @return the existing missions to consider
//     * @throws BiffException
//     * @throws IOException
//     */
//    private HashSet<String> loadExistingMissions(File file) throws BiffException, IOException {
//        System.out.println("Loading existing missions...");
//        Workbook xls = Workbook.getWorkbook(file);
//        Sheet times = xls.getSheet("Discounting Scheme");
//        HashSet<String> out = new HashSet<>();
//
//        for (int i = 1; i < times.getRows(); i++) {
//            Cell[] row = times.getRow(i);
//            if (!row[1].getContents().equals("1")) {
//                continue;
//            }
//            String missionName = row[0].getContents();
//            if (!out.add(missionName)) {
//                throw new IllegalArgumentException(String.format("Mission %s already exists.", missionName));
//            }
//        }
//        xls.close();
//        return out;
//    }
//
//    private DataContinutityMatrix loadPrecursor(String path) throws BiffException, IOException, ParseException, OrekitException {
//        System.out.println("Loading precursor data continuity matrix...");
//        DataContinutityMatrix matrix = new DataContinutityMatrix(timeStep, startDate, endDate);
//
//        Workbook ceos = Workbook.getWorkbook(new File(path + File.separator + "xls" + File.separator + "CEOS" + File.separator + "Measurements CEOS.xls"));
//        Sheet sheet = ceos.getSheet("Sheet1");
//        //map connecting description and measurment name
//        HashMap<String, String> measurementMap = new HashMap<>(sheet.getRows());
//        for (int i = 1; i < sheet.getRows(); i++) {
//            Cell[] row = sheet.getRow(i);
//            if (row[1].getContents().contains("m")) {
//                measurementMap.put(row[0].getContents(), row[2].getContents());
//            }
//        }
//        ceos.close();
//
//        //go through individual measurment excel files
//        SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
//        for (int i = 0; i < measurementMap.size(); i++) {
//            String filename = path + File.separator + "xls" + File.separator + "CEOS" + File.separator + "m" + i + ".xls";
//            System.out.println("Reading file " + filename);
//            Workbook m = Workbook.getWorkbook(new File(filename));
//            Sheet s = m.getSheet(0);
//            for (int j = 1; j < s.getRows(); j++) {
//                Cell[] row = s.getRow(j);
//                String measType = row[0].getContents();
//                String missionName = row[1].getContents();
//                AbsoluteDate launchDate = new AbsoluteDate(f.parse(row[2].getContents()), TimeScalesFactory.getUTC());
//                AbsoluteDate endOfLifeDate = new AbsoluteDate(f.parse(row[3].getContents()), TimeScalesFactory.getUTC());
//                String missionStatus = row[4].getContents();
//                String instrument = row[5].getContents();
//
//                Instrument inst = new Instrument(instrument, null);
//
//                Measurement meas = new Measurement(measurementMap.get(measType), Double.NaN, Double.NaN, Double.NaN, Double.NaN);
//
//                Mission.MissionStatus status = null;
//                switch (missionStatus) {
//                    case "Past Mission":
//                        status = Mission.MissionStatus.PAST;
//                        break;
//                    case "Currently being flown":
//                        status = Mission.MissionStatus.FLYING;
//                        break;
//                    case "Planned":
//                        status = Mission.MissionStatus.PLANNED;
//                        break;
//                    case "Approved":
//                        status = Mission.MissionStatus.APPROVED;
//                        break;
//                    default:
//                        throw new IllegalArgumentException(String.format("Mission status %s not recognized in row %d of %s", missionStatus, j, filename));
//                }
//
//                HashMap spacecraft = new HashMap(1);
//                spacecraft.put(new Spacecraft(Arrays.asList(new Instrument[]{inst})), null);
//
//                Mission miss = new Mission(missionName, spacecraft, launchDate, endOfLifeDate, status, 0, 0.0, null);
//
//                for (AbsoluteDate date : matrix.getAllowableDates()) {
//                    if (date.compareTo(launchDate) >= 0 && date.compareTo(endOfLifeDate) <= 0) {
//                        matrix.addDataContinutity(meas, endDate, miss, inst);
//                    }
//                }
//            }
//        }
//        return matrix;
//    }
//
//    private ArrayList<Mission> loadMissions(File file) throws IOException, SAXException, ParserConfigurationException {
//        ArrayList<Mission> out = new ArrayList<>();
//        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        Document doc = dBuilder.parse(file);
//        doc.getDocumentElement().normalize();
//        NodeList missionsNode = doc.getElementsByTagName("mission");
//        for (int i = 0; i < missionsNode.getLength(); i++) {
//            Element mission = (Element) missionsNode.item(i);
//            String name = mission.getElementsByTagName("name").item(0).getTextContent();
//            double lifetime = Double.parseDouble(mission.getElementsByTagName("lifetime").item(0).getTextContent());
//
//            NodeList budgetingList = mission.getElementsByTagName("budgeting");
//            if (budgetingList.getLength() > 1) {
//                throw new IllegalArgumentException(String.format("Only one budgeting node allowed. Error in mission %s", name));
//            }
//            Element budgeting = (Element) budgetingList.item(0);
//            int devYr = Integer.parseInt(budgeting.getElementsByTagName("developmentYears").item(0).getTextContent());
//            double devCostYr = Double.parseDouble(budgeting.getElementsByTagName("costPerYear").item(0).getTextContent());
//
//            HashMap<Spacecraft, Orbit> crafts = new HashMap<>();
//
//            //a mission may have multiple spacecraft if it is a constellation or distributed satellite mission
//            NodeList craftList = mission.getElementsByTagName("spacecraft");
//            for (int j = 0; j < craftList.getLength(); j++) {
//
//                NodeList payloadList = mission.getElementsByTagName("payload");
//                if (payloadList.getLength() != 1) {
//                    throw new IllegalArgumentException(String.format("Spacecraft must have one and only one paylaod. Error in mission %s", name));
//                }
//                NodeList payload = ((Element) payloadList.item(0)).getElementsByTagName("instrument");
//                ArrayList<Instrument> instruments = new ArrayList<>();
//                for (int k = 0; k < payload.getLength(); k++) {
//                    String instName = payload.item(k).getNodeValue();
//                    instruments.add(EOSSDatabase.getInstrument(instName));
//                }
//
//                NodeList orbitList = mission.getElementsByTagName("orbitName");
//                if (payloadList.getLength() != 1) {
//                    throw new IllegalArgumentException(String.format("Spacecraft must have one and only one orbit. Error in mission %s", name));
//                }
//                String orbitName = orbitList.item(0).getTextContent();
//                crafts.put(new Spacecraft(instruments), EOSSDatabase.getOrbit(orbitName));
//            }
//            out.add(new Mission.Builder(name, crafts).devYr(devYr).devCostYr(devCostYr).lifetime(lifetime).build());
//        }
//        return out;
//    }
//
//    private ArrayList<Panel> loadPanels(File file) throws IOException, SAXException, ParserConfigurationException {
//        ArrayList<Panel> out = new ArrayList<>();
//        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        Document doc = dBuilder.parse(file);
//        doc.getDocumentElement().normalize();
//        NodeList panelList = doc.getElementsByTagName("panel");
//        for (int i = 0; i < panelList.getLength(); i++) {
//            Element panel = (Element) panelList.item(i);
//            String id = panel.getElementsByTagName("id").item(0).getTextContent();
//            double discountRate = Double.parseDouble(panel.getElementsByTagName("discountRate").item(0).getTextContent());
//            out.add(new Panel.Builder(id).discountRate(discountRate).build());
//        }
//        return out;
//    }
//
//    private ArrayList<Budget> loadBudget(File file) throws OrekitException, ParseException {
//        ArrayList<Budget> budget = new ArrayList<>();
//        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                if (line.startsWith("#")) {
//                    continue;
//                } else {
//                    String[] tokens = line.trim().split(",");
//                    SimpleDateFormat f = new SimpleDateFormat("yyyy");
//                    budget.add(new Budget(new AbsoluteDate(f.parse(tokens[0]), TimeScalesFactory.getUTC()),
//                            Double.parseDouble(tokens[1])));
//                }
//            }
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(MissionScheduling.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(MissionScheduling.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return budget;
//    }
//
//    /**
//     * Computes the dates of the launches from the sequence, the available
//     * budget, and the cost profile for each mission
//     *
//     * @param schedule the schedule of the missions
//     * @return the dates of the launches
//     */
//    private HashMap<Mission, AbsoluteDate> computeLaunchDates(MissionSchedulingArchitecture schedule) {
//        final HashMap<Mission, AbsoluteDate> launchDates = new HashMap<>(missionMap.size());
//
//        double currentBudget = 0;
//        int profileCounter = 0;
//        int missionCounter = 0;
//        double[] currentCostProfile = missionMap.get(schedule.getSequence()[missionCounter]).getCostProfile();
//        for (Budget b : budgets) {
//            currentBudget += b.getBudget();
//
//            //check what can be funded
//            while (missionCounter < missionMap.size()) {
//                if (currentBudget >= currentCostProfile[profileCounter]) {
//                    currentBudget -= currentCostProfile[profileCounter];
//                    currentCostProfile[profileCounter] = 0;
//                    profileCounter++;
//                    if (profileCounter == currentCostProfile.length) {
//                        //launch mission
//                        launchDates.put(missionMap.get(missionCounter), b.getDate());
//
//                        //move to next mission
//                        missionCounter++;
//                        currentCostProfile = missionMap.get(schedule.getSequence()[missionCounter]).getCostProfile();
//                        profileCounter = 0;
//                    }
//                } else {
//                    //spend all the budget on the available part of the mission
//                    currentCostProfile[profileCounter] -= currentBudget;
//                    currentBudget = 0;
//                    break;
//                }
//            }
//        }
//
//        return launchDates;
//    }
//
//    /**
//     * Computes the discounted value. The original value is how much each panel
//     * benefits from a specific mission operating at a given date. These values
//     * are discounted as a function of a discount rate r and time dt (d =
//     * d_0*exp(-r*dt))
//     *
//     * @param dates the launch dates computed for the
//     * @param schedule a schedule of missions
//     * @return the discounted value of the given schedule
//     */
//    private double computeDiscountedValue(MissionSchedulingArchitecture schedule) {
//        ArrayList<AbsoluteDate> sortedDates = new ArrayList<>(schedule.getLaunchDates().values());
//        Collections.sort(sortedDates);
//
//        double discountedValue = 0;
//        for (int i = 0; i < missionMap.size(); i++) {
//            double dt = (sortedDates.get(i).durationFrom(startDate) / (365 * 24 * 3600.)); //convert sec to years
//            Mission m = missionMap.get(schedule.getSequence()[i]);
//            HashMap<Panel, Double> missionPanelScore = m.getScores();
//            int j = 0;
//            for (Panel p : panels) {
//                double r = p.getDiscountFactor();
//                discountedValue += missionPanelScore.get(p) * Math.exp(-r * dt);
//                j++;
//            }
//        }
//        return discountedValue;
//    }
//
//    /**
//     * Computes the score for continuing measurements with the given schedule of
//     * missions
//     *
//     * @param schedule schedule of missions
//     * @return the score for continuing measurements with the given schedule of
//     * missions
//     */
//    private double computeDataContinuityScore(MissionSchedulingArchitecture schedule) {
//        int[] permutation = schedule.getSequence();
//        DataContinutityMatrix overallMatrix = precursors;
//        for (int i = 0; i < missionMap.size(); i++) {
//            // Offset corresponding params.MissionMatrices according to launch date and lifetime
//            Mission currentMission = missionMap.get(permutation[i]);
//            DataContinutityMatrix matrix0 = missionDataContinuity.get(currentMission);
//            double lifetime = currentMission.getLifetime();
//            AbsoluteDate launchdate2 = schedule.getLaunchDates().get(currentMission);
//            DataContinutityMatrix matrix1 = matrix0.offset(lifetime, launchdate2);
//
//            // Superimpose all matrices
//            overallMatrix = matrix1.merge(overallMatrix);
//        }
//
//        // Compute data continuity score from new matrix
//        RealMatrix diff = overallMatrix.count().subtract(precursors.count());
//        for (int i = 0; i < diff.getRowDimension(); i++) {
//            for (int j = 0; j < diff.getColumnDimension(); j++) {
//                if (diff.getEntry(i, j) < 0.) {
//                    diff.setEntry(i, j, 0.); //only improvements are considered
//                }
//            }
//        }
//
//        return measurementWeights.dotProduct(diff.operate(continuityWeights));
//    }
//
//    /**
//     * Sets the scores to each mission
//     *
//     * @param path
//     */
//    private HashSet<Mission> getSingleMissionScores(String path, Collection<Mission> missions) {
//        Population pop;
//
//        String filename = path + File.separator + "singleMissionScores.pop";
//        try {
//            //get the scores of each mission
//            pop = ResultIO.loadPopulation(filename);
//            System.out.println("Found single mission scores. Using " + filename);
//            //if no file found, compute the scores
//        } catch (IOException ex) {
//            System.out.println("Did not find single mission scores in " + filename + ". Computing single mission scores");
//
//            InstrumentAssignment prob = new InstrumentAssignment(path, "FUZZY-ATTRIBUTES", "normal", new int[]{1}, false, true);
//
//            //create a population of all single missions
//            pop = new Population();
//            for (Mission m : missions) {
//                InstrumentAssignmentArchitecture arch = (InstrumentAssignmentArchitecture) prob.newSolution();
//                for (Spacecraft s : m.getSpacecraft().keySet()) {
//                    for (Instrument inst : s.getPaylaod()) {
//                        arch.addInstrumentToOrbit(inst, m.getSpacecraft().get(s));
//                    }
//                }
//                pop.add(arch);
//                prob.evaluate(arch);
//            }
//            ResultIO.savePopulation(pop, path + File.separator + "singleMissionScores");
//        }
//
//        HashSet<Mission> scoredMissions = new HashSet<>(missions.size());
//        for (Solution s : pop) {
//            InstrumentAssignmentArchitecture arch = (InstrumentAssignmentArchitecture) s;
//            Mission mission = arch.getMissions().get(0); //since only one mission should be simulated
//            for (Mission m : missions) {
//                if (mission.equals(m)) {
//                    HashMap<Panel, Double> panelScores = new HashMap<>(this.panels.size());
//                    ValueTree tree = arch.getValueTree();
//                    for (Panel p : panels) {
//                        panelScores.put(p, tree.getScore(p.getName()));
//                    }
//                    scoredMissions.add(m.getBuilder().panelScores(panelScores).build());
//                }
//            }
//        }
//
//        //check that all missions have panel scores
//        for (Mission m : missions) {
//            if (!scoredMissions.contains(m)) {
//                throw new IllegalStateException(String.format("One of the missions is not scored: %s", m.getName()));
//            }
//        }
//        return scoredMissions;
//    }
//
//    @Override
//    public Solution newSolution() {
//        return new MissionSchedulingArchitecture(1, 2);
//    }
//
//}
