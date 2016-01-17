/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.util.HashMap;
import jess.ValueVector;
/**
 *
 * @author dani
 */
public class LaunchVehicle {
    private final String id;
    private final HashMap<String,ValueVector> payload_coeffs;
    private final double diameter;
    private final double height;
    private final double cost;
 
    public LaunchVehicle(String id, HashMap<String, ValueVector> payload_coeffs, double diameter, double height, double cost) {
        this.id = id;
        this.payload_coeffs = payload_coeffs;
        this.diameter = diameter;
        this.height = height;
        this.cost = cost;
    }
    public ValueVector getPayload_coeffsOrbit(String orb) {
        return payload_coeffs.get(orb);
    }
 
    public double getDiameter() {
        return diameter;
    }
 
    public double getHeight() {
        return height;
    }
 
 
    public double getCost() {
        return cost;
    }
 
     
}