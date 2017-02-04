/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <em>Consider this class private.</em>
 * <p>
 * Helps access properties. This utility provides a method to override system properties by specifying properties in a
 * properties file.
 * </p>
 */
public final class PropertiesUtil {

    private static final PropertiesUtil LOG4J_PROPERTIES = new PropertiesUtil("log4j2.component.properties");

    private final Environment environment;

    /**
     * Constructs a PropertiesUtil using a given Properties object as its source of defined properties.
     *
     * @param props the Properties to use by default
     */
    public PropertiesUtil(final Properties props) {
        this.environment = new Environment(props);
    }

    /**
     * Constructs a PropertiesUtil for a given properties file name on the classpath. The properties specified in this
     * file are used by default. If a property is not defined in this file, then the equivalent system property is used.
     *
     * @param propertiesFileName the location of properties file to load
     */
    public PropertiesUtil(final String propertiesFileName) {
        final Properties properties = new Properties();
        for (final URL url : LoaderUtil.findResources(propertiesFileName)) {
            try (final InputStream in = url.openStream()) {
                properties.load(in);
            } catch (final IOException ioe) {
                LowLevelLogUtil.logException("Unable to read " + url.toString(), ioe);
            }
        }
        this.environment = new Environment(properties);
    }

    /**
     * Loads and closes the given property input stream. If an error occurs, log to the status logger.
     *
     * @param in a property input stream.
     * @param source a source object describing the source, like a resource string or a URL.
     * @return a new Properties object
     */
    static Properties loadClose(final InputStream in, final Object source) {
        final Properties props = new Properties();
        if (null != in) {
            try {
                props.load(in);
            } catch (final IOException e) {
                LowLevelLogUtil.logException("Unable to read " + source, e);
            } finally {
                try {
                    in.close();
                } catch (final IOException e) {
                    LowLevelLogUtil.logException("Unable to close " + source, e);
                }
            }
        }
        return props;
    }

    /**
     * Returns the PropertiesUtil used by Log4j.
     *
     * @return the main Log4j PropertiesUtil instance.
     */
    public static PropertiesUtil getProperties() {
        return LOG4J_PROPERTIES;
    }

    /**
     * Gets the named property as a boolean value. If the property matches the string {@code "true"} (case-insensitive),
     * then it is returned as the boolean value {@code true}. Any other non-{@code null} text in the property is
     * considered {@code false}.
     *
     * @param name the name of the property to look up
     * @return the boolean value of the property or {@code false} if undefined.
     */
    public boolean getBooleanProperty(final String name) {
        return getBooleanProperty(name, false);
    }

    /**
     * Gets the named property as a boolean value.
     *
     * @param name the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the boolean value of the property or {@code defaultValue} if undefined.
     */
    public boolean getBooleanProperty(final String name, final boolean defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
    }

    /**
     * Gets the named property as a Charset value.
     *
     * @param name the name of the property to look up
     * @return the Charset value of the property or {@link Charset#defaultCharset()} if undefined.
     */
    public Charset getCharsetProperty(final String name) {
        return getCharsetProperty(name, Charset.defaultCharset());
    }

    /**
     * Gets the named property as a Charset value.
     *
     * @param name the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the Charset value of the property or {@code defaultValue} if undefined.
     */
    public Charset getCharsetProperty(final String name, final Charset defaultValue) {
        final String prop = getStringProperty(name);
        return prop == null ? defaultValue : Charset.forName(prop);
    }

