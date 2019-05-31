package io;

import graph.AdjacencyList;
import graph.AdjacencyMatrix;
import graph.Graph;
import graph.InducedSubgraph;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class GraphReader {
    private inputFormat inputFormat;
    private returnFormat returnFormat;

    public void setInputFormat(inputFormat format) {
        this.inputFormat = format;
    }

    public void setReturnFormat(returnFormat format) {
        this.returnFormat = format;
    }

    /**
     * aggregate operation: attempt to read all files in directory
     * @param path to directory to read graphs from
     * @return array of graphs in directory
     */
    public Graph[] fromDirectory(String path) throws IOException {
        return fromDirectory(new File(path));
    }

    public Graph[] fromDirectory(File directory) throws IOException {
        checkFormatSet();
        if(!directory.isDirectory())
            throw new IllegalArgumentException(directory.getName() + " is not a directory");
        File[] files = (File[]) Arrays.stream(directory.listFiles()).filter(File::isFile).toArray();
        Graph[] graphs = new Graph[files.length];
        for (int i = 0; i < files.length; i++) {
            graphs[i] = fromFile(files[i]);
        }
        return graphs;
    }

    /**
     * @param path to file to read graph from
     * @return graph named after file (without filename extension)
     */
    public Graph fromFile(String path) throws IOException{
        return fromFile(new File(path));
    }

    /**
     * @param f file to read graph from
     * @return graph named after f (without filename extension)
     */
    public Graph fromFile(File f) throws IOException {
        checkFormatSet();
        try (BufferedReader br = new BufferedReader(new FileReader(f))){
            Graph g = inputFormat.read(new StreamTokenizer(br), returnFormat);
            g.setName(f.getName().split("\\.")[0]);
            return g;
        } catch (FileFormatException e) {
            throw new FileFormatException(e.getMessage() + " in file " + f.getName());
        }
    }

    public Graph fromString(String s) throws IOException {
        checkFormatSet();
        try {
            return inputFormat.read(new StreamTokenizer(new StringReader(s)), returnFormat);
        } catch (FileFormatException e) {
            throw new FileFormatException(e.getMessage() + " in string " + s);
        }
    }

    public void checkFormatSet() {
        if (inputFormat == null)
            throw new IllegalStateException("cannot read graph: input format is unset");
        if (returnFormat == null)
            throw new IllegalStateException("cannot read graph: return format is unset");
    }



    /*****************
     * INPUT FORMATS *
     *****************/

    private interface inputFormat {
        /**
         * @param in input stream tokenizer
         * @param format format in which to return
         */
        Graph read(StreamTokenizer in, returnFormat format) throws IOException, FileFormatException;

        /**
         * @return EBNF string specifying input format exactly
         */
        String getEBNF();

        /** convenience method for reading next number and rounding to int */
        default int readInt(StreamTokenizer in) throws IOException {
            if(in.ttype == StreamTokenizer.TT_NUMBER)
                return (int) Math.round(in.nval);
            else
                throw new FileFormatException("unknown token " + in.sval + " of type " + in.ttype);
        }
    }

    public static class Metis implements inputFormat {
        @Override
        public Graph read(StreamTokenizer in, GraphReader.returnFormat format) throws IOException {
            in.commentChar('%');
            in.parseNumbers();
            in.nextToken();
            int n = readInt(in);
            in.nextToken();
            int e = readInt(in);
            in.eolIsSignificant(true);
            int eVerify = 0;

            while (in.nextToken() != StreamTokenizer.TT_EOL)
                if(in.ttype == StreamTokenizer.TT_NUMBER)
                    System.err.println("skipping metis header info: " + in.nval);
                else
                    System.err.println("skipping metis header info: " + in.sval);

            Graph g = format.get(n);

            int i = 0;
            while (in.nextToken() != StreamTokenizer.TT_EOF)
                if (in.ttype == StreamTokenizer.TT_NUMBER) {
                    eVerify++;
                    g.addEdge(i, (int) Math.round(in.nval));
                } else if(in.ttype == StreamTokenizer.TT_EOL)
                    i++;
                else
                    throw new FileFormatException("unknown token " + in.sval);

            if(eVerify % 2 != 0)
                throw new FileFormatException("uneven edge count");
            if(eVerify / 2 != e)
                throw new FileFormatException("actual edge count differs from header");
            if(i < n)
                throw new FileFormatException("number of lines is less than number of nodes in header");

            return g;
        }

        @Override
        public String getEBNF() {
            return  "comment = '%' string '\\n' .\n" +
                    "header = n e '\\n' .\n" +
                    "neighbors = { { integer } '\\n' } .\n" +
                    "metis = header { neighbors } .";
        }
    }

    public static class NodeList implements inputFormat {
        private Graph main;

        public NodeList(Graph main) {
            this.main = main;
        }

        @Override
        public Graph read(StreamTokenizer in, returnFormat format) throws IOException {
            // first, read into induced subgraph object; "translate" to returnFormat representation later
            ArrayList<Integer> nodes = new ArrayList<>();

            // setup tokenizer
            in.commentChar('%');
            in.parseNumbers();

            while (in.nextToken() != StreamTokenizer.TT_EOF)
                nodes.add(readInt(in));

            InducedSubgraph sg = new InducedSubgraph(main, nodes);

            // if Graph object returned by format is InducedSubgraph, attempting to add an edge will throw UnsupportedOperationException
            // in this case, we just return sg instead of "translating" to some other Graph subclass
            try {
                Graph ret = format.get(sg.getNodeCount());
                for (int i = 0; i < sg.getNodeCount(); i++) {
                    for (int j = 0; j < sg.getNodeCount(); j++) {
                        if(sg.hasEdge(i, j))
                            ret.addEdge(i, j);
                    }
                }

                return ret;
            } catch (UnsupportedOperationException e) {
                return sg;
            }
        }

        @Override
        public String getEBNF() {
            return  "comment = '%' string '\\n' .\n" +
                    "nodeList = { integer } .";
        }
    }

    public static class NodeListWithEvs implements inputFormat {
        NodeList nl;

        public NodeListWithEvs(Graph main) {
            this.nl = new NodeList(main);
        }

        @Override
        public Graph read(StreamTokenizer in, GraphReader.returnFormat format) throws IOException, FileFormatException {
            in.commentChar('%');
            in.parseNumbers();
            assert in.nextToken() == StreamTokenizer.TT_NUMBER : "first token in nodelist with eigenvalue is not a number";
            double ev = in.nval;
            Graph g = nl.read(in, format);
            g.setEigenvalue(ev);
            return g;
        }

        @Override
        public String getEBNF() {
            return  "comment = '%' string '\\n' .\n" +
                    "nodeListWithEv = eigenvalue { integer } .";
        }
    }



    /******************
     * OUTPUT FORMATS *
     ******************/

    private interface returnFormat {
        Graph get(int n);
    }

    public static class List implements returnFormat {
        @Override
        public Graph get(int n) {
            return new AdjacencyList(n);
        }
    }

    public static class Matrix implements returnFormat {
        @Override
        public Graph get(int n) {
            return new AdjacencyMatrix(n);
        }
    }

    /** special case: can only be read from NodeList currently */
    public static class Subgraph implements returnFormat {
        @Override
        public Graph get(int n) {
            throw new UnsupportedOperationException("attempt to read data into induced subgraph");
        }
    }
}
