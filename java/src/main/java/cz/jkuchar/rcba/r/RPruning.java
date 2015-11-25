package cz.jkuchar.rcba.r;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.jkuchar.rcba.pruning.DCBrCBA;
import cz.jkuchar.rcba.pruning.M1CBA;
import cz.jkuchar.rcba.pruning.M2CBA;
import cz.jkuchar.rcba.pruning.Pruning;
import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;

@Component
@Scope("prototype")
public class RPruning {

	private List<Rule> rules;
	private List<Item> items;
	private String[] cNames;

	private Map<String, Set<String>> cache;

	@Autowired
	M2CBA m2Pruning;

	@Autowired
	M1CBA m1pruning;

	@Autowired
	DCBrCBA dcpruning;

	public RPruning() {
		this.cNames = new String[1];
		this.rules = new ArrayList<Rule>();
		this.items = new ArrayList<Item>();
		this.cache = new HashMap<String, Set<String>>();
	}

	public void setColumns(String[] cNames) {
		this.cNames = cNames;
		for (String cname : cNames) {
			this.cache.put(cname, new HashSet<String>());
		}
	}

	public void loadFromFile(String fileName) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String separator = ",";
			reader.lines().map(line -> line.split(separator)).forEach(line -> addItem(line));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		System.out.println("Number of imported items: " + this.items.size());
	}

	public void addItem(String[] values) {
		Item item = new Item();
		for (int i = 0; i < cNames.length; i++) {
			item.put(cNames[i], values[i]);
			this.cache.get(cNames[i]).add(values[i]);
		}
		this.items.add(item);
	}

	public void addRule(String rule, double confidence, double support, double lift) {
		this.rules.add(Rule.buildRule(rule, this.cache, confidence, support, lift));
	}

	public Rule[] prune(String method) {
		System.out.println("Pruning started...");
		Pruning pruning;
		switch (method) {
		case "dcbrcba":
			pruning = dcpruning;
			break;
		case "m1cba":
			pruning = m1pruning;
			break;
		default:
			pruning = m2Pruning;
			break;
		}
		try {
			System.out.println(rules);
			List<Rule> results = pruning.prune(rules, items);
			System.out.println("Pruning completed: " + rules.size() + "->" + results.size());
			return results.toArray(new Rule[results.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (Rule[]) rules.toArray();
	}

}
