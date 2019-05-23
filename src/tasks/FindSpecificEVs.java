package tasks;

import graph.Graph;
import io.GraphReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FindSpecificEVs {
    public static void main(String[] args) throws IOException {
        double ev = 1.25;
        double epsilon = 0.001;
        int numGraphs = 100;
        int minimumSize = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(new File("resources/communitiesWithStars.txt")))) {
            GraphReader gr = new GraphReader();
            gr.setReturnFormat(new GraphReader.List());
            gr.setInputFormat(new GraphReader.Metis());

            Graph pokec = gr.fromFile("resources/pokec.metis");

            gr.setInputFormat(new GraphReader.NodeList(pokec));
            gr.setReturnFormat(new GraphReader.Subgraph());
            String line;
            while ((line = br.readLine()) != null && numGraphs > 0) {
                Graph g = gr.fromString(line);
                if (g.getNodeCount() >= minimumSize && Math.abs(g.getEigenvalue() - ev) < epsilon) {
                    System.out.println(g);
                    numGraphs--;
                }
            }
        }
    }
}
