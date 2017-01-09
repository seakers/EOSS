/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.evaluation;

/**
 *
 * @author dani
 */
import eoss.problem.EOSSDatabase;
import eoss.problem.Orbit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.util.HashSet;
import java.util.Map;
import rbsa.eoss.NDSM;

public class ArchitectureEvaluatorParams {
    //public static String master_xls;
    //public static boolean recompute_scores;

    public static String path;// used
    public static String initial_pop;
    public static String attribute_set_xls;// used
    public static String instrument_capability_xls;// used
    public static String requirement_satisfaction_xls;
    public static String aggregation_xls;
    public static String capability_rules_xls;//used
    public static String mission_analysis_database_xls;

    public static String instrument_capability_xml;//used
    public static String panel_xml; //used

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
    public static int[] altnertivesForNumberOfSatellites = {1};

    // Results
    public static String path_save_results;

    // Intermediate results
    public static HashMap<String, ArrayList<String>> measurements_to_subobjectives;
    public static HashSet<String> measurements;

    public static int npanels;
    public static ArrayList<Double> panel_weights;
    public static ArrayList<String> panel_names;
    
    
    public static HashMap<HashMap<Orbit, Double>, HashMap<String, Double>> revtimes;//map <orbit, fov>, <region, revtime>
    public static Map<ArrayList<String>, HashMap<String, Double>> scores;
    public static Map<String, NDSM> all_dsms;

    //precomputed models
    public static String revtimes_dat_file;
    public static String dsm_dat_file;
    public static double time_horizon;

