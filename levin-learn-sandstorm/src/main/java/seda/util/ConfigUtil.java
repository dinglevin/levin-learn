package seda.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class ConfigUtil {
    private ConfigUtil() { }
    
    // Convert an array of "key=value" strings to a Map
    public static Map<String, String> stringArrayToMap(String arr[]) throws IOException {
        if (arr == null) {
            return null;
        }

        Map<String, String> map = new HashMap<>(1);
        for (int i = 0; i < arr.length; i++) {
            StringTokenizer st = new StringTokenizer(arr[i], "=");
            String key;
            String val;
            try {
                key = st.nextToken();
                val = st.nextToken();
                while (st.hasMoreTokens()) {
                    val += "=" + st.nextToken();
                }
            } catch (NoSuchElementException e) {
                throw new IOException("Could not convert string '" + arr[i] + "' to key=value pair");
            }
            map.put(key, val);
        }
        return map;
    }
}
