package cz.jkuchar.rcba.pruning;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;

public class DCBrCBA implements Pruning {	
	
	RuleEngine re = new RuleEngine();

	public List<Rule> prune(List<Rule> rules, List<Item> train) {		
		return this.prune(rules, train, true);
	}

	public List<Rule> prune(List<Rule> rules, List<Item> train, boolean parallel) {
		Collections.sort(rules);
		List<Rule> pruned = new LinkedList<Rule>();
		for (Rule rule : rules) {
			String className = rule.getCons().keys().iterator().next();

			List<Integer> matches = (parallel?IntStream.range(0, train.size()).parallel():IntStream.range(0, train.size()))
					.filter(item -> re.matchRule(rule, train.get(item))
							&& rule.getCons().get(className)
							.equals(train.get(item).get(className)))
					.boxed().collect(Collectors.toList());

			Collections.sort(matches, Collections.reverseOrder());

			if (matches.size() > 0) {
				pruned.add(rule);
			}

			for (int match : matches) {
				train.remove(match);
			}

		}
		return pruned;
	}

}
