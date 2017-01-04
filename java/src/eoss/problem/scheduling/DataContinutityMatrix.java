/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import eoss.problem.Instrument;
import eoss.problem.Measurement;
import eoss.problem.Mission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;

/**
 * A matrix structure containing the data continuity for each measurement over
 * time
 *
 * @author nozomihitomi
 */
public class DataContinutityMatrix {

    /**
     * The time step in years to take evaluate each continuity metric
     */
    private final double timestep;

    /**
     * The start date of the analysis
     */
    private final AbsoluteDate startDate;

    /**
     * The end date of the analysis
     */
    private final AbsoluteDate endDate;

    /**
     * A matrix that stores the data continuity information. Each stored date
     * marks the beginning of an interval
     */
    private final HashMap<Measurement, HashMap<AbsoluteDate, HashSet<MissionInstrumentPair>>> matrix;

    /**
     * the allowable dates that are spaced evenly using the start date and the
     * timestep
     */
    private final HashSet<AbsoluteDate> allowableDates;

    /**
     *
     * @param timestep The time step [years] to take evaluate each continuity
     * metric
     * @param startDate The start date of the analysis
     * @param endDate The end date of the analysis
     */
    public DataContinutityMatrix(double timestep, AbsoluteDate startDate, AbsoluteDate endDate) {
        this.timestep = timestep;
        this.startDate = startDate;
        this.endDate = endDate;
        this.matrix = new HashMap<>();
        this.allowableDates = new HashSet<>();

        int i = 0;
        while (endDate.durationFrom(startDate.shiftedBy(timestep * i)) / (3600. * 24. * 365.) > timestep) {
            allowableDates.add(startDate.shiftedBy(i * timestep * 3600. * 24. * 365.));
            i++;
        }
    }

    /**
     * Adds an element to the data continuity matrix.
     *
     * @param measurement the measurement to continue
     * @param time the time of interest
     * @param mission the mission name
     * @param instrument the instrument aboard the given mission that will take
     * the measurement
     * @return true if the added data continuity element changed the matrix
     */
    public boolean addDataContinutity(Measurement measurement, AbsoluteDate time, Mission mission, Instrument instrument) {
        if (!allowableDates.contains(time)) {
            throw new IllegalArgumentException("Given date is not allowed. Must be between start and end date and fall within a time step");
        }

        if (!matrix.containsKey(measurement)) {
            HashMap<AbsoluteDate, HashSet<MissionInstrumentPair>> measMatrix = new HashMap<>();
            for (AbsoluteDate date : allowableDates) {
                measMatrix.put(date, new HashSet<>());
            }
        }

        return matrix.get(measurement).get(time).add(new MissionInstrumentPair(instrument, mission));
    }

    /**
     * The number of instruments available at each time step in this data
     * continuity matrix for each measurement. Rows are measurements and columns
     * are timesteps
     *
     * @return number of instruments available at each time step in this data
     * continuity matrix for each measurement
     */
    public RealMatrix count() {
        RealMatrix out = new Array2DRowRealMatrix(matrix.keySet().size(), allowableDates.size());
        int i = 0;
        int j = 0;
        for (Measurement meas : matrix.keySet()) {
            for (AbsoluteDate date : allowableDates) {
                out.addToEntry(i, j, matrix.get(meas).get(date).size());
                j++;
            }
            i++;
        }
        return out;
    }

