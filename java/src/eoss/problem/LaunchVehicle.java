/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import eoss.spacecraft.Spacecraft;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author nozomi
 */
public class LaunchVehicle implements Serializable {

    private static final long serialVersionUID = 2902112748976731561L;

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

    /**
     * Gets the volume of the fairing. Assumes cylindrical shape
     *
     * @return
     */
    public double getVolume() {
        return (Math.PI * Math.pow(this.diameter / 2.0, 2.0)) * this.height;
    }

    /**
     * Gets the amount of mass that the launch vehicle can carry to the
     * specified orbit
     *
     * @param orbit the orbit to launch to
     * @return the amount of mass that the launch vehicle can carry to the
     * specified orbit. If the launch vehicle cannot be flown to the specified
     * orbit, Negative infinity is returned
     */
    public double getMassBudget(Orbit orbit) {
        List<Double> coeffs;
        try {
            coeffs = payload_coeffs.get(orbit.getType() + "-" + orbit.getInclination());
        } catch (NullPointerException ex) {
            return Double.NEGATIVE_INFINITY; //this launch vehicle does not fly to the specified orbit
        }
        double alt = orbit.getAltitude();
        return coeffs.get(0) + alt * coeffs.get(1) + Math.pow(alt, 2.0) * coeffs.get(2);
    }

    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }

    /**
     * This method will automatically select the launch vehicles for the given
     * missions. It tries to group spacecraft flying to the same orbit on the
     * same launch vehicle. Null is assigned to spacecraft for which no launch
     * vehicle can inject it to its desired orbit
     *
     * @param missions the missions to assign launch vehicles to
     * @return
     */
    public static HashMap<Collection<Spacecraft>, LaunchVehicle> select(Collection<Mission> missions) {
        //first group spacecraft with same orbits together
        HashMap<Orbit, Collection<Spacecraft>> orbitMap = new HashMap<>();
        for (Mission m : missions) {
            for (Spacecraft s : m.getSpacecraft().keySet()) {
                Orbit o = m.getSpacecraft().get(s);
                if (!orbitMap.containsKey(o)) {
                    orbitMap.put(o, new ArrayList<>());
                }
                orbitMap.get(o).add(s);
            }
        }

        //find cheapest launch vehicle(s) for each grouping
        HashMap<Collection<Spacecraft>, LaunchVehicle> out = new HashMap<>();
        for (Orbit o : orbitMap.keySet()) {
            HashMap<Collection<Spacecraft>, LaunchVehicle> lvSelection = select(orbitMap.get(o), o);
            out.putAll(lvSelection);
        }
        return out;
    }

    /**
     * This method will automatically select the launch vehicles for the given
     * mission. Null is assigned to spacecraft for which no launch vehicle can
     * inject it to its desired orbit
     *
     * @param mission
     * @return
     */
    public static HashMap<Collection<Spacecraft>, LaunchVehicle> select(Mission mission) {
        //first group spacecraft with same orbits together
        HashMap<Orbit, Collection<Spacecraft>> orbitMap = new HashMap<>();
        for (Spacecraft s : mission.getSpacecraft().keySet()) {
            Orbit o = mission.getSpacecraft().get(s);
            if (!orbitMap.containsKey(o)) {
                orbitMap.put(o, new ArrayList<>());
            }
            orbitMap.get(o).add(s);
        }

        HashMap<Collection<Spacecraft>, LaunchVehicle> out = new HashMap<>();
        for (Orbit o : orbitMap.keySet()) {
            HashMap<Collection<Spacecraft>, LaunchVehicle> lvSelection = select(orbitMap.get(o), o);
            out.putAll(lvSelection);
        }
        return out;
    }

    /**
     * This method will automatically select the launch vehicles for the given
     * spacecraft and orbit. Null is assigned to spacecraft for which no launch
     * vehicle can inject it to its desired orbit
     *
     * @param spacecraft
     * @param o
     * @return a map assigning each spacecraft subset to a launch vehicle
     * capable of injecting spacecrafts into the given orbit. If a spacecraft
     * cannot be launched to the specified orbit, it will not be present in the
     * map
     */
    private static HashMap<Collection<Spacecraft>, LaunchVehicle> select(Collection<Spacecraft> spacecraft, Orbit o) {
        ArrayList<Spacecraft> spacecraftList = new ArrayList<>(spacecraft);

        HashMap<Collection<Spacecraft>, LaunchVehicle> bestOption = new HashMap<>();
        double lowestCost = Double.POSITIVE_INFINITY;

        //find all combinations of spacecraft
        for (List<Integer> groupIndex : fullfactPartition(spacecraft.size())) {
            HashMap<Integer, ArrayList<Spacecraft>> groups = new HashMap<>();
            int index = 0;
            for (Integer groupNumber : groupIndex) {
                if (!groups.containsKey(groupNumber)) {
                    groups.put(groupNumber, new ArrayList<>());
                }
                groups.get(groupNumber).add(spacecraftList.get(index));
                index++;
            }

            double sumCost = 0;
            HashMap<Collection<Spacecraft>, LaunchVehicle> option = new HashMap();
            for (Integer groupNumber : groupIndex) {
                //find all the feasible launch vehicles for each group of spacecraft
                ArrayList<LaunchVehicle> feasibleLV = new ArrayList<>();
                for (LaunchVehicle lv : EOSSDatabase.getLaunchVehicles()) {
                    if (lv.canLaunch(groups.get(groupNumber), o)) {
                        feasibleLV.add(lv);
                    }
                }
                double cheapestLVCost;
                if (feasibleLV.isEmpty()) {
                    cheapestLVCost = Double.POSITIVE_INFINITY;
                    option.put(groups.get(groupNumber), null);
                } else {
                    //find the cheapest launch vehicle
                    Collections.shuffle(feasibleLV);
                    LaunchVehicle cheapestLV = feasibleLV.get(0);
                    for (LaunchVehicle lv : feasibleLV) {
                        if (lv.getCost() < cheapestLV.getCost()) {
                            cheapestLV = lv;
                        }
                    }
                    option.put(groups.get(groupNumber), cheapestLV);
                    cheapestLVCost = cheapestLV.getCost();
                }
                sumCost += cheapestLVCost;

            }

            if (sumCost < lowestCost) {
                lowestCost = sumCost;
                bestOption = option;
            }
        }
        return bestOption;
    }

    /**
     * Produces a full factorial enumeration of partitions for length n
     * decisions. The list of integers indicate which partition each index
     * belongs to
     *
     * @return
     */
    private static List<List<Integer>> fullfactPartition(int n) {
        LinkedList<List<Integer>> out = new LinkedList<>();
        List<Integer> initialSet = Arrays.asList(new Integer[]{0});
        out.add(initialSet);
        while (out.getFirst().size() < n) {
            List<Integer> list = out.removeFirst();

            int maxSetValue = 0;
            for (int i = 0; i < list.size(); i++) {
                maxSetValue = Math.max(maxSetValue, list.get(i));
            }

            for (int i = 0; i <= maxSetValue + 1; i++) {
                ArrayList<Integer> partialSolution = new ArrayList<>(list);
                partialSolution.add(i);
                out.add(partialSolution);
            }
        }
        return out;
    }

    /**
     * Checks multiple parameters of the spacecraft to see if this launch
     * vehicle is capable of launching the given spacecraft
     *
     * @param spacecrafts to launch
     * @param orbit to launch spacecraft in
     * @return true if this launch vehicle can launch the given spacecraft. Else
     * false.
     */
    public boolean canLaunch(Collection<Spacecraft> spacecrafts, Orbit orbit) {
        return (checkMass(spacecrafts, orbit) >= 0
                && checkDiameter(spacecrafts) >= 0
                && checkVolume(spacecrafts) >= 0);
    }

    /**
     * Checks if this launch vehicle can lift the mass of the given spacecraft
     *
     * @param spacecrafts spacecrafts to launch
     * @param orbit to launch to
     * @return the margin left in the lift capability in kg. Negative values
     * mean that the spacecraft is too heavy to lift to the specified orbit.
     */
    private double checkMass(Collection<Spacecraft> spacecrafts, Orbit orbit) {
        List<Double> coeffs;
        try {
            coeffs = payload_coeffs.get(orbit.getType() + "-" + orbit.getInclination());
        } catch (NullPointerException ex) {
            return Double.NEGATIVE_INFINITY; //this launch vehicle does not fly to the specified orbit
        }
        double alt = orbit.getAltitude();
        double performance = coeffs.get(0) + alt * coeffs.get(1) + Math.pow(alt, 2.0) * coeffs.get(2);

        double totalMass = 0.0;
        for (Spacecraft s : spacecrafts) {
            totalMass += s.getWetMass();
        }

        return performance - totalMass;
    }

    /**
     * Checks if this launch vehicle can fit the given spacecraft in the
     * fairing. Assumes cylindrical shape
     *
     * @param spacecrafts spacecrafts to launch
     * @return the margin left in the volume of the fairing in m^3. Negative
     * values mean that the spacecraft does not fit within the fairing.
     */
    private double checkVolume(Collection<Spacecraft> spacecrafts) {
        double totalVolume = 0.0;
        for (Spacecraft s : spacecrafts) {
            double volume = 1.0;
            for (double d : s.getDimensions()) {
                volume *= d;
            }
            totalVolume += volume;
        }
        return getVolume() - totalVolume;
    }

    /**
     * Checks if this launch vehicle can fit the given spacecraft within the
     * maximum dimension of the fairing
     *
     * @param spacecrafts spacecrafts to launch
     * @return the margin left in the fairing along the maximum dimension of the
     * fairing in m. Negative values mean that the spacecraft does not fit
     * within the fairing.
     */
    private double checkDiameter(Collection<Spacecraft> spacecrafts) {
        double totalMaxDimension = 0.0;
        for (Spacecraft s : spacecrafts) {
            double maxDimensions = 0;
            for (double d : s.getDimensions()) {
                maxDimensions = Math.max(maxDimensions, d);
            }
            totalMaxDimension += maxDimensions;
        }
        double maxDimensionf = Math.max(this.diameter, this.height);
        return maxDimensionf - totalMaxDimension;
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
