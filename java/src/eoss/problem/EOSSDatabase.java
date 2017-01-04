/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import eoss.jess.JessInitializer;
import eoss.problem.assignment.InstrumentAssignmentParams;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.moeaframework.util.TypedProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Stores static information about the problem like the instruments and orbits.
 * This class is not threadsafe so it is assumed that the database is created
 * before any additional threads are created and that nothing is added or
 * removed by any threads throughout search
 *
 * @author nozomihitomi
 */
public final class EOSSDatabase {
    
    /**
     * List of instruments in order by index
     */
    private static ArrayList<Instrument> instrumentList;

    /**
     * Instruments mapping to indices
     */
    private static HashMap<Instrument, Integer> instrumentMap;

    /**
     * Mapping to connect instrument names with instruments
     */
    private static HashMap<String, Instrument> instrumentNames;

    /**
     * List of orbits in order by index
     */
    private static ArrayList<Orbit> orbitList;
    
    /**
     * Orbit mapping to indices
     */
    private static HashMap<Orbit, Integer> orbitMap;

    /**
     * Mapping to connect orbit names with orbits
     */
    private static HashMap<String, Orbit> orbitNames;

    /**
     * List of bus in order by index
     */
    private static ArrayList<Bus> busList;
    
    /**
     * bus mapping to indices
     */
    private static HashMap<Bus, Integer> buses;

    private static EOSSDatabase instance;

    /**
     * Singleton pattern constructor
     */
    private EOSSDatabase() {
        EOSSDatabase.instrumentList = new ArrayList<>();
        EOSSDatabase.instrumentMap = new HashMap<>();
        EOSSDatabase.instrumentNames = new HashMap<>();
        EOSSDatabase.orbitList = new ArrayList<>();
        EOSSDatabase.orbitMap = new HashMap<>();
        EOSSDatabase.orbitNames = new HashMap<>();
        EOSSDatabase.busList = new ArrayList<>();
        EOSSDatabase.buses = new HashMap<>();
    }

    /**
     * Returns the database instance
     *
     * @return
     */
    public static EOSSDatabase getInstance() {
        if (instance == null) {
            instance = new EOSSDatabase();
        }
        return instance;
    }

    public static void addInstrument(Instrument instrument) {
        Integer check = instrumentMap.put(instrument, instrumentList.size());
        if (check != null) {
            throw new IllegalArgumentException(String.format("Instrument %s already exists", instrument.getName()));
        }
        instrumentList.add(instrument);
        instrumentNames.put(instrument.getName(), instrument);
    }

    public static void addAllInstrument(Collection<Instrument> instruments) {
        for (Instrument inst : instruments) {
            addInstrument(inst);
        }
    }

    public static Collection<Instrument> getInstruments() {
        return instrumentMap.keySet();
    }
    
    /**
     * Gets the indexed instrument from the database if it exists. 
     *
     * @param index index of the instrument to get from the database
     * @return the indexed instrument from the database 
     */
    public static Instrument getInstrument(int index) {
        return instrumentList.get(index);
    }

    /**
     * Gets the named instrument from the database if it exists. Else returns
     * null
     *
     * @param name name of the instrument to get from the database
     * @return the named instrument from the database if it exists. Else returns
     * null
     */
    public static Instrument getInstrument(String name) {
        return instrumentNames.get(name);
    }
    
    /**
     * Gets the number of instruments stored in the database
     * @return the number of instruments stored in the database
     */
    public static int getNumberOfInstruments(){
        return instrumentMap.size();
    }