    /**
     * Constructor loads in all the paths from a .properties file stored in the
     * project folder/config/
     *
     * @param p Determines the path to the main project folder. Main project
     * folder needs to contain xls, clp, results folder
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public ArchitectureEvaluatorParams(String p) throws IOException, ClassNotFoundException {
        //this.master_xls = master_xls;
        //this.recompute_scores = recompute_scores;
        ArchitectureEvaluatorParams.path = p;

        //reads in config file that contains paths to all the clp, dat, and xls files
        File configFile = new File(path + File.separator + "config" + File.separator + "Params.properties");
        Properties props = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            props.load(reader);
        } catch (IOException ex) {
            throw ex;
        }

        //paths to look up tables
        String datPath = path + File.separator + "dat" + File.separator;
        revtimes_dat_file = datPath + props.getProperty("revtimes_dat_file");
        dsm_dat_file = datPath + props.getProperty("dsm_dat_file");

        //path to results
        path_save_results = path + File.separator + props.getProperty("path_save_results");
        initial_pop = path_save_results + File.separator + props.getProperty("initial_pop");

        // Paths for common xls files
        String xlsPath = path + File.separator + "xls" + File.separator;
        attribute_set_xls = xlsPath + props.getProperty("attribute_set_xls"); //used
        capability_rules_xls = xlsPath + props.getProperty("capability_rules_xls");//used
        requirement_satisfaction_xls = xlsPath + props.getProperty("requirement_satisfaction_xls");//used
        mission_analysis_database_xls = xlsPath + props.getProperty("mission_analysis_database_xls");//used

        // Paths for common xml files
        String xmlPath = path + File.separator + "config" + File.separator;
        panel_xml = xmlPath + props.getProperty("panels_xml");//used

        // Paths for common clp files
        String clpPath = path + File.separator + "clp" + File.separator;
        module_definition_clp = clpPath + props.getProperty("module_definition_clp");//used
        template_definition_clp = clpPath + props.getProperty("template_definition_clp");//used
        functions_clp[0] = clpPath + props.getProperty("functions_clp0");//used
        functions_clp[1] = clpPath + props.getProperty("functions_clp1");//used
        assimilation_rules_clp = clpPath + props.getProperty("assimilation_rules_clp");//used
        aggregation_rules_clp = clpPath + props.getProperty("aggregation_rules_clp");//used
        fuzzy_aggregation_rules_clp = clpPath + props.getProperty("fuzzy_aggregation_rules_clp");//used
        attribute_inheritance_clp = clpPath + props.getProperty("attribute_inheritance_clp");
        orbit_rules_clp = clpPath + props.getProperty("orbit_rules_clp");
        synergy_rules_clp = clpPath + props.getProperty("synergy_rules_clp");//Used
        explanation_rules_clp = clpPath + props.getProperty("explanation_rules_clp");//Used
        capability_rules_clp = clpPath + props.getProperty("capability_rules_clp");//Used
        fuzzy_attribute_clp = clpPath + props.getProperty("fuzzy_attribute_clp");
        value_aggregation_clp = clpPath + props.getProperty("value_aggregation_clp");
        cost_estimation_rules_clp = clpPath + props.getProperty("cost_estimation_rules_clp"); //Used
        fuzzy_cost_estimation_rules_clp = clpPath + props.getProperty("fuzzy_cost_estimation_rules_clp"); //Used
        mass_budget_rules_clp = clpPath + props.getProperty("mass_budget_rules_clp");
        subsystem_mass_budget_rules_clp = clpPath + props.getProperty("subsystem_mass_budget_rules_clp");
        deltaV_budget_rules_clp = clpPath + props.getProperty("deltaV_budget_rules_clp");
        adcs_design_rules_clp = clpPath + props.getProperty("adcs_design_rules_clp");
        propulsion_design_rules_clp = clpPath + props.getProperty("propulsion_design_rules_clp");
        eps_design_rules_clp = clpPath + props.getProperty("eps_design_rules_clp");
        sat_configuration_rules_clp = clpPath + props.getProperty("sat_configuration_rules.clp");
        launch_vehicle_selection_rules_clp = clpPath + props.getProperty("launch_vehicle_selection_rules_clp");
        adhoc_rules_clp = clpPath + props.getProperty("adhoc_rules_clp");

        // Intermediate results
        measurements_to_subobjectives = new HashMap();
        measurements = new HashSet<>();

        //Load specific adhoc parameters from config folder
        DocumentBuilder dBuilder;
        //Loads in scenario parameters such as time horizon
        File scenarioFile = new File(path + File.separator + "config" + File.separator + "scenarioParams.xml");
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(scenarioFile);
            doc.getDocumentElement().normalize();
            time_horizon = Double.parseDouble(doc.getElementsByTagName("timeHorizon").item(0).getTextContent());

        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ArchitectureEvaluatorParams.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(ArchitectureEvaluatorParams.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ArchitectureEvaluatorParams.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Loading revisit time look-up table...");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(revtimes_dat_file))) {
            revtimes = (HashMap<HashMap<Orbit, Double>, HashMap<String, Double>>) ois.readObject();
            Collections.unmodifiableMap(revtimes);
        } catch (IOException ex) {
            throw ex;
        } catch (ClassNotFoundException ex) {
            throw ex;
        }

//        //map <orbit, fov>, <region, revtime>
//        HashMap<HashMap<Orbit, Double>, HashMap<String, Double>> newDb = new HashMap();
//        for (String s : revtimes.keySet()) {
//            String[] split = s.split(" ");
//            String[] split2 = new String[7];
//            int d = 0;
//            for (String ss : split) {
//                if (!ss.equals("")) {
//                    split2[d] = ss;
//                    d++;
//                }
//            }
//
//            int satPerOrbit = Integer.parseInt(split[0]);
//            if (satPerOrbit == 1) {
//                HashMap<Orbit, Double> orbits = new HashMap();
//                for (int i = 0; i < 5; i++) {
//                    double fov = Double.parseDouble(split2[i + 2]);
//                    if (fov != -1) {
//                        orbits.put(EOSSDatabase.getOrbit(i), fov);
//                    }
//                }
//                newDb.put(orbits, revtimes.get(s));
//            }
//        }
//        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("newRevtimes"));) {
//            os.writeObject(newDb);
//            os.close();
//        } catch (IOException ex) {
//            Logger.getLogger(ArchitectureEvaluatorParams.class.getName()).log(Level.SEVERE, null, ex);
//        }
        System.out.println("Loading dsm_dat ...");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dsm_dat_file))) {
            all_dsms = Collections.unmodifiableMap((HashMap<String, NDSM>) ois.readObject());
        } catch (IOException ex) {
            throw ex;
        } catch (ClassNotFoundException ex) {
            throw ex;
        }

    }

}
