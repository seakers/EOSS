/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

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

    /**
     * The spacecraft's payload
     */
    private final Collection<Instrument> paylaod;

    /**
     * The satellite's wet mass
     */
    private double wetMass;

    /**
     * The satellite's x, y, z dimensions
     */
    private double[] dimensions;

    /**
     * Properties pertaining to this spacecraft
     */
    private final HashMap<String, String> properties;

    public Spacecraft(String name, Collection<Instrument> paylaod) {
        this.name = name;
        this.paylaod = paylaod;
        this.properties = new HashMap<>();
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Gets the name of this spacecraft
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
    public Collection<Instrument> getPaylaod() {
        return paylaod;
    }

    /**
     * Gets the satellite's wetmass in kg
     *
     * @return
     */
    public double getWetMass() {
        return wetMass;
    }

    /**
     * Sets the satellite's wetmass in kg
     *
     * @param wetMass
     */
    public void setWetMass(double wetMass) {
        this.wetMass = wetMass;
    }

    /**
     * Gets the satellite's x, y, and z dimensions in m
     * @return 
     */
    public double[] getDimensions() {
        return dimensions;
    }

    /**
     * Sets the satellite's x, y, and z dimensions in m
     * @param dimensions 
     */
    public void setDimensions(double[] dimensions) {
        this.dimensions = dimensions;
    }
    
    /**
     * Creates a copy of this instance
     * @return 
     */
    public Spacecraft copy(){
        Spacecraft copy =  new Spacecraft(name, paylaod);
        for(String str : properties.keySet()){
            copy.setProperty(str, properties.get(str));
        }
        copy.setDimensions(this.getDimensions());
        copy.setWetMass(this.getWetMass());
        return copy;
    }
    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.paylaod);
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
        if (!Objects.equals(this.paylaod, other.paylaod)) {
            return false;
        }
        if (!Objects.equals(this.properties, other.properties)) {
            return false;
        }
        return true;
    }

}
