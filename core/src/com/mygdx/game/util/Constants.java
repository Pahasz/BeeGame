package com.mygdx.game.util;

import com.badlogic.gdx.graphics.Color;

/**
 * Created by Bbent_000 on 11/23/2014.
 */
public class Constants {

	//For the Grid stuff...
	public final static int GRIDACTIVE = 0;
	public final static int GRIDSTATIC = 1;

	//For interactable types...

	//Grid stuff
	public static int GRID_SQUARESIZE;
	public static int GRID_WIDTH;
	public static int GRID_HEIGHT;

    //CAMERA
    public final static float SCALE = 30;

	//WorldGen stuff
    public static int WORLDGEN_GENERATESPEED = 500;
    public static int WORLDGEN_RESOURCEGENERATESPEED = 5000000;

    //Terrain stuff
	public final static int TERRAIN_WATER = 0;
	public final static int TERRAIN_GRASS = 1;

    public final static int VISIBILITY_UNEXPLORED = 0;
    public final static int VISIBILITY_EXPLORED = 1;
    public final static int VISIBILITY_VISIBLE = 2;

    //Collider tags
    public final static int COLLIDER_CLICKABLE = 1;
    public final static int COLLIDER_DETECTOR = 2;

    //Colors
    public final static Color COLOR_UNEXPLORED = new Color(Color.BLACK);
    public final static Color COLOR_EXPLORED = new Color(0.3f, 0.3f, 0.3f, 1f);
    public final static Color COLOR_VISIBILE = new Color(Color.WHITE);

}
