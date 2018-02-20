/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

import eoss.problem.Instrument;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

/**
 *
 * @author nozomihitomi
 */
public class Spacecraft implements Serializable {

    private static final long serialVersionUID = 42723869369021765L;

    private final String name;

    private ADCS adcs;

    private CDH cdh;

    private Communications comms;

    private EPS eps;

    private GNC gnc;

    private Propulsion prop;

    private Structure struct;

    private Thermal therm;

    private final HashMap<String,BusComponent> bus;

    private LaunchAdapter adapter;

    /**
     * The spacecraft's payload
     */
    private final Collection<Instrument> payload;

    /**
     * The payload's mass
     */
    private final double payloadMass;

    /**
     * The satellite's x, y, z dimensions
     */
    private double[] dimensions;

    /**
     * The satellite's moments of inertia (Ix, Iy, Iz)
     */
    private double[] inertiaMoments;

    /**
     * payload peak power
     */
    private final double payloadPeakPower;

    /**
     * payload average power
     */
    private final double payloadAvgPower;

    /**
     * Properties pertaining to this spacecraft
     */
    private final HashMap<String, String> properties;
    
    /**
     * Data rate
     */
    private final double dataRate;

    public Spacecraft(String name, Collection<Instrument> payload) {
        this.name = name;
        this.payload = payload;
        this.properties = new HashMap<>();

        double peakpower = 0.;
        double avgpower = 0.;
        double mass = 0.;
        double datarate = 0;
        for (Instrument inst : payload) {
            mass += Double.parseDouble(inst.getProperty("mass#"));
            peakpower += Double.parseDouble(inst.getProperty("characteristic-power#"));
            avgpower += Double.parseDouble(inst.getProperty("characteristic-power#"));
            datarate += Double.parseDouble(inst.getProperty("average-data-rate#"));
        }
        this.payloadMass = mass;
        this.payloadPeakPower = peakpower;
        this.payloadAvgPower = avgpower;
        this.dataRate = datarate;
        
        this.bus = new HashMap<>();
        bus.put("adcs",adcs);
        bus.put("cdh",cdh);
        bus.put("comms",comms);
        bus.put("eps",eps);
        bus.put("gnc",gnc);
        bus.put("prop", prop);
        bus.put("struct",struct);
        bus.put("therm",therm);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Gets the name of this spacecraft
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Gets the payload hosted by this spacecraft
     *
     * @return the payload hosted by this spacecraft
     */
    public Collection<Instrument> getPayload() {
        return payload;
    }

    /**
     * Gets the payload mass in kg
     *
     * @return
     */
    public double getPayloadMass() {
        return payloadMass;
    }
    /**
     * Gets the mass of all bus components defined in this spacecraft [kg]
     *
     * @return
     */
    public double getBusMass() {
        double mass = 0.0;
        for (BusComponent bc : bus.values()) {
            if (bc != null || bc instanceof LaunchAdapter) {
                mass += bc.getMass();
            }
        }
        return mass;
    }

    /**
     * Gets the power of all bus components defined in this spacecraft [W].
     * Positive numbers indicate power generated, negative numbers indicate
     * power is consumed.
     *
     * @return
     */
    public double getBusPower() {
        double power = 0.0;
        for (BusComponent bc : bus.values()) {
            if (bc != null) {
                power += bc.getPower();
            }
        }
        return power;
    }

    /**
     * Gets the satellite's dry mass in kg
     *
     * @return
     */
    public double getDryMass() {
        return getPayloadMass() + getBusMass();
    }

    /**
     * Gets the satellite's wet mass in kg
     *
     * @return
     */
    public double getWetMass() {
        return getDryMass() + prop.getMassInjection() + prop.getMassADCS();
    }

    /**
     * Gets the satellite's launch mass in kg
     *
     * @return
     */
    public double getLaunchMass() {
        return getWetMass() + adapter.getMass();
    }

    /**
     * Gets the average data rate of the payload
     * @return 
     */
    public double getDataRate() {
        return dataRate;
    }

    /**
     * Gets the satellite's x, y, and z dimensions in m
     *
     * @return
     */
    public double[] getDimensions() {
        return dimensions;
    }

    /**
     * Sets the satellite's x, y, and z dimensions in m
     *
     * @param dimensions
     */
    public void setDimensions(double[] dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * Gets the satellite's moments of inertia (Ix, Iy, Iz)
     *
     * @return
     */
    public double[] getInertiaMoments() {
        return inertiaMoments;
    }

    /**
     * Sets the satellite's moments of inertia (Ix, Iy, Iz)
     *
     * @param inertiaMoments
     */
    public void setInertiaMoments(double[] inertiaMoments) {
        this.inertiaMoments = inertiaMoments;
    }

    /**
     * Gets the attitude dynamics and control system
     *
     * @return the adcs of the satellite
     */
    public ADCS getADCS() {
        return adcs;
    }

    /**
     * Sets the attitude dynamics and control system
     *
     * @param adcs
     */
    public void setADCS(ADCS adcs) {
        this.adcs = adcs;
        this.bus.put("adcs", adcs);
    }

    /**
     * Gets the command and data handling subsystem
     *
     * @return the C&DH
     */
    public CDH getCDH() {
        return cdh;
    }

    /**
     * Sets the command and data handling subsystem
     *
     * @param cdh
     */
    public void setCDH(CDH cdh) {
        this.cdh = cdh;
        this.bus.put("cdh", cdh);
    }

    /**
     * Gets the communications subsystem
     *
     * @return
     */
    public Communications getComms() {
        return comms;
    }

    /**
     * Sets the communications subsystem
     *
     * @param comms
     */
    public void setComms(Communications comms) {
        this.comms = comms;
        this.bus.put("comms", comms);
    }

    /**
     * Gets the electrical and power subsystem
     *
     * @return
     */
    public EPS getEPS() {
        return eps;
    }

    /**
     * Sets the electrical and power subsystem
     *
     * @param eps
     */
    public void setEPS(EPS eps) {
        this.eps = eps;
        this.bus.put("eps", eps);
    }

    /**
     * Gets the guidance, navigation, and control subsystem
     *
     * @return
     */
    public GNC getGNC() {
        return gnc;
    }

    /**
     * Gets the guidance, navigation, and control subsystem
     *
     * @param gnc
     */
    public void setGNC(GNC gnc) {
        this.gnc = gnc;
        this.bus.put("gnc", gnc);
    }

    /**
     * Gets the propulsion subsystem
     *
     * @return
     */
    public Propulsion getPropulsion() {
        return prop;
    }

    /**
     * Sets the propulsion subsystem
     *
     * @param prop
     */
    public void setPropulsion(Propulsion prop) {
        this.prop = prop;
        this.bus.put("prop", prop);
    }

    /**
     * Gets the structures of the satellite
     *
     * @return
     */
    public Structure getStructure() {
        return struct;
    }

    /**
     * Sets the structures of the satellite
     *
     * @param struct
     */
    public void setStructure(Structure struct) {
        this.struct = struct;
        this.bus.put("struct", struct);
    }

    /**
     * Gets the thermal control subsystem
     *
     * @return
     */
    public Thermal getThermal() {
        return therm;
    }

    /**
     * Sets the thermal control subsystem
     *
     * @param therm
     */
    public void setThermal(Thermal therm) {
        this.therm = therm;
        this.bus.put("therm", therm);
    }

    /**
     * Gets the launch adapter for this satellite
     *
     * @return
     */
    public LaunchAdapter getAdapter() {
        return adapter;
    }

    /**
     * Sets the launch adapter for this satellite
     *
     * @param adapter
     */
    public void setAdapter(LaunchAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Gets the payload's peak power
     *
     * @return
     */
    public double getPayloadPeakPower() {
        return payloadPeakPower;
    }

    /**
     * Gets the payload's average power
     *
     * @return
     */
    public double getPayloadAvgPower() {
        return payloadAvgPower;
    }

    /**
     * Creates a copy of this instance
     *
     * @return
     */
    public Spacecraft copy() {
        Spacecraft copy = new Spacecraft(name, payload);
        for (String str : properties.keySet()) {
            copy.setProperty(str, properties.get(str));
        }
        copy.setDimensions(this.getDimensions());
        return copy;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.payload);
        hash = 67 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Spacecraft other = (Spacecraft) obj;
        if (!Objects.equals(this.payload, other.payload)) {
            return false;
        }
        if (!Objects.equals(this.properties, other.properties)) {
            return false;
        }
        return true;
    }

}
