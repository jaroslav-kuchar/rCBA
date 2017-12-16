package cz.jkuchar.rcba.fpg;

import cz.jkuchar.rcba.rules.Tuple;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class FPGrowthSPPTest {

    @Test
    public void singlePrefixPath(){

        List<List<Tuple>> transactions = new ArrayList<>();
        List<Tuple> sub = null;

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",1));
        sub.add(new Tuple("b", "1",1));
        sub.add(new Tuple("c", "1",1));
        sub.add(new Tuple("d", "1",1));
        sub.add(new Tuple("e", "1",1));
        transactions.add(sub);

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",3));
        sub.add(new Tuple("b", "1",3));
        sub.add(new Tuple("c", "1",3));
        sub.add(new Tuple("d", "1",3));
        sub.add(new Tuple("f", "1",3));
        transactions.add(sub);

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",6));
        sub.add(new Tuple("b", "1",4));
        sub.add(new Tuple("c", "1",3));
        sub.add(new Tuple("e", "1",2));
        transactions.add(sub);

        FPGrowth fpg = new FPGrowth();
        List<FrequentPattern> fps = fpg.run(transactions, 0.0001, 10);
//        System.out.println("----- final -----");
//        System.out.println(fps.size());
//        System.out.println(fps);
        Assert.assertEquals(47, fps.size());

    }



    @Test
    public void singlePrefixPath2(){

        List<List<Tuple>> transactions = new ArrayList<>();
        List<Tuple> sub = null;

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",1));
        sub.add(new Tuple("b", "1",1));
        transactions.add(sub);

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",1));
        sub.add(new Tuple("c", "1",1));
        transactions.add(sub);

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",1));
        sub.add(new Tuple("d", "1",1));
        sub.add(new Tuple("e", "1",1));
        transactions.add(sub);

        sub =new ArrayList<>();
        sub.add(new Tuple("a", "1",1));
        sub.add(new Tuple("d", "1",1));
        sub.add(new Tuple("f", "1",1));
        sub.add(new Tuple("g", "1",1));
        sub.add(new Tuple("h", "1",1));
        transactions.add(sub);

        FPGrowth fpg = new FPGrowth();
        List<FrequentPattern> fps = fpg.run(transactions, 0.0001, 10);
//        System.out.println("----- final -----");
//        System.out.println(fps.size());
//        System.out.println(fps);
        Assert.assertEquals(39, fps.size());

    }
}
