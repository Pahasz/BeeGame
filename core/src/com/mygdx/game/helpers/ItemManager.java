package com.mygdx.game.helpers;

import com.mygdx.game.component.Item;

import java.util.HashMap;

/**
 * Created by Paha on 1/18/2015.
 */
public class ItemManager {
    private static HashMap<String, Item> itemMap = new HashMap<>(20);

    static{
        itemMap.put("Wood Log", new Item("Wood Log", true, 100, 100));
        itemMap.put("Stone", new Item("Stone", true, 100, 100));
    }

    public static Item getItemByName(String name){
        Item item = itemMap.get(name);
        if(item == null)
            throw new RuntimeException("Item of name '"+name+"' does not exist.");

        return new Item(item.getName(), item.isStackable(), item.getStackLimit(), item.getWeight());
    }

}