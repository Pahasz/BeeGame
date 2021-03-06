package com.mygdx.game.behaviourtree.action;

import com.mygdx.game.behaviourtree.BlackBoard;
import com.mygdx.game.behaviourtree.LeafTask;

/**
 * Created by Paha on 8/18/2015.
 * Sets the blackboard.target as the Entity owner of the blackbaord.targetCraftingStation. Also nulls blackboard.targetNode.
 */
public class GetTargetFromCraftingStation extends LeafTask{
    public GetTargetFromCraftingStation(String name, BlackBoard blackBoard) {
        super(name, blackBoard);
    }

    @Override
    public boolean check() {
        return super.check();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        this.blackBoard.target = this.blackBoard.targetCraftingStation.getEntityOwner();
        this.blackBoard.targetNode = null;
        this.control.finishWithSuccess();
    }

    @Override
    public void end() {
        super.end();
    }
}
