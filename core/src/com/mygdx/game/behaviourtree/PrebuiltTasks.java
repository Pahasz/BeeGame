package com.mygdx.game.behaviourtree;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.behaviourtree.action.*;
import com.mygdx.game.behaviourtree.composite.Selector;
import com.mygdx.game.behaviourtree.composite.Sequence;
import com.mygdx.game.behaviourtree.control.ParentTaskController;
import com.mygdx.game.behaviourtree.decorator.AlwaysTrue;
import com.mygdx.game.behaviourtree.decorator.Invert;
import com.mygdx.game.behaviourtree.decorator.RepeatUntilCondition;
import com.mygdx.game.behaviourtree.decorator.RepeatUntilFailure;
import com.mygdx.game.component.*;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.util.Constants;
import com.mygdx.game.util.FloatingText;
import com.mygdx.game.util.Grid;

/**
 * Created by Paha on 4/11/2015.
 */
public class PrebuiltTasks {
    public static Task gatherResource(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Selector:
         *      Sequence:
         *          Sequence
         *              figure out tools for gathering
         *              find the tool shed
         *              check the shed and reserve
         *              find path to shed
         *              move to shed
         *              transfer tools
         *
         *          Repeat (until we are full on the items toggled):
         *              find resource
         *              reserve resource
         *              find path to resource
         *              move to resource
         *              gather resource
         *
         *          find inventory
         *          find path to inventory
         *          move to inventory
         *          transfer items.
         *
         *     explore
         */

        Selector gatherOrExplore = new Selector("Gathering", blackBoard);
        Sequence sequence = new Sequence("Gathering Resource", blackBoard);

        //All this should be under a repeat.
        Sequence innerGatherSeq = new Sequence("Gathering", blackBoard);
        RepeatUntilCondition repeatGatherSequence = new RepeatUntilCondition("Repeat", blackBoard, innerGatherSeq);
        FindResource fr = new FindResource("Finding resource", blackBoard);
        ReserveResource rr = new ReserveResource("Reserving", blackBoard); //Reserve it separately from the find resource, since find resource is on a thread. This avoids data races.
        FindPath fpResource = new FindPath("Finding Path to Resource", blackBoard);
        MoveTo mtResource = new MoveTo("Moving to Resource", blackBoard);
        Gather gather = new Gather("Gathering Resource", blackBoard);

        //Selector - either gather sequence or explore.
        gatherOrExplore.control.addTask(sequence);
        gatherOrExplore.control.addTask(exploreUnexplored(blackBoard, behComp));

        sequence.control.addTask(getTools(blackBoard, behComp));

        //Add the repeat gather task to the main sequence, and then the rest to the inner sequence under repeat.
        sequence.control.addTask(repeatGatherSequence);
        sequence.control.addTask(returnItems(blackBoard, behComp));

        innerGatherSeq.control.addTask(fr);
        innerGatherSeq.control.addTask(rr);
        innerGatherSeq.control.addTask(fpResource);
        innerGatherSeq.control.addTask(mtResource);
        innerGatherSeq.control.addTask(gather);

        //If we are not actually set to find any resources, just fail the task!
        gatherOrExplore.control.callbacks.checkCriteria = task -> !task.blackBoard.resourceTypeTags.isEmpty();



        //Reset some values.
        sequence.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.reset();
        };

        //If we failed during the find resource stage, that means we need to explore. Fail the repeat task so we don't repeat!
        fr.control.callbacks.failureCallback = task -> repeatGatherSequence.getControl().finishWithFailure();

        //Check if we can still add more items that we are looking for. Return true if we can't (to end the repeat)
        //and false to keep repeating.
        repeatGatherSequence.getControl().callbacks.successCriteria = tsk -> {
            Task task = (Task)tsk;
            Inventory inv = task.blackBoard.myInventory;
            //Get a list of the items we are searching for. If we can hold more, keep searching for what we need.
            String[] itemNames = task.blackBoard.resourceTypeTags.getTagsAsString();
            for(String itemName : itemNames) if(inv.canAddItem(itemName)) return false;
            return true;
        };

