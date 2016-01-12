/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.jess;

/**
 *
 * @author Marc
 */
import jess.*;

public class Resource {
    
    private Rete r;
    private QueryBuilder qb;
    
    public Resource()
    {
        r = new Rete();
        qb = new QueryBuilder( r );
        JessInitializer.getInstance().initializeJess( r, qb);
    }
    
    public Rete getRete()
    {
        return r;
    }
    
    public QueryBuilder getQueryBuilder()
    {
        return qb;
    }
}
