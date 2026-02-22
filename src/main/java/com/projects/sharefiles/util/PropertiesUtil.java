package com.projects.sharefiles.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    static Properties prop;
    static{
        loadProperties();
    }
    private static void loadProperties(){
        try(InputStream in = PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties")){
            prop = new Properties();
            if(in != null){
                prop.load(in);

            }
        }catch (IOException ex) {
            System.err.println("Could not read properties: " + ex.getMessage());
        }
    }
    public static String getValue(String key){
        if(prop == null){loadProperties();}
        return prop.getProperty(key);
    }

    public static String getValue(String key, String defaultValue) {
        if(prop == null){loadProperties();}
        if (prop == null) return defaultValue;

        String value = prop.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }
}
