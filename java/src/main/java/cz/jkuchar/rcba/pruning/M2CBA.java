package cz.jkuchar.rcba.pruning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;

/*
 * http://cgi.csc.liv.ac.uk/~frans/KDD/Software/CBA/cba.html
 */

@Component
@Scope("prototype")
public class M2CBA implements Pruning {

	Logger logger = Logger.getLogger(M2CBA.class.getName());

	@Autowired
	private RuleEngine re;

	private List<Integer> Q;
	private List<Integer> U;
	private List<Tuple> A;
	private List<Rule> C;
	private String className;

	@Override
	public List<Rule> prune(List<Rule> rules, List<Item> train) {
		// sort rules
		Collections.sort(rules);

		// final classifier
		C = new LinkedList<Rule>();
		className = rules.get(0).getCons().keySet().iterator().next();

		// collections
		Q = Collections.synchronizedList(new ArrayList<Integer>());
		U = Collections.synchronizedList(new ArrayList<Integer>());
		A = Collections.synchronizedList(new ArrayList<Tuple>());

		// stages
		long startTime = System.nanoTime();
		stage1(rules, train);
		logger.debug("Stage1: " + (System.nanoTime() - startTime) / 1000000
				+ " ms");
		stage2(rules, train);
		logger.debug("Stage2: " + (System.nanoTime() - startTime) / 1000000
				+ " ms");
		stage3(rules, train);
		logger.debug("Stage3: " + (System.nanoTime() - startTime) / 1000000
				+ " ms");

		// debug print
		C.stream().forEach(
				rule -> logger.debug(rule.getText() + ", "
						+ rule.getConfidence() + ", " + rule.getSupport()
						+ ", " + rule.isMarked() + ", "
						+ (rule.getDefaultError() + rule.getRuleError()) + ", "
						+ rule.getClassCasesCovered()));

		return C;
	}

	/*
	 * STAGE 1
	 */
	private void stage1(List<Rule> rules, List<Item> train) {

		IntStream
				.range(0, train.size())
				.parallel()
				.forEach(
						did -> {
							Item item = train.get(did);
							String itemClassValue = item.get(className);

							OptionalInt cRule = OptionalInt.empty();
							OptionalInt wRule = OptionalInt.empty();

							for (int rid = 0; rid < rules.size(); rid++) {
								if (re.matchRule(rules.get(rid), item)) {
									if (!cRule.isPresent() && rules.get(rid).getCons().get(className)
											.iterator().next()
											.equals(itemClassValue)) {
										cRule = OptionalInt.of(rid);
									} 
									if (!wRule.isPresent() && !rules.get(rid).getCons().get(className)
											.iterator().next()
											.equals(itemClassValue)) {
										wRule = OptionalInt.of(rid);
									}
									if (cRule.isPresent() && wRule.isPresent()){
										break;
									}
								}
							}

							if (cRule.isPresent()) {
								if (!U.contains(cRule.getAsInt())) {
									U.add(cRule.getAsInt());
								}
								rules.get(cRule.getAsInt())
										.incClassCasesCovered(itemClassValue);

								if (!wRule.isPresent()
										|| (wRule.isPresent() && cRule
												.getAsInt() < wRule.getAsInt())) {
									if (!Q.contains(cRule.getAsInt())) {
										Q.add(cRule.getAsInt());
									}
									rules.get(cRule.getAsInt()).mark();
								} else {
									Tuple t = new Tuple(did, itemClassValue,
											cRule.getAsInt(), wRule.getAsInt());
									if (!A.contains(t)) {
										A.add(t);
									}
								}
							}
						});
	}

