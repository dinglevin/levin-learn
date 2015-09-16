/* 
 * Copyright (c) 2002 by Matt Welsh and The Regents of the University of 
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

package seda.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class StatsGatherer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsGatherer.class);
    
    private static Map<String, StatsGatherer> sgTbl = Maps.newHashMap();
    
    private Map<Integer, Integer> histogram;
    private int bucketSize;
    private String name;
    private String tag;

    private int skipSamples;
    private int skip = 0;
    public int num = 0;
    public double maxVal = 0;
    public double minVal = Double.MAX_VALUE;
    public double cumulativeVal = 0;

    public StatsGatherer(String name, String tag, int bucketSize, int skipSamples) {
        this.name = name;
        this.tag = tag;
        this.bucketSize = bucketSize;
        this.skipSamples = skipSamples;
        if (bucketSize != 0) {
            histogram = Maps.newHashMap();
        }
        sgTbl.put(name, this);
    }

    public StatsGatherer(String name, String tag, int bucketSize) {
        this(name, tag, bucketSize, 0);
    }

    public static StatsGatherer lookup(String name) {
        return sgTbl.get(name);
    }

    public static Collection<StatsGatherer> lookupAll() {
        return sgTbl.values();
    }

    public static void dumpAll() {
        for (StatsGatherer sg : lookupAll()) {
            sg.dumpHistogram();
        }
    }

    public synchronized void reset() {
        this.num = 0;
        this.maxVal = 0;
        this.minVal = Double.MAX_VALUE;
        this.cumulativeVal = 0;
        if (bucketSize != 0) {
            histogram = Maps.newHashMap();
        }
    }

    public synchronized void add(double val) {
        if (skip < skipSamples) {
            skip++;
            return;
        }

        num++;

        if (val > maxVal)
            maxVal = val;
        if (val < minVal)
            minVal = val;
        cumulativeVal += val;

        if (bucketSize != 0) {
            Integer ct = new Integer((int) val / bucketSize);
            Integer bval = histogram.remove(ct);

            if (bval == null) {
                histogram.put(ct, new Integer(1));
            } else {
                bval = new Integer(bval.intValue() + 1);
                histogram.put(ct, bval);
            }
        }
    }

    public synchronized void dumpHistogram() {
        LOGGER.info("=== Histogram dump of StatsGatherer: " + name + " ===");

        TreeSet<Integer> ts = new TreeSet<Integer>(new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return i1.intValue() - i2.intValue();
            }
        });

        ts.addAll(histogram.keySet());

        Integer arr[] = ts.toArray(new Integer[0]);
        for (int i = 0; i < arr.length; i++) {
            Integer bucket = arr[i];
            int time = bucket.intValue() * bucketSize;
            int val = histogram.get(bucket).intValue();
            LOGGER.info(tag + " " + time + " ms " + val + " count " + (val * 100.0) / (num * 1.0) + " pct");
        }
        LOGGER.info("=== Summary of StatsGatherer: " + name + " ===");
        LOGGER.info(tag + ": num " + num + " avg " + mean() + " max "
                + max() + " 90th " + percentile(0.9));
        LOGGER.info("=== End of summmary for StatsGatherer: " + name + " ===");
    }

    public synchronized int num() {
        return num;
    }

    public synchronized double mean() {
        if (num == 0)
            return 0.0;
        return (cumulativeVal * 1.0) / num;
    }

    public synchronized double min() {
        return minVal;
    }

    public synchronized double max() {
        return maxVal;
    }

    public synchronized double percentile(double pct) {
        if (num == 0)
            return 0.0;
        double data[] = new double[num];
        int n = 0;
        for (int bucket : histogram.keySet()) {
            int time = bucket * bucketSize;
            int val = histogram.get(bucket).intValue();
            for (int j = 0; j < val; j++) {
                data[n++] = time;
            }
        }
        Arrays.sort(data);
        int index = (int) (data.length * pct);
        return data[index];
    }

}
