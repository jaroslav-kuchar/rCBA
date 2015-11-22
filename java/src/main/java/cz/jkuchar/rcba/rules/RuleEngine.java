package cz.jkuchar.rcba.rules;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

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
//		return item.containsAllEntries(rule.getAnt().entrySet());
		for (Entry<String, String> entry : rule.getAnt().entrySet()) {
			if (!entry.getValue().equals(item.get(entry.getKey()))) {
				return false;
			} 
		}
		return true;
	}

}
