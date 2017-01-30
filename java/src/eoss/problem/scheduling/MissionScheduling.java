/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import architecture.problem.SystemArchitectureProblem;
import architecture.util.ValueTree;
import eoss.problem.evaluation.ArchitectureEvaluator;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.problem.Measurement;
import eoss.problem.Mission;
import eoss.problem.Orbit;
import eoss.problem.Panel;
import eoss.problem.Spacecraft;
import eoss.problem.ValueAggregationBuilder;
import eoss.problem.evaluation.RequirementMode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jess.Fact;
import jess.JessException;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author nozomihitomi
 */
public class MissionScheduling extends AbstractProblem implements SystemArchitectureProblem {

    /**
     * Evaluates missions
     */
    private final ArchitectureEvaluator eval;

    /**
     * The budget of a series of dates
     */
    private final ArrayList<Budget> budgets;

    /**
     * The missions to schedule
     */
    private final ArrayList<Mission> missions;

    /**
     * The start date of the analysis
     */
    private final AbsoluteDate startDate;

    /**
     * The end date of the analysis
     */
    private final AbsoluteDate endDate;

    /**
     * The time step in years to take evaluate each continuity metric
     */
    private final double timeStep;

    /**
     * The discount rates for each panel
     */
    private final HashSet<Panel> panels;

    /**
     * The data continuity matrix for each mission
     */
    private final HashMap<Mission, DataContinutityMatrix> missionDataContinuity;
    
    /**
     * The value tree for each mission
     */
    private final HashMap<Mission, ValueTree> missionValueTree;

    /**
     * The pre-existing data continuity matrix
     */
    private final DataContinutityMatrix precursors;

    /**
     * The weights for importance of gaps in each measurement
     */
    private final RealVector measurementWeights;

    /**
     * The weights for relative importance of gaps in the future with respect to
     * gaps now
     */
    private final RealVector continuityWeights;

