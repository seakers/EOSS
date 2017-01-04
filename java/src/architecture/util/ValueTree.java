/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package architecture.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class is a tree that stores information about the weights that determine
 * the value of an architecture. Each node can have multiple children but can
 * only have one parent
 *
 * @author nozomihitomi
 */
public class ValueTree implements Serializable {
    private static final long serialVersionUID = -7838300623785230194L;

    private final Node root;

    /**
     * Map node names to nodes
     */
    private final HashMap<String, Node> nodeMap;

    public ValueTree() {
        this.root = new Node("root", 1.0, "rootNode");
        this.nodeMap = new HashMap<>();
        this.nodeMap.put(this.root.id, this.root);
    }

    private ValueTree(HashMap<String, Node> nodeMap) {
        if (!nodeMap.containsKey("root")) {
            throw new IllegalArgumentException("The nodemap does not have a root node.");
        }
        this.root = nodeMap.get("root");
        this.nodeMap = nodeMap;
    }

    /**
     * Adds a child node to a specified node.
     *
     * @param parentNodeId the parent to attach a new child node to. If parent
     * node = "root" then node is attached to the root of this tree
     * @param id the id of the new child node
     * @param weight the weight of the new child node
     * @param description the description of the new child node
     */
    public void addNode(String parentNodeId, String id, double weight, String description) {
        if (!nodeMap.containsKey(parentNodeId)) {
            throw new IllegalArgumentException(String.format("Parent node %s does not exist", parentNodeId));
        }
        if (nodeMap.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Id name %s already exists.", id));
        }
        Node n = new Node(id, weight, description);
        nodeMap.get(parentNodeId).addChild(n);
        nodeMap.put(id, n);
    }

    /**
     * Gets all the existing node names
     *
     * @return
     */
    public Set<String> getNodeNames() {
        return nodeMap.keySet();
    }

    /**
     * Method to set the score of leaf nodes only. The other intermediate nodes
     * are computed by using the weights of each node
     *
     * @param nodeId the id of a leaf node
     * @param score the score to assign to the leaf node
     */
    public void setScore(String nodeId, double score) {
        Node n = nodeMap.get(nodeId);
        if (n.isLeaf()) {
            n.setScore(score);
        } else {
            throw new IllegalArgumentException(String.format("Node %s is not a leaf node.", nodeId));
        }
    }

    /**
     * Traverses the tree and computes the score at each node in the tree using
     * a weighted sum (using the weights of the child nodes. The overall score
     * at the root node is computed and returned but other levels in the tree
     * are computed and can be accessed by getScore(Node n);
     *
     * @return The overall score at the root node
     */
    public double computeScores() {
        return computeScore(root);
    }

    /**
     * Recursive call to compute scores at each node in the tree. While
     * traversing the tree, checks to make sure that weights add up to 1.0
     *
     * @param n the node to compute the score for
     * @return
     */
    private double computeScore(Node n) {
        double val = 0;
        double weights = 0;
        if (n.isLeaf()) {
            val = n.getScore();
        } else {
            for (Node child : n.getChildren()) {
                val += computeScore(child) * child.getWeight();
                weights += child.getWeight();
            }
            if (Math.abs(weights - 1.0) > 0.00001) {
                throw new IllegalStateException(String.format("Weights for child nodes under %s don't add up to 1.0", n.toString()));
            }
            n.setScore(val);
        }
        return val;
    }

    /**
     * Gets the computed score of the given node. If the scores haven't been
     * computed, the value returned will be Double.NaN
     *
     * @param nodeID the id of the node of interest.
     * @return
     */
    public double getScore(String nodeID) {
        return nodeMap.get(nodeID).getScore();
    }

    /**
     * Traverses the tree and identifies the leaf nodes
     *
     * @return the id's of the leaf nodes of this tree.
     */
    public Collection<String> getLeafNodes() {
        return getLeafNodes(root);
    }

    /**
     * Recursively traverses the tree and identifies the leaf nodes
     *
     * @return the id's of the leaf nodes of this tree.
     */
    private HashSet<String> getLeafNodes(Node n) {
        HashSet<String> out = new HashSet<>();
        if (n.isLeaf()) {
            out.add(n.id);
        } else {
            for (Node child : n.getChildren()) {
                out.addAll(getLeafNodes(child));
            }
        }
        return out;
    }

    /**
     * Copies only the structure of this tree. None of the scores will be copied
     * over.
     *
     * @return a copy of this tree without the scores computed
     */
    public ValueTree copy() {
        HashMap<String, Node> nodeMapCopy = new HashMap<>(nodeMap.size());
        copy(this.root, nodeMapCopy);
        return new ValueTree(nodeMapCopy);
    }

    /**
     * Recursively traverses tree to copy each child node
     *
     * @param original
     * @param nodeMap inserts copied nodes into given nodemap
     * @return
     */
    private Node copy(Node original, HashMap<String, Node> nodeMap) {
        Node copiedNode = new Node(original.getId(), original.getWeight(), original.getDescription());
        if (!original.isLeaf()) {
            for (Node child : original.getChildren()) {
                copiedNode.addChild(copy(child, nodeMap));
            }
        }
        nodeMap.put(copiedNode.id, copiedNode);
        return copiedNode;
    }

    private class Node {

        /**
         * The children assigned to this node
         */
        private final ArrayList<Node> children;

        /**
         * The parent node
         */
        private Node parent;

        /**
         * The weight of the node
         */
        private final double weight;

        /**
         * The description of the node
         */
        private final String description;

        /**
         * the id of the node
         */
        private final String id;

        /**
         * The score given to this node
         */
        private double score = Double.NaN;

        public Node(String id, double weight, String description) {
            this.weight = weight;
            this.description = description;
            this.id = id;
            this.children = new ArrayList<>();
        }

        public double getWeight() {
            return weight;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public ArrayList<Node> getChildren() {
            return children;
        }

        public Node getParent() {
            return parent;
        }

        public double getScore() {
            return this.score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        /**
         * Check if the node is a leaf node of the tree
         *
         * @return true if the node is a leaf node of the tree
         */
        public boolean isLeaf() {
            return children.isEmpty();
        }

        /**
         * Adds a child to the node only if it does not yet have parents
         *
         * @param child
         * @return true as specified by Collection.add
         */
        public boolean addChild(Node child) {
            if (child.getParent() == null) {
                return children.add(child);
            } else {
                return false;
            }
        }

        /**
         * Removes a child node
         *
         * @param child
         * @return true if child is a child of this node
         */
        public boolean rmChild(Node child) {
            return children.remove(child);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.parent);
            hash = 53 * hash + (int) (Double.doubleToLongBits(this.weight) ^ (Double.doubleToLongBits(this.weight) >>> 32));
            hash = 53 * hash + Objects.hashCode(this.description);
            hash = 53 * hash + Objects.hashCode(this.id);
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
            final Node other = (Node) obj;
            if (!Objects.equals(this.parent, other.parent)) {
                return false;
            }
            if (Double.doubleToLongBits(this.weight) != Double.doubleToLongBits(other.weight)) {
                return false;
            }
            if (!Objects.equals(this.description, other.description)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Node{" + "weight=" + weight + ", description=" + description + ", id=" + id + '}';
        }
    }

}
