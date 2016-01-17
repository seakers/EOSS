/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture.util;

import java.util.ArrayList;

/**
 * This class is a tree that stores information about the weights that determine
 * the value of an architecture. Each node can have multiple children but can
 * only have one parent
 *
 * @author nozomihitomi
 * @param <K> Key
 * @param <T> Value
 */
public class ValueTree<K,T> {

    public class Node<K,T> {
        private ArrayList<Node> children;
        private Node parent;
        private final K key;
        private final T value;

        public Node(K key, T value) {
            this.key = key;
            this.value = value;
        }

        public ArrayList<Node> getChildren() {
            return children;
        }

        public Node getParent() {
            return parent;
        }
        
        /**
         * Check if the node is a leaf node of the tree
         * @return true if the node is a leaf node of the tree
         */
        public boolean isLeaf(){
            return children.isEmpty();
        }
        
        /**
         * Adds a child to the node only if it does not yet have parents
         * @param child
         * @return true as specified by Collection.add
         */
        public boolean addChild(Node child){
            if(child.getParent()==null)
                return children.add(child);
            else 
                return false;
        }
        
        /**
         * Removes a child node 
         * @param child
         * @return true if child is a child of this node
         */
        public boolean rmChild(Node child){
            return children.remove(child);
        }
    }

    private final Node<K,T> root;

    public ValueTree(Node<K,T> rootNode) {
        this.root = rootNode;
    }
    
    public Node<K,T> getRoot() {
        return root;
    }

}
