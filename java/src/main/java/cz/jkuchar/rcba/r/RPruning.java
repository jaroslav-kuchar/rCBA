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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.jkuchar.rcba.build.RandomSearch;
import cz.jkuchar.rcba.fpg.AssociationRules;
import cz.jkuchar.rcba.fpg.FPGrowth;
import cz.jkuchar.rcba.fpg.FrequentPattern;
import cz.jkuchar.rcba.pruning.DCBrCBA;
import cz.jkuchar.rcba.pruning.M1CBA;
import cz.jkuchar.rcba.pruning.M2CBA;
import cz.jkuchar.rcba.pruning.Pruning;
import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;
import cz.jkuchar.rcba.rules.Tuple;

public class RPruning {

	private List<Rule> rules;
	private List<Item> items;
	private String[] cNames;
	private String[] values;

	private Map<String, Set<String>> cache;
	
	private RuleEngine re = new RuleEngine();

	private static Logger logger = Logger.getLogger(RPruning.class.getName());

	public RPruning() {
		this.cNames = new String[1];
		this.values = new String[1];
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

	public void setValues(String[] values) {
		this.values = values;
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

	public void addTransactionMatrix(Object matrix[]) {
		int rows = matrix.length;
		if (rows > 0) {
			int columns = ((boolean[]) matrix[0]).length;
			for (int i = 0; i < rows; i++) {
				Item item = new Item();
				for (int j = 0; j < columns; j++) {
					boolean v = ((boolean []) matrix[i])[j];
					if(v){
						item.put(cNames[j], values[j]);
						this.cache.get(cNames[j]).add(values[j]);
					}
				}
				this.items.add(item);
			}
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

	public String[][] fpgrowth(double minSupport, double minConfidence, int maxLength, String consequent) {
		try {
			logger.log(Level.INFO, "FP-Growth - start");
			FPGrowth fpGrowth = new FPGrowth();
			List<List<Tuple>> t = items.stream().map(item -> {
				List<Tuple> tuples = new ArrayList<>();
				for(String key:item.keys()){
					for(String val:item.get(key)){
						tuples.add(new Tuple(key,val));
					}
				}
				return tuples;
			}).collect(Collectors.toList());
			logger.log(Level.INFO, "FP-Growth - data converted");
			List<FrequentPattern> fps = fpGrowth.run(t, minSupport, maxLength);
			logger.log(Level.INFO, "FP-Growth - frequent patterns: "+fps.size());
//			System.out.println(fps.size());
			List<Rule> rules = AssociationRules.generate(fps, fpGrowth, t.size(), minConfidence, consequent);
			logger.log(Level.INFO, "FP-Growth - rules: "+rules.size());
			return rules.stream().map(rule -> new String[]{rule.getText(), Double.toString(rule.getSupport()), Double.toString(rule.getConfidence()), Double.toString(rule.getLift())}).toArray(String[][]::new);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String[0][4];
	}

	public String[][] build(String consequent) {
		try {
			RandomSearch build = new RandomSearch();
			List<List<Tuple>> t = items.stream().map(item -> {
				List<Tuple> tuples = new ArrayList<>();
				for(String key:item.keys()){
					for(String val:item.get(key)){
						tuples.add(new Tuple(key,val));
					}
				}
				return tuples;
			}).collect(Collectors.toList());
			List<Rule> rules = build.build(t, consequent);
			return rules.stream().map(rule -> new String[]{rule.getText(), Double.toString(rule.getSupport()), Double.toString(rule.getConfidence()), Double.toString(rule.getLift())}).toArray(String[][]::new);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String[0][4];
	}

}