    /**
     * This function takes a continuity matrix that is calculated with a
     * launchdate = startdate and an arbitrary lifetime, and creates a new
     * continuity matrix matrix1 that is identical to matrix0 except that it
     * starts in launchdate and has a lifetime of lifetime years. In order to do
     * so, it takes
     *
     * @param lifetime the lifetime in years
     * @param launchdate the launch date
     * @return a matrix offset from a copy of this matrix
     */
    public DataContinutityMatrix offset(double lifetime, AbsoluteDate launchdate) {
        double life = FastMath.floor(lifetime / timestep) * 365. * 24. * 3600.;
        if (!allowableDates.contains(launchdate)) {
            throw new IllegalArgumentException("Given date is not allowed. Must be between start and end date and fall within a time step");
        }
        double offset = launchdate.durationFrom(startDate);

        DataContinutityMatrix out = new DataContinutityMatrix(timestep, startDate, endDate);

        for (Measurement meas : matrix.keySet()) {
            for (AbsoluteDate date : allowableDates) {
                if (date.shiftedBy(offset + life).compareTo(endDate) < 1) {
                    for (MissionInstrumentPair mi : matrix.get(meas).get(date)) {
                        out.addDataContinutity(meas, date.shiftedBy(offset + life),
                                mi.getMission(), mi.getInstrument());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Merge this data continuity matrix with another
     *
     * @param other data continuity matrix
     * @return a new instance of a data continuity matrix that is merged
     */
    public DataContinutityMatrix merge(DataContinutityMatrix other) {
        if (!this.allowableDates.equals(other.getAllowableDates())) {
            throw new IllegalArgumentException("Data continuity matrices must "
                    + "have same start and end dates and the same time step");
        }

        DataContinutityMatrix out = new DataContinutityMatrix(timestep, startDate, endDate);

        HashSet<Measurement> measurements = new HashSet<>();
        measurements.addAll(this.matrix.keySet());
        measurements.addAll(other.getMeasurements());

        for (Measurement meas : measurements) {
            for (AbsoluteDate date : allowableDates) {
                if (this.matrix.containsKey(meas) && this.matrix.get(meas).containsKey(date)) {
                    for (MissionInstrumentPair mi : this.matrix.get(meas).get(date)) {
                        out.addDataContinutity(meas, date, mi.getMission(), mi.getInstrument());
                    }
                }
                if (other.matrix.containsKey(meas) && other.matrix.get(meas).containsKey(date)) {
                    for (MissionInstrumentPair mi : other.matrix.get(meas).get(date)) {
                        out.addDataContinutity(meas, date, mi.getMission(), mi.getInstrument());
                    }
                }
            }
        }
        return out;
    }

    /**
     * Gets the measurements that are stored in the matrix
     *
     * @return the measurements that are stored in the matrix
     */
    public Set<Measurement> getMeasurements() {
        return matrix.keySet();
    }

    /**
     * Gets the dates that are allowed to be declared
     *
     * @return the dates that are allowed to be declared
     */
    public Set<AbsoluteDate> getAllowableDates() {
        return allowableDates;
    }

    /**
     * The time step to take evaluate each continuity metric
     *
     * @return time step to take evaluate each continuity metric
     */
    public double getTimestep() {
        return timestep;
    }

    /**
     * Gets the start date of this analysis
     *
     * @return the start date of this analysis
     */
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    /**
     * Gets the end date of this analysis
     *
     * @return the end date of this analysis
     */
    public AbsoluteDate getEndDate() {
        return endDate;
    }

    /**
     * Returns a copy of this matrix. The copy is created through a deep-clone.
     * Instruments and missions are not cloned.
     *
     * @return a copy of this matrix
     */
    public DataContinutityMatrix copy() {
        DataContinutityMatrix copy = new DataContinutityMatrix(timestep, startDate, endDate);
        for (Measurement m : matrix.keySet()) {
            for (AbsoluteDate d : allowableDates) {
                for (MissionInstrumentPair p : matrix.get(m).get(d)) {
                    copy.addDataContinutity(m, d, p.getMission(), p.getInstrument());
                }
            }
        }
        return copy;
    }

    private class MissionInstrumentPair {

        private final Instrument inst;

        private final Mission mission;

        public MissionInstrumentPair(Instrument inst, Mission mission) {
            this.inst = inst;
            this.mission = mission;
        }

        public Instrument getInstrument() {
            return inst;
        }

        public Mission getMission() {
            return mission;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + Objects.hashCode(this.inst);
            hash = 71 * hash + Objects.hashCode(this.mission);
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
            final MissionInstrumentPair other = (MissionInstrumentPair) obj;
            if (!Objects.equals(this.inst, other.inst)) {
                return false;
            }
            if (!Objects.equals(this.mission, other.mission)) {
                return false;
            }
            return true;
        }

    }
}
