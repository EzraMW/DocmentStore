package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<Undoable> {
    StackList list;
    private int size;

    public StackImpl() {
        this.list = new StackList();
        this.size = 0;
    }

    @Override
    public void push(Undoable element) {
        list.insertValue( element);
        size++;
    }

    @Override
    public Undoable pop() {
        // set Type variable equal to the head/top of the stack to return it. Then, set the new head to be the previous
        // second to head, meaning head.next. Then decrease the int representing the size and return the p.
        Undoable p;
        if (list.getHead() != null) {
            p = list.getHead().getCommand();
            list.head = list.getHead().next;
        }
        else {
            return null;
        }
        size--;
        return p;
    }

    @Override
    public Undoable peek() {
        // if stack is empty, return null
        if (list.getHead() == null){
            return null;
        }
        return list.getHead().getCommand();
    }

    @Override
    public int size() {
        return this.size;
    }

    private class StackList{
        StackList.Member head;

        Member getHead(){return this.head;}

        void insertValue(Undoable c) {
            //Create a node object and set its next to null
            StackList.Member n = new StackList.Member(c);
            // Check if the list is empty and if it is then set this node as the first on the list. If not, then get
            // to the end of the list and insert it there
            if (this.head == null) {
                this.head = n;
                n.next = null;
            } else {
                StackList.Member previoushead = this.head;
                this.head = n;
                n.next = previoushead;
            }
        }

        private class Member{
            Undoable undoable;
            Member next;

            Member(Undoable u){
                this.undoable = u;
                this.next = null;
            }

            Undoable getCommand(){
                return this.undoable;}
        }

    }
}
