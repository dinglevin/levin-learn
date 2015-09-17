/* 
 * Copyright (c) 2001 by Matt Welsh and The Regents of the University of 
 * California. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice and the following
 * two paragraphs appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Author: Matt Welsh <mdw@cs.berkeley.edu>
 * 
 */

package seda.sandstorm.internal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import seda.sandstorm.api.Manager;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.main.SandstormConfig;

/**
 * This class provides an interface allowing operations to be performed on the
 * graph of stages within the application. Used internally (for example, by
 * AggThrottle) to determine stage connectivity and communication statistics.
 * Gathers data from sources such as SinkProxy.
 *
 * @author Matt Welsh
 * @see AggThrottle
 * @see SinkProxy
 */
public class StageGraph {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageGraph.class);

    private List<StageWrapper> stages = Lists.newArrayList();
    private List<StageGraphEdge> edges = Lists.newArrayList();
    private Map<Thread, StageWrapper> threads = Maps.newHashMap();
    private Map<StageWrapper, StageList> edgesFrom = Maps.newHashMap();
    private PrintWriter graphpw = null;

    StageGraph(Manager mgr) {
        SandstormConfig config = mgr.getConfig();
        boolean dumpModuleGraph = config.getBoolean("global.profile.graph");
        if (dumpModuleGraph) {
            String gfilename = config.getString("global.profile.graphfilename");
            try {
                graphpw = new PrintWriter(new FileWriter(gfilename, true));
            } catch (IOException e) {
                LOGGER.error("StageGraph: Warning: Could not open file "
                        + gfilename + " for writing, disabling graph dump.");
            }
        }
    }

    public synchronized StageWrapper[] getStages() {
        return stages.toArray(new StageWrapper[stages.size()]);
    }

    public synchronized StageGraphEdge[] getEdges() {
        return edges.toArray(new StageGraphEdge[edges.size()]);
    }

    public synchronized StageGraphEdge[] getEdgesFromStage(StageWrapper fromStage) {
        StageList list = (StageList) edgesFrom.get(fromStage);
        if (list == null)
            return null;
        else
            return list.getEdges();
    }

    public synchronized StageWrapper getStageFromThread(Thread thread) {
        return threads.get(thread);
    }

    public synchronized void addStage(StageWrapper stage) {
        LOGGER.info("StageGraph: Adding stage " + stage);
        
        if (!stages.contains(stage)) {
            stages.add(stage);
        }
    }

    public synchronized void addThread(Thread thread, StageWrapper stage) {
        LOGGER.info("StageGraph: Adding thread " + thread + " -> stage " + stage);
        addStage(stage);
        threads.put(thread, stage);
    }

    public synchronized void addEdge(StageGraphEdge edge) {
        if (!edges.contains(edge)) {
            if ((edge.fromStage == null) || (edge.toStage == null)
                    || (edge.sink == null))
                return;

            addStage(edge.fromStage);
            addStage(edge.toStage);

            LOGGER.info("StageGraph: Adding edge " + edge);

            edges.add(edge);
            StageList list = (StageList) edgesFrom.get(edge.fromStage);
            if (list == null) {
                list = new StageList();
                list.add(edge);
                edgesFrom.put(edge.fromStage, list);
            } else {
                list.add(edge);
            }
        }
    }

    /**
     * Output the graph in a format that can be used by the AT&amp;T 'graphviz'
     * program: http://www.research.att.com/sw/tools/graphviz/ Makes it easy to
     * draw pretty pictures of stage graphs.
     */
    public synchronized void dumpGraph() {
        if (graphpw == null)
            return;
        graphpw.println("digraph sandstorm {");
        graphpw.println("  rankdir=TB;");
        for (StageGraphEdge edge : edges) {
            String from = edge.fromStage.getStage().getName();
            String to = edge.toStage.getStage().getName();
            int count = 0;
            try {
                count = ((SinkProxy) edge.sink).enqueueCount;
            } catch (ClassCastException cce) {
                // Ignore
            }
            graphpw.println("  \"" + from + "\" -> \"" + to + "\" [label=\"" + count + "\"];");
        }
        graphpw.println("}");
        graphpw.flush();
    }

    class StageList {
        List<StageGraphEdge> vec = Lists.newArrayList();

        void add(StageGraphEdge edge) {
            vec.add(edge);
        }

        StageGraphEdge[] getEdges() {
            return vec.toArray(new StageGraphEdge[vec.size()]);
        }
    }
}
