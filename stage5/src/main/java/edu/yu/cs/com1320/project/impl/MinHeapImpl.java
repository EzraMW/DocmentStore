package edu.yu.cs.com1320.project.impl;

import com.google.gson.internal.bind.util.ISO8601Utils;
import edu.yu.cs.com1320.project.MinHeap;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable> extends MinHeap<E> {
    protected E[] elements;


    protected int count;
    protected Map<E,Integer> elementsToArrayIndex; //used to store the index in the elements array
    protected HashMap<URI,Long> map;

    public MinHeapImpl(){
        this.count = 0;
        this.elements = (E[]) new Comparable[5];
        this.elementsToArrayIndex = new HashMap<>();
        this.map = CompareNode.getMap();
    }

    protected void updateMap(){
        this.map = CompareNode.getMap();
    }

    @Override
    protected boolean isEmpty() {
        return this.count == 0;
    }

    /**
     * is elements[i] > elements[j]?
     *
     * @param i
     * @param j
     */
    @Override
    protected boolean isGreater(int i, int j) {
        if (elements[i] == null || elements[j] == null){
            return false;
        }
        Long a = (map.get(this.elements[i]));
        Long b = map.get(this.elements[j]);
        return (Long.compare(a, b)>0);
    }

    /**
     * while the key at index k is less than its
     * parent's key, swap its contents with its parent’s
     *
     * @param k
     */
    @Override
    protected void upHeap(int k) {
        while (k > 1 && this.isGreater(k /2, k))
        {
            this.swap(k, k / 2);
            k = k / 2;
        }
    }

    /**
     * move an element down the heap until it is less than
     * both its children or is at the bottom of the heap
     *
     * @param k
     */
    @Override
    protected void downHeap(int k) {
        while (2 * k <= this.count)
        {
            //identify which of the 2 children are smaller
            int j = k*2;
            if (j < this.count && this.isGreater(j, j + 1))
            {
                j++;
            }
            //if the current value is < the smaller child, we're done
            if (!this.isGreater(k, j))
            {
                break;
            }
            //if not, swap and continue testing
            this.swap(k, j);
            k = j;
        }
    }

    @Override
    public E removeMin() {
        updateMap();
        if (isEmpty())
        {
            throw new NoSuchElementException("Heap is empty");
        }
        E min = this.elements[1];
        //swap root with last, decrement count
        this.swap(1, this.count--);
        //move new root down as needed
        this.downHeap(1);
        this.elements[this.count + 1] = null; //null it to prepare for GC
        return min;
    }

    /**
     * swap the values stored at elements[i] and elements[j]
     *
     * @param i
     * @param j
     */
    @Override
    protected void swap(int i, int j) {
        E temp = this.elements[i];
        this.elements[i] = this.elements[j];
        this.elements[j] = temp;
        this.elementsToArrayIndex.put(elements[j],j);
        this.elementsToArrayIndex.put(elements[i],i);
    }

    @Override
    public void insert(E x) {
        updateMap();
        // double size of array if necessary
        if (this.count >= this.elements.length - 1)
        {
            this.doubleArraySize();
        }
        //add x to the bottom of the heap
        this.elements[++this.count] = x;
        this.elementsToArrayIndex.put(x, this.count);
        //percolate it up to maintain heap order property
        this.upHeap(this.count);
    }

    @Override
    public void reHeapify(E element) {
        updateMap();
        int arrayindex = getArrayIndex(element);
        E parent = elements[arrayindex/2];
        if (parent != null){
            Long a = map.get(element);
            Long b = map.get(parent);
            if (Long.compare(a, b) < 0){
                upHeap(arrayindex);
            }
            else if (Long.compare(a, b) > 0){
                downHeap(arrayindex);
            }
        }
    }

    @Override
    protected int getArrayIndex(E element) {
        if (elementsToArrayIndex.get(element) != null){
            return elementsToArrayIndex.get(element);
        }
        throw new NoSuchElementException();
    }

    @Override
    protected void doubleArraySize() {
        int originalarraysize = this.elements.length;
        E[] doublearray = (E[]) new Comparable[originalarraysize * 2];
        System.arraycopy(this.elements, 0, doublearray, 0, originalarraysize);
        this.elements = doublearray;
    }
}
