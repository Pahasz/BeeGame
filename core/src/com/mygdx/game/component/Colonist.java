package com.mygdx.game.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.mygdx.game.ColonyGame;
import com.mygdx.game.behaviourtree.PrebuiltTasks;
import com.mygdx.game.behaviourtree.Task;
import com.mygdx.game.component.collider.Collider;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.interfaces.Functional;
import com.mygdx.game.interfaces.IInteractable;
import com.mygdx.game.interfaces.IOwnable;
import com.mygdx.game.ui.PlayerInterface;
import com.mygdx.game.util.DataBuilder;
import com.mygdx.game.util.EventSystem;
import com.mygdx.game.util.Tree;
import com.mygdx.game.util.gui.GUI;
import com.mygdx.game.util.managers.DataManager;
import com.mygdx.game.util.timer.RepeatingTimer;
import com.mygdx.game.util.timer.Timer;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Created by Paha on 1/17/2015.
 */
public class Colonist extends Component implements IInteractable, IOwnable{
    private Colony colony;
    private Inventory inventory;
    private Stats stats;
    private Skills skills;
    private BehaviourManagerComp manager;
    private Collider collider;
    private String firstName, lastName;
    private boolean alert = false;

    private LinkedList<Entity> attackList = new LinkedList<>();

    private Fixture fixture;

    public Colonist() {
        super();
    }

    @Override
    public void start() {
        super.start();

//        getBehManager().getBehaviourStates().addState("idle", (BlackBoard blackBoard, BehaviourManagerComp behComp) -> PrebuiltTasks::idleTask);

        this.inventory = this.getComponent(Inventory.class);
        this.stats = this.getComponent(Stats.class);
        this.skills = this.getComponent(Skills.class);
        this.manager = this.getComponent(BehaviourManagerComp.class);
        this.manager.getBlackBoard().moveSpeed = 200f;
        this.collider = this.getComponent(Collider.class);

        EventSystem.onEntityEvent(this.owner, "damage", onDamage);
        EventSystem.onEntityEvent(this.owner, "attacking_group", onAttackingEvent);
        EventSystem.onEntityEvent(this.owner, "collide_start", onCollideStart);
        EventSystem.onEntityEvent(this.owner, "collide_end", onCollideEnd);

        this.createStats();
        this.createBehaviourButtons();
        this.createSkills();
        this.createRangeSensor();
    }

