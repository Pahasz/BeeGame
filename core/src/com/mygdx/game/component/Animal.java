package com.mygdx.game.component;

import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.mygdx.game.behaviourtree.PrebuiltTasks;
import com.mygdx.game.component.collider.Collider;
import com.mygdx.game.entity.Entity;
import com.mygdx.game.interfaces.Functional;
import com.mygdx.game.interfaces.IInteractable;
import com.mygdx.game.util.Constants;
import com.mygdx.game.util.DataBuilder;
import com.mygdx.game.util.EventSystem;
import com.mygdx.game.util.managers.DataManager;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Created by Paha on 2/26/2015.
 */
public class Animal extends Component implements IInteractable{
    private BehaviourManagerComp behComp;
    private Stats stats;
    private DataBuilder.JsonAnimal animalRef;
    private Collider collider;
    private Group group;

    private LinkedList<Entity> attackList = new LinkedList<>();
    private Fixture attackSensor;

    public Animal(DataBuilder.JsonAnimal animalRef) {
        super();
        this.animalRef = animalRef;
    }

    @Override
    public void start() {
        super.start();

        if(animalRef.boss) this.getEntityOwner().getTags().addTag("boss");
        this.getEntityOwner().name = animalRef.displayName;

        this.stats = this.getComponent(Stats.class);
        this.behComp = this.getComponent(BehaviourManagerComp.class);
        this.collider = this.getComponent(Collider.class);

        behComp.getBlackBoard().attackRange = 15f;

        stats.addStat("health", 100, 100);
        stats.addStat("food", 100, 100);
        stats.addStat("water", 100, 100);

        //Remove its animal properties and make it a resource.
        stats.getStat("health").onZero = onDeath();

        EventSystem.onEntityEvent(this.owner, "collide_start", onCollideStart);
        EventSystem.onEntityEvent(this.owner, "collide_end", onCollideEnd);
        EventSystem.onEntityEvent(this.owner, "damage", onDamage);

        this.behComp.getBlackBoard().moveSpeed = 250f;
        //this.setActive(false);

        if(animalRef.aggressive) addCircleSensor();
        this.group = this.getComponent(Group.class);

        this.makeBehaviourStuff();
    }

    //Adds a circle sensor to this animal.
    public void addCircleSensor(){
        CircleShape circle = new CircleShape();
        circle.setRadius(10f);
        attackSensor = collider.body.createFixture(circle, 1f);
        attackSensor.setSensor(true);
        Collider.ColliderInfo fixtureInfo = new Collider.ColliderInfo(this.owner);
        fixtureInfo.tags.addTag(Constants.COLLIDER_DETECTOR);
        attackSensor.setUserData(fixtureInfo);
        circle.dispose();
    }

    private void makeBehaviourStuff(){
        this.behComp.getBehaviourStates().addState("attackTarget").repeat = false;
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        boolean validTarget = behComp.getBlackBoard().target != null && behComp.getBlackBoard().target.isValid() && behComp.getBlackBoard().target.getTags().hasTag("alive");
        if(attackList.size() > 0 && !validTarget) {
            Entity target = attackList.poll(); //Get the next target off of the list.
            attackTarget(target);
        }
    }

    private void attackTarget(Entity target){
        //If the target is not valid (not alive), let's not use it!
        if(target.getTags().hasTag("alive")) behComp.getBlackBoard().target = target;
        else behComp.getBlackBoard().target = null;

        //If we are already attacking something, give up.
        if(this.behComp.getBehaviourStates().getCurrState().stateName.equals("attackTarget")) return;

        //Attack single or group attack.
        if(this.group != null) groupAttack(behComp.getBlackBoard().target);
        else this.behComp.changeTaskImmediate("attackTarget");
    }

    //The callback to be called when I die!
    private Functional.Callback onDeath(){
        return () -> {
            Interactable interactable = this.owner.getComponent(Interactable.class);

            //If we don't have a resource to turn into, simply die.
            if(animalRef.resourceName == null)
                this.owner.setToDestroy();

            //Otherwise, prepare to be a resource!
            else {
                EventSystem.unregisterEntity(this.owner); //Unregister for events.
                this.owner.getTransform().setRotation(180);
                this.collider.body.setLinearVelocity(0, 0);
                this.owner.getTags().clearTags(); //Clear all tags
                this.owner.getTags().addTag("resource"); //Add the resource tag
                this.owner.addComponent(new Resource(DataManager.getData(animalRef.resourceName, DataBuilder.JsonResource.class))); //Add a Resource Component.
                if (interactable != null) interactable.changeType("resource");
                this.owner.destroyComponent(BehaviourManagerComp.class); //Destroy the BehaviourManagerComp
                this.owner.destroyComponent(Stats.class); //Destroy the Stats component.
                this.owner.destroyComponent(Animal.class); //Destroy this (Animal) Component.
                this.owner.destroyComponent(Group.class); //Destroy this (Animal) Component.
            }
        };
    }

