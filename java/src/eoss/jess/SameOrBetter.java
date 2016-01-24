package eoss.jess;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Dani
 */
import eoss.attributes.GlobalAttributes;
import eoss.attributes.EOAttribute;
import jess.*;
import java.io.Serializable;

public class SameOrBetter implements Userfunction, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "SameOrBetter";
    }

    @Override
    public Value call(ValueVector vv, Context c) throws JessException {
        Value v1 = vv.get(1).resolveValue(c);
        Value v2 = vv.get(2).resolveValue(c);
        Value v3 = vv.get(3).resolveValue(c);

        String attribute = v1.toString();
        String value = v2.toString();
        String target = v3.toString();
        //System.out.println(attribute);
        //System.out.println(value);
        //System.out.println(target);
        if (value.equalsIgnoreCase("nil")) {
            return new Value(-1, RU.INTEGER);
        }
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            System.out.println(value);
            return new Value(-1, RU.INTEGER);
        }

        if (target.equalsIgnoreCase("nil")) {
            return new Value(1, RU.INTEGER);
        }
        if (target.matches("-?\\d+(\\.\\d+)?")) {
            System.out.println(target);
            return new Value(-1, RU.INTEGER);
        }
        EOAttribute tmp = (EOAttribute) GlobalAttributes.measurementAttributeSet.get(attribute);

        EOAttribute newAttrib = (EOAttribute) tmp.cloneAttribute(tmp);
        newAttrib.setValue(value);

        EOAttribute targeAttrib = (EOAttribute) tmp.cloneAttribute(tmp);
        targeAttrib.setValue(target);

        int result = newAttrib.sameOrBetter(targeAttrib);
        //System.out.println(result);
        Value rv = new Value(result, RU.INTEGER);
        return rv;
    }
}
