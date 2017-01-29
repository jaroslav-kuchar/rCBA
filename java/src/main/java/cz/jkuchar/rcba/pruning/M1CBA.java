package cz.jkuchar.rcba.pruning;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;


public class M1CBA implements Pruning {	
	
	RuleEngine re = new RuleEngine();

	@Override
	public List<Rule> prune(List<Rule> rules, List<Item> train) {		
		Collections.sort(rules);
		List<Rule> pruned = new LinkedList<Rule>();

		int minErroValue = train.size();
		int minErrorRid = 0;

		for (int rid = 0; rid < rules.size(); rid++) {
			Rule rule = rules.get(rid);
			String className = rule.getCons().keys().iterator().next();
			// match of antecedents
			List<Integer> temp = IntStream.range(0, train.size()).parallel()
					.filter(item -> re.matchRule(rule, train.get(item)))
					.boxed().collect(Collectors.toList());
			// match both antecedent and consequent
			List<Integer> marked = temp
					.stream()
					.parallel()
					.filter(item -> rule.getCons().get(className).equals(train.get(item).get(className)))
					.collect(Collectors.toList());

			if (marked.size() > 0) {
				pruned.add(rule);
				Collections.sort(temp, Collections.reverseOrder());
				for (int match : temp) {
					train.remove(match);
				}

				List<String> mc = train.stream().parallel()
						.flatMap(tr -> tr.get(className).stream())
						.filter(s -> s!=null)
						.collect(Collectors.toList());
				Entry<String, Integer> mostCommon = mostCommon(mc);
				if (mostCommon != null) {
					Rule defaultRule = Rule.buildRule("{} => {" + className
							+ "=" + mostCommon.getKey() + "}",
							new HashMap<String, Set<String>>() {
								{
									put(className,
											new HashSet<String>(
													Arrays.asList(mostCommon
															.getKey())));
								}
							}, 0, 0);
					rule.setDefaultRule(defaultRule);
					rule.setDefaultError(train.size() - mostCommon.getValue());
				} else {
					rule.setDefaultRule(null);
					rule.setDefaultError(0);
				}

				rule.setRuleError(temp.size() - marked.size());
				if (!pruned.isEmpty()) {
					rule.setRuleError(rule.getRuleError()
							+ pruned.get(pruned.size() - 1).getRuleError());
				}
				if ((rule.getDefaultError() + rule.getRuleError()) < minErroValue) {
					minErroValue = (int) (rule.getDefaultError() + rule
							.getRuleError());
					minErrorRid = pruned.size();
				}
			}

			if (train.size() == 0)
				break;
		}

		pruned = pruned.subList(0, minErrorRid);
		if (pruned.size() > 0
				&& pruned.get(pruned.size() - 1).getDefaultRule() != null) {
			pruned.add(pruned.get(pruned.size() - 1).getDefaultRule());
			
			Rule dRule = pruned.get(pruned.size()-1);			
			String className = dRule.getCons().keys().iterator().next();
			long count = IntStream.range(0, train.size()).parallel()
					.filter(item -> dRule.getCons().get(className).equals(train.get(item).get(className)))
					.count();						
			dRule.setConfidence(count/(double)train.size());
			dRule.setSupport(count/(double)train.size());
		}
		return pruned;
	}

	public static <T> Entry<T, Integer> mostCommon(List<T> list) {
		Map<T, Integer> map = new HashMap<>();

		for (T t : list) {
			Integer val = map.get(t);
			map.put(t, val == null ? 1 : val + 1);
		}

		Entry<T, Integer> max = null;

		for (Entry<T, Integer> e : map.entrySet()) {
			if (max == null || e.getValue() > max.getValue())
				max = e;
		}

		return max;
	}

}
