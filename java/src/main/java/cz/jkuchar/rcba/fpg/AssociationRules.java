package cz.jkuchar.rcba.fpg;

import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class AssociationRules {


    public static List<Rule> generate(List<FrequentPattern> fp, int size, double minConfidence, String consequent) {
        Map<List<Tuple>, Integer> frequenciesMap = fp.parallelStream().collect(Collectors.toMap(p -> p.getPattern(), p -> p.getMinSupportCount()));

        return fp.stream()
                .filter(f -> f.getPattern().stream().anyMatch(tuple -> tuple.getLeft().equals(consequent)))
                .map(f -> {
                    List<Tuple> ant = null;
                    List<Tuple> cons = null;
                    double support = f.getMinSupportCount() / (double) size;
                    double confidence = -1;
                    double lift = -1;
                    for (Tuple item : f.getPattern()) {
                        List<Tuple> tmp = new ArrayList<Tuple>(f.getPattern());
                        tmp.remove(item);
                        if (item.getLeft().equals(consequent)) {
                            ant = new ArrayList<Tuple>(tmp);
                            cons = new ArrayList<Tuple>() {{
                                add(item);
                            }};
                            confidence = support / ((frequenciesMap.get(ant) == null ? size : frequenciesMap.get(ant)) / (double) size);
                            lift = support / (((frequenciesMap.get(ant) == null ? size : frequenciesMap.get(ant)) / (double) size) * (frequenciesMap.get(cons) / (double) size));
                            break;
                        }
                    }
                    return Rule.buildRule(ant, cons, support, confidence, lift);

                })
                .filter(r -> r.getConfidence()>=minConfidence)
                .collect(Collectors.toList());
    }

}