        return gatherOrExplore;
    }

    public static Task exploreUnexplored(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         * Sequence:
         *  find closest unexplored tile
         *  get path to tile
         *  move to tile
         */

        //Reset these. Left over assignments from other jobs will cause the explore behaviour to simply move to the wrong area.
        blackBoard.target = null;
        blackBoard.targetNode = null;

        Sequence sequence = new Sequence("exploreUnexplored", blackBoard);

        FindClosestUnexplored findClosestUnexplored = new FindClosestUnexplored("Finding Closest Unexplored Location", blackBoard);
        FindPath findPathToUnexplored = new FindPath("Finding Path to Unexplored", blackBoard);
        MoveTo moveToLocation = new MoveTo("Moving to Explore", blackBoard);

        //Make sure we clear our targets and target node first to get a fresh unexplored area.
        sequence.control.callbacks.startCallback = task -> {
            task.blackBoard.target = null;
            task.blackBoard.targetNode = null;
        };

        //Get the main building of the colony as our target to explore around.
        findClosestUnexplored.control.callbacks.startCallback = task -> {
            Colonist col = task.blackBoard.myManager.getEntityOwner().getComponent(Colonist.class);
            task.blackBoard.target = col.getOwningColony().getOwnedFromColony(Building.class, building -> building.getEntityOwner().getTags().hasTag("main")).getEntityOwner();
        };

        ((ParentTaskController) sequence.getControl()).addTask(findClosestUnexplored);
        ((ParentTaskController) sequence.getControl()).addTask(findPathToUnexplored);
        ((ParentTaskController) sequence.getControl()).addTask(moveToLocation);

        return sequence;
    }

    private static Task getTools(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Sequence
         *      figure out tools
         *      find tool shed
         *      check and reserve tools
         *      get path to tool shed
         *      move to tool shed
         *      transfer tools
         */

        AlwaysTrue alwaysTrue = new AlwaysTrue("Always true", blackBoard);
        Sequence seq = new Sequence("Getting tools", blackBoard);
        GetToolsForGathering get = new GetToolsForGathering("Figuring tools", blackBoard);
        GetBuildingFromColony findShed = new GetBuildingFromColony("Finding tool shed", blackBoard);
        CheckAndReserve checkShed = new CheckAndReserve("CheckingReserving", blackBoard);
        FindPath fpToShed = new FindPath("PathToShed", blackBoard);
        MoveTo mtShed = new MoveTo("Moving", blackBoard);
        TransferItemsFromTargetToMe transferTools = new TransferItemsFromTargetToMe("Transferring", blackBoard);

        //Reset some values when we start. Reset the itemTransfer, and make sure we are taking reserved items.
        seq.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.reset();
            task.blackBoard.itemTransfer.takingReserved = true;
            task.blackBoard.targetNode = null;
            task.blackBoard.tagsToSearch = new String[]{"building", "equipment"};
        };

        seq.control.addTask(get);
        seq.control.addTask(findShed);
        seq.control.addTask(checkShed);
        seq.control.addTask(fpToShed);
        seq.control.addTask(mtShed);
        seq.control.addTask(transferTools);

        alwaysTrue.setTask(seq);

        return alwaysTrue;
    }

    public static Task returnItems(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Sequence
         *      Find inventory
         *      get path to inventory
         *      move to inventory
         *      transfer all items.  (except tools)
         */

        Sequence mainSeq = new Sequence("Dropping Off", blackBoard);
        GetBuildingFromColony getStorage = new GetBuildingFromColony("Getting building", blackBoard);
        FindPath fpStorage = new FindPath("Finding path", blackBoard);
        MoveTo mtStorage = new MoveTo("Moving to storage", blackBoard);
        TransferItemsFromMeToTarget transfer = new TransferItemsFromMeToTarget("Transferring", blackBoard);

        mainSeq.control.addTask(getStorage);
        mainSeq.control.addTask(fpStorage);
        mainSeq.control.addTask(mtStorage);
        mainSeq.control.addTask(transfer);

        mainSeq.control.callbacks.startCallback = task -> {
            task.blackBoard.targetNode = null;
            task.blackBoard.tagsToSearch = new String[]{"building", "storage"};
        };

        getStorage.control.callbacks.successCallback = task -> {
            task.blackBoard.itemTransfer.reset();
            task.blackBoard.itemTransfer.transferAll = true;
            task.blackBoard.itemTransfer.itemTypesToIgnore.add("tool"); //Ignore tools.
        };

        return mainSeq;
    }

    public static Task returnTools(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Sequence:
         *      get list of tools that we have.
         *      *not implemented and optional* check if inventory has space
         *      find closest tool shed
         *      find path to tool shed
         *      move to tool shed
         *      transfer tools
         */

        Sequence seq = new Sequence("Returning tools", blackBoard);
        GetToolsInInventory getToolsInInventory = new GetToolsInInventory("Getting list of tools", blackBoard);
        GetBuildingFromColony findShed = new GetBuildingFromColony("Finding tool shed", blackBoard);
        FindPath fpToShed = new FindPath("PathToShed", blackBoard);
        MoveTo mtShed = new MoveTo("Moving", blackBoard);
        TransferItemsFromMeToTarget transferTools = new TransferItemsFromMeToTarget("Transferring", blackBoard);

        seq.control.addTask(getToolsInInventory);
        seq.control.addTask(findShed);
        seq.control.addTask(fpToShed);
        seq.control.addTask(mtShed);
        seq.control.addTask(transferTools);

        //Set some flags and get a list of item names to be removed.
        seq.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.reset();
            task.blackBoard.tagsToSearch = new String[]{"building", "equipment"};
            task.blackBoard.targetNode = null;
        };

        return seq;

    }

    public static Task build(BlackBoard blackboard, BehaviourManagerComp behComp){
        /**
         *  Selector
         *      Sequence{
         *          find a building under construction
         *          Sequence - Always true decorator
         *              get list of items needed to build
         *              find inventory
         *              reserve items
         *              find path to inventory
         *              move to inventory
         *              transfer items.
         *         Sequence
         *              get path to building
         *              move to building
         *              transfer any materials
         *              build
         *      }
         *
         *      idle
         */

        /**
         * TODO When the building only needs a limited amount (ie: building needs 5 stone and the colonist has 10 stone), the colonists inventory
         * TODO is cleared, but not all items are transferred. Make sure we only remove from the inventory what we need to. Maybe... can't reproduce it.
        */

        Selector mainSelector = new Selector("Build or Idle", blackboard);
        Sequence constructionSeq = new Sequence("Building Sequence", blackboard);
        GetConstruction getConstruction = new GetConstruction("Getting Construction", blackboard);

        //This is where we possible get items for the construction. Optional branch
        Sequence getItemsForConstSeq = new Sequence("Get Items Sequence", blackboard);
        AlwaysTrue getItemsSeqTrue = new AlwaysTrue("Return True", blackboard, getItemsForConstSeq);
        GetItemsForConstructable getItems = new GetItemsForConstructable("Getting item list for construction", blackboard);
        FindStorageWithItem findItem = new FindStorageWithItem("Finding storage with items", blackboard);
        CheckAndReserve reserve = new CheckAndReserve("Check and reserve items", blackboard);
        FindPath fpToStorage = new FindPath("Find path to storage", blackboard);
        MoveTo mtStorage = new MoveTo("Move to storage", blackboard);
        TransferItemsFromTargetToMe transferItems = new TransferItemsFromTargetToMe("Transfer items from me to construction", blackboard);

        //This is where we build
        Sequence buildSeq = new Sequence("Build construction sequence", blackboard);
        GetTargetFromConstructable getTarget = new GetTargetFromConstructable("Getting target from construction", blackboard);
        CheckTargetOrMeHasAnyItems checkHasItem = new CheckTargetOrMeHasAnyItems("Checking if me or target has item", blackboard);
        FindPath fpToBuilding = new FindPath("Finding path to construction", blackboard);
        MoveTo mtBuilding = new MoveTo("Moving to construction", blackboard);
        TransferItemsFromMeToTarget transferToBuilding = new TransferItemsFromMeToTarget("Transferring items to construction", blackboard);
        Construct construct = new Construct("Constructing", blackboard);

        //The main selector between constructing and idling
        mainSelector.control.addTask(constructionSeq);
        mainSelector.control.addTask(idleTask(blackboard, behComp));

        //The main sequence of construction.
        constructionSeq.control.addTask(getConstruction);
        constructionSeq.control.addTask(getItemsSeqTrue); //Use the always true as getting items isn't necessary
        constructionSeq.control.addTask(buildSeq);

        //Get items sequence
        //Find a storage with any of the items we need.
        getItemsForConstSeq.control.addTask(getItems);
        getItemsForConstSeq.control.addTask(findItem);
        getItemsForConstSeq.control.addTask(reserve);
        getItemsForConstSeq.control.addTask(fpToStorage);
        getItemsForConstSeq.control.addTask(mtStorage);
        getItemsForConstSeq.control.addTask(transferItems);

        //Build sequence
        buildSeq.control.addTask(getTarget);
        buildSeq.control.addTask(checkHasItem);
        buildSeq.control.addTask(fpToBuilding);
        buildSeq.control.addTask(mtBuilding);
        buildSeq.control.addTask(transferToBuilding);
        buildSeq.control.addTask(construct);

        //Time for the crap
        constructionSeq.control.callbacks.startCallback = task -> {
            task.blackBoard.target = null;
            task.blackBoard.targetNode = null;
        };

        mainSelector.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.reset();
            task.blackBoard.itemTransfer.takingReserved = true;
            task.blackBoard.targetNode = null;
        };


        //Set the target and reset the targetNode for pathing.
        buildSeq.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.takingReserved = false;
            task.blackBoard.targetNode = null;
        };

        return mainSelector;
    }

    public static Task idleTask(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         * Sequence:
         *  find random nearby location
         *  find path to location
         *  move to location
         *  idle for random amount of time
         */

        Sequence sequence = new Sequence("idle", blackBoard);

        FindRandomNearbyLocation findNearbyLocation = new FindRandomNearbyLocation("Finding Nearby Location", blackBoard, blackBoard.idleDistance);
        FindPath findPath = new FindPath("Finding Path to Nearby Location", blackBoard);
        MoveTo moveTo = new MoveTo("Moving to Nearby Location", blackBoard);
        Idle idle = new Idle("Standing Still", blackBoard);

        ((ParentTaskController) sequence.getControl()).addTask(findNearbyLocation);
        ((ParentTaskController) sequence.getControl()).addTask(findPath);
        ((ParentTaskController) sequence.getControl()).addTask(moveTo);
        ((ParentTaskController) sequence.getControl()).addTask(idle);

        return sequence;
    }

    public static Task consumeTask(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         * Sequence:
         *  check that the inventory of the inventory has the item effect we want/need
         *  find path to inventory
         *  move to inventory
         *  transfer needed item to me (colonist)
         *  consume item
         */

        //TODO The itemTransfer stuff was refactored and probably broke this area, namely the CheckInventoryHasItemWithEffectAndReserve class.

        Sequence sequence = new Sequence("consume", blackBoard);

        CheckInventoryHasItemWithEffectAndReserve check = new CheckInventoryHasItemWithEffectAndReserve("Checking Inventory", blackBoard);
        FindPath fp = new FindPath("Finding Path to consume item", blackBoard);
        MoveTo moveTo = new MoveTo("Moving to consume item", blackBoard);
        TransferItemsFromTargetToMe tr = new TransferItemsFromTargetToMe("Transferring Consumable", blackBoard);
        Consume consume = new Consume("Consuming Item", blackBoard);

        ((ParentTaskController) sequence.getControl()).addTask(check);
        ((ParentTaskController) sequence.getControl()).addTask(fp);
        ((ParentTaskController) sequence.getControl()).addTask(moveTo);
        ((ParentTaskController) sequence.getControl()).addTask(tr);
        ((ParentTaskController) sequence.getControl()).addTask(consume);

        sequence.getControl().callbacks.startCallback = task->{
            task.blackBoard.itemTransfer.reset(); //Reset item transfers
            task.blackBoard.targetNode = null; //We don't want this set.

            //Get a inventory building from my colony, store the entity as the target, and set the from/to inventory.
            Building storage = task.blackBoard.myManager.getEntityOwner().getComponent(Colonist.class).getOwningColony().getOwnedFromColony(Building.class, b -> b.getEntityOwner().getTags().hasTag("storage"));
            task.blackBoard.target = storage.getEntityOwner();
            task.blackBoard.itemTransfer.fromInventory = storage.getComponent(Inventory.class);
            task.blackBoard.itemTransfer.toInventory = blackBoard.myManager.getEntityOwner().getComponent(Inventory.class);
        };

        return sequence;
    }

    /**
     * Searches for a target, hunts the target, and gathers the resources from it. This Task is composed of the attackTarget and gatherResource Tasks.
     * @param blackBoard The blackboard of this Task.
     * @param behComp THe BehaviourManager that owns this Task.
     * @return The added Task.
     */
    public static Task searchAndHunt(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         * Sequence:
         *  find closest entity (animal to hunt)
         *  repeatUntilCondition: (Attack target)
         *      Parallel:
         *          find path to target
         *          move to target
         *          attack target
         *  Sequence:   (Gather resource)
         *      get path to resource
         *      move to resource
         *      gather resource
         *      find inventory building
         *      find path to building
         *      move to building
         *      transfer itemNames to inventory
         */

        Sequence mainSeq = new Sequence("hunt", blackBoard);

        FindClosestEntity fc = new FindClosestEntity("Finding Closest Animal", blackBoard);

        ((ParentTaskController) mainSeq.getControl()).addTask(fc); //Add the find closest entity job.
        ((ParentTaskController) mainSeq.getControl()).addTask(attackTarget(blackBoard, behComp)); //Add the attack target task to this sequence.
        ((ParentTaskController) mainSeq.getControl()).addTask(gatherTarget(blackBoard, behComp)); //Add the gather target task to this sequence.

        //Get an alive animal.
        fc.control.callbacks.successCriteria = ent -> {
            Entity entity = (Entity)ent;
            return entity.getTags().hasTags("animal", "alive");
        };

        //Creates a floating text object when trying to find an animal fails.
        fc.getControl().callbacks.failureCallback = task -> {
            Vector2 pos = blackBoard.myManager.getEntityOwner().getTransform().getPosition();
            new FloatingText("Couldn't find a nearby animal to hunt!", new Vector2(pos.x, pos.y + 1), new Vector2(pos.x, pos.y + 10), 1.5f, 0.8f);
            //TODO What did this do? Change to default state? Redundant I think.
            //behComp.changeTaskImmediate(behComp.getBehaviourStates().getDefaultState().getUserData().apply(behComp.getBlackBoard(), behComp));
        };

        return mainSeq;
    }

    /**
     * Generates the Task for hunting a target. This Task requires that the blackboard has the 'target' parameter set or it will fail.
     * @param blackBoard The blackboard of the Task.
     * @param behComp The BehaviourComponentComp that will own this Task.
     * @return The added Task.
     */
    public static Task attackTarget(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Sequence
         *      follow
         *      attack target
         */
        Sequence mainSeq = new Sequence("Attack Target", blackBoard);
        Follow follow = new Follow("Following", blackBoard);
        Attack attack = new Attack("Attacking Target", blackBoard);

        mainSeq.control.callbacks.startCallback = task -> {
            task.blackBoard.targetNode = null;
            task.blackBoard.followDis = 5; //TODO Test number
        };

        //Add the main repeat.
        mainSeq.control.addTask(follow);

        //The attack task after we have got within range!
        mainSeq.control.addTask(attack);

        return mainSeq;
    }

    private static Task gatherTarget(BlackBoard blackBoard, BehaviourManagerComp behComo){
        /**
         * Sequence:
         *  find path to resource
         *  move to resource
         *  gather resource
         *  find path to inventory
         *  move to inventory
         *  transfer itemNames to inventory
         */

        Sequence sequence = new Sequence("gatherTarget", blackBoard);

        //Create all the job objects we need...
        FindPath findPath = new FindPath("Finding Path to Resource", blackBoard);
        MoveTo move = new MoveTo("Moving to Resource", blackBoard);
        Gather gather = new Gather("Gathering Resource", blackBoard);
        GetBuildingFromColony findStorage = new GetBuildingFromColony("Finding storage.", blackBoard);
        FindPath findPathToStorage = new FindPath("Finding Path to Storage", blackBoard);
        MoveTo moveToStorage = new MoveTo("Moving to Storage", blackBoard);
        TransferItemsFromMeToTarget transferItems = new TransferItemsFromMeToTarget("Transferring Resources", blackBoard);

        ((ParentTaskController)sequence.getControl()).addTask(findPath);
        ((ParentTaskController)sequence.getControl()).addTask(move);
        ((ParentTaskController)sequence.getControl()).addTask(gather);
        ((ParentTaskController)sequence.getControl()).addTask(findStorage);
        ((ParentTaskController)sequence.getControl()).addTask(findPathToStorage);
        ((ParentTaskController)sequence.getControl()).addTask(moveToStorage);
        ((ParentTaskController)sequence.getControl()).addTask(transferItems);

        //Set the 'fromInventory' field and set the resource as taken by us!
        sequence.getControl().callbacks.startCallback = task -> {
            task.blackBoard.tagsToSearch = new String[]{"building", "storage"};
        };

        return sequence;
    }

    public static Task defendArea(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Parallel
         *      Get closest valid target to attack
         *      Attack target
         */

        return null;
    }

    public static Task fish(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         * Find a fishing spot.
         * Path to it.
         * Move to it.
         * Fish.
         * Find base.
         * Path to it.
         * Move to it.
         * Transfer all resources.
         */

        blackBoard.itemTransfer.fromInventory = blackBoard.myManager.getEntityOwner().getComponent(Inventory.class);

        Sequence seq = new Sequence("fish", blackBoard);

        FindClosestTile fct = new FindClosestTile("Finding fishing spot", blackBoard);
        FindPath fp = new FindPath("Finding path to fishing spot", blackBoard);
        MoveTo mt = new MoveTo("Moving to fishing spot", blackBoard);
        Fish fish = new Fish("Fishing", blackBoard);
        FindClosestEntity fc = new FindClosestEntity("Finding base", blackBoard);
        FindPath fpBase = new FindPath("Finding path to base", blackBoard);
        MoveTo mtBase = new MoveTo("Moving to base", blackBoard);
        TransferItemsFromMeToTarget tr = new TransferItemsFromMeToTarget("Transferring resources", blackBoard);

        seq.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.reset();
            task.blackBoard.itemTransfer.transferAll = true;
        };

        //We need to tell this fct what can pass as a valid tile.
        fct.getControl().callbacks.successCriteria = nd -> {
            Grid.Node node = (Grid.Node)nd;
            Grid.TerrainTile tile = blackBoard.colonyGrid.getNode(node.getX(), node.getY()).getTerrainTile();
            int visibility = blackBoard.colonyGrid.getVisibilityMap()[node.getX()][node.getY()].getVisibility();

            return tile.tileRef.category.equals("LightWater") && visibility != Constants.VISIBILITY_UNEXPLORED;
        };

        //We want to remove the last step in our destination (first in the list) since it will be on the shore line.
        fp.getControl().callbacks.successCallback = task -> blackBoard.path.removeFirst();

        fc.getControl().callbacks.successCallback = task -> blackBoard.itemTransfer.toInventory = blackBoard.target.getComponent(Inventory.class);

        ((ParentTaskController)seq.getControl()).addTask(fct);
        ((ParentTaskController)seq.getControl()).addTask(fp);
        ((ParentTaskController)seq.getControl()).addTask(mt);
        ((ParentTaskController)seq.getControl()).addTask(fish);
        ((ParentTaskController)seq.getControl()).addTask(fc);
        ((ParentTaskController)seq.getControl()).addTask(fpBase);
        ((ParentTaskController)seq.getControl()).addTask(mtBase);
        ((ParentTaskController)seq.getControl()).addTask(tr);

        return seq;
    }

    public static Task fleeTarget(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         * Sequence
         *  FindNearbyTile - find a tile away from the target we are fleeing
         *  MoveTo - Move directly to the tile, no need to find a path
         */

        Task sequence = new Sequence("flee", blackBoard);
        Task repeatFiveTimes = new RepeatUntilCondition("Fleeing", blackBoard, sequence);
        Task findNearbyTile = new FindNearbyTile("Finding place to flee to!", blackBoard);
        Task moveTo = new MoveTo("Moving away!", blackBoard);

        ((ParentTaskController)sequence.getControl()).addTask(findNearbyTile);
        ((ParentTaskController)sequence.getControl()).addTask(moveTo);

        //We want to end when the counter is more than 5.
        repeatFiveTimes.getControl().callbacks.successCriteria = tsk -> {
            Task task = (Task)tsk;
            return task.blackBoard.counter > 5 || (findNearbyTile.getControl().hasFinished() && findNearbyTile.getControl().hasFailed());
        };

        //Reset the counter when we start.
        repeatFiveTimes.getControl().callbacks.startCallback = task -> task.blackBoard.counter = 0;

        //Reset some stuff.
        sequence.getControl().callbacks.startCallback = task -> {
            task.blackBoard.target = null;
            task.blackBoard.targetNode = null;
            task.blackBoard.counter = 0;
        };

        //Calculate my distance.
        sequence.getControl().callbacks.startCallback = task -> {
            task.blackBoard.myDisToTarget = (int)(Math.abs(blackBoard.myManager.getEntityOwner().getTransform().getPosition().x - blackBoard.target.getTransform().getPosition().x)
                + Math.abs(blackBoard.myManager.getEntityOwner().getTransform().getPosition().y - blackBoard.target.getTransform().getPosition().y));
        };

        //Try to find a tile away from our target to flee to.
        findNearbyTile.getControl().callbacks.successCriteria = n -> {
            Grid.Node node = (Grid.Node)n;

            //The distance to the target (node distance).
            float nodeDisToTarget = Math.abs(node.getXCenter() - blackBoard.target.getTransform().getPosition().x)
                    + Math.abs(node.getYCenter() - blackBoard.target.getTransform().getPosition().y);

            //If the node distance is greater than my distance to the target, we'll take it!
            if(nodeDisToTarget > blackBoard.myDisToTarget){
                blackBoard.targetNode = node;
                blackBoard.path.clear();
                blackBoard.path.add(new Vector2(node.getXCenter(), node.getYCenter()));
                return true;
            }

            return false;
        };

        moveTo.getControl().callbacks.finishCallback = task -> task.blackBoard.counter++;

        return repeatFiveTimes;

    }

    public static Task returnToBase(BlackBoard blackBoard, BehaviourManagerComp behComp){
        Sequence seq = new Sequence("Returning to base", blackBoard);
        GetBuildingFromColony getBuilding = new GetBuildingFromColony("Getting main building", blackBoard);

        seq.control.callbacks.startCallback = task -> {
            task.blackBoard.tagsToSearch = new String[]{"main"};
            task.blackBoard.targetNode = null;
        };

        seq.control.addTask(getBuilding);
        seq.control.addTask(moveTo(blackBoard, behComp));

        return seq;
    }

    public static Task moveTo(BlackBoard blackBoard, BehaviourManagerComp behComp){
        //Get the target node/Entity.
        //Find the path.
        //Move to the target.

        Sequence sequence = new Sequence("MoveTo", blackBoard);
        FindPath findPath = new FindPath("FindPath",  blackBoard);
        MoveTo followPath = new MoveTo("FollowPath",  blackBoard);

        ((ParentTaskController)(sequence.getControl())).addTask(findPath);
        ((ParentTaskController)(sequence.getControl())).addTask(followPath);

        return sequence;
    }

    public static Task sleep(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Sequence
         *      Sequence (always true / optional)
         *          Find building
         *          path to building
         *          enter building
         *      sleep...
         *      exit building
         */

        Sequence mainSeq = new Sequence("Sleeping", blackBoard);
        GetBuildingFromColony getBunks = new GetBuildingFromColony("Getting bunk", blackBoard);
        FindPath fp = new FindPath("Pathing", blackBoard);
        MoveTo mt = new MoveTo("Moving", blackBoard);
        Enter enter = new Enter("Entering", blackBoard);
        Sleep sleep = new Sleep("Sleeping", blackBoard);
        Exit leave = new Exit("Leaving", blackBoard);

        //Make sure we have tags to search for!
        mainSeq.control.callbacks.startCallback = task -> {
            task.blackBoard.targetNode = null;
            task.blackBoard.tagsToSearch = new String[]{"enterable"};
        };
        //Check if the enterable is full. Might as well store it while we're at it.
        getBunks.control.callbacks.successCriteria = b -> {
            blackBoard.enterable = ((Building)b).getComponent(Enterable.class);
            return !blackBoard.enterable.isFull();
        };

        //Fail the move to if the enterable is full.
        mt.control.callbacks.failCriteria = task -> ((Task)task).blackBoard.enterable.isFull();

        mainSeq.control.addTask(getBunks);
        mainSeq.control.addTask(fp);
        mainSeq.control.addTask(mt);
        mainSeq.control.addTask(enter);
        mainSeq.control.addTask(sleep);
        mainSeq.control.addTask(leave);

        return mainSeq;
    }

    public static Task craftItem(BlackBoard blackBoard, BehaviourManagerComp behComp){
        /**
         *  Selector
         *  -Sequence
         *  -Get a crafting building with a crafting job in it
         *
         *  //This area will repeatedly transport items/materials until no more are needed.
         *  --Repeat until failure
         *  ---Sequence
         *  ----gather a list of items we need (if none, we end this sequence with failure, we have enough items to craft it)
         *  ----find a storage building with those items
         *  ----reserve items
         *  ----find path to storage
         *  ----move to storage
         *  ----transfer items
         *  ----find path to building with crafting job
         *  ----move to crafting building
         *  ----transfer items
         *
         *  //This area makes sure we have the items/materials to craft and crafts and puts the finished item in storage
         *  --Make sure the building has enough items/materials to craft the item we want.
         *  --Find path to the building
         *  --Move to the building
         *  --Enter the building
         *  --Craft the item
         *  --Exit the building
         *  --Get closest storage (set the TO and FROM inventories)
         *  --Find path to storage
         *  --Move to storage
         *  --Transfer items
         *
         *  //Otherwise, idle.
         *  -idle
         *
         */

        //TODO We need to deal with stalled jobs. Maybe check for stalled jobs first and skip over getting items?

        Selector mainSelector = new Selector("Craft Item", blackBoard);
        Sequence craftItemSequence = new Sequence("CraftingItem", blackBoard);

        GetCraftingStationWithJob getCraftingStationWithJob = new GetCraftingStationWithJob("Getting crafting station", blackBoard);

        Sequence getNeededItemsSequence = new Sequence("GettingNeededItems", blackBoard);
        RepeatUntilFailure getItemsNeededUntilFailure = new RepeatUntilFailure("Repeating", blackBoard, getNeededItemsSequence);

        GetItemsForCrafting getItemsForCrafting = new GetItemsForCrafting("Getting item list", blackBoard);
        FindStorageWithItem findItem = new FindStorageWithItem("Finding item", blackBoard);
        CheckAndReserve reserveItems = new CheckAndReserve("Reserving items", blackBoard);
        FindPath findPathToItemStorage = new FindPath("Getting path to item storage", blackBoard);
        MoveTo moveToItemStorage = new MoveTo("Moving to item storage", blackBoard);
        TransferItemsFromTargetToMe transferItemsFromStorage = new TransferItemsFromTargetToMe("Transferring items", blackBoard);
        GetTargetFromCraftingStation getTargetFromCraftingStation = new GetTargetFromCraftingStation("Getting target", blackBoard);
        FindPath findPathToCraftingStation = new FindPath("Finding path to crafting station", blackBoard);
        MoveTo moveToCraftingStation = new MoveTo("Moving to crafting station", blackBoard);
        TransferItemsFromMeToTarget transferToCraftingStation = new TransferItemsFromMeToTarget("Transfer items to crafting station", blackBoard);

        GetItemsForCrafting checkEnoughItems = new GetItemsForCrafting("Checking if enough", blackBoard); //Don't need to add this anywhere else except to checkEnough.
        Invert checkEnough = new Invert("Checking if enough", blackBoard, checkEnoughItems);
        GetTargetFromCraftingStation getTargetForCrafting = new GetTargetFromCraftingStation("Getting target from the crafting station to go craft", blackBoard);
        GetEnterableFromTarget getEnterableFromTarget = new GetEnterableFromTarget("Getting enterable", blackBoard);
        FindPath findPathToTarget = new FindPath("Getting path to target crafting station", blackBoard);
        MoveTo moveToTargetCrafting = new MoveTo("Move to crafting station", blackBoard);
        Enter enterCraftingStation = new Enter("Entering crafting station", blackBoard);
        CraftItem craftItem = new CraftItem("Crafting item", blackBoard);
        Exit leaveCraftingStation = new Exit("Leaving crafting station", blackBoard);

        //Idle idle = new Idle("Waiting for crafting job", blackBoard);

        mainSelector.control.callbacks.startCallback = task -> {
            task.blackBoard.itemTransfer.reset();
            task.blackBoard.targetNode = null;
        };

        mainSelector.control.addTask(craftItemSequence);
        mainSelector.control.addTask(idleTask(blackBoard, behComp));

        craftItemSequence.control.addTask(getCraftingStationWithJob);
        craftItemSequence.control.addTask(getItemsNeededUntilFailure);
        craftItemSequence.control.addTask(checkEnough);
        craftItemSequence.control.addTask(getTargetForCrafting);
        craftItemSequence.control.addTask(findPathToTarget);
        craftItemSequence.control.addTask(moveToTargetCrafting);
        craftItemSequence.control.addTask(enterCraftingStation);
        craftItemSequence.control.addTask(craftItem);
        craftItemSequence.control.addTask(leaveCraftingStation);

        getNeededItemsSequence.control.addTask(getItemsForCrafting);
        getNeededItemsSequence.control.addTask(findItem);
        getNeededItemsSequence.control.addTask(reserveItems);
        getNeededItemsSequence.control.addTask(findPathToItemStorage);
        getNeededItemsSequence.control.addTask(moveToItemStorage);
        getNeededItemsSequence.control.addTask(transferItemsFromStorage);
        getNeededItemsSequence.control.addTask(getTargetFromCraftingStation);
        getNeededItemsSequence.control.addTask(getEnterableFromTarget);
        getNeededItemsSequence.control.addTask(findPathToCraftingStation);
        getNeededItemsSequence.control.addTask(moveToCraftingStation);
        getNeededItemsSequence.control.addTask(transferToCraftingStation);

        return mainSelector;
    }
}
