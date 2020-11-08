package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.impl.BTreeImpl;

import java.net.URI;

public class SortByWordInstances<T> implements java.util.Comparator<URI>{
    public String word;
    private BTreeImpl<URI, DocumentImpl> btree;

    public SortByWordInstances (String word, BTreeImpl btree){
        this.word = word;
        this.btree = btree;
    }

    @Override
    public int compare(URI o1, URI o2) {
        if (o1 == null || o2 == null){
            return 1000;
        }
        DocumentImpl doc1 = this.btree.get(o1);
        DocumentImpl doc2 = this.btree.get(o2);
        if (doc1 == null || doc2 == null){
            return 1000;
        }
        int word1 = doc1.wordCount(this.word);
        int word2 = doc2.wordCount(this.word);
        if (word1 > word2){
            return -1;
        }
        if (word2 > word1){
            return 1;
        }
        return 0;
    }




}
