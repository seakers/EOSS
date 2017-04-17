/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mining;

import org.jblas.DoubleMatrix;

/**
 * A feature that explains data
 *
 * @author nozomihitomi
 */
public interface Feature {

    /**
     * Gets the support of the feature
     *
     * @return
     */
    public double getSupport();

    /**
     * Gets the forward confidence of the feature. Given that a solution has a
     * feature, what is the likelihood of it also being in target region?
     *
     * @return
     */
    public double getFConfidence();

    /**
     * Gets the reverse confidence of the feature. Given that a solution is in
     * the target region, what is the likelihood of it also containing feature?
     *
     * @return
     */
    public double getRConfidence();

    /**
     * Gets the lift of the feature
     *
     * @return
     */
    public double getLift();
    
    public DoubleMatrix getDatArray();
}
