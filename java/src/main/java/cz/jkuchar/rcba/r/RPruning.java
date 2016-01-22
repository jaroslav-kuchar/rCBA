package cz.jkuchar.rcba.r;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.jkuchar.rcba.pruning.DCBrCBA;
import cz.jkuchar.rcba.pruning.M1CBA;
import cz.jkuchar.rcba.pruning.M2CBA;
import cz.jkuchar.rcba.pruning.Pruning;
import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;

public class RPruning {

	private List<Rule> rules;
	private List<Item> items;
	private String[] cNames;

	private Map<String, Set<String>> cache;
	
	private RuleEngine re = new RuleEngine();

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

	public void loadItemsFromFile(String fileName) {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String separator = ",";
			reader.lines().parallel().map(line -> line.split(separator + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"))
					.map(line -> Arrays.asList(line).stream()
							.map(item -> item.startsWith("\"") ? item.substring(1) : item)
							.map(item -> item.endsWith("\"") ? item.substring(0, item.length() - 1) : item)
							.toArray(String[]::new))
					.forEach(line -> addItem(line));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void addDataFrame(Object dataFrame[]) {
		int columns = dataFrame.length;
		if (columns > 0) {
			int rows = ((String[]) dataFrame[0]).length;
			for (int i = 0; i < rows; i++) {
				String[] row = new String[columns];
				for (int j = 0; j < columns; j++) {
					row[j] = ((String[]) dataFrame[j])[i];
				}
				addItem(row);
			}
		}
	}

	public void addAll(String[] fullFrame) {
		int chunk = this.cNames.length;
		for (int i = 0; i < fullFrame.length; i += chunk) {
			addItem(Arrays.copyOfRange(fullFrame, i, i + chunk));
		}
	}

	public synchronized void addItem(String[] values) {
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

	public void addRuleFrame(Object dataFrame[]) {
		int columns = dataFrame.length;
		if (columns > 0) {
			int rows = ((String[]) dataFrame[0]).length;
			for (int i = 0; i < rows; i++) {
				String rule = ((String[]) dataFrame[0])[i];
				double confidence = (dataFrame[2] instanceof double[]) ? ((double[]) dataFrame[2])[i]
						: (double) ((int[]) dataFrame[2])[i];
				double support = (dataFrame[1] instanceof double[]) ? ((double[]) dataFrame[1])[i]
						: (double) ((int[]) dataFrame[1])[i];
				double lift = (dataFrame[3] instanceof double[]) ? ((double[]) dataFrame[3])[i]
						: (double) ((int[]) dataFrame[3])[i];
				addRule(rule, confidence, support, lift);
			}
		}
	}

	public Rule[] prune(String method) {
		Pruning pruning;
		switch (method) {
		case "dcbrcba":
			pruning = new DCBrCBA();
			break;
		case "m1cba":
			pruning = new M1CBA();
			break;
		default:
			pruning = new M2CBA();
			break;
		}
		try {
			List<Rule> results = pruning.prune(rules, items);
			return results.toArray(new Rule[results.size()]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (Rule[]) rules.toArray();
	}

	public String[] classify() {
		String[] predictions = new String[this.items.size()];
		re.addRules(rules);
		for (int i = 0; i < predictions.length; i++) {
			Rule tm = re.getTopMatch(this.items.get(i));
			if (tm == null) {
				predictions[i] = null;
			} else {
				predictions[i] = tm.getCons().values().iterator().next();
			}
		}
		re.clear();
		return predictions;
	}

}
