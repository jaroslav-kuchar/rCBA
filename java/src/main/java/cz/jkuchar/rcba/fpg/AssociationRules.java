package cz.jkuchar.rcba.fpg;

import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class AssociationRules {

    public static List<Rule> generate(List<FrequentPattern> fp, FPGrowth fpg, int size, double minConfidence, String consequent) {
        return AssociationRules.generate(fp, fpg, size, minConfidence, consequent, true);
    }

    public static List<Rule> generate(List<FrequentPattern> fp, FPGrowth fpg, int size, double minConfidence, String consequent, boolean parallel) {
        Map<List<Tuple>, Integer> frequenciesMap = (parallel?fp.parallelStream():fp.stream())
                .map(a -> {
                    // sort by content
                    Collections.sort(a.getPattern());
                    // move consequent to the end
                    a.getPattern().sort((s1, s2) -> {
                        if (s1.getLeft().equals(consequent))
                            return 1;
                        return -1;
                    });
                    return(a);
                })
                .collect(Collectors.toMap(p -> p.getPattern(), p -> p.getMinSupportCount()));
        return (parallel?fp.parallelStream():fp.stream())
//                .filter(f -> f.getPattern().stream().anyMatch(tuple -> tuple.getLeft().equals(consequent)))
                .filter(f -> f.getPattern().get(f.getPattern().size()-1).getLeft().equals(consequent))
                .map(f -> {
                    List<Tuple> ant = null;
                    List<Tuple> cons = null;
                    double support = f.getMinSupportCount() / (double) size;
                    double confidence = -1;
                    double lift = -1;
//                    for (Tuple item : f.getPattern()) {
//                        if (item.getLeft().equals(consequent)) {
                            Tuple item = f.getPattern().get(f.getPattern().size()-1);
                            List<Tuple> tmp = new ArrayList<Tuple>(f.getPattern());
                            tmp.remove(item);
                            ant = new ArrayList<Tuple>(tmp);
                            cons = new ArrayList<Tuple>() {{
                                add(item);
                            }};
                            int antSupportCount = -1;

                            if(!frequenciesMap.containsKey(ant)){
//                                System.out.println(ant);
//                                frequenciesMap.put(ant, fpg.estimateSupport(new FrequentPattern(ant,1)));
//                                antSupportCount = fpg.estimateSupport(new FrequentPattern(ant,1));
//                                confidence = support / (antSupportCount / (double) size);
//
//                                if(confidence>minConfidence){
//                                    antSupportCount = fpg.computeSupport(new FrequentPattern(ant,1));
//                                }
//                                frequenciesMap.put(ant, fpg.computeSupport(new FrequentPattern(ant,1)));
                                antSupportCount = fpg.computeSupport(new FrequentPattern(ant,1));
//                                frequenciesMap.put(ant, fpg.getSize());
                            } else {
                                antSupportCount = frequenciesMap.get(ant);
                            }
                            confidence = support / (antSupportCount / (double) size);
                            lift = support / ((antSupportCount / (double) size) * (frequenciesMap.get(cons) / (double) size));
//                            break;
//                        }
//                    }
                    return Rule.buildRule(ant, cons, support, confidence, lift);

                })
                .filter(r -> r.getConfidence()>=minConfidence)
                .collect(Collectors.toList());
    }

}
