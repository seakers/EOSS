/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eoss.problem;

import java.util.Objects;
import org.moeaframework.util.TypedProperties;

/**
 * Describes the instrument
 * @author nozomihitomi
 */
public class Instrument {
    
    private final String name;
    private final double size;
    private final double cost;
    private final double mass;

    /**
     * 
     * @param name name of instrument
     * @param size in CubeSat Sizes
     * @param cost in millions of dollars
     * @param mass in kilograms
     */
    public Instrument(String name, double size, double cost, double mass) {
        this.name = name;
        this.size = size;
        this.cost = cost;
        this.mass = mass;
        if(name.equalsIgnoreCase(""))
            System.err.println("Instrument has no name");
        if(size<0 || cost<0 || mass<0)
            throw new IllegalArgumentException("Instrument has invalid parameters: Size = " + size + ", Cost = " + cost + ", Mass = " + mass + ".");
    }
    
    /**
     * Creates an instrument object using the x,y, and z dimensions and assumes cuboid shape.
     * @param name name of instrument
     * @param xSize x dimension
     * @param ySize y dimension
     * @param zSize z dimension
     * @param cost in millions of dollars
     * @param mass in kilograms
     */
    public Instrument(String name, double xSize, double ySize,double zSize, double cost, double mass) {
        this(name,xSize * ySize * zSize,cost,mass);
    }
    
    /**
     * 
     * @param prop properties of the instrument
     */
    public Instrument(TypedProperties prop) {
        this(prop.getString("name", ""),prop.getDouble("xSize", -1),prop.getDouble("ySize", -1),prop.getDouble("zSize", -1),prop.getDouble("cost", -1),prop.getDouble("mass", -1));
    }

    public String getName() {
        return name;
    }

    public double getSize() {
        return size;
    }

    public double getCost() {
        return cost;
    }

    public double getMass() {
        return mass;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.name);
        return hash;
    }

    /**
     * Checks if names are the same
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Instrument other = (Instrument) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
    
}
