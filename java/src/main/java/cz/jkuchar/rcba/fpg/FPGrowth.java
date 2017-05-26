package cz.jkuchar.rcba.fpg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import cz.jkuchar.rcba.rules.Tuple;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */

public class FPGrowth {

    private Map<Tuple, Integer> fr;
    private FPTree<Tuple> tree;
    private Map<Tuple, FPTree<Tuple>> header;
    private int minSupportCount;
    private int maxLength;
    private int size;

    private static Logger logger = Logger.getLogger(FPGrowth.class.getName());

    /*
    build a tree
     */
    public void buildTree(List<List<Tuple>> transactions, int minSupportCount){

        tree = new FPTree<Tuple>(null, null);
        header = new HashMap<>();

        // frequencies of items in db
        fr = computeFrequencies(transactions, minSupportCount);

        // add transaction to the tree
        transactions.forEach(transaction -> {
            // sort by frequency and filter out infrequent items
            List<Tuple> sortedTransaction = sortByFrequencies(transaction, fr);
            insert(tree, sortedTransaction);
        });
    }

    /*
    conditional patterns and convert to transactions
     */
    protected List<List<Tuple>> buildConditionalPatternBase(Tuple p){
        // empty transactions
        List<List<Tuple>> transactions = new ArrayList<>();
        // start in header table
        FPTree tree = header.get(p);
        // until at the end of the list
        while (tree!=null){
            FPTree<Tuple> current = tree.getParent();
            List<Tuple> transaction = new ArrayList<>();
            // traverse to the root
            while(current != null && current.getItem() !=null){
                // insert to the conditional pattern base
                Tuple item = current.getItem().getCopy();
                // also use count -> saves memory and cpu
                // instead of repeat the same transaction multiple times
                item.setCount(tree.getCount());
                transaction.add(item);
                current = current.getParent();
            }
            if(!transaction.isEmpty()) {
                transactions.add(transaction);
            }
            // jump to next pointer with the same item
            tree = tree.getNext();
        }
        return transactions;
    }

    public List<FrequentPattern> run(List<List<Tuple>> transactions, double minSupport, int maxLength){
        // run and compute min support count instead of relative min support
        return run(transactions, new FrequentPattern(1), (int)Math.ceil(minSupport*transactions.size()), maxLength, true);
//        return run(transactions, new FrequentPattern(1), (int)Math.round(minSupport*transactions.size()), maxLength, true);
    }

    public List<FrequentPattern> run(List<List<Tuple>> transactions, int minSupportCount, int maxLength){
        return run(transactions, new FrequentPattern(1), minSupportCount, maxLength, true);
    }

    private List<FrequentPattern> run(List<List<Tuple>> transactions, FrequentPattern pref, int minSupportCount, int maxLength, boolean root){
        this.minSupportCount = minSupportCount;
        this.maxLength = maxLength;
        this.size = transactions.size();
        // patterns for current level of recursion
        List<FrequentPattern> fps = new ArrayList<>();
        // if not empty (=zero level) add to output
        if(!pref.getPattern().isEmpty()) {
            fps.add(pref);
        }
//        TODO: maxLength
        if(pref.getPattern().size()<maxLength) {
            if(root){
                logger.log(Level.INFO, "FPG: start");
            }
            // build tree
            buildTree(transactions, minSupportCount);
            if(root){
                logger.log(Level.INFO, "FPG: tree built ("+fr.size()+")");
            }
            singlePrefixPath();
            int i = 0;
            // iterate from less frequent to more frequent items = bottom up
//            for (Tuple tuple : fr.keySet().stream().sorted((a1, a2) -> fr.get(a2).compareTo(fr.get(a1))).collect(Collectors.toList())) {
            for (Tuple tuple : fr.keySet().stream().sorted((a1, a2) -> fr.get(a1).compareTo(fr.get(a2))).collect(Collectors.toList())) {
                if(root){
                    i++;
                    if(i%10==0) {
                        logger.log(Level.INFO, "FPG: "+i+" tuple - " + tuple + "--" + fr.get(tuple));
                    }
                }
//        for(Tuple tuple:fr.keySet()){
                // extend pattern
                FrequentPattern p = new FrequentPattern(pref.getPattern(), fr.get(tuple));
                p.add(tuple);
//            TODO: maxLength
            if(p.getPattern().size()>maxLength) break;
                // add all of recursive computation
                fps.addAll(new FPGrowth().run(buildConditionalPatternBase(tuple), p, minSupportCount, maxLength, false));
//            if(root){
//                System.out.println(tuple);
//                System.out.println(fps.size());
//            }
            }
        }
        return fps;
    }

