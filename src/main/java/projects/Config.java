package projects;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new RuntimeException("config.properties couldn't found");
            }
            PROPS.load(is);
        } catch (IOException e) {
            throw new RuntimeException("config.properties couldn't read", e);
        }
    }

    //takes the delimeter from config.properties
    public static String get(String key) {
        return PROPS.getProperty(key);
    }
}