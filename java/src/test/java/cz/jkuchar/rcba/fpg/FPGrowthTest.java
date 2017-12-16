package cz.jkuchar.rcba.fpg;

import com.opencsv.CSVReader;
import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class FPGrowthTest {

    List<Item> train;

    public void loadTrain() throws IOException {
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

    public void loadIris() throws IOException {
        train = new ArrayList<Item>();
        CSVReader reader = new CSVReader(new FileReader(this.getClass().getResource("/iris.csv").getFile()),',');
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

    public void loadAudioLogy() throws IOException {
        train = new ArrayList<Item>();
        CSVReader reader = new CSVReader(new FileReader(this.getClass().getResource("/audiology0.csv").getFile()),',');
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
    public void audiologyTest() throws IOException{
        loadAudioLogy();
        FPGrowth fpGrowth = new FPGrowth();
        List<List<Tuple>> t = train.stream().map(item -> {
            List<Tuple> tuples = new ArrayList<>();
            for(String key:item.keys()){
                for(String val:item.get(key)){
                    tuples.add(new Tuple(key,val));
                }
            }
            return tuples;
        }).collect(Collectors.toList());
        List<FrequentPattern> fps = null;
        fps = fpGrowth.run(t, 0.1, 2);
//        System.out.println(fps);
        Assert.assertEquals(40, fps.size());
    }


    @Test
    public void irisTest() throws IOException{
        loadIris();
        FPGrowth fpGrowth = new FPGrowth();
        List<List<Tuple>> t = train.stream().map(item -> {
            List<Tuple> tuples = new ArrayList<>();
            for(String key:item.keys()){
                for(String val:item.get(key)){
                    tuples.add(new Tuple(key,val));
                }
            }
            return tuples;
        }).collect(Collectors.toList());
        List<FrequentPattern> fps = null;
        fps = fpGrowth.run(t, 0.05, 2);
        System.out.println(fps);
        Assert.assertEquals(40, fps.size());
    }

    @Test
    public void zooTest() throws IOException{
        loadTrain();
        FPGrowth fpGrowth = new FPGrowth();
        List<List<Tuple>> t = train.stream().map(item -> {
            List<Tuple> tuples = new ArrayList<>();
            for(String key:item.keys()){
                for(String val:item.get(key)){
                    tuples.add(new Tuple(key,val));
                }
            }
            return tuples;
        }).collect(Collectors.toList());
        List<FrequentPattern> fps = null;
        fps = fpGrowth.run(t, 0.7, 3);
        Assert.assertEquals(23, fps.size());
        fps = fpGrowth.run(t, 0.5, 3);
        fps = fps.stream().
                sorted(
                        Comparator.comparing(fp -> fp.getPattern().toString())

                )
                .collect(Collectors.toList());
        System.out.println(fps);
        Assert.assertEquals(144, fps.size());
        fps = fpGrowth.run(t, 0.001, 3);
        Assert.assertEquals(6505, fps.size());
        fps = fpGrowth.run(t, 0.001, 5);
        Assert.assertEquals(138326, fps.size());
        fps = fpGrowth.run(t, 0.1, 50);
        Assert.assertEquals(240865, fps.size());
    }

    @Test
    public void averageTimeOfZoo() throws IOException{

        List<Double> times = new LinkedList<>();

        for (int i = 0; i < 30; i++) {
            loadTrain();
            long startTime = System.nanoTime();
            FPGrowth fpGrowth = new FPGrowth();
            List<List<Tuple>> t = train.stream().map(item -> {
                List<Tuple> tuples = new ArrayList<>();
                for(String key:item.keys()){
                    for(String val:item.get(key)){
                        tuples.add(new Tuple(key,val));
                    }
                }
                return tuples;
            }).collect(Collectors.toList());
            List<FrequentPattern> fps = fpGrowth.run(t, 0.001, 3);
            System.out.println("FP: "+fps.size());
            System.out.println("FPG: "+(System.nanoTime()-startTime)/1000000+" ms");
            times.add((double) ((System.nanoTime()-startTime)/1000000));
        }
        System.out.println(times.stream().mapToDouble(a -> a).average().getAsDouble());

    }

}