    /**
     * Creates a mission scheduling problem from the path. This method will
     * search for data files within the directory given by the path
     *
     * @param path the path to the directory containing the necessary info files
     * @param reqMode
     * @param starDate the start date of the analysis
     * @param endDate the end date of the analysis
     * @param timeStep the time step [years] for this analysis
     * @throws java.text.ParseException
     * @throws org.orekit.errors.OrekitException
     * @throws jxl.read.biff.BiffException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public MissionScheduling(String path, RequirementMode reqMode, AbsoluteDate starDate, AbsoluteDate endDate, double timeStep) throws ParseException, OrekitException, BiffException, IOException, SAXException, ParserConfigurationException, JessException {
        //2 decisions for Choosing and Assigning Patterns
        super(2, 2);
        this.startDate = starDate;
        this.endDate = endDate;
        this.timeStep = timeStep;

        //load the panels that have stakes in these missions
        this.panels = new HashSet<>(loadPanels(new File(path + File.separator + "config" + File.separator + "panels.xml")));
        ValueTree template = ValueAggregationBuilder.build(new File(path + File.separator + "config" + File.separator + "panels.xml"));

        //create architecture evaluator
        this.eval = new ArchitectureEvaluator(path, reqMode, false, true, template);
        
        //load the existing missions and the measurements they take
        String xlsPath = path + File.separator + "xls" + File.separator;
        HashSet<String> existingMissionNames = loadExistingMissions(new File(xlsPath + "Data Continuity Requirements.xls"));
        this.precursors = loadPrecursor(path);
        
        //load the weights on the measurements invovled in these missions
        HashMap<Measurement, Double> measWts = loadMeasurementWeights(new File(xlsPath + "Data Continuity Requirements.xls"));
        this.measurementWeights = new ArrayRealVector(precursors.getMeasurements().size());
        int i = 0;
        for(Measurement m : precursors.getMeasurements()){
            this.measurementWeights.addToEntry(i, measWts.get(m));
            i++;
        }
        
        //load the continuity weights
        HashMap<AbsoluteDate, Double> contWts = loadContinuityWeights(new File(xlsPath + "Data Continuity Requirements.xls"));
        ArrayList<AbsoluteDate> sortedDates = new ArrayList(contWts.keySet());
        Collections.sort(sortedDates);
        this.continuityWeights = new ArrayRealVector(sortedDates.size());
        for(int j=0; j< sortedDates.hashCode(); j++){
            this.continuityWeights.addToEntry(j, contWts.get(sortedDates.get(j)));
        }

        //load missions that are to be scheduled
        this.missions = loadMissions(new File(path + File.separator + "config" + File.separator + "missions.xml"));
        this.missionValueTree = new HashMap();
        this.missionDataContinuity = new HashMap<>();
        processMissions(path, missions, measWts.keySet());
        Collections.unmodifiableList(missions);

        this.budgets = loadBudget(new File(path + File.separator + "config" + File.separator + "budget.txt"));
    }

    @Override
    public void evaluate(Solution sltn) {
        MissionSchedulingArchitecture arch = (MissionSchedulingArchitecture) sltn;
        arch.setLaunchDates(computeLaunchDates(arch));
        sltn.setObjective(0, computeDiscountedValue(arch));
        sltn.setObjective(1, computeDataContinuityScore(arch));
    }

    /**
     * Computes the value tree and data continuity matrix for the missions that are to be
     * scheduled
     *
     * @param path
     * @param missions the missions to be scheduled
     * @param measurements the measurements involved in this analysis
     * @return
     */
    private void processMissions(
            String path, Collection<Mission> missions,
            Collection<Measurement> measurements) throws JessException {
        //evaluate each mission
        System.out.println("Evaluating candidate missions...");
        for (Mission m : missions) {
            Spacecraft s = m.getSpacecraft().keySet().iterator().next(); //since only one spacecraft in mission
            Orbit o = m.getSpacecraft().get(s);
            ValueTree vt = eval.performance(Arrays.asList(new Mission[]{m}));
            vt.computeScores();
            this.missionValueTree.put(m, vt);
            
            DataContinutityMatrix dcMatrix = new DataContinutityMatrix(timeStep, startDate, endDate);

            for (Measurement meas : measurements) {
                Collection<Fact> facts = eval.makeQuery(
                        "REQUIREMENTS::Measurement (Parameter " + meas.getName() + ")"
                                + " (flies-in " + o.getName() + ")");
                for(Fact f : facts){
                    Instrument inst = new Instrument(f.getSlotValue("Instrument").toString(),
                            Double.parseDouble(f.getSlotValue("Field-of-view#").toString()), null);
                    dcMatrix.addDataContinutity(meas, m.getLaunchDate(), m.getEolDate(), m, inst);
                }
            }

            this.missionDataContinuity.put(m, dcMatrix);
        }
    }

    /**
     * Loads the importance of the measurements from the given excel file
     *
     * @param file the excel file that contains the importance of each
     * measurement
     * @return all the measurements and
     * @throws BiffException
     * @throws IOException
     */
    private HashMap<Measurement, Double> loadMeasurementWeights(File file) throws BiffException, IOException {
        System.out.println("Loading measurement weights...");
        Workbook xls = Workbook.getWorkbook(file);
        Sheet meas = xls.getSheet("Measurement Importance");
        HashMap<Measurement, Double> out = new HashMap<>(meas.getRows());

        for (int i = 1; i < meas.getRows(); i++) {
            Cell[] row = meas.getRow(i);
            String measurementName = row[0].getContents();
            double importance = Double.parseDouble(row[1].getContents());

            if (out.put(new Measurement.Builder(measurementName).build(), importance) != null) {
                throw new IllegalArgumentException(String.format("Measurement %s already exists.", measurementName));
            }
        }
        xls.close();
        return out;
    }

