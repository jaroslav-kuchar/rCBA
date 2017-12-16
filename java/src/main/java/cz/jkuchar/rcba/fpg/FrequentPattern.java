package cz.jkuchar.rcba.fpg;

import cz.jkuchar.rcba.rules.Tuple;

import java.util.ArrayList;
import java.util.Collections;
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
        Collections.sort(pattern);
        this.minSupportCount = minSupportCount;
    }

    public void add(Tuple tuple){
        this.pattern.add(tuple);
        Collections.sort(pattern);
    }

    public FrequentPattern getCopy(){
        List<Tuple> tmp = new ArrayList<>();
        for(Tuple p:pattern){
            tmp.add(p.getCopy());
        }
        return new FrequentPattern(tmp,minSupportCount);
    }


    public List<Tuple> getPattern(){
        return pattern;
    }

    public int getMinSupportCount(){
        return minSupportCount;
    }

    public void setMinSupportCount(int minSupportCount){
        this.minSupportCount = minSupportCount;
    }

    @Override
    public String toString() {
        return "FrequentPattern{" +
                "pattern=" + pattern +
                ", minSupportCount=" + minSupportCount +
                "}\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FrequentPattern that = (FrequentPattern) o;

        if (minSupportCount != that.minSupportCount) return false;
        return pattern != null ? pattern.equals(that.pattern) : that.pattern == null;
    }

    @Override
    public int hashCode() {
        int result = pattern != null ? pattern.hashCode() : 0;
        result = 31 * result + minSupportCount;
        return result;
    }
}
