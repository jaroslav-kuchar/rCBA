package cz.jkuchar.rcba.pruning;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;

@Component
public class DCBrCBA implements Pruning {

	@Autowired
	RuleEngine re;

	public List<Rule> prune(List<Rule> rules, List<Item> train) {
		Collections.sort(rules);
		List<Rule> pruned = new LinkedList<Rule>();
		for (Rule rule : rules) {
			String className = rule.getCons().keySet().iterator().next();

			List<Integer> matches = IntStream
					.range(0, train.size())
					.parallel()
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