    /**
     * Loads the continuity weights
     *
     * @param file the excel file that contains the importance of each
     * measurement
     * @return all the measurements and
     * @throws BiffException
     * @throws IOException
     */
    private HashMap<AbsoluteDate, Double> loadContinuityWeights(File file) throws BiffException, IOException, OrekitException {
        System.out.println("Loading continuity weights...");
        Workbook xls = Workbook.getWorkbook(file);
        Sheet times = xls.getSheet("Discounting Scheme");
        HashMap<AbsoluteDate, Double> out = new HashMap<>(times.getRows());

        for (int i = 1; i < times.getRows(); i++) {
            Cell[] row = times.getRow(i);
            int year = Integer.parseInt(row[0].getContents());
            int month = Integer.parseInt(row[1].getContents());
            AbsoluteDate date = new AbsoluteDate(year, month, 0, TimeScalesFactory.getUTC());
            double importance = Double.parseDouble(row[2].getContents());

            if (out.put(date, importance) != null) {
                throw new IllegalArgumentException(String.format("Date %s already exists.", date));
            }
        }
        xls.close();
        return out;
    }

    /**
     * Loads the names of the existing missions to consider
     *
     * @param file the excel file that contains the importance of each
     * measurement
     * @return the existing missions to consider
     * @throws BiffException
     * @throws IOException
     */
    private HashSet<String> loadExistingMissions(File file) throws BiffException, IOException {
        System.out.println("Loading existing missions...");
        Workbook xls = Workbook.getWorkbook(file);
        Sheet times = xls.getSheet("Discounting Scheme");
        HashSet<String> out = new HashSet<>();

        for (int i = 1; i < times.getRows(); i++) {
            Cell[] row = times.getRow(i);
            if (!row[1].getContents().equals("1")) {
                continue;
            }
            String missionName = row[0].getContents();
            if (!out.add(missionName)) {
                throw new IllegalArgumentException(String.format("Mission %s already exists.", missionName));
            }
        }
        xls.close();
        return out;
    }

    private DataContinutityMatrix loadPrecursor(String path) throws BiffException, IOException, ParseException, OrekitException {
        System.out.println("Loading precursor data continuity matrix...");
        DataContinutityMatrix matrix = new DataContinutityMatrix(timeStep, startDate, endDate);

        Workbook ceos = Workbook.getWorkbook(new File(path + File.separator + "xls" + File.separator + "CEOS" + File.separator + "Measurements CEOS.xls"));
        Sheet sheet = ceos.getSheet("Sheet1");
        //map connecting description and measurment name
        HashMap<String, String> measurementMap = new HashMap<>(sheet.getRows());
        for (int i = 1; i < sheet.getRows(); i++) {
            Cell[] row = sheet.getRow(i);
            if (row[1].getContents().contains("m")) {
                measurementMap.put(row[0].getContents(), row[2].getContents());
            }
        }
        ceos.close();

        //go through individual measurment excel files
        SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
        for (int i = 0; i < measurementMap.size(); i++) {
            String filename = path + File.separator + "xls" + File.separator + "CEOS" + File.separator + "m" + i + ".xls";
            System.out.println("Reading file " + filename);
            Workbook m = Workbook.getWorkbook(new File(filename));
            Sheet s = m.getSheet(0);
            for (int j = 1; j < s.getRows(); j++) {
                Cell[] row = s.getRow(j);
                String measType = row[0].getContents();
                String missionName = row[1].getContents();
                AbsoluteDate launchDate = new AbsoluteDate(f.parse(row[2].getContents()), TimeScalesFactory.getUTC());
                AbsoluteDate endOfLifeDate = new AbsoluteDate(f.parse(row[3].getContents()), TimeScalesFactory.getUTC());
                String missionStatus = row[4].getContents();
                String instrument = row[5].getContents();

                Instrument inst = new Instrument(instrument, Double.NaN , null);

                Measurement meas = new Measurement(measurementMap.get(measType), Double.NaN, Double.NaN, Double.NaN, Double.NaN);

                Mission.MissionStatus status = null;
                switch (missionStatus) {
                    case "Past Mission":
                        status = Mission.MissionStatus.PAST;
                        break;
                    case "Currently being flown":
                        status = Mission.MissionStatus.FLYING;
                        break;
                    case "Planned":
                        status = Mission.MissionStatus.PLANNED;
                        break;
                    case "Approved":
                        status = Mission.MissionStatus.APPROVED;
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Mission status %s not recognized in row %d of %s", missionStatus, j, filename));
                }

                HashMap spacecraft = new HashMap(1);
                spacecraft.put(new Spacecraft(missionName + "_1", Arrays.asList(new Instrument[]{inst})), null);

                Mission miss = new Mission(missionName, spacecraft, launchDate, endOfLifeDate, status, 0, 0.0);
                matrix.addDataContinutity(meas, startDate, endDate, miss, inst);
            }
        }
        return matrix;
    }

