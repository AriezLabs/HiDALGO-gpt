package io;

import graph.Graph;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GraphWriter {
    OutputFormat format;

    public void setFormat(OutputFormat format) {
        this.format = format;
    }

    /**
     * write g to given path.
     * @param override override file if it already exists (if false, appends date to filename)
     */
    public void toFile(Graph g, String path, boolean override) throws IOException {
        File destination = new File(path);
        if(!override && destination.exists()) {
            DateFormat df = new SimpleDateFormat("dd.MM-HH:mm:ss");
            destination = new File(path + "-" + df.format(new Date()));
        }

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(destination))) {
            format.write(g, bw);
        }
    }

    /** default to override=false */
    public void toFile(Graph g, String path) throws IOException {
        toFile(g, path, false);
    }

    private interface OutputFormat {
        /**
         * write graph g with specific output format to w
         */
        void write(Graph g, Writer w) throws IOException;
    }

    public static class Metis implements OutputFormat {
        @Override
        public void write(Graph g, Writer w) throws IOException {
            // header
            StringBuilder line = new StringBuilder().append(g.getNodeCount()).append(" ").append(g.getEdgeCount());
            w.write(line.toString());

            // write adjacency lists linewise
            for (int i = 0; i < g.getNodeCount(); i++) {
                line = new StringBuilder();
                for(int neighbor : g.getNeighbors(i))
                    line.append(neighbor).append(" ");
                w.write(line.deleteCharAt(line.length() - 1).toString());
            }
        }
    }
}
