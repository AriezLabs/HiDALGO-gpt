package tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class tests {
    public static void main(String[] args) {
        HashMap<String, Integer> asd = new HashMap<>();
        String s = new String("asd");
        asd.put(s, 21);
        asd.put(s, 22);
        Set<Integer> nodes = new HashSet<>();
        nodes.add(6);
        nodes.add(6);
        System.out.println(nodes);
    }
}
