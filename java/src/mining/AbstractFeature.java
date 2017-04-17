/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

/**
 * An abstract class for a feature that explains the data
 *
 * @author nozomihitomi
 */
public abstract class AbstractFeature implements Feature {

    private final double support;
    private final double lift;
    private final double fconfidence;
    private final double rconfidence;

    public AbstractFeature(double support, double lift, double fconfidence, double rconfidence) {
        this.support = support;
        this.lift = lift;
        this.fconfidence = fconfidence;
        this.rconfidence = rconfidence;
    }

    @Override
    public double getSupport() {
        return support;
    }

    @Override
    public double getFConfidence() {
        return fconfidence;
    }

    @Override
    public double getRConfidence() {
        return rconfidence;
    }

    @Override
    public double getLift() {
        return lift;
    }

}
