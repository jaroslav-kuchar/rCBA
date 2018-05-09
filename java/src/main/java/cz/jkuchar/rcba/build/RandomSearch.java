package cz.jkuchar.rcba.build;

import cz.jkuchar.rcba.fpg.AssociationRules;
import cz.jkuchar.rcba.fpg.FPGrowth;
import cz.jkuchar.rcba.fpg.FrequentPattern;
import cz.jkuchar.rcba.pruning.M2CBA;
import cz.jkuchar.rcba.pruning.Pruning;
import cz.jkuchar.rcba.rules.Item;
import cz.jkuchar.rcba.rules.Rule;
import cz.jkuchar.rcba.rules.RuleEngine;
import cz.jkuchar.rcba.rules.Tuple;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class RandomSearch {

    private List<List<Tuple>> train = new ArrayList<>();
    private List<Item> trainItems = new ArrayList<>();
    private List<Item> test = new ArrayList<>();

    private List<Rule> currentRules = new ArrayList<>();
    private List<Rule> bestRules = new ArrayList<>();
    private double bestAccuracy = 0.0;


    private List<Rule> cachedRules = null;
    private double cachedSupport = 1.0;
    private double cachedConfidence = 1.0;
    private int cachedLength = 1;

    public List<Rule> build(List<List<Tuple>> transactions, String consequent){
        return this.build(transactions, consequent, true);
    }

    public List<Rule> build(List<List<Tuple>> transactions, String consequent, boolean parallel){

        double minSupport = Math.random();
        double minConfidence = Math.random();
        int maxLength = (int) randomInRange(1,5);
        boolean running = true;
        int iterations = 0;
        List<Double> accuracies = new ArrayList<>();
//        int previousRulesSize = 0;
//        List<Integer> operations = Arrays.asList(1,2,3);

        // stratified split
        splitData(transactions, consequent, parallel);
        while (running){
            iterations+=1;
            // mine currentRules on train
            State state = evaluate(minSupport, minConfidence, maxLength, consequent, parallel);
            System.out.println("Final rules: "+currentRules.size());

            // compute accuracy on test
            double accuracy = computeAccuracy(currentRules);
            if(accuracy>0) {
                accuracies.add(accuracy);
            }
            System.out.println("Accuracy: "+accuracy);

            if(state.equals(State.TIMEOUT)){
                maxLength = (int) randomInRange(1,5);
                minSupport = randomInRange(minSupport, minSupport+(1.0-minSupport)/2.0);
                minConfidence = randomInRange(minConfidence, minConfidence + (1.0-minConfidence)/2.0);
            } else {
//                if(currentRules.size()==0) {
                    minSupport = randomInRange(minSupport - minSupport / 5.0, minSupport);
                    minConfidence = randomInRange(minConfidence - minConfidence / 5.0, minConfidence);
//                }
                if (bestAccuracy>0 && accuracy==bestAccuracy){
                    maxLength = maxLength < transactions.get(0).size() - 1 ? maxLength + 1 : transactions.get(0).size() - 1;
                }

            }

//            Collections.shuffle(operations);
//            switch(operations.get(0)){
//                case 1:
//                    if(state.equals(State.TIMEOUT)){
//                        minSupport = randomInRange(minSupport, minSupport+(1.0-minSupport)/5.0);
//                        maxLength = maxLength>1?maxLength-1:1;
//                    } else if(currentRules.size()==0){
//                        minSupport = randomInRange(minSupport-minSupport/5.0, minSupport);
//                    } else {
//                        minSupport = randomInRange(0.0, 1.0);
//                    }
//                    break;
//                case 2:
//                    if(state.equals(State.TIMEOUT)){
//                        minConfidence = randomInRange(minConfidence, minConfidence + (1.0-minConfidence)/5.0);
//                        maxLength = maxLength>1?maxLength-1:1;
//                    } else if(currentRules.size()==0){
//                        minConfidence = randomInRange(minConfidence-minConfidence/5.0, minConfidence);
//                    } else {
//                        minConfidence = randomInRange(0.0, 1.0);
//                    }
//                    break;
//                case 3:
//                default:
//                    if(state.equals(State.TIMEOUT)){
//                        maxLength = maxLength>1?maxLength-1:1;
//                    } else {
////                        if (currentRules.size()>0 && currentRules.size()==bestRules.size()){
//                        if (bestAccuracy>0 && accuracy==bestAccuracy){
//                            maxLength = maxLength < transactions.get(0).size() - 1 ? maxLength + 1 : transactions.get(0).size() - 1;
//                        }
//                    }
//                    break;
//            }

            if(accuracy>bestAccuracy) {
                bestRules = currentRules;
                bestAccuracy = accuracy;
            }
//            previousRulesSize = currentRules.size();

            System.out.println("Best rules size: "+bestRules.size());
            if(iterations>30 || accuracy>=1.0 || bestRules.size()>30){
                running = false;
            }
            if(bestAccuracy>0 && accuracies.size()>15 && Math.abs(accuracies.subList(accuracies.size()-14, accuracies.size()-1).stream().mapToDouble(i->i).average().orElse(0)-bestAccuracy)<0.01 ){
                running = false;
            }
        }

        return bestRules;

    }

    private State evaluate(double minSupport, double minConfidence, int maxLength, String consequent, boolean parallel){
        System.out.println("Evaluate: "+minSupport +","+ minConfidence+","+maxLength);


        if(inCache(minSupport, minConfidence, maxLength)){
            System.out.println("From cache!");
            currentRules = cachedRules.stream()
                    .filter(rule -> rule.getConfidence()>=minConfidence
                            && rule.getSupport()>=minSupport
                            && (rule.getAnt().size()+rule.getCons().size())<=maxLength)
                    .collect(Collectors.toList());
            return State.OK;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<List<Rule>> task = executorService.submit(() -> {
            FPGrowth fpGrowth = new FPGrowth();
            fpGrowth.setParallel(parallel);
            List<FrequentPattern> fps = fpGrowth.run(train, minSupport, maxLength);
            System.out.println("Frequent patterns: "+fps.size());
            List<Rule> rs = AssociationRules.generate(fps, fpGrowth, train.size(), minConfidence, consequent, parallel);
            System.out.println("Rules: "+rs.size());
            if(rs.size()>0) {
                Pruning pruning = new M2CBA();
                return pruning.prune(rs, trainItems);
            }
            return rs;
        });

        try {
            this.currentRules = task.get(10, TimeUnit.SECONDS);
            cache(minSupport, minConfidence, maxLength);
            return State.OK;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            this.currentRules.clear();
            return State.TIMEOUT;
        }

    }

    private boolean inCache(double minSupport, double minConfidence, int maxLength){
        return cachedConfidence<=minConfidence && cachedSupport<=minSupport && cachedLength>=maxLength;
    }

    private void cache(double minSupport, double minConfidence, int maxLength){
        if(cachedConfidence>=minConfidence && cachedSupport>=minSupport && cachedLength<=maxLength){
            System.out.println("Cached");
            cachedRules = currentRules;
            cachedLength = maxLength;
            cachedSupport = minSupport;
            cachedConfidence = minConfidence;
        }
    }

    private void splitData(List<List<Tuple>> transactions, String consequent, boolean parallel){

        // compute frequencies of classes
        Map<String, Integer> fr = (parallel?transactions.parallelStream():transactions.stream())
                .map(list -> list.stream().filter(t->t.getLeft().equals(consequent)).findFirst().get().getRight())
                .collect(Collectors.toMap(Function.identity(), v -> 1, Integer::sum));
//        System.out.println(fr);

        for(String key: fr.keySet()){
            List<List<Tuple>> filtered = (parallel?transactions.parallelStream():transactions.stream())
                    .filter(list -> list.stream().filter(t->t.getLeft().equals(consequent)).findFirst().get().getRight().equals(key))
                    .collect(Collectors.toList());
            Collections.shuffle(filtered);
            int cut = (int)Math.ceil(filtered.size()*0.75);
            train.addAll(filtered.subList(0,cut));
            trainItems.addAll(filtered.subList(0,cut).stream().map(l -> {
                Item i = new Item();
                l.forEach(t -> i.put(t.getLeft(), t.getRight()));
                return i;
            }).collect(Collectors.toList()));
            test.addAll(filtered.subList((cut>=filtered.size()?filtered.size()-1:cut),filtered.size()-1).stream().map(l -> {
                Item i = new Item();
                l.forEach(t -> i.put(t.getLeft(), t.getRight()));
                return i;
            }).collect(Collectors.toList()));
        }
//        System.out.println(train.size());
//        System.out.println(trainItems.size());
//        System.out.println(test.size());
    }

    private double computeAccuracy(List<Rule> rules) {
        RuleEngine re = new RuleEngine();
        re.addRules(rules);
        double accuracy = 0;
        for (int i = 0; i < test.size(); i++) {
            Rule tm = re.getTopMatch(test.get(i));
            if (tm != null && tm.getCons().values().iterator().next().equals(test.get(i).get(tm.getCons().keys().iterator().next()).iterator().next())) {
                accuracy+=1;
            }
        }
        re.clear();
        return accuracy/(double) test.size();
    }

    double randomInRange(double min, double max){
        double range = Math.abs(max - min);
        return (Math.random() * range) + (min <= max ? min : max);
    }


    private enum State {
        OK, TIMEOUT
    }

}
