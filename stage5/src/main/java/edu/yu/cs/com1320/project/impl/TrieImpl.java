package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.stage5.impl.SortByWordInstances;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {

    private static final int alphabetSize = 256; // extended ASCII
    private Node<Value> root; // root of trie
    private SortByWordInstances<Value> sorter;

    public static class Node<Value>
    {
        protected ArrayList<Value> val = new ArrayList<>();
        protected Node<Value>[] nodearray = new Node[TrieImpl.alphabetSize];

        protected ArrayList<Value> getValue(){
            return this.val;
        }
        protected Node<Value>[] getNodeArray(){
            return this.nodearray;
        }

    }

    public TrieImpl(){
//        this.root = new Node<Value>();
    }

    private Node<Value> get(Node<Value> x, String key, int d)
    {
        key = key.toLowerCase();
        //link was null - return null, indicating a miss
        if (x == null)
        {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length())
        {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(x.nodearray[c], key, d + 1);
    }

    private Node<Value> put(Node<Value> x, String key, Value val, int d)
    {
        key = key.toLowerCase();
        key = key.trim();
        //create a new node
        if (x == null)
        {
            x = new Node<>();
        }
        //we've reached the last node in the key,
        //set the value for the key and return the node
        if (d == key.length())
        {
            if (!x.val.contains(val)){
                x.val.add(val);
            }
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char r = key.charAt(d);
        x.nodearray[r] = this.put(x.nodearray[r], key, val, d + 1);
        return x;
    }

    private Node<Value> deleteAll(Node<Value> x, String key, int d)
    {
        key = key.toLowerCase();
        if (x == null)
        {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length())
        {
            x.val = null;
        }
        //continue down the trie to the target node
        else
        {
            char c = key.charAt(d);
            x.nodearray[c] = this.deleteAll(x.nodearray[c], key, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.val != null)
        {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <TrieImpl.alphabetSize; c++)
        {
            if (x.nodearray[c] != null)
            {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    private ArrayList<Node<Value>> getNodes(Value val){
        if (val == null){
            return null;
        }
        ArrayList<Node<Value>> nodes = new ArrayList<>();
        nodes = allNodesWithValue(nodes, root, val);
        return nodes;
    }

    private ArrayList<Node<Value>> allNodesWithValue(ArrayList<Node<Value>> list, Node<Value> x, Value value){
        if (x == null){
            return null;
        }
        if(x.getValue() == value){
            list.add(x);
        }
        if (x.getNodeArray() != null){
            for (Node<Value> n: x.getNodeArray()){
                allNodesWithValue(list, n, value);
            }
        }
        return list;
    }


    @Override
    public void put(String key, Value val) {
        key = key.toLowerCase();
        //deleteAll the value from this key
        if (val != null)
        {
            if (this.root == null){
                this.root = new Node<>();
            }
            put(root, key, val, 0);
        }
    }

    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        key = key.toLowerCase();
        Node<Value> x = this.get(root, key, 0);
        if (x == null || x.val == null) {
            return new ArrayList<>();
        }
        // then use the comparator to sort the list
        x.getValue().sort(comparator);
        return x.getValue();
    }

    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        prefix = prefix.toLowerCase();
        Node<Value> node = this.get(root, prefix, 0);
        ArrayList<Value> list = new ArrayList<>();
        if (node == null){
            return list;
        }
        List<Value> fulllist = getAllValuesUnderThisNode(node, list);
        // then use the comparator to sort this list
        fulllist.sort(comparator);
        return fulllist;
    }

    private ArrayList<Value> getAllValuesUnderThisNode(Node<Value> x, ArrayList<Value> list){
        if (x == null){
            return null;
        }
        if (x.getValue() != null){
            for (Object v: x.getValue()){
                if (!list.contains(v)){
                    list.add((Value) v);
                }
            }
        }
        if (x.getNodeArray() != null){
            for (Node<Value> i: x.getNodeArray()){
                if (i != null){
                    getAllValuesUnderThisNode(i, list);
                }
            }
        }
        return list;
    }

    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        prefix = prefix.toLowerCase();
        //make a list for nodes and get the node at the beggining of the prefix subtree. Then, add each node in the
        // subtree to the list and eventually call deleteall on each node to delete it from the subtree.
        // Make another list for the all the values that exist in this subtree and add on each value
        ArrayList<Node<Value>> nodeslist = new ArrayList<>();
        Node<Value> x = get(root, prefix, 0);
        nodeslist = getAllNodesWithPrefix(nodeslist, x);
        ArrayList<Value> valuelist = new ArrayList<>();
        valuelist = getAllValuesUnderThisNode(x, valuelist);
        if (nodeslist == null){
            return new HashSet<>();
        }
        for (Node<Value> node: nodeslist){
            Node<Value> n = this.deleteAll(node, prefix, 0);
            if (n != null){
                for (Object v: n.val){
                    ArrayList<Node<Value>> keyvaluenodes = getNodes((Value) v);
                    for (Node<Value> nodes: keyvaluenodes){
                        if (nodes.val != null){
                            nodes.val.remove(v);
                        }
                    }
                }
            }
        }
        return new HashSet<>(valuelist);
    }

    private ArrayList<Node<Value>> getAllNodesWithPrefix(ArrayList<Node<Value>> list, Node<Value> x){
        if (x == null){
            return null;
        }
        list.add(x);
        if (x.getNodeArray() != null){
            for(Node<Value> n: x.getNodeArray()){
                if (n != null){
                    getAllNodesWithPrefix(list, n);
                }
            }
        }
        return list;
    }

    @Override
    public Set<Value> deleteAll(String key) {
        Node<Value> x = get(root, key, 0);
        if (x == null){
            return null;
        }
        if (x.val == null){
            return new HashSet<>();
        }
        HashSet<Value> set = new HashSet<>(x.val);
        for (Object v: x.val){
            ArrayList<Node<Value>> keyvaluenodes = getNodes((Value) v);
            for (Node<Value> n: keyvaluenodes){
                if (n.val != null){
                    n.val.remove(v);
                }
            }
        }
        this.deleteAll(root, key, 0);
        return set;
    }

    @Override
    public Value delete(String key, Value val) {
        Node<Value> n = this.get(root, key, 0);
        if (n == null || n.val == null){
            return null;
        }
        for (Value v: n.val){
            if (v == val){
                n.val.remove(v);
                return val;
            }
        }
        return null;
    }
}
