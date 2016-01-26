/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rbsa.eoss;

import java.util.Objects;

/**
 *
 * @author nozomihitomi
 */
public class Interaction {
    private final Nto1pair ntpair;
    private final double value;

    public Interaction(Nto1pair ntpair, double value) {
        this.ntpair = ntpair;
        this.value = value;
    }

    public Nto1pair getNtpair() {
        return ntpair;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
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
        final Interaction other = (Interaction) obj;
        if (!Objects.equals(this.ntpair, other.ntpair)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Interaction{" + "ntpair=" + ntpair + ", value=" + value + '}';
    }
}
