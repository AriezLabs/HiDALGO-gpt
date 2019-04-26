package io;

import graph.AdjacencyList;
import graph.AdjacencyMatrix;
import graph.Graph;
import graph.InducedSubgraph;

import java.io.*;
import java.util.ArrayList;

public class GraphReader {
    private inputFormat inputFormat;
    private returnFormat returnFormat;

    public void setInputFormat(inputFormat format) {
        this.inputFormat = format;
    }

    public void setReturnFormat(returnFormat format) {
        this.returnFormat = format;
    }

    public Graph fromFile(String path) throws IOException, FileFormatException {
        checkFormatSet();
        try {
            return inputFormat.read(new StreamTokenizer(new BufferedReader(new FileReader(new File(path)))), returnFormat);
        } catch (FileFormatException e) {
            throw new FileFormatException(e.getMessage() + " in file " + path);
        }
    }

    public Graph fromString(String s) throws FileFormatException {
        checkFormatSet();
        try {
            return inputFormat.read(new StreamTokenizer(new StringReader(s)), returnFormat);
        } catch (IOException e) {
            throw new RuntimeException("IOException thrown while reading graph from string? [disguised as runtimeexception to avoid exception in method signature]");
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

        default int readInt(StreamTokenizer in) throws IOException {
            if(in.nextToken() == StreamTokenizer.TT_NUMBER)
                return (int) Math.round(in.nval);
            else
                throw new FileFormatException("unknown token " + in.sval);
        }
    }

    public static class Metis implements inputFormat {
        @Override
        public Graph read(StreamTokenizer in, GraphReader.returnFormat format) throws IOException {
            in.commentChar('%');
            in.parseNumbers();
            in.eolIsSignificant(true);
            int n = readInt(in);
            int e = readInt(in);
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

    public static class Subgraph implements returnFormat {

        @Override
        public Graph get(int n) {
            throw new UnsupportedOperationException("attempt to read data into induced subgraph");
        }
    }
}
