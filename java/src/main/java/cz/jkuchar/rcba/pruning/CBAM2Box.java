package cz.jkuchar.rcba.pruning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CBAM2Box implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected List<CBAM2BoxASet> A = new ArrayList<CBAM2BoxASet>();
	protected Map<Integer, Map<String, Integer>> dClasses = new HashMap<Integer, Map<String, Integer>>();
	protected Set<Integer> crules = new TreeSet<Integer>();
	protected Set<Integer> marked = new TreeSet<Integer>();
	protected Map<Integer,List<CBAM2BoxASet>> replaces = new HashMap<Integer, List<CBAM2BoxASet>>();

	protected void initDClass(int cRule, String dClass) {
		dClasses.put(cRule, new HashMap<String, Integer>() {
			{
				put(dClass, 1);
			}
		});
	}
	
	protected void mergeDClasses(Map<Integer, Map<String, Integer>> input){
		input.keySet().stream().forEach(cRule -> {
			if(dClasses.containsKey(cRule)){
				input.get(cRule).keySet().stream().forEach(dClass -> {
					if(dClasses.get(cRule).containsKey(dClass)){
						dClasses.get(cRule).put(dClass, dClasses.get(cRule).get(dClass)+input.get(cRule).get(dClass));
					} else {
						dClasses.get(cRule).put(dClass, input.get(cRule).get(dClass));
					}
				});
			} else {
				dClasses.put(cRule, input.get(cRule));
			}
		});
	}
	
	protected void update(int rule, String dclass, int update){
		if(dClasses.containsKey(rule)){
			if(dClasses.get(rule).containsKey(dclass)){
				dClasses.get(rule).put(dclass, update+dClasses.get(rule).get(dclass));
			} else {
				dClasses.get(rule).put(dclass, 0+update);
			}
		} else {
			dClasses.put(rule, new HashMap<String, Integer>(){{put(dclass, 0+update);}});
		}
	}
	
	protected void updateReplaces(int rule, CBAM2BoxASet aset){
		if(replaces.containsKey(rule)){
			replaces.get(rule).add(aset);
		} else {
			replaces.put(rule, new LinkedList<CBAM2BoxASet>(){{add(aset);}});
		}
	}

}
