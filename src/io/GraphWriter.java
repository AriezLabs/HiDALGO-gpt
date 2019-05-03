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
