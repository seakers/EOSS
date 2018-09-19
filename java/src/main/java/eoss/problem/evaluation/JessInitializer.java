/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.evaluation;

/**
 *
 * @author dani
 */
import eoss.attributes.EOAttribute;
import java.io.*;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jess.*;
import jxl.*;
import jxl.read.biff.BiffException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eoss.problem.EOSSDatabase;
import eoss.problem.Instrument;
import eoss.attributes.GlobalAttributes;
import eoss.attributes.OLAttribute;
import eoss.attributes.NLAttribute;
import eoss.jess.Improve;
import eoss.jess.QueryBuilder;
import eoss.jess.SameOrBetter;
import eoss.jess.SlotType;
import eoss.jess.Worsen;
import rbsa.eoss.Weight;

public class JessInitializer {

//    private static JessInitializer instance = null;
    private HashMap<String, SlotType> capabilitiesToRequirementAttribInheritance;

    public JessInitializer() {
    }

    public void initializeJess(RequirementMode mode, Rete r, QueryBuilder qb) {
        Locale.setDefault(Locale.ENGLISH);

        System.out.println("Initializing Jess...");
        try {
//            r.eval("(set-node-index-hash  13)"); //tunable hash value for facts (tradeoff between memory and performance. small number has small memory footprint)

            // Create global variable path
            String tmp = ArchitectureEvaluatorParams.path.replaceAll("\\\\", "\\\\\\\\");
            r.eval("(defglobal ?*app_path* = \"" + tmp + "\")");
            r.eval("(import eoss.*)");
            r.eval("(import rbsa.eoss.*)");
            r.eval("(import architecture.util.*)");
            r.eval("(import architecture.util.ValueMap)");

            // Load modules
            r.batch(ArchitectureEvaluatorParams.module_definition_clp);

            // Load templates
            Workbook attribute_xls = Workbook.getWorkbook(new File(ArchitectureEvaluatorParams.attribute_set_xls));
            loadTemplates(r, attribute_xls);

            // Load functions
            loadFunctions(r, ArchitectureEvaluatorParams.functions_clp);

            //Load  launhc vehicles
            Workbook mission_analysis_xls = Workbook.getWorkbook(new File(ArchitectureEvaluatorParams.mission_analysis_database_xls));
            loadOrderedDeffacts(r, mission_analysis_xls, "Launch Vehicles", "DATABASE::launch-vehicle-information-facts", "DATABASE::Launch-vehicle");
            r.eval("reset");

            // Load instrument database
            Workbook instrument_xls = Workbook.getWorkbook(new File(ArchitectureEvaluatorParams.capability_rules_xls));
            loadInstrumentsJess(r);

            //Load orbit rules;
            r.batch(ArchitectureEvaluatorParams.orbit_rules_clp);

            //Load attribute inheritance rules
            loadAttributeInheritanceRules(r, attribute_xls, "Attribute Inheritance", ArchitectureEvaluatorParams.attribute_inheritance_clp);

            //Load cost estimation rules;
            r.batch(ArchitectureEvaluatorParams.cost_estimation_rules_clp);
            r.batch(ArchitectureEvaluatorParams.fuzzy_cost_estimation_rules_clp);

            //Load fuzzy attribute rules
            loadFuzzyAttributeRules(r, attribute_xls, "Fuzzy Capability Attributes", "CAPABILITIES::Manifested-instrument", "FUZZY-CAPABILITY-ATTRIBUTE");
            loadFuzzyAttributeRules(r, attribute_xls, "Fuzzy Requirement Attributes", "REQUIREMENTS::Measurement", "FUZZY-REQUIREMENT-ATTRIBUTE");

            //Load mass budget rules;
            r.batch(ArchitectureEvaluatorParams.mass_budget_rules_clp);
            r.batch(ArchitectureEvaluatorParams.subsystem_mass_budget_rules_clp);
            r.batch(ArchitectureEvaluatorParams.deltaV_budget_rules_clp);

            //Load eps design rules;
            r.batch(ArchitectureEvaluatorParams.eps_design_rules_clp);
            r.batch(ArchitectureEvaluatorParams.adcs_design_rules_clp);
            r.batch(ArchitectureEvaluatorParams.propulsion_design_rules_clp);

            //Load cost estimation rules;
            r.batch(ArchitectureEvaluatorParams.cost_estimation_rules_clp);
            r.batch(ArchitectureEvaluatorParams.fuzzy_cost_estimation_rules_clp);

            //Load launch vehicle selection rules
            r.batch(ArchitectureEvaluatorParams.launch_vehicle_selection_rules_clp);

            //Load requirement rules
            Workbook requirements_xls = Workbook.getWorkbook(new File(ArchitectureEvaluatorParams.requirement_satisfaction_xls));
            switch (mode) {
                case CRISPATTRIBUTE:
                    //                loadRequirementRulesAttribsWithContinuousSatisfactionScore(r, requirements_xls, "Attributes");//last parameter is mode: CASES, FUZZY, ATTRIBUTES
                    loadRequirementRulesAttribs(r, requirements_xls, "Requirements");
                    break;
                case FUZZYATTRIBUTE:
                    loadFuzzyRequirementRulesAttribs(r, requirements_xls, "Requirements");
                    break;
            }

            //Load capability rules
            loadCapabilityRules(r, instrument_xls, ArchitectureEvaluatorParams.capability_rules_clp);

            //Load synergy rules
            loadSynergyRules(r, ArchitectureEvaluatorParams.synergy_rules_clp);

            // Load assimilation rules
            r.batch(ArchitectureEvaluatorParams.assimilation_rules_clp);

            //Ad-hoc rules
            if (!ArchitectureEvaluatorParams.adhoc_rules_clp.isEmpty()) {
                System.out.println("WARNING: Loading ad-hoc rules");
                r.batch(ArchitectureEvaluatorParams.adhoc_rules_clp);
            }

            // Load explanation rules
            loadExplanationRules(r, ArchitectureEvaluatorParams.explanation_rules_clp);

            //Load aggregation rules
            File aggregationFile = new File(ArchitectureEvaluatorParams.panel_xml);
            loadAggregationRules(r, aggregationFile, new String[]{ArchitectureEvaluatorParams.aggregation_rules_clp, ArchitectureEvaluatorParams.fuzzy_aggregation_rules_clp});

            r.eval("reset");

            attribute_xls.close();
            requirements_xls.close();
            instrument_xls.close();
            mission_analysis_xls.close();

        } catch (JessException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
        } catch (IOException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
        } catch (BiffException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
        } catch (SAXException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); //program shuts down if Jess initializer catches any exception
        }
    }

    private void loadTemplates(Rete r, Workbook xls) throws JessException {
        HashMap<String, SlotType> measurementAttribs = loadMeasurementTemplate(r, xls);
        HashMap<String, SlotType> instrumentAttribs = loadInstrumentTemplate(r, xls);
        capabilitiesToRequirementAttribInheritance = new HashMap<>();
        for (String attrib : instrumentAttribs.keySet()) {
            if (measurementAttribs.containsKey(attrib)) {
                capabilitiesToRequirementAttribInheritance.put(attrib, measurementAttribs.get(attrib));
            }
        }
        loadSimpleTemplate(r, xls, "Mission", "MANIFEST::Mission");
        loadSimpleTemplate(r, xls, "Orbit", "DATABASE::Orbit");
        loadSimpleTemplate(r, xls, "Launch-vehicle", "DATABASE::Launch-vehicle");
        try {
            r.batch(ArchitectureEvaluatorParams.template_definition_clp);

        } catch (JessException ex) {
            Logger.getLogger(JessInitializer.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * loads the templates for REQUIREMENTS::Measurement. Returns the slot names
     * in an ArrayList.
     *
     * @param r
     * @param xls
     * @return
     */
    private HashMap<String, SlotType> loadMeasurementTemplate(Rete r, Workbook xls) throws JessException {
        HashMap<String, SlotType> out = new HashMap<>();
        HashMap<String, Integer> attribs_to_keys = new HashMap<>();
        HashMap<Integer, String> keys_to_attribs = new HashMap<>();
        HashMap<String, String> attribs_to_types = new HashMap<>();
        HashMap<String, EOAttribute> attribSet = new HashMap<>();
        Sheet meas = xls.getSheet("Measurement");
        String call = "(deftemplate REQUIREMENTS::Measurement ";
        int nslots = meas.getRows();
        for (int i = 1; i < nslots; i++) {
            Cell[] row = meas.getRow(i);
            String slot_type = row[0].getContents();
            String name = row[1].getContents();
            String str_id = row[2].getContents();
            int id = Integer.parseInt(str_id);
            String type = row[3].getContents();

            attribs_to_keys.put(name, id);
            keys_to_attribs.put(id, name);
            attribs_to_types.put(name, type);

            EOAttribute attrib = null;
            if (type.equalsIgnoreCase("NL") || type.equalsIgnoreCase("OL")) {

                String str_num_atts = row[4].getContents();
                int num_vals = Integer.parseInt(str_num_atts);
                HashMap<String, Integer> accepted_values = new HashMap<>();
                for (int j = 0; j < num_vals; j++) {
                    accepted_values.put(row[j + 5].getContents(), j);
                }
                if (type.equalsIgnoreCase("NL")) {
                    attrib = new NLAttribute(name, "N/A", accepted_values);
                } else {
                    attrib = new OLAttribute(name, "N/A", accepted_values);
                }
            }
            attribSet.put(name, attrib);
            call = call.concat(" (" + slot_type + " " + name + ") ");

            //when storing the attribute names get rid of any calls to default values for the slots
            String[] attribName = name.split(" ");
            if (slot_type.equalsIgnoreCase("slot")) {
                out.put(attribName[0], SlotType.SLOT);
            } else if (slot_type.equalsIgnoreCase("multislot")) {
                out.put(attribName[0], SlotType.MULTISLOT);
            } else {
                throw new IllegalArgumentException("Slot type " + slot_type + " not recognized");
            }
        }

        GlobalAttributes.defineMeasurement(attribs_to_keys, keys_to_attribs, attribs_to_types, attribSet);

        call = call.concat(")");
        r.eval(call);
        return out;
    }

    /**
     * loads the templates for CAPABILITIES::Manifested-instrument and
     * DATABASE::Instrument. Returns the slot names in an ArrayList.
     *
     * @param r
     * @param xls
     * @return
     */
    private HashMap<String, SlotType> loadInstrumentTemplate(Rete r, Workbook xls) throws JessException {
        HashMap<String, SlotType> out = new HashMap<>();
        Sheet meas = xls.getSheet("Instrument");
        String call = "(deftemplate CAPABILITIES::Manifested-instrument ";
        String call2 = "(deftemplate DATABASE::Instrument ";
        int nslots = meas.getRows();
        for (int i = 1; i < nslots; i++) {
            Cell[] row = meas.getRow(i);
            String slot_type = row[0].getContents();
            String name = row[1].getContents();

            call = call.concat(" (" + slot_type + " " + name + ") ");
            call2 = call2.concat(" (" + slot_type + " " + name + ") ");

            //when storing the attribute names get rid of any calls to default values for the slots
            String[] attribName = name.split(" ");
            if (slot_type.equalsIgnoreCase("slot")) {
                out.put(attribName[0], SlotType.SLOT);
            } else if (slot_type.equalsIgnoreCase("multislot")) {
                out.put(attribName[0], SlotType.MULTISLOT);
            } else {
                throw new IllegalArgumentException("Slot type " + slot_type + " not recognized");
            }
        }

        call = call.concat(")");
        call2 = call2.concat(")");
        r.eval(call);
        r.eval(call2);

        String call3 = "(deftemplate DATABASE::list-of-instruments (multislot list))";
        String call4 = "(deffacts DATABASE::list-of-instruments (DATABASE::list-of-instruments (list (create$";
        for (Instrument inst : EOSSDatabase.getInstruments()) {
            call4 += " " + inst.getName();
        }
        call4 += "))))";
        r.eval(call3);
        r.eval(call4);
        return out;
    }

    private void loadSimpleTemplate(Rete r, Workbook xls, String sheet, String template_name) throws JessException {
        Sheet meas = xls.getSheet(sheet);
        String call = "(deftemplate " + template_name + " ";
        int nslots = meas.getRows();
        for (int i = 1; i < nslots; i++) {
            Cell[] row = meas.getRow(i);
            String slot_type = row[0].getContents();
            String name = row[1].getContents();
            call = call.concat(" (" + slot_type + " " + name + ") ");
        }

        call = call.concat(")");
        r.eval(call);
    }

    private void loadFunctions(Rete r, String[] clps) throws JessException {
        r.addUserfunction(new SameOrBetter());
        r.addUserfunction(new Improve());
        r.addUserfunction(new Worsen());

        for (int i = 0; i < clps.length; i++) {
            r.batch(clps[i]);
        }
        r.eval("(deffunction update-objective-variable (?obj ?new-value) \"Update the value of the global variable with the new value only if it is better \" (bind ?obj (max ?obj ?new-value)))");
        r.eval("(deffunction ContainsRegion (?observed-region ?desired-region)  \"Returns true if the observed region i.e. 1st param contains the desired region i.e. 2nd param \" (bind ?tmp1 (eq ?observed-region Global)) (bind ?tmp2 (eq ?desired-region ?observed-region)) (if (or ?tmp1 ?tmp2) then (return TRUE) else (return FALSE)))");
        r.eval("(deffunction ContainsBands (?list-bands ?desired-bands)  \"Returns true if the list of bands contains the desired bands \" (if (subsetp ?desired-bands ?list-bands) then (return TRUE) else (return FALSE)))");

        r.eval("(deffunction numerical-to-fuzzy (?num ?values ?mins ?maxs)"
                + "(bind ?ind 1)"
                + "(bind ?n (length$ ?values))"
                + "(while (<= ?ind ?n)"
                + "(if (and (< ?num (nth$ ?ind ?maxs)) (>= ?num (nth$ ?ind ?mins))) then (return (nth$ ?ind ?values))"
                + "else (++ ?ind))))");
        r.eval("(deffunction revisit-time-to-temporal-resolution (?region ?values)"
                + "(if (eq ?region Global) then "
                + "(return (nth$ 1 ?values))"
                + " elif (eq ?region Tropical-regions) then"
                + "(return (nth$ 2 ?values))"
                + " elif (eq ?region Northern-hemisphere) then"
                + "(return (nth$ 3 ?values))"
                + " elif (eq ?region Southern-hemisphere) then"
                + "(return (nth$ 4 ?values))"
                + " elif (eq ?region Cold-regions) then"
                + "(return (nth$ 5 ?values))"
                + " elif (eq ?region US) then"
                + "(return (nth$ 6 ?values))"
                + " else (throw new JessException \"revisit-time-to-temporal-resolution: The region of interest is invalid\")"
                + "))");

        r.eval("(deffunction fuzzy-max (?att ?v1 ?v2) "
                + "(if (>= (SameOrBetter ?att ?v1 ?v2) 0) then "
                + "?v1 else ?v2))");

        r.eval("(deffunction fuzzy-min (?att ?v1 ?v2) "
                + "(if (<= (SameOrBetter ?att ?v1 ?v2) 0) then "
                + "?v1 else ?v2))");

        r.eval("(deffunction fuzzy-avg (?v1 ?v2) "
                + "(if (or (and (eq ?v1 High) (eq ?v2 Low)) (and (eq ?v1 Low) (eq ?v2 High))) then "
                + " \"Medium\" "
                + " else (fuzzy-min Accuracy ?v1 ?v2)))");

        r.eval("(deffunction member (?elem ?list) "
                + "(if (listp ?list) then "
                + " (neq (member$ ?elem ?list) FALSE) "
                + " else (?list contains ?elem)))");

        r.eval("(deffunction valid-orbit (?typ ?h ?i ?raan) "
                + "(bind ?valid TRUE)"
                + "(if (and (eq ?typ GEO) (or (neq ?h GEO) (neq ?i 0))) then (bind ?valid FALSE))"
                + "(if (and (neq ?typ GEO) (eq ?h GEO)) then (bind ?valid FALSE))"
                + "(if (and (eq ?typ SSO) (neq ?i SSO)) then (bind ?valid FALSE))"
                + "(if (and (neq ?typ SSO) (eq ?i SSO)) then (bind ?valid FALSE))"
                + "(if (and (neq ?typ SSO) (neq ?raan NA)) then (bind ?valid FALSE))"
                + "(if (and (eq ?typ SSO) (eq ?raan NA)) then (bind ?valid FALSE))"
                + "(if (and (or (eq ?h 1000) (eq ?h 1300)) (neq ?i near-polar)) then (bind ?valid FALSE))"
                + "(if (and (< ?h 400) (or (neq ?typ LEO) (eq ?i SSO) (eq ?i near-polar))) then (bind ?valid FALSE))"
                + " (return ?valid))");

        r.eval("(deffunction worth-improving-measurement (?meas) "
                + "(bind ?worth TRUE)"
                + "(bind ?arr (matlabf get_related_suboj ?meas))"
                + "(if (eq ?arr nil) then (return FALSE))"
                + "(bind ?iter (?arr iterator))"
                + "(while (?iter hasNext) "
                + "(bind ?subobj (?iter next)) "
                + "(if (eq (eval ?subobj) 1) then (bind ?worth FALSE))) "
                + "(return ?worth))");

        r.eval("(deffunction meas-group (?p ?gr)"
                + "(if (eq (str-compare (sub-string 1 1 ?p) A) 0) then (return FALSE))"
                + "(bind ?pos (str-index \" \" ?p)) "
                + "(bind ?str (sub-string 1 (- ?pos 1) ?p)) "
                + "(bind ?meas-1 (nth$ 1 (get-meas-group ?str))) "
                + "(bind ?meas-2 (nth$ 2 (get-meas-group ?str)))"
                + "(bind ?meas-3 (nth$ 3 (get-meas-group ?str))) "
                + "(bind ?gr-1 (nth$ 1 (get-meas-group ?gr))) "
                + "(bind ?gr-2 (nth$ 2 (get-meas-group ?gr))) "
                + "(bind ?gr-3 (nth$ 3 (get-meas-group ?gr)))"
                + "(if (and (neq (str-compare ?gr-1 ?meas-1) 0) (neq (str-compare ?gr-1 0) 0)) then (return FALSE)) "
                + "(if (and (neq (str-compare ?gr-2 ?meas-2) 0) (neq (str-compare ?gr-2 0) 0)) then (return FALSE))"
                + "(if (and (neq (str-compare ?gr-3 ?meas-3) 0) (neq (str-compare ?gr-3 0) 0)) then (return FALSE)) "
                + " (return TRUE))");

        r.eval("(deffunction get-meas-group (?str)"
                + "(bind ?pos (str-index . ?str)) "
                + "(bind ?gr1 (sub-string 1 (- ?pos 1) ?str)) "
                + "(bind ?new-str (sub-string (+ ?pos 1) (str-length ?str) ?str)) "
                + "(bind ?pos2 (str-index . ?new-str)) "
                + "(bind ?gr2 (sub-string 1 (- ?pos2 1) ?new-str)) "
                + "(bind ?gr3 (sub-string (+ ?pos2 1) (str-length ?new-str) ?new-str)) "
                + "(return (create$ ?gr1 ?gr2 ?gr3)))");
    }

    private void loadOrderedDeffacts(Rete r, Workbook xls, String sheet, String name, String template) throws JessException {
        Sheet meas = xls.getSheet(sheet);
        String call = "(deffacts " + name + " ";
        int nfacts = meas.getRows();
        int nslots = meas.getColumns();
        Cell[] slotNames = meas.getRow(0);
        String[] slot_names = new String[nslots];
        for (int i = 0; i < nslots; i++) {
            slot_names[i] = slotNames[i].getContents();
        }
        for (int i = 1; i < nfacts; i++) {
            Cell[] row = meas.getRow(i);
            call = call.concat(" (" + template + " ");
            for (int j = 0; j < nslots; j++) {
                String slot_value = row[j].getContents();
                if (slot_value.matches("\\[(.+)(,(.+))+\\]")) {
                    call = call.concat(" (" + slot_names[j] + " " + createJessList(slot_value) + ") ");
                } else {
                    call = call.concat(" (" + slot_names[j] + " " + slot_value + ") ");
                }
            }
            call = call.concat(") ");
        }
        call = call.concat(")");
        r.eval(call);
    }

    /**
     * Loads the characteristics of the instruments and adds the to the Jess
     * database
     *
     * @param r
     */
    private void loadInstrumentsJess(Rete r) throws IOException, BiffException, JessException {
        Workbook xls = Workbook.getWorkbook(new File(ArchitectureEvaluatorParams.capability_rules_xls));
        Sheet meas = xls.getSheet("CHARACTERISTICS");
        String call = "(deffacts instrument-database-facts ";
        int nfacts = meas.getRows();
        int nslots = meas.getColumns();
        Cell[] header = meas.getRow(0);

        for (int i = 1; i < nfacts; i++) {
            Cell[] row = meas.getRow(i);
            call = call.concat(" (DATABASE::Instrument ");
            for (int j = 0; j < nslots; j++) {
                String[] slotNameDefault = header[j].getContents().trim().split(" ");
                String slotName = slotNameDefault[0];
                String slotValue = row[j].getContents().trim();
                if (slotValue.isEmpty() && slotNameDefault.length == 2) {
                    //if the slot is empty and there is a default value, assign default value
                    slotValue = slotNameDefault[1];
                }
                call = call.concat(" (" + slotName + " " + slotValue + ") ");
            }
            call = call.concat(") ");
        }
        call = call.concat(")");
        r.eval(call);
        xls.close();
    }

    private void loadAttributeInheritanceRules(Rete r, Workbook xls, String sheet, String clp) throws JessException {
        r.batch(clp);
        Sheet meas = xls.getSheet(sheet);

        int nrules = meas.getRows();

        for (int i = 1; i < nrules; i++) {
            Cell[] row = meas.getRow(i);
            String template1 = row[0].getContents();
            String copy_slot_type1 = row[1].getContents();
            String copy_slot_name1 = row[2].getContents();
            String matching_slot_type1 = row[3].getContents();
            String matching_slot_name1 = row[4].getContents();
            String template2 = row[5].getContents();
            String matching_slot_name2 = row[6].getContents();
            String copy_slot_name2 = row[7].getContents();
            String module = row[8].getContents();
            String call = "(defrule " + module + "::inherit-" + template1.split("::")[1] + "-" + copy_slot_name1 + "-TO-" + template2.split("::")[1] + " ";
            if (copy_slot_type1.equalsIgnoreCase("slot")) {
                call = call + " (" + template1 + " (" + copy_slot_name1 + " ?x&~nil) ";
            } else {
                call = call + " (" + template1 + " (" + copy_slot_name1 + " $?x&:(> (length$ $?x) 0)) ";
            }
            if (matching_slot_type1.equalsIgnoreCase("slot")) {
                call = call + " (" + matching_slot_name1 + " ?id&~nil)) ";
            } else {
                call = call + " (" + matching_slot_name1 + " $?id&:(> (length$ $?id) 0))) ";
            }
            call = call + " ?old <- (" + template2 + " ";
            if (matching_slot_type1.equalsIgnoreCase("slot")) {
                call = call + " (" + matching_slot_name2 + " ?id) ";
            } else {
                call = call + " (" + matching_slot_name2 + " $?id) ";
            }
            if (copy_slot_type1.equalsIgnoreCase("slot")) {
                call = call + " (" + copy_slot_name2 + " nil) ";
            } else {
                call = call + " (" + copy_slot_name2 + " $?x&:(eq (length$ $?x) 0)) ";
            }
            call = call + ") => (modify ?old (" + copy_slot_name2 + " ?x)))";
            r.eval(call);
        }
    }

    private void loadFuzzyAttributeRules(Rete r, Workbook xls, String sheet, String template, String module) throws JessException {
        Sheet meas = xls.getSheet(sheet);

        int nrules = meas.getRows();

        for (int i = 1; i < nrules; i++) {
            Cell[] row = meas.getRow(i);
            String att = row[0].getContents();
            String param = row[1].getContents();
            //String unit = row[2].getContents();
            int num_values = Integer.parseInt(row[3].getContents());
            String[] fuzzy_values = new String[num_values];
            String[] mins = new String[num_values];
            String[] maxs = new String[num_values];
            StringBuilder call_values = new StringBuilder("(create$ ");
            StringBuilder call_mins = new StringBuilder("(create$ ");
            StringBuilder call_maxs = new StringBuilder("(create$ ");
            for (int j = 1; j <= num_values; j++) {
                fuzzy_values[j - 1] = row[4 * j].getContents();
                call_values.append(fuzzy_values[j - 1]).append(" ");
                mins[j - 1] = row[1 + 4 * j].getContents();
                call_mins.append(mins[j - 1]).append(" ");
                maxs[j - 1] = row[3 + 4 * j].getContents();
                call_maxs.append(maxs[j - 1]).append(" ");
            }
            call_values.append(")");
            call_mins.append(")");
            call_maxs.append(")");

            String call = "(defrule " + module + "::numerical-to-fuzzy-" + att + " ";
            if (param.equalsIgnoreCase("all")) {
                call = call + "?f <- (" + template + " (" + att + "# ?num&~nil) (" + att + " nil)) => ";
                call = call + "(bind ?value (numerical-to-fuzzy ?num " + call_values + " " + call_mins + " " + call_maxs + " )) (modify ?f (" + att + " ?value ))) ";
            } else {
                String att2 = att.substring(0, att.length() - 1);
                call = call + "?f <- (" + template + " (Parameter \"" + param + "\") (" + att2 + "# ?num&~nil) (" + att + " nil)) => ";
                call = call + "(bind ?value (numerical-to-fuzzy ?num " + call_values + " " + call_mins + " " + call_maxs + " )) (modify ?f (" + att + " ?value ))) ";
            }
            r.eval(call);
        }
    }

    private void loadRequirementRulesAttribs(Rete r, Workbook xls, String sheet) throws JessException {
        Sheet meas = xls.getSheet(sheet);
        int nlines = meas.getRows();
        String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
        //String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ ))";
        String lhs = "";
        String rhs = "";
        String rhs2 = " (bind ?list (create$ ";
        String current_subobj = "";
        int nattrib = 0;
        String req_rule = "";
        String attribs = "";
        String param = "";
        String current_param = "";
        HashMap<String, ArrayList<String>> subobj_tests = null;
        for (int i = 1; i < nlines; i++) {
            Cell[] row = meas.getRow(i);
            String subobj = row[0].getContents();
            param = row[1].getContents();

            ArrayList<String> attrib_test = new ArrayList<>();
            if (!subobj.equalsIgnoreCase(current_subobj)) {

                if (nattrib > 0) {
                    //finish this requirement rule
                    String[] tokens = current_subobj.split("-", 2);// limit = 2 so that remain contains RegionofInterest Global
                    String parent = tokens[0];
                    String index = tokens[1];
                    call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + " ))) ";
                    String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + "))";
                    req_rule = lhs + rhs0 + rhs + rhs2 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
                    req_rule = req_rule + ")";
                    r.eval(req_rule);

                    //start next requirement rule
                    rhs = "";
                    rhs2 = " (bind ?list (create$ ";
                    attribs = "";
                    lhs = "(defrule REQUIREMENTS::" + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (power-duty-cycle# ?pc) (data-rate-duty-cycle# ?dc)  (Parameter " + param + ")";
                    current_subobj = subobj;
                    current_param = param;
                    nattrib = 0;
                    subobj_tests = new HashMap<>();
                } else {
                    //start next requirement rule
                    rhs = "";
                    rhs2 = " (bind ?list (create$ ";
                    attribs = "";
                    lhs = "(defrule REQUIREMENTS::" + subobj + "-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom)  (power-duty-cycle# ?pc) (data-rate-duty-cycle# ?dc)  (Parameter " + param + ")";
                    current_subobj = subobj;
                    current_param = param;
                    subobj_tests = new HashMap<>();
                    //nattrib = 0;
                }
            }

            String attrib = row[2].getContents();
            attribs = attribs + " " + attrib;
            String type = row[3].getContents();
            String thresholds = row[4].getContents();
            String scores = row[5].getContents();
            String justif = row[6].getContents();
            attrib_test.add(type);
            attrib_test.add(thresholds);
            attrib_test.add(scores);
            subobj_tests.put(attrib, attrib_test);
            nattrib++;
            lhs = lhs + " (" + attrib + " ?val" + nattrib + "&~nil) ";
            rhs = rhs + "(bind ?x" + nattrib + " (nth$ (find-bin-num ?val" + nattrib + " " + toJessList(thresholds) + " ) " + toJessList(scores) + "))";
            rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif
                    + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
            rhs2 = rhs2 + " ?x" + nattrib;
        }
        //Last rule has not been processed
        String[] tokens = current_subobj.split("-", 2);// limit = 2 so that remain contains RegionofInterest Global
        String parent = tokens[0];
        String index = tokens[1];
        call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + " ))) ";
        String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + "))";
        req_rule = lhs + rhs0 + rhs + rhs2 + ")) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
        req_rule = req_rule + ")";

        r.eval(req_rule);
        call2 = call2 + ")";
        r.eval(call2);
    }

    private void loadFuzzyRequirementRulesAttribs(Rete r, Workbook xls, String sheet) throws JessException {

        Sheet meas = xls.getSheet(sheet);
        int nlines = meas.getRows();
        String call2 = "(deffacts REQUIREMENTS::init-subobjectives ";
        //String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ ))";
        String lhs = "";
        String rhs = "";
        String rhs2 = " (bind ?list (create$ ";
        String current_subobj = "";
        int nattrib = 0;
        String req_rule = "";
        String attribs = "";
        String param = "";
        String current_param = "";
        HashMap<String, ArrayList<String>> subobj_tests = null;

        for (int i = 1; i < nlines; i++) {
            Cell[] row = meas.getRow(i);
            String subobj = row[0].getContents();
            param = row[1].getContents();

            ArrayList<String> attrib_test = new ArrayList<>();
            if (!subobj.equalsIgnoreCase(current_subobj)) {

                if (nattrib > 0) {
                    //finish this requirement rule
                    String[] tokens = current_subobj.split("-", 2);// limit = 2 so that remain contains RegionofInterest Global
                    String parent = tokens[0];
                    String index = tokens[1];
                    call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value (new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (call ValueMap getValueInterval))) (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") (reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + " ))) ";
                    String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + "))";
                    req_rule = lhs + rhs0 + rhs + rhs2 + " ?dc ?pc)) (assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) "
                            + "(attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue \"Value\" (call "
                            + "(new FuzzyValue \"Value\" (new Interval \"interval\" (*$ ?list) (*$ ?list)) \"utils\" "
                            + "(call ValueMap getIntervalValue)) getFuzzy_val) \"utils\" (call ValueMap getValueInterval))) "
                            + " (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
                    req_rule = req_rule + ")";
                    r.eval(req_rule);

                    //start next requirement rule
                    rhs = "";
                    rhs2 = " (bind ?list (create$ ";
                    attribs = "";
                    lhs = "(defrule FUZZY-REQUIREMENTS::" + subobj + "-attrib "
                            + "?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (data-rate-duty-cycle# ?dc) (power-duty-cycle# ?pc) (Parameter " + param + ")";
                    current_subobj = subobj;
                    current_param = param;
                    nattrib = 0;
                    subobj_tests = new HashMap<>();
                } else {
                    //start next requirement rule
                    rhs = "";
                    rhs2 = " (bind ?list (create$ ";
                    attribs = "";
                    lhs = "(defrule FUZZY-REQUIREMENTS::" + subobj + "-attrib "
                            + "?m <- (REQUIREMENTS::Measurement (taken-by ?whom) (data-rate-duty-cycle# ?dc) (power-duty-cycle# ?pc) (Parameter " + param + ") ";
                    current_subobj = subobj;
                    current_param = param;
                    subobj_tests = new HashMap<>();
                    //nattrib = 0;
                }
            }

            String attrib = row[2].getContents();
            attribs = attribs + " " + attrib;
            String type = row[3].getContents();
            String thresholds = row[4].getContents();
            String scores = row[5].getContents();
            String justif = row[6].getContents();
            attrib_test.add(type);
            attrib_test.add(thresholds);
            attrib_test.add(scores);
            subobj_tests.put(attrib, attrib_test);
            nattrib++;
            lhs = lhs + " (" + attrib + " ?val" + nattrib + "&~nil) ";
            rhs = rhs + "(bind ?x" + nattrib + " (nth$ (find-bin-num ?val" + nattrib + " " + toJessList(thresholds) + " ) " + toJessList(scores) + "))";
            rhs = rhs + "(if (< ?x" + nattrib + " 1.0) then (bind ?new-reasons (replace$  ?new-reasons " + nattrib + " " + nattrib + " " + justif
                    + " )) (bind ?reason (str-cat ?reason " + " " + justif + "))) ";
            rhs2 = rhs2 + " ?x" + nattrib;
        }
        //Last rule has not been processed
        String[] tokens = current_subobj.split("-", 2);// limit = 2 so that remain contains RegionofInterest Global
        String parent = tokens[0];
        String index = tokens[1];
        call2 = call2 + " (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (fuzzy-value "
                + "(new FuzzyValue \"Value\" 0.0 0.0 0.0 \"utils\" (call ValueMap getValueInterval)))"
                + " (id " + current_subobj + ") (index " + index + ") (parent " + parent + ") "
                + "(reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + " ))) ";
        String rhs0 = ") => (bind ?reason \"\") (bind ?new-reasons (create$ " + StringUtils.repeat("N-A ", nattrib) + "))";
        req_rule = lhs + rhs0 + rhs + rhs2 + " ?dc ?pc))(assert (AGGREGATION::SUBOBJECTIVE (id " + current_subobj + ") (attributes " + attribs + ") (index " + index + ") (parent " + parent + " ) "
                + "(attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue \"Value\" (call "
                + "(new FuzzyValue \"Value\" (new Interval \"interval\" (*$ ?list) (*$ ?list)) \"utils\" "
                + "(call ValueMap getIntervalValue)) getFuzzy_val) \"utils\" (call ValueMap getValueInterval))) "
                + " (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason )))";
        req_rule = req_rule + ")";

        r.eval(req_rule);
        call2 = call2 + ")";
        r.eval(call2);
    }

    /**
     * All measurements for a given instruments must have the same attributes.
     * If it does not have a specific attribute need to set it to nill in excel
     * sheet.
     *
     * @param r
     * @param xls
     */
    private void loadCapabilityRules(Rete r, Workbook xls, String clp) throws JessException {
        r.batch(clp);
        for (Instrument instrument : EOSSDatabase.getInstruments()) {
            String instrumentName = instrument.getName();
            Sheet sh = xls.getSheet(instrumentName);
            int nmeasurements = sh.getRows();
            System.out.println("Loading capabilities for " + instrumentName + "...");
            String call = "(defrule MANIFEST::" + instrumentName + "-init-can-measure " + "(declare (salience -20)) ?this <- (CAPABILITIES::Manifested-instrument  (Name ?ins&" + instrumentName
                    + ") (Id ?id) (flies-in ?miss) (Intent ?int) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il)) "
                    + " (not (CAPABILITIES::can-measure (instrument ?ins) (can-take-measurements no))) => "
                    + "(assert (CAPABILITIES::can-measure (instrument ?ins) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (data-rate-duty-cycle# nil) (power-duty-cycle# nil)(orbit-RAAN ?raan)"
                    + "(in-orbit ?miss) (can-take-measurements yes) (reason \"by default\"))))";
            r.eval(call);

            String lhs1 = "(defrule CAPABILITIES-GENERATE::" + instrumentName + "-measurements " + "?this <- (CAPABILITIES::Manifested-instrument  (Name " + instrumentName + ") (Id ?id)(generated-measurements ?gm&nil)";
            StringBuilder lhs2 = null;
            String lhs3 = " (CAPABILITIES::can-measure (instrument " + instrumentName + ") (can-take-measurements yes) (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p)) => ";
            String rhs1 = "(assert (CAPABILITIES::resource-limitations (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p))) ";
            String list_of_measurements = "";
            StringBuilder rhs2 = new StringBuilder();
            for (int i = 0; i < nmeasurements; i++) {
                Cell[] row = sh.getRow(i);
                if (row.length == 0) {
                    continue;
                }
                if (row[0].getType().equals(jxl.CellType.EMPTY)) {
                    continue;
                }
                rhs2.append("(assert (REQUIREMENTS::Measurement");

                //inherit slots from capabilities to requirements that are not defined in the instumnt capability xls
                HashMap<String, SlotType> needsInheritance = new HashMap<>(capabilitiesToRequirementAttribInheritance);

                String capability_type = row[0].getContents();//Measurement
                if (!capability_type.equalsIgnoreCase("Measurement")) {
                    throw new IllegalArgumentException("loadCapabilityRules: Type of capability " + capability_type + " not recognized (use Measurement)");
                }
                String att_value_pair = row[1].getContents();
                String[] tokens2 = att_value_pair.split(" ", 2);
                String att = tokens2[0];//Parameter
                String meas = tokens2[1];//"x.x.x Soil moisture"
                ArchitectureEvaluatorParams.measurements.add(meas);

                for (int j = 1; j < row.length; j++) {
                    String att_value_pair2 = row[j].getContents();
                    rhs2.append(" (").append(att_value_pair2).append(") ");
                    String[] tokens = att_value_pair2.split(" ", 2);
                    needsInheritance.remove(tokens[0]);
                }
                rhs2.append("(taken-by ").append(instrumentName).append(") (Id ").append(instrumentName).append(i).append(") (Instrument ").append(instrumentName).append(")(synergy-level# 0) ");

                //create part of the rule to inherit the rest of the 
                lhs2 = new StringBuilder();
                Iterator<String> iter = needsInheritance.keySet().iterator();
                int ind = 0;
                while (iter.hasNext()) {
                    String slotName = iter.next();
                    SlotType slotType = needsInheritance.get(slotName);
                    if (slotName.equalsIgnoreCase("Id")) {
                        //since id is used in assert, need to make it a special case
                        continue;
                    } else if (slotType.equals(SlotType.SLOT)) {
                        lhs2.append("(").append(slotName).append(" ?var").append(String.valueOf(ind)).append(")");
                        rhs2.append("(").append(slotName).append(" ?var").append(String.valueOf(ind)).append(")");
                    } else if (slotType.equals(SlotType.MULTISLOT)) {
                        lhs2.append("(").append(slotName).append(" $?var").append(String.valueOf(ind)).append(")");
                        rhs2.append("(").append(slotName).append(" $?var").append(String.valueOf(ind)).append(")");
                    }
                    ind++;
                }
                lhs2.append(")");
                rhs2.append("))");

                list_of_measurements = list_of_measurements + " " + instrumentName + i + " ";
            }
            String rhs3 = "(assert (SYNERGY::cross-registered (measurements " + list_of_measurements + " ) (degree-of-cross-registration instrument) (platform ?id  )))";
            rhs3 = rhs3 + "(modify ?this (measurement-ids " + list_of_measurements + ")(generated-measurements TRUE)))";
            r.eval(lhs1 + lhs2 + lhs3 + rhs1 + rhs2 + rhs3);
        }
    }

    private void loadSynergyRules(Rete r, String clp) throws JessException {
        r.batch(clp);
        Iterator<Map.Entry<String, ArrayList<String>>> it = ArchitectureEvaluatorParams.measurements_to_subobjectives.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ArrayList<String>> es = it.next();
            String meas = (String) es.getKey();
            ArrayList<String> subobjs2 = (ArrayList<String>) es.getValue();
            Iterator subobjs = subobjs2.iterator();
            String call = "(defrule SYNERGIES::stop-improving-" + meas.substring(1, meas.indexOf(" ")) + " ";
            while (subobjs.hasNext()) {
                String var = (String) subobjs.next();
                call = call + " (REASONING::fully-satisfied (subobjective " + var + "))";
            }
            call = call + " => (assert (REASONING::stop-improving (Measurement " + meas + "))))";
            r.eval(call);
        }
    }

    private void loadAggregationRules(Rete r, File aggregationFile, String[] clps) throws JessException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder dBuilder;
        for (String clp : clps) {
            r.batch(clp);
        }

        //Stakeholders or panels
        dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(aggregationFile);
        doc.getDocumentElement().normalize();
        NodeList panelNode = doc.getElementsByTagName("panel");
        ArchitectureEvaluatorParams.npanels = panelNode.getLength();
        String call = "(deffacts AGGREGATION::init-aggregation-facts ";
        ArchitectureEvaluatorParams.panel_names = new ArrayList<>(ArchitectureEvaluatorParams.npanels);
        ArchitectureEvaluatorParams.panel_weights = new ArrayList<>(ArchitectureEvaluatorParams.npanels);

        for (int i = 0; i < panelNode.getLength(); i++) {
            Element panel = (Element) panelNode.item(i);
            ArchitectureEvaluatorParams.panel_names.add(panel.getElementsByTagName("id").item(0).getTextContent());
            ArchitectureEvaluatorParams.panel_weights.add(Weight.parseWeight(panel.getElementsByTagName("weight").item(0).getTextContent()));
        }
        call = call + " (AGGREGATION::VALUE (sh-scores (repeat$ -1.0 " + ArchitectureEvaluatorParams.npanels + ")) (sh-fuzzy-scores (repeat$ -1.0 " + ArchitectureEvaluatorParams.npanels + ")) (weights " + javaArrayList2JessList(ArchitectureEvaluatorParams.panel_weights) + "))";

        //load objectives and subobjective weights
        for (int i = 0; i < panelNode.getLength(); i++) {
            Element panel = (Element) panelNode.item(i);
            NodeList objNode = panel.getElementsByTagName("objective");
            ArrayList<Double> obj_weights = new ArrayList<>();

            for (int j = 0; j < objNode.getLength(); j++) { //cycle through objectives
                Element obj = (Element) objNode.item(j);
                obj_weights.add(Weight.parseWeight(obj.getElementsByTagName("weight").item(0).getTextContent()));
                NodeList subobjNode = obj.getElementsByTagName("subobjective");

                ArrayList<Double> subobj_weights_o = new ArrayList<>();
                for (int k = 0; k < subobjNode.getLength(); k++) { //cycle through subobjectives
                    Element subobj = (Element) subobjNode.item(k);
                    Double weight = Weight.parseWeight(subobj.getElementsByTagName("weight").item(0).getTextContent());
                    subobj_weights_o.add(weight);
                }
                call = call + " (AGGREGATION::OBJECTIVE (id " + ArchitectureEvaluatorParams.panel_names.get(i) + (j + 1) + " ) (parent " + ArchitectureEvaluatorParams.panel_names.get(i) + ") (index " + (j + 1) + " ) (subobj-fuzzy-scores (repeat$ -1.0 " + subobj_weights_o.size() + ")) (subobj-scores (repeat$ -1.0 " + subobj_weights_o.size() + ")) (weights " + javaArrayList2JessList(subobj_weights_o) + ")) ";
            }
            call = call + " (AGGREGATION::STAKEHOLDER (id " + ArchitectureEvaluatorParams.panel_names.get(i) + " ) (index " + (i + 1) + " ) (obj-fuzzy-scores (repeat$ -1.0 " + obj_weights.size() + ")) (obj-scores (repeat$ -1.0 " + obj_weights.size() + ")) (weights " + javaArrayList2JessList(obj_weights) + ")) ";
        }

        call = call + ")";//close deffacts
        r.eval(call);
    }

    private String javaArrayList2JessList(ArrayList list) {
        String call = "(create$";
        for (int i = 0; i < list.size(); i++) {
            call = call + " " + list.get(i);
        }
        call = call + ")";
        return call;
    }

    private void loadExplanationRules(Rete r, String clp) throws JessException {
        r.batch(clp);
        String call = "(defquery REQUIREMENTS::search-all-measurements-by-parameter \"Finds all measurements of this parameter in the campaign\" "
                + "(declare (variables ?param)) "
                + "(REQUIREMENTS::Measurement (Parameter ?param) (flies-in ?flies) (launch-date ?ld) (lifetime ?lt) (Instrument ?instr)"
                + " (Temporal-resolution ?tr) (All-weather ?aw) (Horizontal-Spatial-Resolution ?hsr) (Spectral-sampling ?ss)"
                + " (taken-by ?tk) (Vertical-Spatial-Resolution ?vsr) (sensitivity-in-low-troposphere-PBL ?tro) (sensitivity-in-upper-stratosphere ?str)))";
        r.eval(call);
    }

    public String toJessList(String str) {//str = [a,b,c]; goal is to return (create$ a b c)
        String str2 = str.substring(1, str.length() - 1);//get rid of []
        return " (create$ " + str2.replace(",", " ") + ")";
    }

    private String createJessList(String str) {
        String s = "(create$ ";

        str = str.substring(1, str.length() - 1);
        String[] list = str.split(",");

        for (int i = 0; i < list.length; i++) {
            s = s + list[i] + " ";
        }

        return (s + ")");
    }
}