    /**
     * Loads the characteristics of the instruments and adds them to the
     * database
     *
     * @param file the file containing the instrument parameters
     */
    public static void loadInstruments(File file) {
        try {
            Workbook xls = Workbook.getWorkbook(file);
            Sheet meas = xls.getSheet("CHARACTERISTICS");
            int ninst = meas.getRows();
            int nattributes = meas.getColumns();

            //Headers and default values
            Cell[] header = meas.getRow(0);
            HashMap<String, String> headerVals = new HashMap<>(header.length);
            ArrayList<String> headerOrder = new ArrayList<>(header.length);
            for (Cell c : header) {
                String[] contents = c.getContents().split(" ");
                headerOrder.add(contents[0]);
                if (contents[0].equalsIgnoreCase("Name")) {
                    continue;
                }
                headerVals.put(contents[0], contents[1]);
            }

            for (int i = 1; i < ninst; i++) {
                Cell[] row = meas.getRow(i);
                String instrumentName = "";
                HashMap<String, String> properties = new HashMap();
                for (int j = 0; j < nattributes; j++) {
                    String cell_value = row[j].getContents();

                    String propertyName = headerOrder.get(j);
                    if (propertyName.equals("Name")) {
                        instrumentName = cell_value.trim();
                    } else {
                        String[] splitted = cell_value.split(" ");
                        String propertyValue;
                        if (splitted.length == 0) {
                            //use default value;
                            propertyValue = headerVals.get(propertyName);
                        } else {
                            //some columns have more than one value like a list
                            propertyValue = splitted[0];
                            for (int kk = 1; kk < splitted.length; kk++) {
                                propertyValue = propertyValue + " " + splitted[kk];
                            }
                        }
                        properties.put(propertyName, propertyValue);
                    }
                }
                //instruments must have names
                if (instrumentName.isEmpty()) {
                    throw new IllegalStateException(String.format("Could not find name for instrument in row %d", i));
                }
                Instrument newInstrument = new Instrument(instrumentName, properties);
                EOSSDatabase.addInstrument(newInstrument);
            }
            
            xls.close();
        } catch (IOException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BiffException ex) {
            Logger.getLogger(JessInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void removeInstrument(Instrument instrument) {
        EOSSDatabase.instrumentList.set(instrumentMap.get(instrument), null);
        EOSSDatabase.instrumentMap.remove(instrument);
        EOSSDatabase.instrumentNames.remove(instrument.getName());
    }

    public static List<Orbit> getOrbits() {
        Orbit[] out = new Orbit[orbitMap.size()];
        for (Orbit orb : orbitMap.keySet()) {
            out[orbitMap.get(orb)] = orb;
        }
        return Arrays.asList(out);
    }
    
    /**
     * Gets the orbit by querying the name
     * @param name orbit name
     * @return the orbit in the database with the given name
     */
    public static Orbit getOrbit(String name){
        return orbitNames.get(name);
    }
    

    /**
     * Gets the indexed orbit from the database if it exists. 
     *
     * @param index index of the orbit to get from the database
     * @return the indexed orbit from the database 
     */
    public static Orbit getOrbit(int index) {
        return orbitList.get(index);
    }
    
    /**
     * Gets the number of instruments stored in the database
     * @return the number of instruments stored in the database
     */
    public static int getNumberOfOrbits(){
        return orbitMap.size();
    }

    public static void addOrbit(Orbit orbit) {
        Integer check = orbitMap.put(orbit, orbitList.size());
        if (check != null) {
            throw new IllegalArgumentException(String.format("Orbit %s already exists", orbit.getName()));
        }
        EOSSDatabase.orbitNames.put(orbit.getName(), orbit);
        EOSSDatabase.orbitList.add(orbit);
    }

    public static void addAllOrbit(Collection<Orbit> orbits) {
        for (Orbit orb : orbits) {
            addOrbit(orb);
        }
    }

    public static void removeOrbit(Orbit orbit) {
        EOSSDatabase.orbitList.set(orbitMap.get(orbit), null);
        EOSSDatabase.orbitMap.remove(orbit);
        EOSSDatabase.orbitNames.remove(orbit.getName());
    }

    /**
     * Loads the characteristics of the orbits and adds them to the database
     *
     * @param file the file containing the orbit parameters
     */
    public static void loadOrbits(File file) {
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList orbitNodeList = doc.getElementsByTagName("orbit");
            for (int i = 0; i < orbitNodeList.getLength(); i++) {
                Element orbit = (Element) orbitNodeList.item(i);
                String orbName = orbit.getElementsByTagName("name").item(0).getTextContent().trim();
                String orbType = orbit.getElementsByTagName("type").item(0).getTextContent().trim();
                Double semimajorAxis = Double.valueOf(orbit.getElementsByTagName("semimajoraxis").item(0).getTextContent());
                String inclination = orbit.getElementsByTagName("inclination").item(0).getTextContent().trim();
                String RAAN = orbit.getElementsByTagName("raan").item(0).getTextContent().trim();
                Double meanAnomaly = Double.valueOf(orbit.getElementsByTagName("meananomaly").item(0).getTextContent());
                Double eccentricity = Double.valueOf(orbit.getElementsByTagName("eccentricity").item(0).getTextContent());
                Double argPeri = Double.valueOf(orbit.getElementsByTagName("argumentperigee").item(0).getTextContent());
                EOSSDatabase.addOrbit(new Orbit(orbName, Orbit.OrbitType.valueOf(orbType), semimajorAxis, inclination, RAAN, meanAnomaly, eccentricity, argPeri));
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InstrumentAssignmentParams.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InstrumentAssignmentParams.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InstrumentAssignmentParams.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static ArrayList<Bus> getBuses() {
        ArrayList<Bus> out = new ArrayList<>(buses.size());
        for (Bus bus : buses.keySet()) {
            out.set(buses.get(bus), bus);
        }
        return out;
    }

    public static void addBus(Bus bus) {
        Integer check = buses.put(bus, buses.size());
        if (check != null) {
            throw new IllegalArgumentException(String.format("Bus %s already exists", bus.getName()));
        }
    }

    public static void removeBus(Bus bus) {
        EOSSDatabase.buses.remove(bus);
    }

    /**
     * Loads the characteristics of the buses and adds them to the database
     *
     * @param file the file containing the bus parameters
     */
    public static void loadBuses(File file) {
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList instrumentNode = doc.getElementsByTagName("instrument");
            for (int i = 0; i < instrumentNode.getLength(); i++) {
                Element bus = (Element) instrumentNode.item(i);
                String busName = bus.getElementsByTagName("name").item(0).getTextContent();
                Double busSize = Double.valueOf(bus.getElementsByTagName("size").item(0).getTextContent());
                Double busCost = Double.valueOf(bus.getElementsByTagName("cost").item(0).getTextContent());
                Double busLife = Double.valueOf(bus.getElementsByTagName("lifetime").item(0).getTextContent());
                EOSSDatabase.addBus(new Bus(busName, busSize, busCost, busLife));
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InstrumentAssignmentParams.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InstrumentAssignmentParams.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InstrumentAssignmentParams.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * finds and returns the index of the given instrument
     *
     * @param instrument
     * @return returns the index of the given instrument if found in Database.
     * else returns -1;
     */
    public static int findInstrumentIndex(Instrument instrument) {
        return instrumentMap.get(instrument);
    }

    /**
     * finds and returns the index of the given orbit
     *
     * @param orbit
     * @return returns the index of the given orbit if found in Database. else
     * returns -1;
     */
    public static int findOrbitIndex(Orbit orbit) {
        for (int i = 0; i < orbitMap.size(); i++) {
            if (orbitMap.get(i).equals(orbit)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * finds and returns the index of the given instrument
     *
     * @param bus
     * @return returns the index of the given instrument if found in Database.
     * else returns -1;
     */
    public static int findBusIndex(Bus bus) {
        for (int i = 0; i < buses.size(); i++) {
            if (buses.get(i).equals(bus)) {
                return i;
            }
        }
        return -1;
    }

}
