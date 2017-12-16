package cz.jkuchar.rcba.fpg;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class FPTree<T> {

    // item
    private T item = null;
    // set of children
    private List<FPTree<T>> children = null;
    // pointer to parent for building conditional pattern bases
    private FPTree parent = null;
    // next node with same item
    private FPTree next = null;
    // support count
    private int count = 0;

    /*
    construct root
     */
    public FPTree(T item) {
        this.item = item;
        this.children = new ArrayList<>();
    }

    /*
    construct node and connect to parent
     */
    public FPTree(T item, FPTree parent){
        this(item);
        this.parent = parent;
    }


    public T getItem(){
        return this.item;
    }

    public void setItem(T item){
        this.item = item;
    }

    public void addChild(FPTree<T> tree){
        children.add(tree);
    }

    public List<FPTree<T>> getChildren(){
        return children;
    }

    public void incCount(){
        incCount(1);
    }
    public void incCount(int val){
        count += val;
    }

    public int getCount(){
        return count;
    }

    public void setNext(FPTree<T> tree) {
        next = tree;
    }

    public FPTree<T> getNext(){
        return next;
    }

    public FPTree<T> getParent(){
        return parent;
    }

    @Override
    public String toString() {
        return "FPTree{" +
                "item=" + item +
                ", count=" + count +
                ", children={\n" + children +
                ", next={\n" + next +
                "\n}";
    }

}
