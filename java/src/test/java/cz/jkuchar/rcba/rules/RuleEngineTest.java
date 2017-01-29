
package cz.jkuchar.rcba.rules;

import org.junit.Assert;
import org.junit.Test;

public class RuleEngineTest {
	
	RuleEngine re = new RuleEngine();
	
	@Test
	public void emptyAtInit(){
		Assert.assertEquals(0,re.getMemoryLength());
	}
	
	@Test
	public void lenAfterAdd(){
		int before = re.getMemoryLength();
		re.add(Rule.buildRule("{}=>{y=1}", 0.0, 0.0));
		Assert.assertEquals(before+1,re.getMemoryLength());
		re.clear();
	}
	
	@Test
	public void lenAfterReset(){
		re.add(Rule.buildRule("{}=>{y=1}", 0.0, 0.0));
		re.clear();
		Assert.assertEquals(0,re.getMemoryLength());
	}
	
	@Test
	public void topMatch(){
		re.clear();
		re.add(Rule.buildRule("{}=>{y=1}", 0.0, 0.0));
		Item item = new Item(123);
		item.put("a", "b");
		Rule result = re.getTopMatch(item);
		Assert.assertNotNull(result);
		Assert.assertEquals(result.getCons().get("y").get(0),"1");
	}
	
	@Test
	public void match1(){
		Item item = new Item(123);
		item.put("a", "b");
		Assert.assertTrue(re.matchRule(Rule.buildRule("{}=>{y=1}", 0.0, 0.0), item));
	}
	
	@Test
	public void match2(){
		Item item = new Item(123);
		item.put("a", "b");
		Assert.assertFalse(re.matchRule(Rule.buildRule("{a=c}=>{y=1}", 0.0, 0.0), item));
	}
	
	@Test
	public void match3(){
		Item item = new Item(123);
		item.put("q", "1");
		Assert.assertTrue(re.matchRule(Rule.buildRule("{q=1}=>{y=1}", 0.0, 0.0), item));
	}

	@Test
	public void match4(){
		Item item = new Item(123);
		item.put("q", "1");
		item.put("q", "2");
		Assert.assertTrue(re.matchRule(Rule.buildRule("{q=2}=>{y=1}", 0.0, 0.0), item));
	}

	@Test
	public void match7(){
		Item item = new Item(123);
		item.put("q", "1");
		item.put("q", "2");
		Assert.assertTrue(re.matchRule(Rule.buildRule("{q=1}=>{y=1}", 0.0, 0.0), item));
	}

	@Test
	public void match8(){
		Item item = new Item(123);
		item.put("q", "1");
		Assert.assertFalse(re.matchRule(Rule.buildRule("{q=1,q=2}=>{y=1}", 0.0, 0.0), item));
	}

	@Test
	public void match5(){
		Item item = new Item(123);
		item.put("q", "1");
		item.put("q", "2");
		Assert.assertFalse(re.matchRule(Rule.buildRule("{q=3}=>{y=1}", 0.0, 0.0), item));
	}

	@Test
	public void match6(){
		Item item = new Item(123);
		item.put("q", "1");
		item.put("q", "2");
		Assert.assertFalse(re.matchRule(Rule.buildRule("{q=1,q=3}=>{y=1}", 0.0, 0.0), item));
	}
	
}
