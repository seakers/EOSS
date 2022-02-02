/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eoss.problem;

import architecture.util.ValueTree;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rbsa.eoss.Weight;

/**
 * Creates a value tree that can aggregate scores by taking the weighted sum of
 * child nodes at each level
 *
 * @author nozomihitomi
 */
public class ValueAggregationBuilder {

    public static ValueTree build(File file) throws IOException, SAXException, ParserConfigurationException {
        ValueTree tree = new ValueTree();

        DocumentBuilder dBuilder;
        dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList panelNode = doc.getElementsByTagName("panel");
        //panels
        for (int i = 0; i < panelNode.getLength(); i++) {
            Element panel = (Element) panelNode.item(i);
            String panelId = panel.getElementsByTagName("id").item(0).getTextContent().trim();
            double panelWt = Weight.parseWeight(panel.getElementsByTagName("weight").item(0).getTextContent());
            String panelDes = panel.getElementsByTagName("description").item(0).getTextContent().trim();
            tree.addNode("root", panelId, panelWt, panelDes);

            NodeList objNode = panel.getElementsByTagName("objective");
            for (int j = 0; j < objNode.getLength(); j++) { //cycle through objectives
                Element obj = (Element) objNode.item(j);
                String objId = obj.getElementsByTagName("id").item(0).getTextContent().trim();
                double objWt = Weight.parseWeight(obj.getElementsByTagName("weight").item(0).getTextContent());
                String objDes = obj.getElementsByTagName("description").item(0).getTextContent().trim();
                tree.addNode(panelId, objId, objWt, objDes);

                NodeList subobjNode = obj.getElementsByTagName("subobjective");
                for (int k = 0; k < subobjNode.getLength(); k++) { //cycle through subobjectives
                    Element subobj = (Element) subobjNode.item(k);
                    String sObjId = subobj.getElementsByTagName("id").item(0).getTextContent().trim();
                    double sObjWt = Weight.parseWeight(subobj.getElementsByTagName("weight").item(0).getTextContent());
                    String sObjDes = subobj.getElementsByTagName("description").item(0).getTextContent().trim();
                    tree.addNode(objId, sObjId, sObjWt, sObjDes);
                }
            }
        }
        return tree;
    }

}
