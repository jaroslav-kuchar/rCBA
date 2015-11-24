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
		rules = new ArrayList<Rule>(rules);
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
		logger.debug("Stage1: " + (System.nanoTime() - startTime) / 1000000 + " ms");
		stage2(rules, train);
		logger.debug("Stage2: " + (System.nanoTime() - startTime) / 1000000 + " ms");
		stage3(rules, train);
		logger.debug("Stage3: " + (System.nanoTime() - startTime) / 1000000 + " ms");

		// // debug print
		// C.stream().forEach(
		// rule -> logger.debug(rule.getText() + ", "
		// + rule.getConfidence() + ", " + rule.getSupport()
		// + ", " + rule.isMarked() + ", "
		// + (rule.getDefaultError() + rule.getRuleError()) + ", "
		// + rule.getClassCasesCovered()));

		return C;
	}

	/*
	 * STAGE 1
	 */
	private void stage1(List<Rule> rules, List<Item> train) {
		IntStream.range(0, train.size()).parallel().forEach(did -> {
			Item item = train.get(did);
			String itemClassValue = item.get(className);

			OptionalInt cRule = OptionalInt.empty();
			OptionalInt wRule = OptionalInt.empty();
			int cR = -1;
			int wR = -1;
			
//			int rid = -1;
//			for(Rule rule:rules){
//				rid++;

			for (int rid = 0; rid < rules.size(); rid++) {
				Rule rule = rules.get(rid);
				if (re.matchRule(rule, item)) {
					if (cR ==-1 && rule.getCons().get(className).equals(itemClassValue)) {
						cR = rid;
					}
					if (wR==-1 && !rule.getCons().get(className).equals(itemClassValue)) {
						wR=rid;
					}
					if(cR!=-1 && wR!=-1){
						break;
					}
				}
			}
			if(cR!=-1){
				cRule = OptionalInt.of(cR);
			}
			if(wR!=-1){
				wRule = OptionalInt.of(wR);
			}

			if (cRule.isPresent()) {
				if (!U.contains(cRule.getAsInt())) {
					U.add(cRule.getAsInt());
				}
				rules.get(cRule.getAsInt()).incClassCasesCovered(itemClassValue);

				if (!wRule.isPresent() || (wRule.isPresent() && cRule.getAsInt() < wRule.getAsInt())) {
					if (!Q.contains(cRule.getAsInt())) {
						Q.add(cRule.getAsInt());
					}
					rules.get(cRule.getAsInt()).mark();
				} else {
					Tuple t = new Tuple(did, itemClassValue, cRule.getAsInt(), wRule.getAsInt());
					if (!A.contains(t)) {
						A.add(t);
					}
				}
			}
			// // solve cases depending on availability of rules
			// CBAM2Box box = new CBAM2Box();
			// if (cRule.isPresent()) {
			// // Ulist
			// box.crules.add(cRule.getAsInt());
			// // class cases covered
			// box.initDClass(cRule.getAsInt(), item.get(className));
			// // box.cRules.add(cRule.getAsInt());
			// if (!wRule.isPresent()) {
			// // mark crule
			// box.marked.add(cRule.getAsInt());
			// // Qlist
			// } else {
			// if (cRule.getAsInt() < wRule.getAsInt()) {
			// // mark crule
			// box.marked.add(cRule.getAsInt());
			// // Qlist
			// } else {
			// // Alist
			// box.A.add(new ASet(did, item.get(className),
			// cRule.getAsInt(), wRule.getAsInt(), item));
			// }
			// }
			// }
			// return box;
			// }).reduce((lBox, rBox) -> {
			// // reduce all
			// CBAM2Box box = new CBAM2Box();
			//
			// box.crules.addAll(lBox.crules);
			// box.crules.addAll(rBox.crules);
			//
			// box.marked.addAll(lBox.marked);
			// box.marked.addAll(rBox.marked);
			//
			// box.mergeDClasses(lBox.dClasses);
			// box.mergeDClasses(rBox.dClasses);
			//
			// box.A.addAll(lBox.A);
			// box.A.addAll(rBox.A);
			//
			// return box;
			// }).get();
			// logger.debug("stage1 checkpoint");
			// U.addAll(result.crules);
			// Q.addAll(result.marked);
			// result.marked.stream().forEach(rid -> {
			// rules.get(rid).mark();
			// });
			// result.A.stream().forEach(AA -> {
			// A.add(new Tuple(AA.did, AA.dClass, AA.cRule, AA.wRule));
			// });
			// result.dClasses.keySet().stream().forEach(rid ->{
			// result.dClasses.get(rid).entrySet().stream().forEach(entry ->{
			// rules.get(rid).setClassCasesCovered(entry.getKey(),
			// entry.getValue());
			// });
			// });
		});
	}

	/*
	 * STAGE 2
	 */
	private void stage2(List<Rule> rules, List<Item> train) {
		A.stream().forEach(tuple -> {
			if (rules.get(tuple.wRule).isMarked()) {
				rules.get(tuple.cRule).decClassCasesCovered(tuple.dclass);
				rules.get(tuple.wRule).incClassCasesCovered(tuple.dclass);
			} else {
				List<Integer> wSet = U.stream().filter(rid -> rid < tuple.cRule && rid > tuple.wRule)
						.collect(Collectors.toList()).stream()
						.filter(rid -> re.matchRule(rules.get(rid), train.get(tuple.did))).collect(Collectors.toList());
				for (int w : wSet) {
					rules.get(w).addReplace(new Tuple(tuple.did, tuple.dclass, tuple.cRule, -1));
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
		Map<String, Integer> classDistr = train.parallelStream().map(tr -> tr.get(className))
				.collect(Collectors.toList()).stream().collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
		List<Integer> QQ = Q.parallelStream().distinct().collect(Collectors.toList());
		Collections.sort(QQ);
		for (int rid : QQ) {
			if (rules.get(rid).getClassCasesCovered().containsKey(rules.get(rid).getCons().get(className))
					&& rules.get(rid).getClassCasesCovered().get(rules.get(rid).getCons().get(className)) > 0) {
				for (Tuple tuple : rules.get(rid).getReplaces()) {
					if (C.stream().anyMatch(rule -> re.matchRule(rule, train.get(tuple.did)))) {
						rules.get(rid).decClassCasesCovered(tuple.dclass);
					} else {
						rules.get(tuple.cRule).decClassCasesCovered(tuple.dclass);
					}
				}
				for (String cn : rules.get(rid).getClassCasesCovered().keySet()) {
					if (!cn.equals(rules.get(rid).getCons().get(className)))
						ruleErrors += rules.get(rid).getClassCasesCovered().get(cn);
					classDistr.put(cn, classDistr.get(cn) - rules.get(rid).getClassCasesCovered().get(cn));
				}
				String dc = mc(classDistr);
				Rule defaultRule = Rule.buildRule("{} => {" + className + "=" + dc + "}",
						new HashMap<String, Set<String>>() {
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
				if ((rules.get(rid).getDefaultError() + rules.get(rid).getRuleError()) < minErroValue) {
					minErroValue = (int) (rules.get(rid).getDefaultError() + rules.get(rid).getRuleError());
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
			long count = IntStream.range(0, train.size()).parallel()
					.filter(item -> dRule.getCons().get(className).equals(train.get(item).get(className))).count();
			double tmp = count / (double) train.size();
			dRule.setConfidence(tmp);
			dRule.setSupport(tmp);
			dRule.setLift(1);

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