	/*
	 * STAGE 2
	 */
	private void stage2(List<Rule> rules, List<Item> train) {
		A.stream().forEach(
				tuple -> {
					if (rules.get(tuple.wRule).isMarked()) {
						rules.get(tuple.cRule).decClassCasesCovered(
								tuple.dclass);
						rules.get(tuple.wRule).incClassCasesCovered(
								tuple.dclass);
					} else {
						List<Integer> wSet = U
								.stream()
								.filter(rid -> rid < tuple.cRule
										&& rid > tuple.wRule)
								.collect(Collectors.toList())
								.stream()
								.filter(rid -> re.matchRule(rules.get(rid),
										train.get(tuple.did)))
								.collect(Collectors.toList());
						for (int w : wSet) {
							rules.get(w).addReplace(
									new Tuple(tuple.did, tuple.dclass,
											tuple.cRule, -1));
							rules.get(w).incClassCasesCovered(tuple.dclass);
							if (!Q.contains(w)) {
								Q.add(w);
							}
						}
					}
				});
	}

	private void stage3(List<Rule> rules, List<Item> train) {
		/*
		 * STAGE 3
		 */

		int minErroValue = train.size();
		int minErrorRid = 0;
		int ruleErrors = 0;
		Map<String, Integer> classDistr = train.parallelStream()
				.map(tr -> tr.get(className)).collect(Collectors.toList())
				.stream()
				.collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
		List<Integer> QQ = Q.parallelStream().distinct()
				.collect(Collectors.toList());
		Collections.sort(QQ);
		for (int rid : QQ) {
			if (rules
					.get(rid)
					.getClassCasesCovered()
					.containsKey(
							rules.get(rid).getCons().get(className).iterator()
									.next())
					&& rules.get(rid)
							.getClassCasesCovered()
							.get(rules.get(rid).getCons().get(className)
									.iterator().next()) > 0) {
				for (Tuple tuple : rules.get(rid).getReplaces()) {
					if (C.stream().anyMatch(
							rule -> re.matchRule(rule, train.get(tuple.did)))) {
						rules.get(rid).decClassCasesCovered(tuple.dclass);
					} else {
						rules.get(tuple.cRule).decClassCasesCovered(
								tuple.dclass);
					}
				}
				for (String cn : rules.get(rid).getClassCasesCovered().keySet()) {
					if (!cn.equals(rules.get(rid).getCons().get(className)
							.iterator().next()))
						ruleErrors += rules.get(rid).getClassCasesCovered()
								.get(cn);
					classDistr.put(cn, classDistr.get(cn)
							- rules.get(rid).getClassCasesCovered().get(cn));
				}
				String dc = mc(classDistr);
				Rule defaultRule = Rule.buildRule("{} => {" + className + "="
						+ dc + "}", new HashMap<String, Set<String>>() {
					{
						put(className, new HashSet<String>(Arrays.asList(dc)));
					}
				}, 0, 0);
				rules.get(rid).setDefaultRule(defaultRule);
				rules.get(rid).setRuleError(ruleErrors);
				int defError = 0;
				for (String k : classDistr.keySet()) {
					if (!k.equals(dc)) {
						defError += classDistr.get(k);
					}
				}
				rules.get(rid).setDefaultError(defError);
				C.add(rules.get(rid));
				if ((rules.get(rid).getDefaultError() + rules.get(rid)
						.getRuleError()) < minErroValue) {
					minErroValue = (int) (rules.get(rid).getDefaultError() + rules
							.get(rid).getRuleError());
					minErrorRid = C.size();
				}
			}
		}

		C = C.subList(0, minErrorRid);
		Collections.sort(C);
		if (C.size() > 0 && C.get(C.size() - 1).getDefaultRule() != null) {
			C.add(C.get(C.size() - 1).getDefaultRule());

			Rule dRule = C.get(C.size() - 1);
			String className = dRule.getCons().keySet().iterator().next();
			long count = IntStream
					.range(0, train.size())
					.parallel()
					.filter(item -> dRule.getCons().get(className).iterator()
							.next().equals(train.get(item).get(className)))
					.count();
			dRule.setConfidence(count / (double) train.size());
			dRule.setSupport(count / (double) train.size());

		}
	}

	private String mc(Map<String, Integer> classDistr) {
		int max = 0;
		String mc = classDistr.keySet().iterator().next();
		for (String k : classDistr.keySet()) {
			if (classDistr.get(k) > max) {
				max = classDistr.get(k);
				mc = k;
			}
		}
		return mc;
	}
}
