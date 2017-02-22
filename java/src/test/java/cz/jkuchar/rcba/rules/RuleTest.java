package cz.jkuchar.rcba.rules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

public class RuleTest {

	Logger logger = Logger.getLogger(RuleTest.class.getCanonicalName());

	@Test(expected = BadRuleFormatException.class)
	public void formating1() {
		Rule.buildRule("{}{a=3}", 0.0, 0.0);
	}

	@Test(expected = BadRuleFormatException.class)
	public void formating2() {
		Rule.buildRule("{a=3}=> {}", 0.0, 0.0);
	}

	@Test(expected = BadRuleFormatException.class)
	public void formating3() {
		Rule.buildRule("{a=1} > {b=2}", 0.0, 0.0);
	}

	@Test(expected = BadRuleFormatException.class)
	public void formating4() {
		Rule.buildRule("{a} => {b=2}", 0.0, 0.0);
	}

	@Test(expected = BadRuleFormatException.class)
	public void formating5() {
		Rule.buildRule("{a-1} => {b}", 0.0, 0.0);
	}

	@Test(expected = BadRuleFormatException.class)
	public void emptyRule() {
		Rule r = Rule.buildRule("{} => {}", 0.0, 0.0);
	}
	
	@Test
	public void emptyValue() {
		Rule r = Rule.buildRule("{a=} => {b=2}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 1);
		Assert.assertEquals(r.getCons().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().get("a").contains(""));
		Assert.assertTrue(r.getCons().containsKey("b"));
		Assert.assertTrue(r.getCons().get("b").contains("2"));
	}
	
	@Test
	public void nullValue() {
		Rule r = Rule.buildRule("{a=NULL} => {b=2}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 1);
		Assert.assertEquals(r.getCons().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().get("a").contains("NULL"));
		Assert.assertTrue(r.getCons().containsKey("b"));
		Assert.assertTrue(r.getCons().get("b").contains("2"));
	}

	@Test
	public void oneItem() {
		Rule r = Rule.buildRule("{a=1} => {b=2}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 1);
		Assert.assertEquals(r.getCons().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getCons().containsKey("b"));
		Assert.assertTrue(r.getCons().get("b").contains("2"));
	}
	
	@Test
	public void charInValue() {
		Rule r = Rule.buildRule("{a=>1} => {b=2}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 1);
		Assert.assertEquals(r.getCons().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().get("a").contains(">1"));
		Assert.assertTrue(r.getCons().containsKey("b"));
		Assert.assertTrue(r.getCons().get("b").contains("2"));
	}

	@Test
	public void multipleItems() {
		Rule r = Rule.buildRule("{a=1,b=2,c=3} => {d=4}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 3);
		Assert.assertEquals(r.getCons().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("b"));
		Assert.assertTrue(r.getAnt().containsKey("c"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getAnt().get("b").contains("2"));
		Assert.assertTrue(r.getAnt().get("c").contains("3"));
		Assert.assertTrue(r.getCons().containsKey("d"));
		Assert.assertTrue(r.getCons().get("d").contains("4"));
	}

	@Test
	public void valueWhiteSpaces() {
		Rule r = Rule.buildRule("{a=1,b=2 5,c=3} => {d=4}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 3);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("b"));
		Assert.assertTrue(r.getAnt().containsKey("c"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getAnt().get("b").contains("2 5"));
		Assert.assertTrue(r.getAnt().get("c").contains("3"));
	}

	@Test
	public void columnWhiteSpaces() {
		Rule r = Rule.buildRule("{a=1,a b c =2,c=3} => {d=4}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 3);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("a b c "));
		Assert.assertTrue(r.getAnt().containsKey("c"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getAnt().get("a b c ").contains("2"));
		Assert.assertTrue(r.getAnt().get("c").contains("3"));
	}

	@Test
	public void valueDiacritic() {
		Rule r = Rule.buildRule("{a=1,b=_ěščř_ýáí_éůú,c=3} => {d=4}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 3);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("b"));
		Assert.assertTrue(r.getAnt().containsKey("c"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getAnt().get("b").contains("_ěščř_ýáí_éůú"));
		Assert.assertTrue(r.getAnt().get("c").contains("3"));
	}

	@Test
	public void columnDiacritic() {
		Rule r = Rule.buildRule("{a=1,b=2,cžšč__=3} => {d=4}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 3);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("b"));
		Assert.assertTrue(r.getAnt().containsKey("cžšč__"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getAnt().get("b").contains("2"));
		Assert.assertTrue(r.getAnt().get("cžšč__").contains("3"));
	}

	@Test
	public void meta1() {
		Map<String, Set<String>> meta = new HashMap<String, Set<String>>() {
			{
				put("a", new HashSet<String>(Arrays.asList("b,a")));
				put("c", new HashSet<String>(Arrays.asList("5")));
				put("d", new HashSet<String>(Arrays.asList("10")));
			}
		};
		Rule r = Rule.buildRule("{a=b,a,c=5} => {d=10}", meta, 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 2);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().get("a").contains("b,a"));
		Assert.assertTrue(r.getAnt().containsKey("c"));
		Assert.assertTrue(r.getAnt().get("c").contains("5"));
	}

	@Test
	public void meta2() {
		Map<String, Set<String>> meta = new HashMap<String, Set<String>>() {
			{
				put("a=c", new HashSet<String>(Arrays.asList("b=d")));
				put("q", new HashSet<String>(Arrays.asList("3")));
				put("d", new HashSet<String>(Arrays.asList("10")));
			}
		};
		Rule r = Rule.buildRule("{q=3,a=c=b=d} => {d=10}", meta, 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 2);
		Assert.assertTrue(r.getAnt().containsKey("a=c"));
		Assert.assertTrue(r.getAnt().get("a=c").contains("b=d"));
		Assert.assertTrue(r.getAnt().containsKey("q"));
		Assert.assertTrue(r.getAnt().get("q").contains("3"));
	}

	@Test
	public void meta3() {
		Map<String, Set<String>> meta = new HashMap<String, Set<String>>() {
			{
				put("a,c", new HashSet<String>(Arrays.asList("b=d")));
				put("i", new HashSet<String>(Arrays.asList("5")));
				put("d", new HashSet<String>(Arrays.asList("10")));
			}
		};
		Rule r = Rule.buildRule("{a,c=b=d,i=5} => {d=10}", meta, 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 2);
		Assert.assertTrue(r.getAnt().containsKey("a,c"));
		Assert.assertTrue(r.getAnt().get("a,c").contains("b=d"));
		Assert.assertTrue(r.getAnt().containsKey("i"));
		Assert.assertTrue(r.getAnt().get("i").contains("5"));
	}
	
	@Test
	public void meta4() {
		Map<String, Set<String>> meta = new HashMap<String, Set<String>>() {
			{
				put("age=20,age=30", new HashSet<String>(Arrays.asList("age<20", "age<20,age>20")));
				put("age", new HashSet<String>(Arrays.asList("5")));
			}
		};
		Rule r = Rule.buildRule("{age=5} => {age=20,age=30=age<20,age>20}", meta, 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("age"));
		Assert.assertTrue(r.getAnt().get("age").contains("5"));
		Assert.assertTrue(r.getCons().containsKey("age=20,age=30"));
		Assert.assertTrue(r.getCons().get("age=20,age=30").contains("age<20,age>20"));
	}

	@Test
	public void multipleSameKeys() {
		Rule r = Rule.buildRule("{a=1,a=2,c=3} => {d=4}", 0.0, 0.0);
		Assert.assertEquals(r.getAnt().size(), 2);
		Assert.assertEquals(r.getCons().size(), 1);
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("a"));
		Assert.assertTrue(r.getAnt().containsKey("c"));
		Assert.assertTrue(r.getAnt().get("a").contains("1"));
		Assert.assertTrue(r.getAnt().get("a").contains("2"));
		Assert.assertTrue(r.getAnt().get("c").contains("3"));
		Assert.assertTrue(r.getCons().containsKey("d"));
		Assert.assertTrue(r.getCons().get("d").contains("4"));
	}

}
