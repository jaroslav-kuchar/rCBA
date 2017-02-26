package cz.jkuchar.rcba.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cz.jkuchar.rcba.pruning.ASet;


public class Rule implements Comparable<Rule> {

	// textual representation of rule
	private String text;
	// confidence + support
	private double confidence;
	private double support;
	private double lift;

	private double ruleError;
	private double defaultError;
	private Rule defaultRule;

	private boolean marked = false;
	private List<ASet> replaces = new ArrayList<ASet>();
	private Map<String, Integer> classCasesCovered = new HashMap<String, Integer>();

//	private Map<String, String> antecendent = new HashMap<String, String>();
//	private Map<String, String> consequent = new HashMap<String, String>();

	private TupleCollection antecendent = new TupleCollection();
	private TupleCollection consequent = new TupleCollection();

	private Rule() {
		super();
	}

	public String getText() {
		return new String(text);
	}

	public double getConfidence() {
		return confidence;
	}

	public double getSupport() {
		return support;
	}
	
	public double getLift() {
		return lift;
	}

	public TupleCollection getAnt() {
		return antecendent;
	}

	public TupleCollection getCons() {
		return consequent;
	}

	public static Rule buildRule(String text, Map<String, Set<String>> meta,
			double confidence, double support) {
		return buildRule(text, meta, confidence, support, 0);
	}
	
	public static Rule buildRule(String text, Map<String, Set<String>> meta,
			double confidence, double support, double lift) {
		Rule out = new Rule();
		out.text = text;
		out.confidence = confidence;
		out.support = support;
		out.lift = lift;
		out.parse(meta);
		return out;
	}

	public static Rule buildRule(String text, double confidence, double support) {
		return buildRule(text, null, confidence, support, 0);
	}

	public static Rule buildRule(List<Tuple> antecedent, List<Tuple> consequent, double support, double confidence, double lift) {
		Rule out = new Rule();
		out.text = antecedent.stream().map(t -> t.getLeft()+"="+t.getRight()).collect(Collectors.joining(",", "{", "}"))+
				" => "+
				consequent.stream().map(t -> t.getLeft()+"="+t.getRight()).collect(Collectors.joining(",", "{", "}"));
		out.confidence = confidence;
		out.support = support;
		out.lift = lift;
		antecedent.stream().forEach(t -> out.antecendent.put(t.getLeft(),t.getRight()));
		consequent.stream().forEach(t -> out.consequent.put(t.getLeft(),t.getRight()));
		return out;
	}

	private void parse(Map<String, Set<String>> meta) {
		if (text != null && text.length() > 0
				&& text.matches("\\{(.*?)\\}\\s*=>\\s*\\{(.+?)\\}")) {

			String[] parts = text.trim().split("\\}\\s*=>\\s*\\{");
			if (parts.length != 2) {
				throw new BadRuleFormatException("Wrong formattting of: "
						+ text);
			}
			if (meta != null) {
				antecendent = parsePart(parts[0].substring(1), meta);
				consequent = parsePart(parts[1].substring(0, parts[1].length()-1), meta);
				if(consequent.size()<=0){
					consequent = parsePart(parts[1].substring(0, parts[1].length()-1));
				}
			} else {
				antecendent = parsePart(parts[0].substring(1));
				consequent = parsePart(parts[1].substring(0, parts[1].length()-1));
			}
		} else {
			throw new BadRuleFormatException("Wrong formattting of: " + text);
		}
	}

	private TupleCollection parsePart(String part,
			Map<String, Set<String>> meta) {
		// remove {}
//		part = part.substring(1, part.length() - 1);
		TupleCollection out = new TupleCollection();
		
		for (int i = 0; i < meta.keySet().size(); i++) {
			for (String key : meta.keySet()) {
				if (part.startsWith(key + "=")) {
					for (String val : meta.get(key)) {
						if (part.startsWith(key + "=" + val + ",")
								|| part.equals(key + "=" + val)) {
							out.put(key,val);

							int subIndex = (key + "=" + val + ",").length();
							if(subIndex<part.length()){
								part = part.substring(subIndex);
							} else {
								part = "";
							}
							break;
						}
					}

				}
			}
		}
		return out;
	}

	private TupleCollection parsePart(String part) {
		// remove {}
//		part = part.substring(1, part.length() - 1);
		TupleCollection out = new TupleCollection();
		String[] attrs = part.split(",");
		for (String attr : attrs) {
			Pattern pattern = Pattern.compile("(.*)=(.*)");
			Matcher matcher = pattern.matcher(attr);
			if (matcher.matches()) {
				Set<String> p = new HashSet<String>();
				for (String pp : matcher.group(2).split(",")) {
					p.add(pp);
				}
				out.put(matcher.group(1), p.iterator().next());
			} else if (attr != null && attr.length() > 0) {
				throw new BadRuleFormatException(
						"Wrong formattting of rule items " + attr);
			}
		}
		return out;
	}

	public double getRuleError() {
		return ruleError;
	}

	public void setRuleError(double ruleError) {
		this.ruleError = ruleError;
	}

	public double getDefaultError() {
		return defaultError;
	}

	public void setDefaultError(double defaultError) {
		this.defaultError = defaultError;
	}

	public Rule getDefaultRule() {
		return defaultRule;
	}

	public void setDefaultRule(Rule defaultRule) {
		this.defaultRule = defaultRule;
	}

	public synchronized void incClassCasesCovered(String className) {
		if (!this.classCasesCovered.containsKey(className)) {
			classCasesCovered.put(className, 0);
		}
		classCasesCovered.put(className, classCasesCovered.get(className) + 1);
	}

	public synchronized void decClassCasesCovered(String className) {
		if (!this.classCasesCovered.containsKey(className)) {
			classCasesCovered.put(className, 0);
		}
		classCasesCovered.put(className, classCasesCovered.get(className) - 1);
	}
	
	public synchronized void setClassCasesCovered(String className, int value) {
		if (!this.classCasesCovered.containsKey(className)) {
			classCasesCovered.put(className, 0);
		}
		classCasesCovered.put(className, value);
	}

	public Map<String, Integer> getClassCasesCovered() {
		return this.classCasesCovered;
	}

	public synchronized void addReplace(ASet aset) {
		if (!this.replaces.contains(aset)) {
			replaces.add(aset);
		}
	}

	public List<ASet> getReplaces() {
		return this.replaces;
	}

	public boolean isMarked() {
		return this.marked;
	}

	public synchronized void mark() {
		this.marked = true;
	}

	public synchronized void unmark() {
		this.marked = false;
	}	

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public void setSupport(double support) {
		this.support = support;
	}
	
	public void setLift(double lift) {
		this.lift = lift;
	}

	@Override
	public String toString() {
		return "" + text + "\n---support=" + support + "\n---confidence="
				+ confidence + "\n---ant=" + antecendent + "\n---cons="
				+ consequent + "\n---defaultError=" + defaultError
				+ "\n---ruleError=" + ruleError + "\n---defaultRule={"
				+ defaultRule + "}" + "\n";
	}

	public int compareTo(Rule o) {
		int result = Double.compare(o.confidence, this.confidence);
		if (result != 0) {
			return result;
		}
		result = Double.compare(o.support, this.support);
		if (result != 0) {
			return result;
		}
		result = this.antecendent.size() - o.antecendent.size();
		return result;
	}

}
