package com.mygdx.game.helpers.managers;

import com.mygdx.game.helpers.GH;

import java.util.HashMap;

/**
 * Created by Paha on 3/30/2015.
 */
public class DataManager {
    private static HashMap<Class<?>, HashMap<String, Object>> dataMap = new HashMap<>();

    /**
     * Adds a piece of data to this manager.
     * @param dataName The name of the data to add.
     * @param data The actual Object of data.
     * @param c The class of the data.
     * @param <T> The class type of the Data.
     */
    public static <T> void addData(String dataName, T data, Class<T> c){
        HashMap<String, Object> map = dataMap.get(c);
        if(map == null){
            map = new HashMap<>();
            dataMap.put(c, map);
        }

        map.put(dataName, data);
    }

    /**
     * Retrieves data from this manager.
     * @param dataName The name of the data we are retrieving.
     * @param c The Class of the data we are retrieving.
     * @param <T> The Class type of data.
     * @return The T data retrieved from this manager. Throws an exception if no data is found.
     */
    public static <T> T getData(String dataName, Class<T> c){
        HashMap<String, Object> map = dataMap.get(c);
        if(map == null || !map.containsKey(dataName)) GH.writeErrorMessage("Data of class "+c.getName()+" with name "+dataName+" does not exist!");

        return (T)map.get(dataName);
    }
}