    //Creates a range sensor for when we get a ranged weapon.
    private void createRangeSensor(){

        CircleShape shape = new CircleShape();
        shape.setRadius(0f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.isSensor = true;
        fixtureDef.shape = shape;
        this.fixture = this.collider.body.createFixture(fixtureDef);
        Collider.ColliderInfo info = new Collider.ColliderInfo(this.owner);
        info.tags.addTag("attack_sensor");
        this.fixture.setUserData(info);
        shape.dispose();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if(alert && this.attackList.size() > 0){
            Entity ent = this.attackList.peek();
            if(!ent.getTags().hasTag("alive"))
                this.attackList.removeFirst();
            else{
                this.getBehManager().getBlackBoard().target = ent;
                if(!this.getBehManager().getBehaviourStates().getCurrState().stateName.equals("attackTarget"))
                    this.getBehManager().changeTaskImmediate("attackTarget");
            }
        }
    }

    public boolean setAlert(boolean alert){
        //If alert is active and we are setting it to not active.
        if(this.alert && !alert) {
            this.attackList = new LinkedList<>();
            this.fixture.getShape().setRadius(0);
        //If alert is not active and we are setting it to active.
        }else if(!this.alert && alert)
            this.fixture.getShape().setRadius(20f);

        return this.alert = alert;
    }

    /**
     * Toggles the attack range sensor on this colonist.
     * @return True if alert was set to true, false if alert was set to false.
     */
    public boolean toggleRangeSensor(){
        return this.setAlert(!this.alert);
    }

    //Creates the stats for this colonist.
    private void createStats(){
        //Create these 4 stats.
        Stats.Stat healthStat = stats.addStat("health", 100, 100);
        Stats.Stat foodStat = stats.addStat("food", 100, 100);
        Stats.Stat waterStat = stats.addStat("water", 20, 100);
        stats.addStat("energy", 100, 100).color = Color.YELLOW;

        healthStat.color = Color.GREEN;
        foodStat.color = Color.RED;
        waterStat.color = Color.CYAN;

        foodStat.effect = "feed";
        waterStat.effect = "thirst";

        //Add some timers.
        //Subtract food every 5 seconds and try to eat when it's too low.
        stats.addTimer(new RepeatingTimer(5f, () -> {
            foodStat.addToCurrent(-1);
            //If under 20, try to eat.
            if(foodStat.getCurrVal() <= 20) {
                getBehManager().getBlackBoard().itemEffect = "feed";
                getBehManager().getBlackBoard().itemEffectAmount = 1;
                getBehManager().changeTaskQueued("consume");
            }
        }));

        //Subtract water every 10 seconds and try to drink when it's too low.
        stats.addTimer(new RepeatingTimer(10f, () -> {
            waterStat.addToCurrent(-1);
            //If under 20, try to drink.
            if (waterStat.getCurrVal() <= 20) {
                getBehManager().getBlackBoard().itemEffect = "thirst";
                getBehManager().getBlackBoard().itemEffectAmount = 1;
                getBehManager().changeTaskQueued("consume");
            }
        })); //Subtract water every 10 seconds.

        //If food or water is 0, subtract health.
        Timer timer = stats.addTimer(new RepeatingTimer(5f, null));
        timer.setCallback(() -> {
            //If out of food OR water, degrade health.
            if (stats.getStat("food").getCurrVal() <= 0 || stats.getStat("water").getCurrVal() <= 0) {
                stats.getStat("health").addToCurrent(-1);
                timer.setLength(5f);
                //If we have both food AND water, improve health.
            } else if (stats.getStat("food").getCurrVal() > 0 && stats.getStat("water").getCurrVal() > 0) {
                stats.getStat("health").addToCurrent(1);
                timer.setLength(10f);
            }
        });

        healthStat.onZero = onZero;
    }

    //Creates all the buttons for the colonists behaviours.
    private void createBehaviourButtons(){
        this.getBehManager().getBlackBoard().attackDamage = 30f;
        this.getBehManager().getBlackBoard().attackRange = 500f;

        Tree taskTree = this.getBehManager().getTaskTree();

        Tree.TreeNode[] nodeList = taskTree.addNode("root", "gather", "hunt", "explore", "idle");

        //For each node, we set up a TaskInfo object and assign it to the node's userData field.
        for(Tree.TreeNode node : nodeList) {
            BehaviourManagerComp.TaskInfo taskInfo = new BehaviourManagerComp.TaskInfo(node.nodeName);
            taskInfo.callback = () -> getBehManager().changeTaskImmediate(node.nodeName);
            taskInfo.userData = DataManager.getData(node.nodeName+"Style", GUI.GUIStyle.class);
            if(taskInfo.userData == null) taskInfo.userData = DataManager.getData("blankStyle", GUI.GUIStyle.class);

            node.userData = taskInfo;
        }

        nodeList = taskTree.addNode("gather", "food", "water", "herbs", "wood", "stone", "metal");

        for(Tree.TreeNode node : nodeList) {
            BehaviourManagerComp.TaskInfo taskInfo = new BehaviourManagerComp.TaskInfo(node.nodeName);
            taskInfo.callback = () -> {
                taskInfo.active = !taskInfo.active;
                getBehManager().getBlackBoard().resourceTypeTags.toggleTag(node.nodeName);
            };
            taskInfo.userData = DataManager.getData("blankStyle", GUI.GUIStyle.class);

            node.userData = taskInfo;
        }

        getBehManager().getBehaviourStates().addState("gather", false, PrebuiltTasks::gatherResource).repeat = true;
        getBehManager().getBehaviourStates().addState("explore", false, PrebuiltTasks::exploreUnexplored).repeat = true;
        getBehManager().getBehaviourStates().addState("consume", false, PrebuiltTasks::consumeTask).repeat = false;
        getBehManager().getBehaviourStates().addState("attackTarget", false, PrebuiltTasks::attackTarget).repeat = false;
        getBehManager().getBehaviourStates().addState("hunt", false, PrebuiltTasks::searchAndHunt).repeat = true;

        EventSystem.onEntityEvent(this.owner, "task_started", args -> {
            Task task = (Task)args[0];
            if(task.getName().equals("exploreUnexplored")) task.getBlackboard().target = this.getColony().getEntityOwner();
        });
    }

    //Creates the skills, which aren't used anymore :(
    private void createSkills(){
        this.skills.addSkill("foraging", 10, 100, 2.25f, 0.1f);
        this.skills.addSkill("lumberjack", 10, 100, 2.25f, 0.1f);
        this.skills.addSkill("hunting", 10, 100, 2.25f, 0.1f);
        this.skills.addSkill("exploration", 10, 100, 2.25f, 1.5f);
        this.skills.addSkill("butchering", 10, 100, 2.25f, 0.1f);
        this.skills.addSkill("mining", 10, 100, 2.25f, 0.1f);
    }

    //The onDamage event when this colonist takes damage.
    private Consumer<Object[]> onDamage = args -> {
        Entity entity = (Entity) args[0];
        float amount = (float) args[1];

        Stats.Stat health = this.stats.getStat("health");
        if (health == null) return;

        health.addToCurrent(amount);
        if(this.manager != null) {
            this.manager.getBlackBoard().target = entity;
            this.manager.changeTaskImmediate("attackTarget");
        }
    };

    //The callback for when our health is 0.
    private Functional.Callback onZero = () -> {
        this.owner.getTags().removeTag("alive");
        this.owner.getTransform().setRotation(90f);
        this.owner.destroyComponent(BehaviourManagerComp.class);
        this.collider.body.destroyFixture(this.fixture); //TODO THIS PROBABLY WILL BREAK IT!
        this.stats.clearTimers();
        this.manager = null;
        GridComponent gridComp = this.getComponent(GridComponent.class);
        gridComp.setActive(false);
        ColonyGame.worldGrid.removeViewer(gridComp);
    };

    //The function for when a "attack" signal is sent to this colonist.
    private Consumer<Object[]> onAttackingEvent = args -> {
        Group attackingGroup = (Group)args[0];
        if(attackingGroup.getLeader().getTags().hasTag("boss")) {
            DataBuilder.JsonPlayerEvent event = DataManager.getData("bossencounter", DataBuilder.JsonPlayerEvent.class);
            event.eventTarget = this.getEntityOwner();
            event.eventTargetOther = attackingGroup.getLeader();
            PlayerInterface.getInstance().newPlayerEvent(event);
        }
    };

    //When one of our fixtures collides with something.
    private Consumer<Object[]> onCollideStart = args -> {
        Fixture myFixture = (Fixture)args[0];
        Fixture otherFixture = (Fixture)args[1];

        Collider.ColliderInfo myInfo = (Collider.ColliderInfo)myFixture.getUserData();
        Collider.ColliderInfo otherInfo = (Collider.ColliderInfo)otherFixture.getUserData();

        //If a body (not a sensor) is hitting our 'attack_sensor' fixture and it's an animal
        if(myInfo.tags.hasTag("attack_sensor") && otherInfo.tags.hasTag("entity") && otherInfo.owner.getTags().hasTag("animal")){
            Animal animal = otherInfo.owner.getComponent(Animal.class);
            if(animal == null) return;
            if(animal.getAnimalRef().aggressive)
                attackList.add(otherInfo.owner);
        }
    };

    //When one of our fixtures collides with something.
    private Consumer<Object[]> onCollideEnd = args -> {
        Fixture myFixture = (Fixture)args[0];
        Fixture otherFixture = (Fixture)args[1];

        Collider.ColliderInfo myInfo = (Collider.ColliderInfo)myFixture.getUserData();
        Collider.ColliderInfo otherInfo = (Collider.ColliderInfo)otherFixture.getUserData();

        //If a (entity) fixture is hitting our 'attack_sensor' fixture and it's an animal
        if(myInfo.tags.hasTag("attack_sensor") && otherInfo.owner.getTags().hasTag("animal") && otherInfo.tags.hasTag("entity")){
            Animal animal = otherInfo.owner.getComponent(Animal.class);
            if(animal == null) return;
            if(animal.getAnimalRef().aggressive)
                attackList.remove(otherInfo.owner);
        }
    };

    public Colony getColony() {
        return colony;
    }

    public void setColony(Colony colony) {
        this.colony = colony;
    }

    public void setName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.getEntityOwner().name = firstName;
    }

    @Override
    public Skills getSkills() {
        return this.skills;
    }

    @Override
    public String getName() {
        return this.firstName;
    }

    @Override
    public Inventory getInventory(){
        return this.inventory;
    }

    @Override
    public Stats getStats(){
        return this.stats;
    }

    @Override
    public String getStatsText() {
        return null;
    }

    @Override
    public BehaviourManagerComp getBehManager() {
        return this.manager;
    }

    @Override
    public void addedToColony(Colony colony) {
        this.colony = colony;
    }

    @Override
    public Colony getOwningColony() {
        return this.colony;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void destroy(Entity destroyer) {
        super.destroy(destroyer);
    }
}
