package cz.jkuchar.rcba.rules;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class RuleEngine {

	private List<Rule> memory = new LinkedList<Rule>();

	public void add(Rule rule) {
		this.memory.add(rule);
	}

	public void addRules(List<Rule> rules) {
		this.memory.addAll(rules);
	}

	public void clear() {
		this.memory = new LinkedList<Rule>();
	}

	public int getMemoryLength() {
		return this.memory.size();
	}

	public synchronized Rule getTopMatch(Item item) {
		Collections.sort(this.memory);
		for (Rule rule : memory) {
			if (matchRule(rule, item)) {
				return rule;
			}
		}
		return null;
	}

	public boolean matchRule(Rule rule, Item item) {
		for (String partAnt : rule.getAnt().keySet()) {
			if (item.containsKey(partAnt)
					&& rule.getAnt().get(partAnt).contains(item.get(partAnt))) {
				// match = true;
			} else {
				return false;
			}
		}
		return true;
	}

}