    private ArrayList<Mission> loadMissions(File file) throws IOException, SAXException, ParserConfigurationException {
        ArrayList<Mission> out = new ArrayList<>();
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList missionsNode = doc.getElementsByTagName("mission");
        for (int i = 0; i < missionsNode.getLength(); i++) {
            Element mission = (Element) missionsNode.item(i);
            String name = mission.getElementsByTagName("name").item(0).getTextContent();
            double lifetime = Double.parseDouble(mission.getElementsByTagName("lifetime").item(0).getTextContent());

            NodeList budgetingList = mission.getElementsByTagName("budgeting");
            if (budgetingList.getLength() > 1) {
                throw new IllegalArgumentException(String.format("Only one budgeting node allowed. Error in mission %s", name));
            }
            Element budgeting = (Element) budgetingList.item(0);
            int devYr = Integer.parseInt(budgeting.getElementsByTagName("developmentYears").item(0).getTextContent());
            double devCostYr = Double.parseDouble(budgeting.getElementsByTagName("costPerYear").item(0).getTextContent());

            HashMap<Spacecraft, Orbit> crafts = new HashMap<>();

            //a mission may have multiple spacecraft if it is a constellation or distributed satellite mission
            NodeList craftList = mission.getElementsByTagName("spacecraft");
            for (int j = 0; j < craftList.getLength(); j++) {

                NodeList payloadList = mission.getElementsByTagName("payload");
                if (payloadList.getLength() != 1) {
                    throw new IllegalArgumentException(String.format("Spacecraft must have one and only one paylaod. Error in mission %s", name));
                }
                NodeList payload = ((Element) payloadList.item(0)).getElementsByTagName("instrument");
                ArrayList<Instrument> instruments = new ArrayList<>();
                for (int k = 0; k < payload.getLength(); k++) {
                    String instName = payload.item(k).getNodeValue();
                    instruments.add(EOSSDatabase.getInstrument(instName));
                }

                NodeList orbitList = mission.getElementsByTagName("orbitName");
                if (payloadList.getLength() != 1) {
                    throw new IllegalArgumentException(String.format("Spacecraft must have one and only one orbit. Error in mission %s", name));
                }
                String orbitName = orbitList.item(0).getTextContent();
                crafts.put(new Spacecraft(name + "_0", instruments), EOSSDatabase.getOrbit(orbitName));
            }
            out.add(new Mission.Builder(name, crafts).devYr(devYr).devCostYr(devCostYr).lifetime(lifetime).build());
        }
        return out;
    }

    private ArrayList<Panel> loadPanels(File file) throws IOException, SAXException, ParserConfigurationException {
        ArrayList<Panel> out = new ArrayList<>();
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList panelList = doc.getElementsByTagName("panel");
        for (int i = 0; i < panelList.getLength(); i++) {
            Element panel = (Element) panelList.item(i);
            String id = panel.getElementsByTagName("id").item(0).getTextContent();
            double discountRate = Double.parseDouble(panel.getElementsByTagName("discountRate").item(0).getTextContent());
            out.add(new Panel.Builder(id).discountRate(discountRate).build());
        }
        return out;
    }

    private ArrayList<Budget> loadBudget(File file) throws OrekitException, ParseException {
        ArrayList<Budget> budget = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                } else {
                    String[] tokens = line.trim().split(",");
                    SimpleDateFormat f = new SimpleDateFormat("yyyy");
                    budget.add(new Budget(new AbsoluteDate(f.parse(tokens[0]), TimeScalesFactory.getUTC()),
                            Double.parseDouble(tokens[1])));

                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MissionScheduling.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MissionScheduling.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return budget;
    }

