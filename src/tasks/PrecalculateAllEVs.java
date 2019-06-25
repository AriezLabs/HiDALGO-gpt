package tasks;

import graph.Graph;
import graph.InducedSubgraph;
import io.GraphReader;

import java.io.*;

public class PrecalculateAllEVs {
    public static void main(String[] args) throws IOException {
        // look for these files in resources folder, give filenames without .txt ending!
        String[] files = {"NCe1n0", "NCe2n0"};

        for (String file : files)
            try(BufferedReader br = new BufferedReader(new FileReader(new File("resources/" + file + ".txt")));
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File("resources/" + file + "WithEvs.txt")))) {
                GraphReader gr = new GraphReader();
                gr.setInputFormat(new GraphReader.Metis());
                gr.setReturnFormat(new GraphReader.List());
                Graph pokec = gr.fromFile("resources/pokec.metis");

                gr.setReturnFormat(new GraphReader.Subgraph());
                gr.setInputFormat(new GraphReader.NodeList(pokec));
                InducedSubgraph subg;
                double ev;

                String line;
                while ((line = br.readLine()) != null) {
                    subg = (InducedSubgraph) gr.fromString(line);
                    ev = subg.getEigenvalue();
                    bw.write(ev + "");
                    for(int i : subg.toNodeList())
                        bw.write(" " + i);
                    bw.write("\n");
                }
            }
    }
}
