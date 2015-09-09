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

package seda.sandStorm.main;

import static seda.util.ConfigUtil.stringArrayToMap;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import seda.sandStorm.api.StageNameAlreadyBoundException;

/**
 * This class is used to pass configuration parameters into Sandstorm at startup
 * time. It reads initial configuration parameters from a file, using an
 * XML-like format. Various operations can be performed upon this class to
 * modify the configuration of the Sandstorm runtime.
 *
 * @author Matt Welsh
 * @see Sandstorm
 * @see Main
 *
 */
public class SandstormConfig implements Cloneable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SandstormConfig.class);
    
    private static final String DELIM_CHAR = ".";
    
    public static final String LIST_ELEMENT_DELIMITER = " ";

    /** Value for defaultThreadMgr to use the thread-per-CPU thread manager. */
    public static final String THREADMGR_TPPTM = "TPPTM";
    /**
     * Value for defaultThreadMgr to use the thread-per-stage thread manager.
     */
    public static final String THREADMGR_TPSTM = "TPSTM";
    /** Value for defaultThreadMgr to use the aggregating TPSTM. */
    public static final String THREADMGR_AggTPSTM = "AggTPSTM";

    /** String value for setting boolean configuration entries to true. */
    public static final String CONFIG_TRUE = "true";
    /** String value for setting boolean configuration entries to false. */
    public static final String CONFIG_FALSE = "false";
    
    private static final Properties DEFAULT_CONFIGS;
    static {
        Properties props = new Properties();
        try {
            props.load(SandstormConfig.class.getClassLoader().getResourceAsStream("seda.default.properties"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load seda.default.properties", ex);
        }
        DEFAULT_CONFIGS = props;
    }
    
    private ConfigSection root;
    private Map<String, StageDescr> stages;
    private Map<String, String> cmdLineArgs;
    /** Default initialization arguments passed to every stage. */
    public Map<String, String> defaultInitArgs;

    /**
     * Create a new SandstormConfig with the default settings.
     */
    public SandstormConfig() {
        stages = new HashMap<>(1);
        root = new ConfigSection("sandstorm");
        setDefaultValues();
    }

    /**
     * Create a new SandstormConfig with the default settings, with the given
     * default init args, which will be passed to every stage. Each element of
     * defaultArgs[] is a String in the format "key=value". If "key" contains a
     * dot ("."), then it will be treated as a key to be added to the Sandstorm
     * configuration's global parameters. Otherwise, the key-value pair will be
     * added to the "initargs" section for each stage.
     *
     */
    public SandstormConfig(String defaultArgs[]) throws IOException {
        this();
        if (defaultArgs != null) {
            this.cmdLineArgs = stringArrayToMap(defaultArgs);
        }
    }

    /**
     * Create a new SandstormConfig, reading the configration from the given
     * file. The configuration file uses an XML-like structure; see the
     * Sandstorm documentation for more information on its format.
     */
    public SandstormConfig(String fname) throws IOException {
        this();
        readFile(fname);
    }

    /**
     * Create a new SandstormConfig, reading the configration from the given
     * file. The configuration file uses an XML-like structure; see the
     * Sandstorm documentation for more information on its format.
     * 
     * @param defaultInitArgs
     *            Default initialization arguments passed to every stage. These
     *            override any arguments found in the config file. Each element
     *            of this array must be a string with the format
     *            <tt>"key=value"</tt>.
     */
    public SandstormConfig(String fname, String defaultArgs[]) throws IOException {
        this(defaultArgs);
        readFile(fname);
    }

    // Return the value associated with the given key in cs; recursive.
    private String getVal(ConfigSection cs, String key) {
        String car, cdr;
        int c = key.indexOf(DELIM_CHAR);
        if (c == -1) {
            car = key;
            cdr = null;
        } else {
            car = key.substring(0, c);
            cdr = key.substring(c + 1, key.length());
        }

        LOGGER.debug("getVal: cs={}, key={}, car={}, cdr={}", cs, key, car, cdr);

        if (cdr == null) {
            // OK, we are at a terminal node
            return cs.getVal(car);
        } else {
            // We are at an intermediate node
            ConfigSection subsec = cs.getSubsection(car);
            if (subsec == null) {
                return null;
            }
            return getVal(subsec, cdr);
        }
    }

    // Set the given value in cs; recursive.
    private void putVal(ConfigSection cs, String key, String val) {
        String car, cdr;
        int c = key.indexOf(DELIM_CHAR);
        if (c == -1) {
            car = key;
            cdr = null;
        } else {
            car = key.substring(0, c);
            cdr = key.substring(c + 1, key.length());
        }

        if (cdr == null) {
            // OK, we are at a terminal node
            cs.putVal(key, val);
            return;
        } else {
            // We are at an intermediate node
            ConfigSection subsec = cs.getSubsection(car);
            if (subsec == null) {
                subsec = new ConfigSection(car);
                cs.addSubsection(subsec);
            }
            putVal(subsec, cdr, val);
            return;
        }
    }

    /**
     * Return the configuration option associated with the given key as a
     * String. Returns null if not set.
     */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * Return the configuration option associated with the given key as a
     * String. Returns default if not set.
     */
    public String getString(String key, String defaultval) {
        String val = getVal(root, key);
        if (val == null) {
            return defaultval;
        }
        return val;
    }

    /**
     * Set the given configuration option specified as a String.
     */
    public void putString(String key, String val) {
        putVal(root, key, val);
    }

    /**
     * Return the configuration option associated with the given key as a
     * boolean. Returns false if not set.
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Return the configuration option associated with the given key as a
     * boolean. Returns default if not set.
     */
    public boolean getBoolean(String key, boolean defaultval) {
        String val = getVal(root, key);
        if (val == null) {
            return defaultval;
        }
        
        return "true".equalsIgnoreCase(val);
    }

    /**
     * Set the given configuration option specified as a boolean.
     */
    public void putBoolean(String key, boolean val) {
        putVal(root, key, (val) ? (CONFIG_TRUE) : (CONFIG_FALSE));
    }

    /**
     * Return the configuration option associated with the given key as an int.
     * Returns -1 if not set or if the value of the key cannot be expressed as
     * an int.
     */
    public int getInt(String key) {
        return getInt(key, -1);
    }

    /**
     * Return the configuration option associated with the given key as an int.
     * Returns default if not set or if the value of the key cannot be expressed
     * as an int.
     */
    public int getInt(String key, int defaultval) {
        String val = getVal(root, key);
        if (val == null)
            return defaultval;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            return defaultval;
        }
    }

    /**
     * Set the given configuration option specified as an int.
     */
    public void putInt(String key, int val) {
        putVal(root, key, Integer.toString(val));
    }

    /**
     * Return the configuration option associated with the given key as a
     * double. Returns -1 if not set or if the value of the key cannot be
     * expressed as a double.
     */
    public double getDouble(String key) {
        return getDouble(key, -1);
    }

    /**
     * Return the configuration option associated with the given key as a
     * double. Returns default if not set or if the value of the key cannot be
     * expressed as a double.
     */
    public double getDouble(String key, double defaultval) {
        String val = getVal(root, key);
        if (val == null)
            return defaultval;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException nfe) {
            return defaultval;
        }
    }

    /**
     * Get the string list value corresponding to the given key. Returns null if
     * not set.
     */
    public String[] getStringList(String key) {
        String val = (String) getVal(root, key);
        return StringUtils.split(val, LIST_ELEMENT_DELIMITER);
    }

    /**
     * Set the given configuration option specified as an double.
     */
    public void putDouble(String key, double val) {
        putVal(root, key, Double.toString(val));
    }

    /**
     * Set the given key to the given string list value.
     */
    public void puttStringList(String key, String valarr[]) {
        String listValue = StringUtils.join(valarr, LIST_ELEMENT_DELIMITER);
        putVal(root, key, listValue);
    }

    private List<String> getAllUnderlyingKeys(ConfigSection cs) {
        List<String> result = Lists.newArrayList();
        Iterator<String> keys = cs.getKeys();
        if (keys != null) {
            while (keys.hasNext()) {
                result.add(keys.next());
            }
        }
        Iterator<ConfigSection> sections = cs.getSubsections();
        if (sections != null) {
            while (sections.hasNext()) {
                ConfigSection subsec = sections.next();
                result.add(subsec.getName() + DELIM_CHAR);
            }
        }
        return result;
    }
    
    // Return enumeration of keys matching prefix starting with cs. Recursive.
    private Iterator<String> getKeys(ConfigSection cs, String prefix) {
        // We are at the end of the prefix
        if (prefix == null) {
            List<String> underlyingKeys = getAllUnderlyingKeys(cs);
            return underlyingKeys.iterator();
        }

        // First look for single item matching prefix
        StringTokenizer st = new StringTokenizer(prefix, DELIM_CHAR);
        String tok = st.nextToken();
        if (tok == null)
            return null;
        String val = cs.getVal(tok);
        if (val != null) {
            List<String> result = Lists.newArrayList(tok);
            return result.iterator();
        }

        // Look for subsection matching prefix
        ConfigSection subsec = cs.getSubsection(tok);
        if (subsec == null)
            return null;
        String tok2 = st.nextToken();
        return getKeys(subsec, tok2);
    }

    /**
     * Return an Iterator of the keys matching the given prefix. A given key
     * maps onto a set of child keys if it ends in a "." character (that is, it
     * is an internal node within the tree). A key not ending in "." is a
     * terminal node and maps onto a value that may be obtained using getString,
     * getInt, or getDouble.
     */
    public Iterator<String> getKeys(String prefix) {
        return getKeys(root, prefix);
    }

    /**
     * Return an enumeration of the top-level keys in this configuration.
     */
    public Iterator<String> getKeys() {
        return getKeys(root, null);
    }

    /**
     * Return a copy of this object.
     */
    public SandstormConfig getCopy() {
        try {
            return (SandstormConfig) (this.clone());
        } catch (CloneNotSupportedException e) {
            throw new Error("Internal error: SandstormConfig must support clone!");
        }
    }

    /**
     * Add a stage to this SandstormConfig.
     *
     * @param stageName
     *            The name of the stage as it should be registered.
     * @param className
     *            The fully-qualified class name of the stage event handler.
     * @param initArgs
     *            The initial arguments to pass into the stage.
     */
    public void addStage(String stageName, String className, String initArgs[])
            throws StageNameAlreadyBoundException, IOException {
        if (stages.get(stageName) != null) {
            throw new StageNameAlreadyBoundException("Stage " + stageName + " already registered in SandstormConfig");
        }

        StageDescr descr = new StageDescr(stageName, className, stringArrayToMap(initArgs));
        stages.put(stageName, descr);
    }

    /**
     * Return an Enumeration of the stages specified by this SandstormConfig.
     */
    public Iterator<StageDescr> getStages() {
        return stages.values().iterator();
    }

    /**
     * Read the configuration from the given file.
     */
    public void readFile(String fname) throws IOException {
        Reader in = new DirectiveReader(new BufferedReader(new FileReader(fname)));
        root = new ConfigSection(in);

        ConfigSection global_initargs = null;

        if (!root.getName().equals("sandstorm"))
            throw new IOException("Outermost section config file named "
                    + root.getName() + ", expecting 'sandstorm'");

        setDefaultValues();

        // Set command line values
        this.defaultInitArgs = Maps.newHashMap();
        if (cmdLineArgs != null) {
            Iterator<Map.Entry<String, String>> e = cmdLineArgs.entrySet().iterator();
            while (e.hasNext()) {
                Map.Entry<String, String> entry = e.next();
                String key = entry.getKey();
                if (key.indexOf('.') != -1) {
                    putString(key, entry.getValue());
                } else {
                    this.defaultInitArgs.put(key, entry.getValue());
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DOING DUMP: -----------------------");
            root.dump();
            LOGGER.debug("DONE WITH DUMP ---------------------");
        }

        // Get global init args
        ConfigSection global = root.getSubsection("global");
        if (global != null) {
            global_initargs = global.getSubsection("initargs");
        }

        // Get stages
        LOGGER.debug("Parsing stages");
        
        ConfigSection stagesec = root.getSubsection("stages");
        if (stagesec != null) {
            for (int i = 0; i < stagesec.subSections.size(); i++) {
                ConfigSection sec = stagesec.subSections.get(i);
                String stageName = sec.getName();
                String className = sec.getVal("class");
                if (className == null)
                    throw new IOException("Missing class name in <stage> section of config file");
                
                LOGGER.debug("Parsing stage {}", stageName);

                Map<String, String> initArgs = Maps.newHashMap();

                // Add global args
                if (global_initargs != null) {
                    Iterator<String> e2 = global_initargs.getKeys();
                    while (e2.hasNext()) {
                        String key = e2.next();
                        String val = global_initargs.getVal(key);
                        initArgs.put(key, val);
                    }
                }

                // Add stage-specific args
                ConfigSection args = sec.getSubsection("initargs");
                if (args != null) {
                    Iterator<String> e2 = args.getKeys();
                    while (e2.hasNext()) {
                        String key = e2.next();
                        String val = args.getVal(key);
                        initArgs.put(key, val);
                    }
                }

                // Add defaultInitArgs
                if (defaultInitArgs != null) {
                    Iterator<Map.Entry<String, String>> e2 = defaultInitArgs.entrySet().iterator();
                    while (e2.hasNext()) {
                        Map.Entry<String, String> entry = e2.next();
                        initArgs.put(entry.getKey(), entry.getValue());
                    }
                }

                int queueThreshold = -1;
                try {
                    String val = sec.getVal("queueThreshold");
                    if (val != null)
                        queueThreshold = Integer.parseInt(val);
                } catch (NumberFormatException ne) {
                    queueThreshold = -1;
                }
                
                LOGGER.info("Adding stage {}", stageName);
                
                StageDescr descr = new StageDescr(stageName, className, initArgs, queueThreshold);
                stages.put(descr.stageName, descr);
            }
        }
    }
    
    private void setDefaultValues() {
        for (String key : DEFAULT_CONFIGS.stringPropertyNames()) {
            String value = DEFAULT_CONFIGS.getProperty(key);
            if (getString(key) == null) {
                putString(key, value);
            }
        }
    }

    // ----------------------------------------------------------------------

    // Internal class to represent configuration file format
    class ConfigSection {
        private String secname;
        private StreamTokenizer tok;
        private List<ConfigSection> subSections;
        private Map<String, String> vals;

        private ConfigSection() {
            subSections = Lists.newArrayList();
            vals = Maps.newHashMap();
        }

        ConfigSection(Reader in) throws IOException {
            this();
            tok = new StreamTokenizer(in);
            tok.resetSyntax();
            tok.wordChars((char) 0, (char) 255);
            tok.whitespaceChars('\u0000', '\u0020');
            tok.commentChar('#');
            tok.eolIsSignificant(true);
            doRead();
        }

        private ConfigSection(StreamTokenizer tok) throws IOException {
            this();
            this.tok = tok;
            tok.pushBack();
            tok.wordChars('0', '9');
            doRead();
        }

        ConfigSection(String name) {
            this();
            this.secname = name;
        }

        String getName() {
            return secname;
        }

        ConfigSection getSubsection(String name) {
            for (int i = 0; i < subSections.size(); i++) {
                ConfigSection sec = subSections.get(i);
                if (sec.getName().equals(name))
                    return sec;
            }
            return null;
        }

        void addSubsection(ConfigSection subsec) {
            subSections.add(subsec);
        }

        // Return the string associated with key in this section
        // If not specified, null is returned
        String getVal(String key) {
            return vals.get(key);
        }

        Iterator<ConfigSection> getSubsections() {
            if (subSections == null)
                return null;
            return subSections.iterator();
        }

        Iterator<String> getKeys() {
            return vals.keySet().iterator();
        }

        int numKeys() {
            return vals.size();
        }

        Map<String, String> getVals() {
            return vals;
        }

        void putVal(String key, String val) {
            vals.put(key, val);
        }

        // Read next section name, parse recursively until we see the
        // end of that section
        private void doRead() throws IOException {
            String word, key, value;

            // Get initial section name
            word = nextWord();
            if (word.startsWith("<") && word.endsWith(">")) {
                secname = word.substring(1, word.length() - 1);
            } else {
                throw new IOException("No section name found at line "
                        + tok.lineno() + " of config file, read " + word);
            }

            boolean done = false;
            while (!done) {

                key = null;
                while (true) {
                    // Read key
                    word = nextWord();
                    if (word.startsWith("<") && word.endsWith(">")) {
                        String val = word.substring(1, word.length() - 1);
                        if (val.equals("/" + secname)) {
                            // Done reading this section
                            done = true;
                            break;
                        } else {
                            // Found a new section; recurse
                            ConfigSection subsec = new ConfigSection(tok);
                            if (getSubsection(subsec.getName()) != null) {
                                throw new IOException("subsection " + subsec.getName()
                                        + " redefined at line " + tok.lineno() + " of config file");
                            }
                            if (vals.get(subsec.getName()) != null) {
                                throw new IOException("subsection " + subsec.getName()
                                        + " conflicts with key " + subsec.getName() + " at line "
                                        + tok.lineno() + " of config file");
                            }

                            subSections.add(subsec);
                        }
                    } else {
                        key = word;
                        break;
                    }
                }

                if (done)
                    break;

                // Read value
                word = nextLine();
                if (word.startsWith("<") && word.endsWith(">")) {
                    // Bad format: Should not have section tag here
                    throw new IOException("Unexpected section tag " + word
                            + " on line " + tok.lineno() + " of config file");
                } else {
                    value = word;
                }

                if (key == null)
                    throw new IOException("key is null at line " + tok.lineno()
                            + " of config file");
                if (vals.get(key) != null) {
                    throw new IOException("key " + key + " redefined at line "
                            + tok.lineno() + " of config file");
                }
                if (getSubsection(key) != null) {
                    throw new IOException("key " + key
                            + " conflicts with subsection " + key + " at line "
                            + tok.lineno() + " of config file");
                }
                if (key.indexOf(DELIM_CHAR) != -1) {
                    throw new IOException("key " + key
                            + " may not contain character '" + DELIM_CHAR
                            + "' at line " + tok.lineno() + " of config file");
                }
                vals.put(key, value);
            }

        }

        // Read next whitespace-delimited word from tok
        private String nextWord() throws IOException {
            while (true) {
                int type = tok.nextToken();
                switch (type) {

                case StreamTokenizer.TT_EOL:
                    continue;

                case StreamTokenizer.TT_EOF:
                    throw new EOFException("EOF in config file");

                case StreamTokenizer.TT_WORD:
                    LOGGER.debug("nextWord returning {}", tok.sval);
                    return tok.sval;

                case StreamTokenizer.TT_NUMBER:
                    LOGGER.debug("nextWord returning number");
                    return Double.toString(tok.nval);

                default:
                    continue;
                }
            }
        }

        // Read rest of line from tok
        private String nextLine() throws IOException {
            String line = new String("");
            boolean first = true;

            while (true) {
                switch (tok.nextToken()) {

                case StreamTokenizer.TT_EOL:
                    LOGGER.debug("nextLine returning {}", line);
                    return line;

                case StreamTokenizer.TT_EOF:
                    throw new EOFException("EOF in config file");

                case StreamTokenizer.TT_WORD:
                    if (first) {
                        line = tok.sval;
                        first = false;
                    } else {
                        line += " " + tok.sval;
                    }
                    break;

                case StreamTokenizer.TT_NUMBER:
                    if (first) {
                        line = Double.toString(tok.nval);
                        first = false;
                    } else {
                        line += " " + Double.toString(tok.nval);
                    }
                    break;

                default:
                    continue;
                }
            }
        }

        // Debugging only
        void dump() {
            LOGGER.debug("<" + secname + ">");
            
            Iterator<Map.Entry<String, String>> iterator = vals.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                LOGGER.debug("   key={}, value={}", entry.getKey(), entry.getValue());
            }

            for (ConfigSection sec : subSections) {
                sec.dump();
            }
            
            System.err.println("</" + secname + ">");
        }

        public String toString() {
            return "configSection <" + secname + ">";
        }
    }

    /**
     * Internal class to preprocess special directives in the config file.
     */
    class DirectiveReader extends Reader {
        private Reader under, includedFile, markStream;
        private boolean markIsIncluded = false, closed = false;
        private boolean inComment = false;

        DirectiveReader(Reader under) throws IOException {
            this.under = under;
            if (!under.markSupported()) {
                throw new IOException("SandstormConfig: Internal error: directiveReader.under must support mark() -- contact mdw@cs.berkeley.edu");
            }
        }

        public int read() throws IOException {
            if (closed)
                throw new IOException("directiveReader is closed");
            if (includedFile != null) {
                int ret = includedFile.read();
                if (ret == -1)
                    includedFile = null;
                else
                    return ret;
            }

            boolean done = false;

            while (!done) {
                int c = under.read();

                // Ignore special directives inside of comments
                if (c == '#') {
                    inComment = true;
                }
                if (c == '\n') {
                    inComment = false;
                }

                if (!inComment && (c == '<')) {
                    under.mark(100);
                    if (under.read() == '!') {
                        // Process special directive; read until '>'
                        String directive = "<!";
                        char c1 = ' ';
                        while (c1 != '>') {
                            try {
                                c1 = (char) under.read();
                                if (c1 == -1)
                                    throw new IOException("End of file");
                            } catch (IOException ioe) {
                                throw new IOException("SandstormConfig: Unterminated directive "
                                        + directive.substring(0, Math.min(directive.length(), 10)) 
                                        + " in configuration file");
                            }
                            directive += c1;
                        }
                        
                        LOGGER.debug("Got special directive: {}", directive);

                        if (directive.startsWith("<!include")) {
                            StringTokenizer st = new StringTokenizer(directive);
                            //String dir = st.nextToken();
                            st.nextToken();
                            String fname = st.nextToken();
                            fname = fname.substring(0, fname.length() - 1).trim();
                            
                            LOGGER.debug("Including file: {}", fname);
                            
                            includedFile = new DirectiveReader(new BufferedReader(new FileReader(fname)));
                            int ret = includedFile.read();
                            if (ret == -1) {
                                includedFile = null;
                                continue;
                            } else {
                                return ret;
                            }
                        } else {
                            throw new IOException("SandstormConfig: Unrecognized directive " + directive + " in config file");
                        }

                    } else {
                        // Got a '<' with no following '!'
                        under.reset();
                        return c;
                    }
                } else {
                    // Got something other than '<'
                    return c;
                }
            }
            // Should never get here
            return -1;
        }

        public int read(char cbuf[]) throws IOException {
            return read(cbuf, 0, cbuf.length);
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            int n = 0;
            for (int i = off; i < len; i++) {
                int c = read();
                if (cbuf[i] == -1)
                    return n;
                cbuf[i] = (char) c;
                n++;
            }
            return n;
        }

        public long skip(long n) throws IOException {
            if (n < 0)
                throw new IllegalArgumentException("directiveReader.skip: n must be nonzero");
            long skipped = 0;
            for (long l = n; l >= 0; l--) {
                int c = read();
                if (c == -1)
                    return skipped;
                skipped++;
            }
            return skipped;
        }

        public boolean ready() throws IOException {
            if (includedFile != null)
                return includedFile.ready();
            return under.ready();
        }

        public boolean markSupported() {
            return true;
        }

        public void mark(int readAheadLimit) throws IOException {
            if (includedFile != null) {
                markStream = includedFile;
                markIsIncluded = true;
            } else {
                markStream = under;
            }
            markStream.mark(readAheadLimit);
        }

        public void reset() throws IOException {
            markStream.reset();
            if (markIsIncluded)
                includedFile = markStream;
        }

        public void close() throws IOException {
            if (includedFile != null)
                includedFile.close();
            under.close();
            closed = true;
        }
    }

}