    /**
     * Computes the dates of the launches from the sequence, the available
     * budget, and the cost profile for each mission
     *
     * @param schedule the schedule of the missions
     * @return the dates of the launches
     */
    private HashMap<Mission, AbsoluteDate> computeLaunchDates(MissionSchedulingArchitecture schedule) {
        final HashMap<Mission, AbsoluteDate> launchDates = new HashMap<>(missions.size());

        double currentBudget = 0;
        int profileCounter = 0;
        int missionCounter = 0;
        double[] currentCostProfile = missions.get(schedule.getSequence()[missionCounter]).getCostProfile();
        for (Budget b : budgets) {
            currentBudget += b.getBudget();

            //check what can be funded
            while (missionCounter < missions.size()) {
                if (currentBudget >= currentCostProfile[profileCounter]) {
                    currentBudget -= currentCostProfile[profileCounter];
                    currentCostProfile[profileCounter] = 0;
                    profileCounter++;
                    if (profileCounter == currentCostProfile.length) {
                        //launch mission
                        launchDates.put(missions.get(missionCounter), b.getDate());

                        //move to next mission
                        missionCounter++;
                        currentCostProfile = missions.get(schedule.getSequence()[missionCounter]).getCostProfile();
                        profileCounter = 0;
                    }
                } else {
                    //spend all the budget on the available part of the mission
                    currentCostProfile[profileCounter] -= currentBudget;
                    currentBudget = 0;
                    break;
                }
            }
        }

        return launchDates;
    }

    /**
     * Computes the discounted value. The original value is how much each panel
     * benefits from a specific mission operating at a given date. These values
     * are discounted as a function of a discount rate r and time dt (d =
     * d_0*exp(-r*dt))
     *
     * @param dates the launch dates computed for the
     * @param schedule a schedule of missions
     * @return the discounted value of the given schedule
     */
    private double computeDiscountedValue(MissionSchedulingArchitecture schedule) {
        ArrayList<AbsoluteDate> sortedDates = new ArrayList<>(schedule.getLaunchDates().values());
        Collections.sort(sortedDates);

        double discountedValue = 0;
        for (int i = 0; i < missions.size(); i++) {
            double dt = (sortedDates.get(i).durationFrom(startDate) / (365 * 24 * 3600.)); //convert sec to years
            Mission m = missions.get(schedule.getSequence()[i]);
            ValueTree vt = this.missionValueTree.get(m);
            
            int j = 0;
            for (Panel p : panels) {
                double r = p.getDiscountFactor();
                discountedValue += vt.getScore(p.getName()) * Math.exp(-r * dt);
                j++;
            }
        }
        return discountedValue;
    }

    /**
     * Computes the score for continuing measurements with the given schedule of
     * missions
     *
     * @param schedule schedule of missions
     * @return the score for continuing measurements with the given schedule of
     * missions
     */
    private double computeDataContinuityScore(MissionSchedulingArchitecture schedule) {
        int[] permutation = schedule.getSequence();
        DataContinutityMatrix overallMatrix = precursors;
        for (int i = 0; i < missions.size(); i++) {
            // Offset corresponding params.MissionMatrices according to launch date and lifetime
            Mission currentMission = missions.get(permutation[i]);
            DataContinutityMatrix matrix0 = missionDataContinuity.get(currentMission);
            double lifetime = currentMission.getLifetime();
            AbsoluteDate launchdate2 = schedule.getLaunchDates().get(currentMission);
            DataContinutityMatrix matrix1 = matrix0.offset(lifetime, launchdate2);

            // Superimpose all matrices
            overallMatrix = matrix1.merge(overallMatrix);
        }

        // Compute data continuity score from new matrix
        RealMatrix diff = overallMatrix.count().subtract(precursors.count());
        for (int i = 0; i < diff.getRowDimension(); i++) {
            for (int j = 0; j < diff.getColumnDimension(); j++) {
                if (diff.getEntry(i, j) < 0.) {
                    diff.setEntry(i, j, 0.); //only improvements are considered
                }
            }
        }
        return this.measurementWeights.dotProduct(diff.operate(this.continuityWeights));
    }

    @Override
    public Solution newSolution() {
        return new MissionSchedulingArchitecture(1, 2);
    }

}
