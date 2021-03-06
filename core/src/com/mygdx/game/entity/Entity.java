package com.mygdx.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mygdx.game.component.Component;
import com.mygdx.game.component.Transform;
import com.mygdx.game.component.graphic.GraphicIdentity;
import com.mygdx.game.interfaces.IDelayedDestroyable;
import com.mygdx.game.interfaces.ISaveable;
import com.mygdx.game.interfaces.IScalable;
import com.mygdx.game.util.Tags;
import com.mygdx.game.util.managers.MessageEventSystem;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.function.Consumer;

/**
 * @author Bbent_000
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "@class")
public class Entity implements IDelayedDestroyable, ISaveable{
    @JsonIgnore
    protected static long counterID = (long)(-Long.MAX_VALUE*0.5);
    @JsonProperty
	public String name = "Entity";
    @JsonProperty
	public int drawLevel = 0;
    @JsonProperty
	public boolean active = true;
    @JsonProperty
    protected long ID;
    @JsonProperty
    protected Tags tags = new Tags("entity");
    @JsonIgnore
	protected Components components = new Components(this);
    @JsonProperty
    protected boolean destroyed=false, setToDestroy=false;

    public Entity(){

    }

	/**
	 * Creates an Entity that will start with a GraphicIdentity component.
	 * @param position The initial position of the Entity
	 * @param rotation The initial rotation of the Entity.
	 */
	public Entity(Vector2 position, float rotation, int drawLevel){
		this.components.transform = this.components.addComponent(new Transform(position, rotation));

		this.getNewID();
		this.drawLevel = drawLevel;
	}

	private void getNewID(){
		this.ID = counterID++;
		if(this.ID == 0) this.ID = counterID++;
	}

	/**
	 * Creates an Entity without a GraphicIdentity, but allows for a variable amount of components to be immediately added.
	 * @param position The initial position of the Entity
	 * @param rotation The initial rotation of the Entity.
	 * @param comps A variable amount of Components to construct this Entity with.
	 */
	public Entity(Vector2 position, float rotation, int drawLevel, Component... comps){
		this.components.transform = this.components.addComponent(new Transform(position, rotation));
		for(Component comp : comps)
			this.components.addComponent(comp);

		this.getNewID();
		this.drawLevel = drawLevel;
	}

	public void start(){
		MessageEventSystem.notifyGameEvent("entity_created", this);
	}

    @Override
    public void addedLoad(TLongObjectHashMap<Entity> entityMap, TLongObjectHashMap<Component> compMap) {

    }

    @Override
    public void save() {

    }

    @Override
    public void initLoad(TLongObjectHashMap<Entity> entityMap, TLongObjectHashMap<Component> compMap) {

    }

    @Override
    public void load(TLongObjectHashMap<Entity> entityMap, TLongObjectHashMap<Component> compMap) {

    }

    /**
     * Updates the Entity and all active components.
     * @param delta The time between the current and last frame.
     */
	public void update(float delta){
		this.components.update(delta);
	}

	/**
	 * Renders the renderable components.
	 * @param delta Delta time between frames.
	 * @param batch The SpriteBatch to draw with.
	 */
	public void render(float delta, SpriteBatch batch){
		if(this.components.getIdentity() != null && this.components.getIdentity().isActive()) this.components.identity.render(delta, batch);
	}

    public final <T extends Component> T addComponent(T comp){
        return components.addComponent(comp);
    }

    @JsonIgnore
    public final <T extends Component> T getComponent(Class<T> c){
        return components.getComponent(c);
    }

    @JsonIgnore
    public GraphicIdentity getGraphicIdentity(){
        return this.getComponents().getIdentity();
    }

    @JsonIgnore
    public Components getComponents(){
        return this.components;
    }

    @JsonIgnore
    public Transform getTransform(){
        return this.getComponents().getTransform();
    }

    @JsonIgnore
    public Tags getTags(){
        return this.tags;
    }

    /**
     * @return The double ID of this Entity.
     */
    @JsonIgnore
    public long getID(){
        return this.ID;
    }

    public void destroyComponent(Component component){
        components.destroyComponent(component);
    }

    public <T extends Component> void destroyComponent(Class<T> cls){
        components.destroyComponent(cls);
    }

    public boolean removeComponent(Component comp){
        return components.removeComponent(comp);
    }

	/**
	 * @return True if this Entity is valid (not destroyed or set to be destroyed), false otherwise.
	 */
    @JsonIgnore
	public boolean isValid(){
		return !this.destroyed && !this.isSetToBeDestroyed();
	}
	@Override
	public void setToDestroy() {
		this.setToDestroy = true;
	}
