package cz.jkuchar.rcba.fpg;

import cz.jkuchar.rcba.rules.Tuple;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class FrequentPattern {

    private List<Tuple> pattern;
    private int minSupportCount;


    public FrequentPattern(int minSupportCount){
        this.pattern = new ArrayList<>();
        this.minSupportCount = minSupportCount;
    }

    public FrequentPattern(List<Tuple> pattern, int minSupportCount){
        this.pattern = new ArrayList<>(pattern);
        this.minSupportCount = minSupportCount;
    }

    public void add(Tuple tuple){
        this.pattern.add(tuple);
    }


    public List<Tuple> getPattern(){
        return pattern;
    }

    public int getMinSupportCount(){
        return minSupportCount;
    }

    @Override
    public String toString() {
        return "FrequentPattern{" +
                "pattern=" + pattern +
                ", minSupportCount=" + minSupportCount +
                "}\n";
    }
}
