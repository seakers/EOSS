/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

/**
 *
 * @author dani
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eoss.problem.Orbit.OrbitType;

public class Params {
    //public static String master_xls;
    //public static boolean recompute_scores;

    public static String path;// used
    public static String req_mode;//used
    public static String run_mode;//used
    public static String initial_pop;
    public static String attribute_set_xls;// used
    public static String instrument_capability_xls;// used
    public static String requirement_satisfaction_xls;
    public static String aggregation_xls;
    public static String capability_rules_xls;//used
    public static String mission_analysis_database_xls;

    public static String instrument_capability_xml;//used
    public static String aggregation_xml; //used

    public static String module_definition_clp;// used
    public static String template_definition_clp;// used
    public static String[] functions_clp = new String[2];// used
    public static String assimilation_rules_clp;// used
    public static String aggregation_rules_clp;// used
    public static String fuzzy_aggregation_rules_clp;//used
    public static String attribute_inheritance_clp;
    public static String orbit_rules_clp;
    public static String capability_rules_clp;
    public static String synergy_rules_clp;//Used
    public static String explanation_rules_clp;//Used
    public static String fuzzy_attribute_clp;
    public static String value_aggregation_clp;
    public static String requirement_satisfaction_clp;
    public static String cost_estimation_rules_clp; // used
    public static String fuzzy_cost_estimation_rules_clp; //used
    public static String mass_budget_rules_clp;
    public static String subsystem_mass_budget_rules_clp;
    public static String deltaV_budget_rules_clp;
    public static String adcs_design_rules_clp;
    public static String propulsion_design_rules_clp;
    public static String eps_design_rules_clp;
    public static String sat_configuration_rules_clp;
    public static String launch_vehicle_selection_rules_clp;
    public static String adhoc_rules_clp;

    // Instruments and orbits
    public static ArrayList<Orbit> orbits;
    public static int[] altnertivesForNumberOfSatellites = {1};

    // Results
    public static String path_save_results;

    // Intermediate results
    public static HashMap requirement_rules;
    public static HashMap measurements_to_subobjectives;
    public static HashMap measurements_to_objectives;
    public static HashMap measurements_to_panels;
    public static ArrayList parameter_list;
    public static ArrayList objectives;
    public static ArrayList subobjectives;
    public static HashMap instruments_to_measurements;
    public static HashMap instruments_to_subobjectives;
    public static HashMap instruments_to_objectives;
    public static HashMap instruments_to_panels;
    public static HashMap measurements_to_instruments;
    public static HashMap subobjectives_to_instruments;
    public static HashMap objectives_to_instruments;
    public static HashMap panels_to_instruments;
    public static HashMap subobjectives_to_measurements;
    public static HashMap objectives_to_measurements;
    public static HashMap panels_to_measurements;

    public static int npanels;
    public static ArrayList<Double> panel_weights;
    public static ArrayList<String> panel_names;
    public static ArrayList obj_weights;
    public static ArrayList<Integer> num_objectives_per_panel;
    public static ArrayList subobj_weights;
    public static HashMap subobj_weights_map;
    public static HashMap revtimes;
    public static HashMap<ArrayList<String>, HashMap<String,Double>> scores;
    public static HashMap subobj_scores;
    public static HashMap capabilities;
    public static HashMap all_dsms;
    //Cubesat costs model
    public static String capability_dat_file;
    public static String revtimes_dat_file;
    public static String dsm_dat_file;
    public static String scores_dat_file;
    public static double time_horizon;

    //To access Dropbox files
    public final String APP_KEY = "501z5dhek2czvcm";
    public final String APP_SECRET = "q21mrispb7be3oz";

    /**
     * Constructor loads in all the paths from a .properties file stored in the
     * project folder/config/
     *
     * @param p Determines the path to the main project folder. Main project
     * folder needs to contain xls, clp, results folder
     * @param mode Can choose between slow and fast. Slow generates all
     * capabilities. Fast has a precomputed look-up table of the capabilities
     * @param name Name of the run
     * @param run_mode Can choose between normal and debug. Normal will attach
     * all explanation to each architecture. Debug bypasses this. Result file
     * from debug mode will require less storage space
     */
    public Params(String p, String mode, String name, String run_mode) {
        //this.master_xls = master_xls;
        //this.recompute_scores = recompute_scores;
        Params.path = p;
        Params.req_mode = mode;
        Params.run_mode = run_mode;

        //reads in config file that contains paths to all the clp, dat, and xls files
        File configFile = new File(path + File.separator + "config" + File.separator + "Params.properties");
        Properties props = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            props.load(reader);
        } catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }

        //paths to look up tables
        capability_dat_file = path + File.separator + "dat" + File.separator + props.getProperty("capability_dat_file");
        revtimes_dat_file = path + File.separator + "dat" + File.separator + props.getProperty("revtimes_dat_file");
        dsm_dat_file = path + File.separator + "dat" + File.separator + props.getProperty("dsm_dat_file");
        scores_dat_file = path + File.separator + "dat" + File.separator + props.getProperty("scores_dat_file");

        //path to results
        path_save_results = path + File.separator + props.getProperty("path_save_results");
        initial_pop = path_save_results + File.separator + props.getProperty("initial_pop");

        // Paths for common xls files
        attribute_set_xls = path + File.separator + "xls" + File.separator + props.getProperty("attribute_set_xls"); //used
        capability_rules_xls = path + File.separator + "xls" + File.separator + props.getProperty("capability_rules_xls");//used
        requirement_satisfaction_xls = path + File.separator + "xls" + File.separator + props.getProperty("requirement_satisfaction_xls");//used
        mission_analysis_database_xls = path + File.separator + "xls" + File.separator + props.getProperty("mission_analysis_database_xls");//used

        // Paths for common xml files
        instrument_capability_xml = path + File.separator + "config" + File.separator + props.getProperty("capability_rules_xml");//used
        aggregation_xml = path + File.separator + "config" + File.separator + props.getProperty("aggregation_xml");//used

        // Paths for common clp files
        module_definition_clp = path + File.separator + "clp" + File.separator + props.getProperty("module_definition_clp");//used
        template_definition_clp = path + File.separator + "clp" + File.separator + props.getProperty("template_definition_clp");//used
        functions_clp[0] = path + File.separator + "clp" + File.separator + props.getProperty("functions_clp0");//used
        functions_clp[1] = path + File.separator + "clp" + File.separator + props.getProperty("functions_clp1");//used
        assimilation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("assimilation_rules_clp");//used
        aggregation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("aggregation_rules_clp");//used
        fuzzy_aggregation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("fuzzy_aggregation_rules_clp");//used
        attribute_inheritance_clp = path + File.separator + "clp" + File.separator + props.getProperty("attribute_inheritance_clp");
        orbit_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("orbit_rules_clp");
        synergy_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("synergy_rules_clp");//Used
        explanation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("explanation_rules_clp");//Used
        fuzzy_attribute_clp = path + File.separator + "clp" + File.separator + props.getProperty("fuzzy_attribute_clp");
        value_aggregation_clp = path + File.separator + "clp" + File.separator + props.getProperty("value_aggregation_clp");
        requirement_satisfaction_clp = path + File.separator + "clp" + File.separator + props.getProperty("requirement_satisfaction_clp");
        cost_estimation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("cost_estimation_rules_clp"); //Used
        fuzzy_cost_estimation_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("fuzzy_cost_estimation_rules_clp"); //Used
        mass_budget_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("mass_budget_rules_clp");
        subsystem_mass_budget_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("subsystem_mass_budget_rules_clp");
        deltaV_budget_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("deltaV_budget_rules_clp");
        adcs_design_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("adcs_design_rules_clp");
        propulsion_design_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("propulsion_design_rules_clp");
        eps_design_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("eps_design_rules_clp");
        sat_configuration_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("sat_configuration_rules.clp");
        launch_vehicle_selection_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("launch_vehicle_selection_rules_clp");
        adhoc_rules_clp = path + File.separator + "clp" + File.separator + props.getProperty("adhoc_rules_clp");

        // Intermediate results
        measurements_to_subobjectives = new HashMap();
        measurements_to_objectives = new HashMap();
        measurements_to_panels = new HashMap();
        objectives = new ArrayList();
        subobjectives = new ArrayList();
        instruments_to_measurements = new HashMap();
        instruments_to_subobjectives = new HashMap();
        instruments_to_objectives = new HashMap();
        instruments_to_panels = new HashMap();

        measurements_to_instruments = new HashMap();
        subobjectives_to_instruments = new HashMap();
        objectives_to_instruments = new HashMap();
        panels_to_instruments = new HashMap();

        subobjectives_to_measurements = new HashMap();
        objectives_to_measurements = new HashMap();
        panels_to_measurements = new HashMap();

        //Load specific adhoc parameters from config folder
        DocumentBuilder dBuilder;

        loadInstruments();
        loadOrbits();
        loadBuses();

        //Loads in scenario parameters such as time horizon
        File scenarioFile = new File(path + File.separator + "config" + File.separator + "scenarioParams.xml");
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(scenarioFile);
            doc.getDocumentElement().normalize();
            time_horizon = Double.parseDouble(doc.getElementsByTagName("timeHorizon").item(0).getTextContent());

        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            if (!run_mode.equalsIgnoreCase("update_revtimes")) {
                FileInputStream fis = new FileInputStream(revtimes_dat_file);
                System.out.println("Loading revisit time look-up table...");
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    revtimes = (HashMap) ois.readObject();
                    ois.close();
                } catch (IOException ex) {
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (!run_mode.equalsIgnoreCase("update_capabilities")) {

                FileInputStream fis2 = new FileInputStream(capability_dat_file);
                System.out.println("Loading precomputed capabiltiy look-up table...");
                try {
                    ObjectInputStream ois2 = new ObjectInputStream(fis2);
                    capabilities = (HashMap) ois2.readObject();
                    ois2.close();
                } catch (IOException ex) {
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (!run_mode.equalsIgnoreCase("update_dsms")) {
                FileInputStream fis3 = new FileInputStream(dsm_dat_file);
                System.out.println("Loading dsm_dat ...");
                try {
                    ObjectInputStream ois3 = new ObjectInputStream(fis3);
                    all_dsms = (HashMap) ois3.readObject();
                    ois3.close();
                } catch (IOException ex) {
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (!run_mode.equalsIgnoreCase("update_scores")) {
                FileInputStream fis4 = new FileInputStream(scores_dat_file);
                System.out.println("Loading scores_dat_file ...");
                try {
                    ObjectInputStream ois4 = new ObjectInputStream(fis4);
                    scores = (HashMap) ois4.readObject();
                    subobj_scores = (HashMap) ois4.readObject();
                    ois4.close();
                } catch (IOException ex) {
                    Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadInstruments() {
        File instrumentFile = new File(path + File.separator + "config" + File.separator + "candidateInstruments.xml");
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = dBuilder.parse(instrumentFile);
            doc.getDocumentElement().normalize();
            NodeList instrumentNode = doc.getElementsByTagName("instrument");
            for (int i = 0; i < instrumentNode.getLength(); i++) {
                Element inst = (Element) instrumentNode.item(i);
                String instName = inst.getElementsByTagName("name").item(0).getTextContent();
                Double instSize = Double.valueOf(inst.getElementsByTagName("size").item(0).getTextContent());
                Double instCost = Double.valueOf(inst.getElementsByTagName("cost").item(0).getTextContent());
                Double instMass = Double.valueOf(inst.getElementsByTagName("mass").item(0).getTextContent());
                EOSSDatabase.addInstrument(new Instrument(instName, instSize, instCost, instMass));
            }

        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadOrbits() {
        File instrumentFile = new File(path + File.separator + "config" + File.separator + "candidateOrbits.xml");
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = dBuilder.parse(instrumentFile);
            doc.getDocumentElement().normalize();
            NodeList instrumentNode = doc.getElementsByTagName("orbit");
            for (int i = 0; i < instrumentNode.getLength(); i++) {
                Element orbit = (Element) instrumentNode.item(i);
                String orbName = orbit.getElementsByTagName("name").item(0).getTextContent();
                String orbType = orbit.getElementsByTagName("type").item(0).getTextContent();
                OrbitType type;
                switch (orbType) {
                    case "LEO":
                        type = OrbitType.LEO;
                        break;
                    case "SSO":
                        type = OrbitType.SSO;
                        break;
                    case "MEO":
                        type = OrbitType.MEO;
                        break;
                    case "HEO":
                        type = OrbitType.HEO;
                        break;
                    case "GEO":
                        type = OrbitType.GEO;
                        break;
                    default:
                        throw new IllegalArgumentException("Expected OrbitType. Found " + orbType + "which is not a valid OrbitType");
                }
                Double semimajorAxis = Double.valueOf(orbit.getElementsByTagName("semimajoraxis").item(0).getTextContent());
                String inclination = orbit.getElementsByTagName("inclination").item(0).getTextContent();
                String RAAN = orbit.getElementsByTagName("raan").item(0).getTextContent();
                Double meanAnomaly = Double.valueOf(orbit.getElementsByTagName("meananomaly").item(0).getTextContent());
                Double eccentricity = Double.valueOf(orbit.getElementsByTagName("eccentricity").item(0).getTextContent());
                Double argPeri = Double.valueOf(orbit.getElementsByTagName("argumentperigee").item(0).getTextContent());
                EOSSDatabase.addOrbit(new Orbit(orbName, type, semimajorAxis, inclination, RAAN, meanAnomaly, eccentricity, argPeri));
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadBuses() {
        File instrumentFile = new File(path + File.separator + "config" + File.separator + "candidateBuses.xml");
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = dBuilder.parse(instrumentFile);
            doc.getDocumentElement().normalize();
            NodeList instrumentNode = doc.getElementsByTagName("instrument");
            for (int i = 0; i < instrumentNode.getLength(); i++) {
                Element bus = (Element) instrumentNode.item(i);
                String busName = bus.getElementsByTagName("name").item(0).getTextContent();
                Double busSize = Double.valueOf(bus.getElementsByTagName("size").item(0).getTextContent());
                Double busCost = Double.valueOf(bus.getElementsByTagName("cost").item(0).getTextContent());
                Double busLife = Double.valueOf(bus.getElementsByTagName("lifetime").item(0).getTextContent());
                EOSSDatabase.addBus(new Bus(busSize, busCost, busLife));
            }

        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Params.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getConfiguration() {
        return "requirement_satisfaction_xls = " + requirement_satisfaction_xls + ";capability_rules_xls = " + capability_rules_xls;
    }

}
