package com.mygdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.component.CraftingStation;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.util.DataBuilder;
import com.mygdx.game.util.GH;
import com.mygdx.game.util.ItemNeeded;
import com.mygdx.game.util.gui.GUI;
import com.mygdx.game.util.managers.DataManager;
import com.sun.istack.internal.Nullable;

/**
 * Created by Paha on 8/16/2015.
 * A Window for interacting with Entities that provide crafting options.
 */
public class CraftingWindow extends Window{
    private Rectangle craftRect, selectRect, infoRect, stalledRect, openRect, craftButtonRect;
    private TextureRegion craftBackground, selectBackground, infoBackground, stalledBackground, openBackground;
    private com.badlogic.gdx.scenes.scene2d.ui.Window CraftingWindow;
    private CraftingStation craftingStation;
    private DataBuilder.JsonItem selectedItem;

    private Vector2 offset;

    public CraftingWindow(PlayerInterface playerInterface, Entity target) {
        super(playerInterface, target);

        this.craftRect = new Rectangle();
        this.selectRect = new Rectangle();
        this.infoRect = new Rectangle();
        this.stalledRect = new Rectangle();
        this.openRect = new Rectangle();

        this.craftBackground = new TextureRegion(ColonyGame.assetManager.get("craftingWindowBackground", Texture.class));
        this.selectBackground = new TextureRegion(ColonyGame.assetManager.get("craftingWindowSelectionBackground", Texture.class));
        this.infoBackground = new TextureRegion(ColonyGame.assetManager.get("craftingWindowInfoBackground", Texture.class));
        this.stalledBackground = new TextureRegion(ColonyGame.assetManager.get("stalledJobsBackground", Texture.class));
        this.openBackground = new TextureRegion(ColonyGame.assetManager.get("openJobsBackground", Texture.class));

        this.craftingStation = this.target.getComponent(CraftingStation.class);
        this.offset = new Vector2();

        com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle style = new com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(this.playerInterface.UIStyle.font, Color.BLACK, new TextureRegionDrawable(this.craftBackground));
        this.CraftingWindow = new com.badlogic.gdx.scenes.scene2d.ui.Window("WindowTop", style);
        this.CraftingWindow.addListener(new ClickListener(){

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                offset.set(GH.getFixedScreenMouseCoords().x - CraftingWindow.getX(), GH.getFixedScreenMouseCoords().y - CraftingWindow.getY());
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                super.touchDragged(event, x, y, pointer);
                Vector2 mouse = GH.getFixedScreenMouseCoords();
                CraftingWindow.setPosition(mouse.x - offset.x, mouse.y - offset.y);
            }
        });
        this.CraftingWindow.setSize(700, 500);
        this.CraftingWindow.setPosition(Gdx.graphics.getWidth()/2 - 700/2, Gdx.graphics.getHeight()/2 - 500/2);
        this.playerInterface.stage.addActor(this.CraftingWindow);

    }

    @Override
    public boolean update(SpriteBatch batch) {
        if(this.active) {

//            GUI.Texture(this.craftBackground, batch, this.craftRect);
//            GUI.Texture(this.selectBackground, batch, this.selectRect);
//            GUI.Texture(this.infoBackground, batch, this.infoRect);
//            GUI.Texture(this.openBackground, batch, this.openRect);
//            GUI.Texture(this.stalledBackground, batch, this.stalledRect);

            //GUI.Texture(new TextureRegion(this.playerInterface.blueSquare), ColonyGame.batch, this.dragWindowRect);
//            this.drawCraftingWindow(batch, this.playerInterface.UIStyle);
        }

        return super.update(batch);
    }

    private void drawCraftingWindow(SpriteBatch batch, GUI.GUIStyle style){
        String[] items = this.craftingStation.getCraftingList();
        style.alignment = Align.left;
        style.paddingLeft = 5;

        float labelHeight = 25;
        float startX = selectRect.x;
        float startY = selectRect.y + selectRect.height - labelHeight;

        for(int i=0;i<items.length;i++){
            String item = items[i];
            DataBuilder.JsonItem _itemRef = DataManager.getData(item, DataBuilder.JsonItem.class);

            Rectangle.tmp.set(startX, startY - i*labelHeight, selectRect.width, labelHeight);
            if(GUI.Label(_itemRef.getDisplayName(), batch, Rectangle.tmp, style) == GUI.JUSTUP)
                this.selectedItem = _itemRef;
        }

        if(this.selectedItem != null) {
            this.drawCraftingInfo(this.selectedItem, batch, this.infoRect, style, this.infoBackground);
        }
    }

    /**
     * Draws the crafting info for an item inside a Rectangle.
     * @param itemRef The item reference.
     * @param batch The SpriteBatch to draw with.
     * @param rect The Rectangle to draw inside.
     * @param style The GUIStyle to draw with. This may be null.
     */
    private void drawCraftingInfo(DataBuilder.JsonItem itemRef, SpriteBatch batch, Rectangle rect, @Nullable GUI.GUIStyle style, TextureRegion windowTexture){
        GUI.Texture(windowTexture, batch, rect); //Draw the texture
        Array<ItemNeeded> mats = itemRef.materialsForCrafting, raw = itemRef.rawForCrafting; //Get the lists.

        float iconSize = 32;
        float labelWidth = 50;
        float startX = rect.x + labelWidth;
        float startY = rect.y + 20;
        float spacingX = iconSize + 5;
        float spacingY = iconSize + 10;
        TextureRegion _icon;
        DataBuilder.JsonItem _item;

        style.alignment = Align.left;
        style.paddingLeft = 5;
        GUI.Label("Raw:", batch, rect.x, startY, labelWidth, iconSize, style);
        style.alignment = Align.center;
        style.paddingLeft = 0;

        //Draw the raw mats first
        for(int i=0;i<raw.size;i++){
            _item = DataManager.getData(raw.get(i).itemName, DataBuilder.JsonItem.class);
            _icon = _item.iconTexture;
            float x = startX + spacingX*i, y = startY;
            GUI.Texture(_icon, batch, x, startY, iconSize, iconSize);
            GUI.Label("x"+raw.get(i).amountNeeded, batch, x, startY - 15, iconSize, 15, style);
        }

        style.alignment = Align.left;
        style.paddingLeft = 5;
        GUI.Label("Mats:", batch, rect.x, startY + spacingY, labelWidth, iconSize, style);
        style.alignment = Align.center;
        style.paddingLeft = 0;

        //Draw the materials
        for(int i=0;i<mats.size;i++){
            _item = DataManager.getData(mats.get(i).itemName, DataBuilder.JsonItem.class);
            _icon = _item.iconTexture;
            float x = startX + spacingX*i, y = startY + spacingY;
            GUI.Texture(_icon, batch, startX + spacingX*i, startY + spacingY, iconSize, iconSize);
            GUI.Label("x"+mats.get(i).amountNeeded, batch, x, y - 15, iconSize, 15, style);
        }

        //Draw the name of the item above the icon...
        GUI.Label(""+itemRef.getDisplayName(), batch, rect.x, rect.y + rect.height - 25,
                rect.width, 25);

        //Draw the icon for the item we are crafting...
        GUI.Texture(itemRef.iconTexture, batch, rect.x + rect.width / 2 - iconSize / 2,
                rect.y + rect.height - iconSize - 25, iconSize, iconSize);
    }

    @Override
    public void resize(int width, int height) {
        this.craftRect.set(width / 2 - 600 / 2, height / 2 - 500 / 2, 600, 500);

        this.selectRect.set(this.craftRect.x + this.craftRect.width * 0.034f, this.craftRect.y + this.craftRect.height * 0.03f,
                this.craftRect.width * 0.333f, this.craftRect.height * 0.892f);

        this.infoRect.set(this.craftRect.x + this.craftRect.width * 0.678f, this.craftRect.y + this.craftRect.height * 0.502f,
                this.craftRect.width * 0.2857f, this.craftRect.height * 0.422f);

        this.openRect.set(this.craftRect.x + this.craftRect.width * 0.357f, this.craftRect.y + this.craftRect.height * 0.502f,
                this.craftRect.width * 0.2857f, this.craftRect.height * 0.422f);

        this.stalledRect.set(this.craftRect.x + this.craftRect.width * 0.357f, this.craftRect.y + this.craftRect.height * 0.03f,
                this.craftRect.width * 0.2857f, this.craftRect.height * 0.422f);

    }

    @Override
    public void destroy() {
        this.CraftingWindow.remove();
        super.destroy();
    }
}
