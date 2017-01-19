/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author nozomi
 */
public class LaunchVehicle {

    private final String name;
    private final HashMap<String, List<Double>> payload_coeffs;
    private final double diameter;
    private final double height;
    private final double cost;

    public LaunchVehicle(String name, HashMap<String, List<Double>> payload_coeffs, double diameter, double height, double cost) {
        this.name = name;
        this.payload_coeffs = payload_coeffs;
        this.diameter = diameter;
        this.height = height;
        this.cost = cost;
    }

    public List<Double> getPayload_coeffsOrbit(String orb) {
        return payload_coeffs.get(orb);
    }

    public double getDiameter() {
        return diameter;
    }

    public double getHeight() {
        return height;
    }

    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }

    /**
     * This method will automatically select the launch vehicles for the given
     * mission. Null is assigned to spacecraft for which no launch vehicle can
     * inject it to its desired orbit
     *
     * @param mission
     * @return
     */
    public static HashMap<Spacecraft, LaunchVehicle> select(Mission mission) {
        HashMap<Spacecraft, LaunchVehicle> out = new HashMap<>();
        for (Spacecraft s : mission.getSpacecraft().keySet()) {
            LaunchVehicle lv = select(s, mission.getSpacecraft().get(s));
            out.put(s, lv);
        }
        return out;
    }

    /**
     * This method will automatically select the launch vehicles for the given
     * spacecraft and orbit. Null is assigned to spacecraft for which no launch
     * vehicle can inject it to its desired orbit
     *
     * @param s
     * @param o
     * @return launch vehicle capable of injecting spacecraft s into orbit o
     */
    private static LaunchVehicle select(Spacecraft s, Orbit o) {
        //find all the feasible launch vehicles
        ArrayList<LaunchVehicle> feasibleLV = new ArrayList<>();
        for (LaunchVehicle lv : EOSSDatabase.getLaunchVehicles()) {
            if (lv.canLaunch(s, o)) {
                feasibleLV.add(lv);
            }
        }
        if (feasibleLV.isEmpty()) {
            return null;
        }

        //find the cheapest launch vehicle
        Collections.shuffle(feasibleLV);
        LaunchVehicle cheapestLV = feasibleLV.get(0);
        for (LaunchVehicle lv : feasibleLV) {
            if (lv.getCost() < cheapestLV.getCost()) {
                cheapestLV = lv;
            }
        }
        return cheapestLV;
    }

    /**
     * Checks multiple parameters of the spacecraft to see if this launch
     * vehicle is capable of launching the given spacecraft
     *
     * @param s
     * @param orbit
     * @return
     */
    public boolean canLaunch(Spacecraft s, Orbit orbit) {
        return (checkMass(s, orbit) >= 0
                && checkDiameter(s) >= 0
                && checkVolume(s) >= 0);
    }

    /**
     * Checks if this launch vehicle can lift the mass of the given spacecraft
     *
     * @param s spacecraft to launch
     * @param orbit to launch to
     * @return the margin left in the lift capability in kg. Negative values
     * mean that the spacecraft is too heavy to lift to the specified orbit.
     */
    private double checkMass(Spacecraft s, Orbit orbit) {
        List<Double> coeffs;
        try {
            coeffs = payload_coeffs.get(orbit.getType() + "-" + orbit.getInclination());
        } catch (NullPointerException ex) {
            return Double.NEGATIVE_INFINITY; //this launch vehicle does not fly to the specified orbit
        }
        double alt = orbit.getAltitude();
        double performance = coeffs.get(0) + alt * coeffs.get(1) + Math.pow(alt, 2.0) * coeffs.get(2);
        return performance - s.getWetMass();
    }

    /**
     * Checks if this launch vehicle can fit the given spacecraft in the
     * fairing. Assumes cylindrical shape
     *
     * @param s spacecraft to launch
     * @return the margin left in the volume of the fairing in m^3. Negative
     * values mean that the spacecraft does not fit within the fairing.
     */
    private double checkVolume(Spacecraft s) {
        double volume = 0;
        for (double d : s.getDimensions()) {
            volume *= d;
        }
        return (Math.PI * Math.pow(this.diameter / 2.0, 2.0)) * this.height - volume;
    }

    /**
     * Checks if this launch vehicle can fit the given spacecraft within the
     * maximum dimension of the fairing
     *
     * @param s spacecraft to launch
     * @return the margin left in the fairing along the maximum dimension of the
     * fairing in m. Negative values mean that the spacecraft does not fit
     * within the fairing.
     */
    private double checkDiameter(Spacecraft s) {
        double maxDimensions = 0;
        for (double d : s.getDimensions()) {
            maxDimensions = Math.max(maxDimensions, d);
        }
        double maxDimensionf = Math.max(this.diameter, this.height);
        return maxDimensionf - maxDimensions;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.name);
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.diameter) ^ (Double.doubleToLongBits(this.diameter) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.height) ^ (Double.doubleToLongBits(this.height) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.cost) ^ (Double.doubleToLongBits(this.cost) >>> 32));
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
        final LaunchVehicle other = (LaunchVehicle) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (Double.doubleToLongBits(this.diameter) != Double.doubleToLongBits(other.diameter)) {
            return false;
        }
        if (Double.doubleToLongBits(this.height) != Double.doubleToLongBits(other.height)) {
            return false;
        }
        if (Double.doubleToLongBits(this.cost) != Double.doubleToLongBits(other.cost)) {
            return false;
        }
        return true;
    }

}