public static class Components implements IDelayedDestroyable{
		protected Array<Component> newComponentList = new Array<>();
		protected Array<Component> activeComponentList = new Array<>();
		protected Array<Component> inactiveComponentList = new Array<>();
		protected Array<Component> scalableComponents = new Array<>();
		protected Array<Component> destroyComponentList = new Array<>();
		protected Entity owner;
		protected boolean setToDestroy, destroyed;
		private Transform transform;
        private GraphicIdentity identity;

		public Components(Entity owner){
			this.owner = owner;
		}

		public void update(float delta){
			//Only update if active.
			if(this.owner.active && !this.destroyed) {
				if(destroyComponentList.size > 0) {
					for (Component comp : destroyComponentList) internalDestroyComponent(comp); //Destory the component
					destroyComponentList.clear(); //Clear the list.
				}

				//Init all new components
				if (this.newComponentList.size > 0) {
					Array<Component> newCompCopy = new Array<>(this.newComponentList); //Make a copy.
                    this.newComponentList = new Array<>(); //Clear this before we call init() in case we add more components during init().
					newCompCopy.forEach(component -> {if(!component.isInitiated()) component.init();}); //Init all new comps that haven't already been inited.
					newCompCopy.forEach(component -> {if(!component.isStarted()) component.start();}); //Start all new comps that haven't already been started.
				}

				Array<Component> activeCompCopy = new Array<>(this.activeComponentList);
				//Update all Components
				for (Component comp : activeCompCopy) {
					comp.update(delta);
					comp.lateUpdate(delta);
				}
			}
		}

		/**
		 *	Destroys and removes a Component from this Entity.
		 * @param component The Component to destroy and remove.
		 */
		private void internalDestroyComponent(Component component){
			if(component.isActive())
				this.activeComponentList.removeValue(component, false);
			else
				this.inactiveComponentList.removeValue(component, false);

			component.destroy(owner);
		}

		/**
		 * Adds a component to this Entity. The Component has added(Entity owner) immediately called and is put into a newComponent list. On the next tick, all new Components
		 * have init() called and then start() in that order.
		 * @param comp The Component to add to this Entity.
		 * @param <T> The Component class interType of the component being added.
		 * @return The Component that was added.
		 */
		public final <T extends Component> T addComponent(T comp){
            if(!comp.isStarted()) {
				comp.added(this.owner);
				this.newComponentList.add(comp); //Add it to the new list for the init() method.
			}
			//Add it to the active or inactive list.
            if (comp.isActive()) this.activeComponentList.add(comp);
            else this.inactiveComponentList.add(comp);

			return comp;
		}

		/**
		 * Destroys and removes a component from this entity.
		 * @param cls The class interType of the Component to remove.
		 */
		public <T extends Component> void destroyComponent(Class<T> cls){
			//Search the inactive list.
			for (Component comp : this.inactiveComponentList)
				if (comp.getClass() == cls) {
					destroyComponentList.add(comp);
					return;
				}

			//Search the active list.
			for (Component comp : this.activeComponentList)
				if (comp.getClass() == cls) {
					destroyComponentList.add(comp);
					return;
				}
		}

		/**
		 * Destroys the passed in component and removes it from this Entity.
		 * @param component The Component to destroy and remove.
		 */
		public void destroyComponent(Component component){
			destroyComponentList.add(component);
		}

		/**
		 * Removes a Component from this Entity without destroying it.
		 * @param comp The Component to remove.
		 * @return True if the Component was removed, false otherwise.
		 */
		public boolean removeComponent(Component comp){
			if(comp.isActive()) return this.activeComponentList.removeValue(comp, true);
			return this.inactiveComponentList.removeValue(comp, true);
		}

        @JsonIgnore
        public Array<Component> getNewComponentList() {
            return newComponentList;
        }

        @JsonIgnore
        public Array<Component> getActiveComponentList() {
            return activeComponentList;
        }

        @JsonIgnore
        public Array<Component> getInactiveComponentList() {
            return inactiveComponentList;
        }

        @JsonIgnore
        public Array<Component> getScalableComponents() {
            return scalableComponents;
        }

        @JsonIgnore
        public Array<Component> getDestroyComponentList() {
            return destroyComponentList;
        }

