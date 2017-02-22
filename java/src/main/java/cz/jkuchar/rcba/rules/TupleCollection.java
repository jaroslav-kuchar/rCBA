package cz.jkuchar.rcba.rules;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jaroslav Kuchar - https://github.com/jaroslav-kuchar
 */
public class TupleCollection {
    private Map<String, List<String>> memory;

    public TupleCollection(){
        this.memory = new HashMap<>();
    }

    public void put(String key, String value){
        if(memory.containsKey(key)){
            memory.get(key).add(value);
        } else {
            memory.put(key, new ArrayList<String>(){{ add(value);}});
        }
    }

    public List<String> get(String key){
        return memory.get(key);
    }

    public int size(){
        return memory.size();
    }

    public List<String> values(){
        return memory.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<String> keys(){
        return new ArrayList<>(memory.keySet());
    }

    public boolean containsKey(String key){
        return memory.containsKey(key);
    }

    @Override
    public String toString() {
        return "TupleCollection{" +
                "memory=" + memory +
                '}';
    }
}
