package cz.jkuchar.rcba.pruning;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.opencsv.CSVReader;

import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;

// https://netbeans.org/kb/docs/java/profiler-intro.html

public class PruningTest {
	List<Rule> rules;
	List<Item> train;
	List<Item> test;
	 
	RuleEngine re;
	
	DCBrCBA dcp = new DCBrCBA();
	
	M1CBA m1p = new M1CBA();
	
	M2CBA m2p = new M2CBA();
	
	@Before
	public void loadRules() throws IOException{
		rules = new ArrayList<Rule>();
		CSVReader reader = new CSVReader(new FileReader(this.getClass().getResource("/rules.csv").getFile()));
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
		CSVReader reader = new CSVReader(new FileReader(this.getClass().getResource("/train.csv").getFile()),';');
	     String [] nextLine;
	     String[] cnames = reader.readNext(); 
		 for(int i=1;i<cnames.length-1;i++){
        	cnames[i] = "c"+String.valueOf(i-1);
        }
	     int counter = 0;
	     while ((nextLine = reader.readNext()) != null) {
	        Item item = new Item(counter);	        
	        for(int i=0;i<nextLine.length;i++){
	        	item.put(cnames[i], nextLine[i]);
	        }
	        item.put("y", nextLine[nextLine.length-1]);
	        train.add(item);
	        counter++;
	     }
	     reader.close();
	}
	
//	@Ignore
	@Test
	public void m2Pruning(){
		int pre = rules.size();
		System.out.println(pre);
		long startTime = System.nanoTime();
		List<Rule> after = m2p.prune(rules, train);
		System.out.println("Pruning: "+(System.nanoTime()-startTime)/1000000+" ms");
		System.out.println(after.size());
		Assert.assertTrue(after.size()>0 && after.size()<pre);
		Assert.assertEquals(189, after.size());
	}

}