    protected void singlePrefixPath(){
        int i = 0;
        FPTree current = tree;
        while(current.getChildren().size()==1){
            i++;
            current = (FPTree)current.getChildren().get(0);
        }
        if(i>0){
            logger.log(Level.INFO, "SinglePrefixPath: "+i);
        }
    }

    protected int estimateSupport(FrequentPattern fp){
        if(fp.getPattern().size()==0){
            return this.size;
        }
        if(fp.getPattern().size()==1){
            return this.fr.get(fp.getPattern().get(0));
        }

        List<Tuple> sorted = fp.getPattern().stream().sorted((a,b) -> fr.get(b).compareTo(fr.get(a))).collect(Collectors.toList());
        return fr.get(sorted.get(sorted.size()-1));

    }


    protected int computeSupport(FrequentPattern fp){
//        http://wimleers.com/article/fp-growth-powered-association-rule-mining-with-support-for-constraints
//        https://github.com/wimleers/master-thesis/blob/implementation-milestone-1/code/Analytics/FPGrowth.cpp#L76

        if(fp.getPattern().size()==0){
            return this.size;
        }
        if(fp.getPattern().size()==1){
            return this.fr.get(fp.getPattern().get(0));
        }

        FPGrowth fpg = null;
        List<Tuple> sorted = fp.getPattern().stream().sorted((a,b) -> fr.get(b).compareTo(fr.get(a))).collect(Collectors.toList());
        for(int i=sorted.size()-1;i>0;i--){
            Tuple tuple = sorted.get(i);
//            System.out.println(tuple + "::::" +fr.get(tuple));
            if(fpg==null){
                fpg = new FPGrowth();
                fpg.buildTree(buildConditionalPatternBase(tuple),minSupportCount);
//                fpg.run(buildConditionalPatternBase(tuple), new FrequentPattern(1), minSupportCount, maxLength, false);
            } else {
                List<List<Tuple>> cpb = fpg.buildConditionalPatternBase(tuple);
                if(cpb.size()==0){
                    break;
                }
                fpg.buildTree(cpb,minSupportCount);
//                fpg.run(fpg.buildConditionalPatternBase(tuple), new FrequentPattern(1), minSupportCount, maxLength, false);
            }
//            System.out.println(fpg.fr);
        }
//        System.out.println(fpg.fr);
        if(fpg.fr.containsKey(sorted.get(0))){
            return fpg.fr.get(sorted.get(0));
        }
        return fpg.fr.entrySet().iterator().next().getValue();
    }

    /*
    insert
     */
    protected void insert(FPTree<Tuple> tree, List<Tuple> sortedTransaction){
        // recursive insert item by item
        if(sortedTransaction.isEmpty()) return;
        boolean completed = false;
        // get most frequent one item
        Tuple item = sortedTransaction.get(0);
        for(FPTree child: tree.getChildren()){
            // if it is in the children list
            if(item.equals(child.getItem())) {
                // increase support -> use count if not equal to one (=compression of identical transactions)
                child.incCount(item.getCount());
                completed = true;
                // recursively call the insert with the rest of transaction -> item removed from the list
                insert(child, sortedTransaction.subList(1,sortedTransaction.size()));
                break;
            }
        }
        // if not in children list
        if(!completed){
            // add as new node
            FPTree<Tuple> newChild = new FPTree<Tuple>(item.getCopy(),tree);
            // increase support -> use count if not equal to one (=compression of identical transactions)
            newChild.incCount(item.getCount());
            tree.addChild(newChild);
            // update next list in header
            if(header.containsKey(item)){
                newChild.setNext(header.get(item));
                header.replace(item, newChild);
            } else {
                header.put(item, newChild);
            }
            // recursively call the insert with the rest of transaction -> item removed from the list
            insert(newChild, sortedTransaction.subList(1,sortedTransaction.size()));
        }

    }


    protected Map<Tuple, Integer> computeFrequencies(List<List<Tuple>> transactions, int minSupportCount){
        // compute frequencies of items
        Map<Tuple, Integer> fr = transactions.parallelStream().flatMap(list -> list.stream()).collect(Collectors.toMap(w -> w, w -> w.getCount(), Integer::sum));
        // filter out infrequent
        fr = fr.entrySet().stream().filter(es -> es.getValue()>= minSupportCount).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        return fr;
    }

    protected List<Tuple> sortByFrequencies(List<Tuple> item, Map<Tuple, Integer> frequencies){
        // filter out infrequent
        List<Tuple> sorted = item.stream().filter(t -> frequencies.containsKey(t)).collect(Collectors.toList());
        // sort decreasingly its items
        sorted.sort((a1,a2) -> {
            int i = frequencies.get(a2).compareTo(frequencies.get(a1));
            if(i!=0) return i;
            return a1.toString().compareTo(a2.toString());
        });
        return sorted;
    }

    public FPTree getTree(){
        return tree;
    }

    public int getSize() {return size; }

}
