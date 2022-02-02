/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem.scheduling;

import java.util.Date;
import java.util.Objects;
import org.orekit.time.AbsoluteDate;

/**
 * A collection containing the budget for a series of years
 *
 * @author nozomihitomi
 */
public class Budget implements Comparable<Budget> {

    /**
     * The date of the budget
     */
    private final AbsoluteDate date;

    /**
     * The allowable budget for the given date
     */
    private final double budget;

    public Budget(AbsoluteDate date, double budget) {
        this.date = date;
        this.budget = budget;
    }

    /**
     * Gets the date
     * @return  the date
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Gets the budget
     * @return the budget
     */
    public double getBudget() {
        return budget;
    }

    /**
     * in this implementation of compareTo, the date is checked first and then
     * the budget value. If this is earlier than other than returns -1. If this
     * has the same date as other then the budget value is compared to using a
     * double compareTo.
     *
     * @param o
     * @return -1 if this is earlier than other. 1 if other is earlier than this.
     */
    @Override
    public int compareTo(Budget o) {
        int dateComp = this.date.compareTo(o.getDate());
        if(dateComp !=0){
            return dateComp;
        }else{
            return Double.compare(this.budget, o.getBudget());
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.date);
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.budget) ^ (Double.doubleToLongBits(this.budget) >>> 32));
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
        final Budget other = (Budget) obj;
        if (!Objects.equals(this.date, other.date)) {
            return false;
        }
        if (Double.doubleToLongBits(this.budget) != Double.doubleToLongBits(other.budget)) {
            return false;
        }
        return true;
    }



}
