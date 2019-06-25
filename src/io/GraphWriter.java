package io;

import graph.Graph;
import index.MergeCandidate;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class GraphWriter {
    OutputFormat format;

    public void setFormat(OutputFormat format) {
        this.format = format;
    }

    /**
     * write g to given path, appending file extension according to set format
     * @param override override file if it already exists (if false, appends date to filename)
     */
    public void toFile(Graph g, String path, boolean override) throws IOException {
        checkFormatSet();
        File destination = new File(path + format.getExtension());
        if(!override && destination.exists()) {
            DateFormat df = new SimpleDateFormat("dd.MM-HH:mm:ss");
            destination = new File(path + "-" + df.format(new Date()) + format.getExtension());
        }

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(destination))) {
            format.write(g, bw);
        }
    }

    /** default to override=false */
    public void toFile(Graph g, String path) throws IOException {
        toFile(g, path, false);
    }

    public String toString(Graph g) throws IOException {
        checkFormatSet();
        StringWriter sw = new StringWriter();
        format.write(g, sw);
        return sw.toString();
    }

    public void checkFormatSet() {
        if (format == null)
            throw new IllegalStateException("cannot read graph: input format is unset");
    }

    // this method is an abomination
    public String toGraphViz(MergeCandidate candidate) {
        final String colorA = "cyan3";
        final String colorB = "crimson";
        final String colorC = "darkgoldenrod";
        final String colorEdges = "darkolivegreen";
        final String overlap = "prism";
        final int penwidth = 3;

        HashSet<Integer> a = new HashSet<>(candidate.a.toNodeList());
        HashSet<Integer> b = new HashSet<>(candidate.b.toNodeList());
        HashSet<Integer> c = new HashSet<>(candidate.b.toNodeList());
        a.removeAll(b);
        c.removeAll(candidate.a.toNodeList());
        b.removeAll(c);
        Graph og = candidate.a.getOriginalGraph();

        StringBuilder sb = new StringBuilder(String.format("strict graph {\n\tedge [penwidth=%d]\n\tnode [style=filled]\n\toverlap=\"%s\"\n\toutputorder=\"edgesfirst\"\n", penwidth, overlap));

        for (int node : b) {
            sb.append(String.format("\t%d [color=%s]\n", node, colorB));
            for (int neighbor : a)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorA));
            for (int neighbor : b)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorB));
            for (int neighbor : c)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorC));
        }

        for (int node : a) {
            sb.append(String.format("\t%d [color=%s]\n", node, colorA));
            for (int neighbor : a)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorA));
            for (int neighbor : b)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorA));
            for (int neighbor : c)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorEdges));
        }

        for (int node : c) {
            sb.append(String.format("\t%d [color=%s]\n", node, colorC));
            for (int neighbor : a)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorEdges));
            for (int neighbor : b)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorC));
            for (int neighbor : c)
                if (og.hasEdge(node, neighbor))
                    sb.append(String.format("\t%d -- %d [color=%s]\n", node, neighbor, colorC));
        }

        return sb.append("}").toString();
    }

    private interface OutputFormat {
        /**
         * write graph g with specific output format to w
         */
        void write(Graph g, Writer w) throws IOException;

        /** name of format (for filename extension) */
        String getExtension();
    }

    public static class Metis implements OutputFormat {
        @Override
        public void write(Graph g, Writer w) throws IOException {
            int e = g.getUndirectedEdgeCount();

            // header
            StringBuilder line = new StringBuilder().append(g.getNodeCount()).append(" ").append(e);
            w.write(line.toString());
            w.write("\n");

            // write adjacency lists linewise
            for (int i = 0; i < g.getNodeCount(); i++) {
                line = new StringBuilder();
                for(int neighbor : g.getNeighbors(i))
                    line.append(neighbor).append(" ");
                if(line.length() > 0)
                    line.deleteCharAt(line.length() - 1);
                w.write(line.toString());
                w.write("\n");
            }
        }

        @Override
        public String getExtension() {
            return ".metis";
        }
    }
}
