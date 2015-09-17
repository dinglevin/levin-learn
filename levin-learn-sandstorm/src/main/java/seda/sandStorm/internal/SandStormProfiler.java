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
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import seda.sandstorm.api.Manager;
import seda.sandstorm.api.Profilable;
import seda.sandstorm.api.Profiler;
import seda.sandstorm.main.SandstormConfig;

/**
 * sandStormProfiler is an implementation of the ProfilerIF interface for
 * Sandstorm. It is implemented using a thread that periodically samples the set
 * of ProfilableIF's registered with it, and outputs the profile to a file.
 *
 * @author Matt Welsh
 * @see Profiler
 * @see Profilable
 */
class SandStormProfiler extends Thread implements Profiler {
    private int delay;
    private PrintWriter pw;
    private List<Profile> profilables;
    private boolean started = false;
    private StageGraph graphProfiler;

    SandStormProfiler(Manager mgr) throws IOException {
        graphProfiler = new StageGraph(mgr);
        SandstormConfig config = mgr.getConfig();
        delay = config.getInt("global.profile.delay");
        String filename = config.getString("global.profile.filename");
        if (config.getBoolean("global.profile.enable")) {
            pw = new PrintWriter(new FileWriter(filename, true));
        }
        profilables = Lists.newArrayList();
    }

    /**
     * Returns true if the profiler is enabled.
     */
    public boolean enabled() {
        return started;
    }

    /**
     * Add a class to this profiler.
     */
    public void add(String name, Profilable pr) {
        if (pr == null)
            return;
        if (pw == null)
            return;
        synchronized (profilables) {
            pw.println("# Registered " + profilables.size() + " " + name);
            profilables.add(new Profile(name, pr));
        }
    }

    public void run() {
        if (pw == null)
            return;
        started = true;
        pw.println("##### Profile started at " + (new Date()).toString());
        pw.println("##### Sample delay " + delay + " msec");
        Runtime r = Runtime.getRuntime();

        while (true) {
            long totalmem = r.totalMemory() / 1024;
            long freemem = r.freeMemory() / 1024;

            pw.print("totalmem(kb) " + totalmem + " freemem(kb) " + freemem + " ");

            synchronized (profilables) {
                if (profilables.size() > 0) {
                    for (int i = 0; i < profilables.size(); i++) {
                        Profile p = profilables.get(i);
                        pw.print("pr" + i + " " + p.pr.profileSize() + " ");
                    }
                }
            }
            pw.println("");
            pw.flush();

            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
            }
        }
    }

    public StageGraph getGraphProfiler() {
        return graphProfiler;
    }

    class Profile {
        String name;
        Profilable pr;

        Profile(String name, Profilable pr) {
            this.name = name;
            this.pr = pr;
        }
    }
}
