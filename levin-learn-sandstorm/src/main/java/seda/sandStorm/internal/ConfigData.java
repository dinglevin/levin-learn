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

package seda.sandStorm.internal;

import static seda.util.ConfigUtil.stringArrayToMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.ManagerIF;
import seda.sandStorm.api.StageIF;
import seda.sandStorm.main.SandstormConfig;

/**
 * ConfigData is used to pass configuration arguments into various components.
 */
public class ConfigData implements ConfigDataIF {
    private Map<String, String> vals;
    private ManagerIF mgr;
    private StageIF stage;

    /**
     * Create a ConfigData with the given manager and no argument list.
     */
    public ConfigData(ManagerIF mgr) {
        this.mgr = mgr;
        this.vals = new HashMap<>(1);
    }

    /**
     * Create a ConfigData with the given manager and argument list.
     */
    public ConfigData(ManagerIF mgr, Map<String, String> args) {
        this.mgr = mgr;
        this.vals = args;
        if (vals == null) {
            vals = new HashMap<>(1);
        }
    }

    /**
     * Create a ConfigData with the given manager and argument list, specified
     * as an array of strings of the form "key=value".
     *
     * @throws IOException
     *             If any of the strings to not match the pattern "key=value".
     */
    public ConfigData(ManagerIF mgr, String args[]) throws IOException {
        this.mgr = mgr;
        this.vals = stringArrayToMap(args);
        if (vals == null) {
            vals = new HashMap<>(1);
        }
    }

    /**
     * Returns true if the given key is set.
     */
    public boolean contains(String key) {
        if (vals.get(key) != null) {
            return true;
        }

        return false;
    }

    /**
     * Get the string value corresponding to the given key. Returns null if not
     * set.
     */
    public String getString(String key) {
        return vals.get(key);
    }

    /**
     * Get the integer value corresponding to the given key. Returns -1 if not
     * set or if the value is not an integer.
     */
    public int getInt(String key) {
        String val = vals.get(key);
        if (val == null) {
            return -1;
        }

        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Get the double value corresponding to the given key. Returns -1.0 if not
     * set or if the value is not a double.
     */
    public double getDouble(String key) {
        String val = (String) vals.get(key);
        if (val == null)
            return -1;
        try {
            return new Double(val).doubleValue();
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    /**
     * Get the boolean value corresponding to the given key. Returns false if
     * not set.
     */
    public boolean getBoolean(String key) {
        String val = (String) vals.get(key);
        if (val == null)
            return false;
        else if (val.equals("true") || val.equals("TRUE"))
            return true;
        else
            return false;
    }

    /**
     * Get the string list value corresponding to the given key. Returns null if
     * not set.
     */
    public String[] getStringList(String key) {
        String val = vals.get(key);
        if (val == null) {
            return null;
        }

        StringTokenizer st = new StringTokenizer(val, SandstormConfig.LIST_ELEMENT_DELIMITER);
        List<String> list = new ArrayList<>(1);
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        
        return list.toArray(new String[list.size()]);
    }

    /**
     * Set the given key to the given string value.
     */
    public void setString(String key, String val) {
        vals.put(key, val);
    }

    /**
     * Set the given key to the given integer value.
     */
    public void setInt(String key, int val) {
        vals.put(key, Integer.toString(val));
    }

    /**
     * Set the given key to the given double value.
     */
    public void setDouble(String key, double val) {
        vals.put(key, Double.toString(val));
    }

    /**
     * Set the given key to the given boolean value.
     */
    public void setBoolean(String key, boolean val) {
        vals.put(key, (val == true) ? "true" : "false");
    }

    /**
     * Set the given key to the given string list value.
     */
    public void setStringList(String key, String valarr[]) {
        String s = "";
        for (int i = 0; i < valarr.length; i++) {
            s += valarr[i];
            if (i != valarr.length - 1)
                s += SandstormConfig.LIST_ELEMENT_DELIMITER;
        }
        vals.put(key, s);
    }

    /**
     * Return the local manager.
     */
    public ManagerIF getManager() {
        return mgr;
    }

    /**
     * Return the stage for this ConfigData.
     */
    public StageIF getStage() {
        return stage;
    }

    // Used to set stage after creating wrapper
    public void setStage(StageIF stage) {
        this.stage = stage;
    }

    // Used to reset manager after creating stage
    // (for proxying the manager)
    void setManager(ManagerIF mgr) {
        this.mgr = mgr;
    }
}
