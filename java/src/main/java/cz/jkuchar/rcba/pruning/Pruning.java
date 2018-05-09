package cz.jkuchar.rcba.pruning;

import java.util.List;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;


public interface Pruning {

	public List<Rule> prune(List<Rule> rules, List<Item> train);

	public List<Rule> prune(List<Rule> rules, List<Item> train, boolean parallel);

}
