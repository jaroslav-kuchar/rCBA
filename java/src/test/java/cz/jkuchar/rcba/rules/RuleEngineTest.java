
package cz.jkuchar.rcba.rules;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cz.jkuchar.rcba.TestConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={TestConfiguration.class})
public class RuleEngineTest {
	
	@Autowired
	RuleEngine re;
	
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
		Assert.assertEquals(result.getCons().get("y"),"1");
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
	
}
