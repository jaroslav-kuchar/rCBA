package cz.jkuchar.rcba.pruning;

import java.util.List;

import org.springframework.stereotype.Component;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;

@Component
public interface Pruning {

	public List<Rule> prune(List<Rule> rules, List<Item> train);

}