        @JsonIgnore
        public Long[] getComponentIDs(){
            Array<Long> longs = new Array<>();
            for(Component comp : activeComponentList) longs.add(comp.getCompID());
            for(Component comp : inactiveComponentList) longs.add(comp.getCompID());
            for(Component comp : newComponentList) longs.add(comp.getCompID());
            for(Component comp : destroyComponentList) longs.add(comp.getCompID());

            return longs.toArray(Long.class);
        }

        @JsonIgnore
        public Array<Component> getAllComponents(){
            Array<Component> comps = new Array<>();
            for(Component comp : activeComponentList) comps.add(comp);
            for(Component comp : inactiveComponentList) comps.add(comp);
            for(Component comp : newComponentList) comps.add(comp);
            for(Component comp : destroyComponentList) comps.add(comp);

            return comps;
        }

        @JsonIgnore
        public GraphicIdentity getIdentity() {
            if(this.identity == null) this.identity = getComponent(GraphicIdentity.class);
            return identity;
        }

		/**
		 * Retrieves a Component from this Entity.
		 * @param c The Component class interType to retrieve.
		 * @return The Component if it was found, otherwise null.
		 */
		@SuppressWarnings("unchecked")
        @JsonIgnore
		public final <T extends Component> T getComponent(Class<T> c){
			for(Component comp : this.inactiveComponentList){
				if(comp.getClass() == c || comp.getClass().getSuperclass() == c)
					return (T)comp;
			}

			for(Component comp : this.activeComponentList){
				if(comp.getClass() == c || comp.getClass().getSuperclass() == c)
					return (T)comp;
			}

			return null;
		}
        public final <T extends Component & IScalable> void registerScalable(T scalable){
			this.scalableComponents.add(scalable);
        }
        public final void scaleComponents(float scale){
			for(Component scalable : this.scalableComponents)
                ((IScalable)scalable).scale(scale);
		}
		public void iterateOverComponents(Consumer<Component> consumer){
			for(int i=0;i<activeComponentList.size;i++)
                consumer.accept(activeComponentList.get(i));
            for(int i=0;i<inactiveComponentList.size;i++)
                consumer.accept(inactiveComponentList.get(i));
            for(int i=0;i<newComponentList.size;i++
                    ) consumer.accept(newComponentList.get(i));
            for(int i=0;i<destroyComponentList.size;i++)
                consumer.accept(destroyComponentList.get(i));
            for(int i=0;i<scalableComponents.size;i++)
                consumer.accept(scalableComponents.get(i));
		}@JsonIgnore
        public Transform getTransform() {
            if(this.transform == null) this.transform = getComponent(Transform.class);
            return transform;
        }









		@Override
		public void setToDestroy() {
			this.setToDestroy = true;
		}

		@Override
		public void destroy(Entity destroyer) {
			//Destroy all children
			this.getTransform().getChildren().forEach(transform -> transform.destroy(transform.getEntityOwner()));

			//Remove myself from any parent.
			if(this.transform.parent != null){
				this.transform.parent.removeChild(this.transform);
				this.transform.parent = null;
			}

			//Destroy active components.
			this.activeComponentList.forEach(comp -> comp.destroy(this.owner));

			//Destroy inactive components
			this.inactiveComponentList.forEach(comp -> comp.destroy(this.owner));

			//Clear both lists.
			this.activeComponentList.clear();
			this.inactiveComponentList.clear();

			//Destroy and clear identity.
			if(this.identity != null) {
				this.identity.destroy(this.owner);
				this.identity = null;
			}

			//Destroy and clear transform.
			this.transform.destroy(this.owner);
			this.transform = null;

			this.destroyed = true;
		}

		@Override
        @JsonIgnore
		public boolean isDestroyed() {
			return this.destroyed;
		}

		@Override
        @JsonIgnore
		public boolean isSetToBeDestroyed() {
			return this.setToDestroy;
		}
	}/**
	 * @return True if this Entity has been destroyed, false otherwise.
	 */
	@Override
    @JsonIgnore
	public boolean isDestroyed(){
		return this.destroyed;
	}



    	@Override
    @JsonIgnore
	public boolean isSetToBeDestroyed() {
		return this.setToDestroy;
	}



	@Override
	public void destroy(Entity destroyer){
		MessageEventSystem.unregisterEntity(this);

        components.destroy(destroyer);

		//Set destroyed to true.
		this.destroyed = true;
	}


}