    //The Consumer function to call when I collide with something.
    private Consumer<Object[]> onCollideStart = args -> {
        Fixture me = (Fixture)args[0];
        Fixture other = (Fixture)args[1];

        Collider.ColliderInfo otherInfo = (Collider.ColliderInfo) other.getUserData();
        Collider.ColliderInfo myInfo = (Collider.ColliderInfo) me.getUserData();

        //If it is not a detector, the other is a bullet, hurt me! and kill the bullet!
        if (!myInfo.tags.hasTag(Constants.COLLIDER_DETECTOR) && otherInfo.owner.getTags().hasTag("projectile")) {
            this.getComponent(Stats.class).getStat("health").addToCurrent(-20);
            behComp.getBlackBoard().target = otherInfo.owner.getComponent(Projectile.class).projOwner;
            //If not aggressive, flee. Otherwise, attack!
            if(!animalRef.aggressive) behComp.changeTaskImmediate(PrebuiltTasks.fleeTarget(behComp.getBlackBoard(), behComp));
            else attackTarget(behComp.getBlackBoard().target);
            otherInfo.owner.setToDestroy();

        //If I am a detector and the other is a colonist, we must attack it!
        }else if (myInfo.tags.hasTag(Constants.COLLIDER_DETECTOR) && otherInfo.tags.hasTag("entity") && otherInfo.owner.getTags().hasTag("colonist") && animalRef.aggressive) {
            if(otherInfo.owner.getTags().hasTag("alive")) attackList.add(otherInfo.owner);
        }
    };

    //The Consumer function to call when I stop colliding with something.
    private Consumer<Object[]> onCollideEnd = args -> {
        Fixture me = (Fixture) args[0];
        Fixture other = (Fixture) args[1]; //Get the other entity.

        Collider.ColliderInfo otherInfo = (Collider.ColliderInfo) other.getUserData();
        Collider.ColliderInfo myInfo = (Collider.ColliderInfo) me.getUserData();

        if (myInfo.tags.hasTag(Constants.COLLIDER_DETECTOR) && otherInfo.owner.getTags().hasTag("colonist")) {
            attackList.remove(otherInfo.owner);
        }
    };

    //The Consume function to call when I take damage.
    private Consumer<Object[]> onDamage = args -> {
        Entity other = (Entity) args[0];
        float damage = (float) args[1];

        Stats.Stat stat = stats.getStat("health");
        if (stat == null) return;
        stat.addToCurrent(damage);
    };

    private void groupAttack(Entity target){
        if(group.getLeader() == null) return;

        BehaviourManagerComp leaderComp = group.getLeader().getComponent(BehaviourManagerComp.class);
        leaderComp.getBlackBoard().target = target;
        leaderComp.changeTaskImmediate("attackTarget");
        EventSystem.notifyEntityEvent(target, "attacking_group", this.group);

        this.group.getGroupList().forEach(ent ->{
            BehaviourManagerComp entComp = ent.getComponent(BehaviourManagerComp.class);
            entComp.getBlackBoard().target = target;
            entComp.changeTaskImmediate("attackTarget");
        });
    }

    public void setGroup(Group group){
        this.group = group;
    }

    public DataBuilder.JsonAnimal getAnimalRef() {
        return animalRef;
    }


    @Override
    public Inventory getInventory() {
        return null;
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    @Override
    public String getStatsText() {
        return null;
    }

    @Override
    public Skills getSkills() {
        return null;
    }

    @Override
    public String getName() {
        return this.getEntityOwner().name;
    }

    @Override
    public BehaviourManagerComp getBehManager() {
        return this.behComp;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void destroy(Entity destroyer) {
        super.destroy(destroyer);
        if(attackSensor != null) this.collider.body.destroyFixture(attackSensor);
    }
}
