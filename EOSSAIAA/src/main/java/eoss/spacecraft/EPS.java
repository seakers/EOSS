/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.spacecraft;

/**
 * The electrical power system
 *
 * @author nozomihitomi
 */
public class EPS extends AbstractBusComponent {
    private static final long serialVersionUID = -4829214708833816808L;

    /**
     * Area of the solar arrays
     */
    private final double solarArrayArea;

    /**
     * Mass of the solar arrays
     */
    private final double solarArrayMass;

    /**
     * Mass of the battery
     */
    private final double batteryMass;

    /**
     * Mass of the regulators, converters, wires
     */
    private final double otherMass;

    /**
     * Power at beginning of life
     */
    private final double powerBOL;

    /**
     * This subsystem consists of the solar arrays, batteries and other smaller
     * components such as regulators, converters, and wires.
     *
     * @param solarArrayArea
     * @param solarArrayMass
     * @param batterMass
     * @param otherMass
     * @param powerBOL
     */
    public EPS(double solarArrayArea, double solarArrayMass, double batterMass, double otherMass, double powerBOL) {
        super(solarArrayMass + batterMass + otherMass, powerBOL);
        this.solarArrayArea = solarArrayArea;
        this.solarArrayMass = solarArrayMass;
        this.batteryMass = batterMass;
        this.otherMass = otherMass;
        this.powerBOL = powerBOL;
    }

    /**
     * Gets the solar array area in m^2
     *
     * @return the solar array area in m^2
     */
    public double getSolarArrayArea() {
        return solarArrayArea;
    }

    /**
     * Gets the solar array mass in kg
     *
     * @return the solar array mass in kg
     */
    public double getSolarArrayMass() {
        return solarArrayMass;
    }

    /**
     * Gets the battery mass in kg
     *
     * @return the battery mass in kg
     */
    public double getBatteryMass() {
        return batteryMass;
    }

    /**
     * Gets the mass of the converters, regulators, and wires in kg
     *
     * @return the mass of the converters, regulators, and wires in kg
     */
    public double getOtherMass() {
        return otherMass;
    }

    /**
     * Gets the power at beginning of life in Watts
     *
     * @return
     */
    public double getPowerBOL() {
        return powerBOL;
    }

}
