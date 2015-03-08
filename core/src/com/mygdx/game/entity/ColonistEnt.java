package com.mygdx.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.component.*;
import com.mygdx.game.component.collider.Collider;
import com.mygdx.game.helpers.Constants;

/**
 * Created by Paha on 1/18/2015.
 */
public class ColonistEnt extends Entity{
    public ColonistEnt(Vector2 position, float rotation, Texture graphic, SpriteBatch batch, int drawLevel) {
        super(position, rotation, graphic, batch, drawLevel);
        this.name = "Colonist";

        this.addComponent(new Colonist());
        this.addComponent(new GridComponent(Constants.GRIDACTIVE, ColonyGame.worldGrid, 1));
        this.addComponent(new Interactable("humanoid"));
        this.addComponent(new Stats());
        this.addComponent(new Skills());
        this.addComponent(new Inventory());
        this.addComponent(new BehaviourManagerComp("colonist"));
        this.makeCollider();

        this.addTag(Constants.ENTITY_HUMANOID);
        this.addTag(Constants.ENTITY_COLONIST);
    }

    private void makeCollider(){
        CircleShape shape = new CircleShape();
        shape.setRadius(6f);
        Collider collider = this.addComponent(new Collider(ColonyGame.world, shape));

        collider.body.setType(BodyDef.BodyType.DynamicBody);
        collider.fixture.setSensor(true);

        collider.fixture.setFriction(0.5f);
        collider.fixture.setDensity(1f);
    }
}