    /**
     * Gets the named property as a double.
     *
     * @param name the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed double value of the property or {@code defaultValue} if it was undefined or could not be parsed.
     */
    public double getDoubleProperty(final String name, final double defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Double.parseDouble(prop);
            } catch (final Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets the named property as an integer.
     *
     * @param name the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed integer value of the property or {@code defaultValue} if it was undefined or could not be
     *         parsed.
     */
    public int getIntegerProperty(final String name, final int defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (final Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets the named property as a long.
     *
     * @param name the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed long value of the property or {@code defaultValue} if it was undefined or could not be parsed.
     */
    public long getLongProperty(final String name, final long defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Long.parseLong(prop);
            } catch (final Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @return the String value of the property or {@code null} if undefined.
     */
    public String getStringProperty(final String name) {
        return environment.get(name);
    }

    /**
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the String value of the property or {@code defaultValue} if undefined.
     */
    public String getStringProperty(final String name, final String defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    /**
     * Return the system properties or an empty Properties object if an error occurs.
     *
     * @return The system properties.
     */
    public static Properties getSystemProperties() {
        try {
            return new Properties(System.getProperties());
        } catch (final SecurityException ex) {
            LowLevelLogUtil.logException("Unable to access system properties.", ex);
            // Sandboxed - can't read System Properties
            return new Properties();
        }
    }

    /**
     * Provides support for looking up global configuration properties via environment variables, property files,
     * and system properties, in three variations:
     *
     * Normalized: all log4j-related prefixes removed, remaining property is camelCased with a log4j2 prefix for
     * property files and system properties, or follows a LOG4J_FOO_BAR format for environment variables.
     *
     * Legacy: the original property name as defined in the source pre-2.9.
     *
     * Tokenized: loose matching based on word boundaries.
     */
    private static class Environment {

        private static final String NORMALIZED_PREFIX = "log4j2.";
        private static final String PREFIXES = "(?:[Ll]og4j2?|org\\.apache\\.logging\\.log4j\\.)?";
        private static final Pattern PROPERTY_TOKENIZER = Pattern.compile(PREFIXES + "([A-Z]*[a-z0-9]+)[-._/]?");

        private final Map<String, String> literal = new ConcurrentHashMap<>();
        private final Map<String, String> normalized = new ConcurrentHashMap<>();
        private final Map<List<CharSequence>, String> tokenized = new ConcurrentHashMap<>();

        private Environment(final Properties props) {
            for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
                final String envKey = entry.getKey();
                final String value = entry.getValue();
                if (envKey.startsWith("LOG4J_")) {
                    final String key = envKey.substring(6);
                    literal.put(key, value);
                    final List<CharSequence> tokens = tokenize(envKey);
                    if (!tokens.isEmpty()) {
                        tokenized.put(tokens, value);
                        normalized.put("LOG4J" + joinAsAllCapsUnderscored(tokens), value);
                    }
                }
            }
            for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                literal.put(key, value);
                final List<CharSequence> tokens = tokenize(key);
                if (tokens.isEmpty()) {
                    normalized.put(NORMALIZED_PREFIX + key, value);
                } else {
                    tokenized.put(tokens, value);
                    normalized.put(NORMALIZED_PREFIX + joinAsCamelCase(tokens), value);
                }

            }
            final Properties systemProperties = getSystemProperties();
            for (Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                literal.put(key, value);
                // TODO: should this include tokenized lookups? could get screwy
                final List<CharSequence> tokens = tokenize(key);
                if (tokens.isEmpty()) {
                    normalized.put(NORMALIZED_PREFIX + key, value);
                } else {
                    tokenized.put(tokens, value);
                    normalized.put(NORMALIZED_PREFIX + joinAsCamelCase(tokens), value);
                }
            }
        }

        private static CharSequence joinAsAllCapsUnderscored(final List<CharSequence> tokens) {
            final StringBuilder sb = new StringBuilder();
            for (final CharSequence token : tokens) {
                sb.append('_');
                for (int i = 0; i < token.length(); i++) {
                    sb.append(Character.toUpperCase(token.charAt(i)));
                }
            }
            return sb.toString();
        }

        private static CharSequence joinAsCamelCase(final List<CharSequence> tokens) {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (final CharSequence token : tokens) {
                if (first) {
                    sb.append(token);
                } else {
                    sb.append(Character.toUpperCase(token.charAt(0)));
                    if (token.length() > 1) {
                        sb.append(token.subSequence(1, token.length()));
                    }
                }
                first = false;
            }
            return sb.toString();
        }

        private static List<CharSequence> tokenize(final CharSequence value) {
            // TODO: cache?
            List<CharSequence> tokens = new ArrayList<>();
            final Matcher matcher = PROPERTY_TOKENIZER.matcher(value);
            while (matcher.find()) {
                tokens.add(matcher.group(1).toLowerCase());
            }
            return tokens;
        }

        private String get(final String key) {
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
            if (literal.containsKey(key)) {
                return literal.get(key);
            }
            String prop = null;
            try {
                prop = System.getProperty(key);
            } catch (final SecurityException ignored) {
                // Ignore
            }
            if (prop != null) {
                return prop;
            }
            return tokenized.get(tokenize(key));
        }
    }

    /**
     * Extracts properties that start with or are equals to the specific prefix and returns them in a new Properties
     * object with the prefix removed.
     *
     * @param properties The Properties to evaluate.
     * @param prefix The prefix to extract.
     * @return The subset of properties.
     */
    public static Properties extractSubset(final Properties properties, final String prefix) {
        final Properties subset = new Properties();

        if (prefix == null || prefix.length() == 0) {
            return subset;
        }

        final String prefixToMatch = prefix.charAt(prefix.length() - 1) != '.' ? prefix + '.' : prefix;

        final List<String> keys = new ArrayList<>();

        for (final String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefixToMatch)) {
                subset.setProperty(key.substring(prefixToMatch.length()), properties.getProperty(key));
                keys.add(key);
            }
        }
        for (final String key : keys) {
            properties.remove(key);
        }

        return subset;
    }

    /**
     * Partitions a properties map based on common key prefixes up to the first period.
     *
     * @param properties properties to partition
     * @return the partitioned properties where each key is the common prefix (minus the period) and the values are
     * new property maps without the prefix and period in the key
     * @since 2.6
     */
    public static Map<String, Properties> partitionOnCommonPrefixes(final Properties properties) {
        final Map<String, Properties> parts = new ConcurrentHashMap<>();
        for (final String key : properties.stringPropertyNames()) {
            final String prefix = key.substring(0, key.indexOf('.'));
            if (!parts.containsKey(prefix)) {
                parts.put(prefix, new Properties());
            }
            parts.get(prefix).setProperty(key.substring(key.indexOf('.') + 1), properties.getProperty(key));
        }
        return parts;
    }

    /**
     * Returns true if system properties tell us we are running on Windows.
     * @return true if system properties tell us we are running on Windows.
     */
    public boolean isOsWindows() {
        return getStringProperty("os.name").startsWith("Windows");
    }

}
