package cz.jkuchar.rcba.pruning;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.opencsv.CSVReader;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;

public class PruningTestZoo {
	List<Rule> rules;
	List<Item> train;
	 
	RuleEngine re;
		
	DCBrCBA dcp = new DCBrCBA();
	M1CBA m1p = new M1CBA();
	M2CBA m2p = new M2CBA();
	
	@Before
	public void loadRules() throws IOException{
		rules = new ArrayList<Rule>();
		CSVReader reader = new CSVReader(new FileReader(this.getClass().getResource("/zoorules.csv").getFile()));
	     String [] nextLine;
	     reader.readNext();
	     while ((nextLine = reader.readNext()) != null) {
	        rules.add(Rule.buildRule(nextLine[0], Double.valueOf(nextLine[2]), Double.valueOf(nextLine[1])));
	     }
	     reader.close();		
	}
	
	@Before
	public void loadTrain() throws IOException{
		train = new ArrayList<Item>();
		CSVReader reader = new CSVReader(new FileReader(this.getClass().getResource("/zoo.csv").getFile()),',');
	     String [] nextLine;
	     String[] cnames = reader.readNext(); 
	     int counter = 0;
	     while ((nextLine = reader.readNext()) != null) {
	        Item item = new Item(counter);	        
	        for(int i=0;i<nextLine.length;i++){
	        	item.put(cnames[i], nextLine[i]);
	        }
	        train.add(item);
	        counter++;
	     }
	     reader.close();
	}
	
	@Test
	public void m2Pruning() throws IOException{
		System.out.println("Train data: "+train.size());
		int pre = rules.size();
		System.out.println(pre);

		List<Double> times = new LinkedList<>();

		for (int i = 0; i < 10; i++) {
			loadRules();
			loadTrain();
			long startTime = System.nanoTime();
			List<Rule> after = m2p.prune(rules, train);
			System.out.println("Pruning: "+(System.nanoTime()-startTime)/1000000+" ms");
			times.add((double) ((System.nanoTime()-startTime)/1000000));
			System.out.println(after.size());
			Assert.assertTrue(after.size()>0 && after.size()<pre);
			Assert.assertEquals(8, after.size());
		}		
		System.out.println(times.stream().mapToDouble(a -> a).average().getAsDouble());

	}

}
