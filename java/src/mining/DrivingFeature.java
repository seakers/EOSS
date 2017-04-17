package mining;

import java.io.Serializable;
import org.jblas.DoubleMatrix;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Bang
 */
public class DrivingFeature extends AbstractFeature implements Comparable<DrivingFeature>, Serializable {

    private static final long serialVersionUID = -4894252014929283438L;

    private int id;
    private final String name;
    private final String expression;
    private DoubleMatrix datArray;

    public DrivingFeature(int id, String name, String expression, double support, double lift, double fconfidence, double rconfidence) {
        super(support, lift, fconfidence, rconfidence);
        this.id = id;
        this.name = name;
        this.expression = expression;
    }

    public int getID() {
        return id;
    }

    public String getExpression() {
        return expression;
    }

    public String getName() {
        return name;
    }

    public void setDatArray(DoubleMatrix m) {
        datArray = m;
    }

    public DoubleMatrix getDatArray() {
        return datArray;
    }

    @Override
    public int compareTo(DrivingFeature other) {
        if (this.getName().compareTo(other.getName()) == 0) {
            return 0;
        } else {
            return 1;
        }
    }
}
