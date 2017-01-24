/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

/**
 *
 * @author nozomihitomi
 */
public class Mission implements Serializable {

    private static final long serialVersionUID = -4727595371483693432L;

    /**
     * The cost costProfile of the mission. $/year over a number of years
     */
    private final double[] costProfile;

    /**
     * Lifetime of the missions in years
     */
    private final double lifetime;

    /**
     * The name of the mission
     */
    private final String name;

    /**
     * The spacecraft that are part of this mission and their orbits
     */
    private final HashMap<Spacecraft, Orbit> spacecraft;

    /**
     * Launch date name
     */
    private final AbsoluteDate launchDate;

    /**
     * End of life date
     */
    private final AbsoluteDate eolDate;

    /**
     * Mission status
     */
    private final MissionStatus status;

    public Mission(String name, HashMap<Spacecraft, Orbit> spacecraft,
            AbsoluteDate launchDate, double lifetime, MissionStatus status,
            int devYears, double costPerYear) {
        this.costProfile = new double[devYears];
        Arrays.fill(this.costProfile, costPerYear);
        this.name = name;
        this.spacecraft = spacecraft;
        this.launchDate = launchDate;
        this.eolDate = launchDate.shiftedBy(lifetime * 365. * 24. * 3600.);
        this.status = status;
        this.lifetime = lifetime;
    }

    public Mission(String name, HashMap<Spacecraft, Orbit> spacecraft,
            AbsoluteDate launchDate, AbsoluteDate endOfLife, MissionStatus status,
            int devYears, double costPerYear) {
        this.costProfile = new double[devYears];
        Arrays.fill(this.costProfile, costPerYear);
        this.name = name;
        this.spacecraft = spacecraft;
        this.launchDate = launchDate;
        this.eolDate = endOfLife;
        this.status = status;
        this.lifetime = endOfLife.durationFrom(launchDate) / (365. * 24. * 3600.);
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the cost costProfile of the mission over a number of years ($/yr)
     *
     * @return the cost costProfile of the mission over a number of years ($/yr)
     */
    public double[] getCostProfile() {
        return Arrays.copyOf(costProfile, costProfile.length);
    }

    /**
     * Gets the lifetime of this mission in years
     *
     * @return the lifetime of this mission in years
     */
    public double getLifetime() {
        return lifetime;
    }

    /**
     * Gets the spacecraft for this mission and their orbits
     *
     * @return the spacecraft for this mission and their orbits
     */
    public HashMap<Spacecraft, Orbit> getSpacecraft() {
        return spacecraft;
    }

    public AbsoluteDate getLaunchDate() {
        return launchDate;
    }

    public AbsoluteDate getEolDate() {
        return eolDate;
    }

    public MissionStatus getStatus() {
        return status;
    }

    /**
     * Gets a builder that copies all parameters over into the builder. This
     * builder can be used to make new instances that are variations of this
     * Mission
     *
     * @return a builder that copies all parameters over into the builder
     */
    public Builder getBuilder() {
        Builder out = new Builder(this.name, this.spacecraft).
                devYr(this.costProfile.length).devCostYr(this.costProfile[0]).
                lifetime(this.lifetime).status(this.status).
                launchDate(this.launchDate);
        return out;
    }

    /**
     * Creates a copy of the mission. Deep copy of the spacecraft stored in this
     * mission
     *
     * @return copy of this mission
     */
    public Mission copy() {
        HashMap<Spacecraft, Orbit> map = new HashMap<>(this.spacecraft.size());
        for(Spacecraft s : getSpacecraft().keySet()){
            map.put(s.copy(), getSpacecraft().get(s));
        }
        return new Mission(this.name, map, this.launchDate, this.eolDate, 
                this.status, this.costProfile.length, this.costProfile[0]);
    }

    /**
     * A builder pattern to set parameters for the missions
     */
    public static class Builder implements Serializable {

        private static final long serialVersionUID = 1806638584783338638L;

        //required fields
        /**
         * The name of the mission
         */
        private final String name;

        /**
         * The spacecraft that are part of this mission and their orbits
         */
        private final HashMap<Spacecraft, Orbit> spacecraft;

        //optional parameters - initialized to default parameters
        /**
         * Launch date name
         */
        private AbsoluteDate launchDate;

        /**
         * The lifetime of the mission in years
         */
        private double lifetime = 5;

        /**
         * Mission status
         */
        private MissionStatus status = MissionStatus.PLANNED;

        /**
         * Years to develop the mission
         */
        private int developmentYears = 1;

        /**
         * Cost per year to develop the mission
         */
        private double developmentCostYr = 0;

        /**
         * The constructor for the builder
         *
         * @param name the name of the mission
         * @param spacecraft the spacecraft that are part of this mission and
         * their orbit
         */
        public Builder(String name, HashMap<Spacecraft, Orbit> spacecraft) {
            this.name = name;
            this.spacecraft = spacecraft;
            try {
                this.launchDate = new AbsoluteDate(2015, 01, 01, TimeScalesFactory.getUTC());
            } catch (OrekitException ex) {
                Logger.getLogger(Mission.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /**
         * Option to set the launch date for the mission. Default is past
         * infinity.
         *
         * @param launchDate the launch date for the mission
         * @return the modified builder
         */
        public Builder launchDate(AbsoluteDate launchDate) {
            this.launchDate = launchDate;
            return this;
        }

        /**
         * Option to set the lifetime of the mission in years. Default is set to
         * 3 years;
         *
         * @param lifetime the lifetime of the mission
         * @return the modified builder
         */
        public Builder lifetime(double lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        /**
         * The current mission status
         *
         * @param status The current mission status
         * @return the modified builder
         */
        public Builder status(MissionStatus status) {
            this.status = status;
            return this;
        }

        /**
         * The cost per year to develop the mission
         *
         * @param devCostYr Cost per year to develop the mission
         * @return the modified builder
         */
        public Builder devCostYr(double devCostYr) {
            this.developmentCostYr = devCostYr;
            return this;
        }

        /**
         * The number of years to develop the mission
         *
         * @param devYr Years to develop the mission
         * @return the modified builder
         */
        public Builder devYr(int devYr) {
            this.developmentYears = devYr;
            return this;
        }

        /**
         * Builds an instance of a mission with all the specified parameters.
         *
         * @return
         */
        public Mission build() {
            return new Mission(name, spacecraft, launchDate, lifetime, status, developmentYears, developmentCostYr);
        }
    }

    /**
     * The status of a mission
     */
    public static enum MissionStatus {

        PAST, //past mission
        FLYING, //currently flying
        APPROVED, //approved to fly at a future date
        PLANNED //planned but not approved
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Arrays.hashCode(this.costProfile);
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.lifetime) ^ (Double.doubleToLongBits(this.lifetime) >>> 32));
        hash = 29 * hash + Objects.hashCode(this.name);
        hash = 29 * hash + Objects.hashCode(this.spacecraft);
        hash = 29 * hash + Objects.hashCode(this.launchDate);
        hash = 29 * hash + Objects.hashCode(this.eolDate);
        hash = 29 * hash + Objects.hashCode(this.status);
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
        final Mission other = (Mission) obj;
        if (!Arrays.equals(this.costProfile, other.costProfile)) {
            return false;
        }
        if (Double.doubleToLongBits(this.lifetime) != Double.doubleToLongBits(other.lifetime)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.spacecraft, other.spacecraft)) {
            return false;
        }
        if (!Objects.equals(this.launchDate, other.launchDate)) {
            return false;
        }
        if (!Objects.equals(this.eolDate, other.eolDate)) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        return true;
    }

}
