/*******************************************************************************
 * Copyright (c) 2011, Daniel Murphy
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
/**
 * Created at 2:21:03 PM Jul 17, 2010
 */
package ubc.swim.tests;

import java.util.ArrayList;
import java.util.LinkedList;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.callbacks.DestructionListener;
import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.Collision;
import org.jbox2d.collision.Collision.PointState;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;

import ubc.swim.dynamics.controllers.DynamicsController;
import ubc.swim.gui.ContactPoint;
import ubc.swim.gui.SwimModel;
import ubc.swim.gui.SwimSettings;
import ubc.swim.world.characters.SwimCharacter;
import ubc.swim.world.scenario.Scenario;
import ubc.swim.world.scenario.ScenarioLibrary;

/**
 * A single test world for the swimmer app; modeled on org.jbox2d.testbed.framework.TestbedTest by Daniel Murphy
 * 
 * @author Ben Humberston
 */
public abstract class SwimTest implements ContactListener {
	public static final int MAX_CONTACT_POINTS = 2048;

	protected static final long GROUND_BODY_TAG = 1897450239847L;
	protected static final long BOMB_TAG = 98989788987L;
	protected static final long MOUSE_JOINT_TAG = 4567893364789L;
	
	protected float defaultCameraScale = 100;
	protected Vec2 defaultCameraPos = new Vec2(0, 10); //at default fluid height

//	private static final Logger log = LoggerFactory
//			.getLogger(SwimTest.class);

	// keep these static so we don't have to recreate them every time
	public final static ContactPoint[] points = new ContactPoint[MAX_CONTACT_POINTS];
	static {
		for (int i = 0; i < MAX_CONTACT_POINTS; i++) {
			points[i] = new ContactPoint();
		}
	}
	
	protected float runtime; //total simulated time in world since last reset
	protected long frameCount; //total frames run since last reset
	
	protected Scenario scenario;
	
	private Body groundBody;
	private MouseJoint mouseJoint;
	
	//If true, renders test world to screen on each dynamics step
	private boolean drawingEnabled = true;
	
	private Body bomb;
	private final Vec2 bombSpawnPoint = new Vec2();
	private boolean bombSpawning = false;

	private final Vec2 mouseWorld = new Vec2();
	private int pointCount;
	private int stepCount;

	private SwimModel model;
	private DestructionListener destructionListener;

	private final LinkedList<QueueItem> inputQueue;

	private String title = null;
	protected int textLine;
	private final LinkedList<String> textList = new LinkedList<String>();

	private float cachedCameraScale;
	private final Vec2 cachedCameraPos = new Vec2();
	private boolean hasCachedCamera = false;
	
	private boolean dialogOnSaveLoadErrors = true;

	private boolean resetPending = false;
	
	/** List of IDs for characters that appear in this test */
	protected ArrayList<String> charIDs;
	
	/** List of char ID suffices used in this test */
	protected ArrayList<String> suffixes;

	public SwimTest() {
		inputQueue = new LinkedList<QueueItem>();
		charIDs = new ArrayList<String>();
		suffixes = new ArrayList<String>();
	}

	public void init(SwimModel argModel) {
		model = argModel;
		destructionListener = new DestructionListener() {

			public void sayGoodbye(Fixture fixture) {
			}

			public void sayGoodbye(Joint joint) {
				if (mouseJoint == joint) {
					mouseJoint = null;
				} else {
					jointDestroyed(joint);
				}
			}
		};
		 
		scenario = ScenarioLibrary.getBasicScenario(charIDs);

		bomb = null;
		mouseJoint = null;
		runtime = 0.0f;
		frameCount = 0;

		BodyDef bodyDef = new BodyDef();
		groundBody = getWorld().createBody(bodyDef);
		
		pointCount = 0;
		stepCount = 0;
		bombSpawning = false;

		getWorld().setDestructionListener(destructionListener);
		getWorld().setContactListener(this);
		getWorld().setDebugDraw(model.getDebugDraw());

		if (hasCachedCamera) {
			setCamera(cachedCameraPos, cachedCameraScale);
		} else {
			setCamera(getDefaultCameraPos(), getDefaultCameraScale());
		}
		setTitle(getTestName());

		initTest();
	}

	/**
	 * Gets the current world
	 * 
	 * @return
	 */
	public World getWorld() {
		return scenario.getWorld();
	}

	/**
	 * Gets the swim app model
	 * 
	 * @return
	 */
	public SwimModel getModel() {
		return model;
	}

	/**
	 * Gets the contact points for the current test
	 * 
	 * @return
	 */
	public static ContactPoint[] getContactPoints() {
		return points;
	}
	
	/** Returns total frames simulated since last reset */
	public long getFrameCount() {return frameCount;}

	/**
	 * Gets the ground body of the world, used for some joints
	 * 
	 * @return
	 */
	public Body getGroundBody() {
		return groundBody;
	}

	/**
	 * Gets the debug draw for the testbed
	 * 
	 * @return
	 */
	public DebugDraw getDebugDraw() {
		return model.getDebugDraw();
	}

	/**
	 * Gets the world position of the mouse
	 * 
	 * @return
	 */
	public Vec2 getWorldMouse() {
		return mouseWorld;
	}

	public int getStepCount() {
		return stepCount;
	}

	/**
	 * The number of contact points we're storing
	 * 
	 * @return
	 */
	public int getPointCount() {
		return pointCount;
	}

	/**
	 * Gets the 'bomb' body if it's present
	 * 
	 * @return
	 */
	public Body getBomb() {
		return bomb;
	}

	public float getCachedCameraScale() {
		return cachedCameraScale;
	}

	public void setCachedCameraScale(float cachedCameraScale) {
		this.cachedCameraScale = cachedCameraScale;
	}

	public Vec2 getCachedCameraPos() {
		return cachedCameraPos;
	}

	public void setCachedCameraPos(Vec2 argPos) {
		cachedCameraPos.set(argPos);
	}

	public boolean isHasCachedCamera() {
		return hasCachedCamera;
	}

	public void setHasCachedCamera(boolean hasCachedCamera) {
		this.hasCachedCamera = hasCachedCamera;
	}

	public boolean isDialogOnSaveLoadErrors() {
		return dialogOnSaveLoadErrors;
	}

	public void setDialogOnSaveLoadErrors(boolean dialogOnSaveLoadErrors) {
		this.dialogOnSaveLoadErrors = dialogOnSaveLoadErrors;
	}

	public Vec2 getDefaultCameraPos() {
		return defaultCameraPos;
	}

	public void setDefaultCameraPos(float x, float y) {
		defaultCameraPos.set(x, y);
	}
	
	public float getDefaultCameraScale() {
		return defaultCameraScale;
	}
	
	public void setDefaultCameraScale(float val) { 
		this.defaultCameraScale = val;
	}

	/**
	 * Gets the filename of the current test. Default implementation uses the
	 * test name with no spaces".
	 * 
	 * @return
	 */
	public String getFilename() {
		return getTestName().toLowerCase().replaceAll(" ", "_") + ".box2d";
	}

	/**
	 * Resets the test
	 */
	public void reset() {
		resetPending = true;
	}

	protected void _reset() {
		init(model);
	}

	/**
	 * Sets the current testbed camera
	 * 
	 * @param argPos
	 * @param scale
	 */
	public void setCamera(Vec2 argPos, float scale) {
		model.getDebugDraw().setCamera(argPos.x, argPos.y, scale);
		hasCachedCamera = true;
		cachedCameraScale = scale;
		cachedCameraPos.set(argPos);
	}

	/**
	 * Initializes the current test. Override for test-specific setup
	 */
	public abstract void initTest();

	/**
	 * The name of the test
	 * 
	 * @return
	 */
	public abstract String getTestName();
	
	/**
	 * The machine-friendly test ID
	 * 
	 * @return
	 */
	public abstract String getTestID();

	/**
	 * called when the tests exits
	 */
	public void exit() {
	}

	public void update() {
		if (resetPending) {
			_reset();
			resetPending = false;
		}

		textLine = 30;

		// process our input
		if (!inputQueue.isEmpty()) {
			synchronized (inputQueue) {
				while (!inputQueue.isEmpty()) {
					QueueItem i = inputQueue.pop();
					switch (i.type) {
					case KeyPressed:
						keyPressed(i.c, i.code);
						break;
					case KeyReleased:
						keyReleased(i.c, i.code);
						break;
					case MouseDown:
						mouseDown(i.p);
						break;
					case MouseMove:
						mouseMove(i.p);
						break;
					case MouseUp:
						mouseUp(i.p);
						break;
					case ShiftMouseDown:
						shiftMouseDown(i.p);
						break;
					case CtrlMouseDown:
						ctrlMouseDown(i.p);
						break;
					}
				}
			}
		}

		step(model.getSettings());
	}

	private final Color3f color1 = new Color3f(.3f, .95f, .3f);
	private final Color3f color2 = new Color3f(.3f, .3f, .95f);
	private final Color3f color3 = new Color3f(.9f, .9f, .9f);
	private final Color3f color4 = new Color3f(.6f, .61f, 1);
	private final Color3f mouseColor = new Color3f(0f, 1f, 0f);
	private final Vec2 p1 = new Vec2();
	private final Vec2 p2 = new Vec2();

	/**
	 * Returns time step to be used on next simulation update based on given settings
	 */
	protected float getTimeStep(SwimSettings settings) {
		float hz = (float)settings.getSetting(SwimSettings.Hz).value;
		float dt = hz > 0f ? 1f / hz : 0;
		
		if (settings.pause && settings.singleStep == false) 
			dt = 0;
		
		return dt;
	}
	
	/**
	 * Executes a world update and (if enabled) rendering of the test.
	 * @param settings
	 */
	public synchronized void step(SwimSettings settings) {
		float dt = getTimeStep(settings);
		
		if (settings.singleStep && !settings.pause)
			settings.pause = true;

		if (settings.pause && settings.singleStep)
			settings.singleStep = false;

		getWorld().setWarmStarting(settings
				.getSetting(SwimSettings.WarmStarting).enabled);
		getWorld().setContinuousPhysics(settings
				.getSetting(SwimSettings.ContinuousCollision).enabled);

		pointCount = 0;
		
		scenario.step(settings, dt);
		
		if (drawingEnabled)
			debugDraw(settings);
		
		if (dt > 0)
			frameCount++;
	}
	
	/** 
	 * Renders the current world & debugging details for this test
	 * @arg settings
	 */
	public void debugDraw(SwimSettings settings) {
		float hz = (float)settings.getSetting(SwimSettings.Hz).value;
		float timeStep = hz > 0f ? 1f / hz : 0;
		
		if (settings.pause) {
			model.getDebugDraw().drawString(5, textLine, "****PAUSED****", Color3f.WHITE);
			textLine += 15;
		}

		int flags = 0;
		flags += settings.getSetting(SwimSettings.DrawShapes).enabled ? DebugDraw.e_shapeBit
				: 0;
		flags += settings.getSetting(SwimSettings.DrawJoints).enabled ? DebugDraw.e_jointBit
				: 0;
		flags += settings.getSetting(SwimSettings.DrawAABBs).enabled ? DebugDraw.e_aabbBit
				: 0;
		flags += settings.getSetting(SwimSettings.DrawPairs).enabled ? DebugDraw.e_pairBit
				: 0;
		flags += settings.getSetting(SwimSettings.DrawCOMs).enabled ? DebugDraw.e_centerOfMassBit
				: 0;
		flags += settings.getSetting(SwimSettings.DrawTree).enabled ? DebugDraw.e_dynamicTreeBit
				: 0;
		model.getDebugDraw().setFlags(flags);
		
		World world = getWorld();
		
		world.drawDebugData();

		if (timeStep > 0f) {
			++stepCount;
		}

		if (settings.getSetting(SwimSettings.DrawStats).enabled) {
			if (title != null)
				model.getDebugDraw().drawString(model.getPanelWidth() / 2, 15, title, Color3f.WHITE);
			
			// Vec2.watchCreations = true;
			model.getDebugDraw().drawString(5, textLine, "Engine Info",
					color4);
			textLine += 15;
			model.getDebugDraw().drawString(5, textLine,
					"Framerate: " + model.getCalculatedFps(), Color3f.WHITE);
			textLine += 15;
			model.getDebugDraw().drawString(
					5,
					textLine,
					"bodies/contacts/joints/proxies = "
							+ world.getBodyCount() + "/"
							+ world.getContactCount() + "/"
							+ world.getJointCount() + "/"
							+ world.getProxyCount(), Color3f.WHITE);
			textLine += 20;
		}

		if (settings.getSetting(SwimSettings.DrawHelp).enabled) {
			model.getDebugDraw().drawString(5, textLine, "Help", color4);
			textLine += 15;
			model.getDebugDraw().drawString(5, textLine,
					"Click and drag the left mouse button to move objects.",
					Color3f.WHITE);
			textLine += 15;
			model.getDebugDraw().drawString(5, textLine,
					"Shift-Click to aim a bullet, or press space.",
					Color3f.WHITE);
			textLine += 15;
			model.getDebugDraw().drawString(5, textLine,
					"Click and drag the right mouse button to move the view.",
					Color3f.WHITE);
			textLine += 15;
			model.getDebugDraw().drawString(5, textLine,
					"Scroll to zoom in/out.", Color3f.WHITE);
			textLine += 15;
			model.getDebugDraw().drawString(5, textLine,
					"Press '[' or ']' to change tests, and 'r' to restart.",
					Color3f.WHITE);
			textLine += 20;
		}

		if (!textList.isEmpty()) {
			model.getDebugDraw().drawString(5, textLine, "Test Info", color4);
			textLine += 15;
			for (String s : textList) {
				model.getDebugDraw()
						.drawString(5, textLine, s, Color3f.WHITE);
				textLine += 15;
			}
			textList.clear();
		}

		if (mouseJoint != null) {
			mouseJoint.getAnchorB(p1);
			Vec2 p2 = mouseJoint.getTarget();

			model.getDebugDraw().drawSegment(p1, p2, mouseColor);
		}

		if (bombSpawning) {
			model.getDebugDraw().drawSegment(bombSpawnPoint, mouseWorld,
					Color3f.WHITE);
		}

		if (settings.getSetting(SwimSettings.DrawContactPoints).enabled) {
			final float axisScale = .3f;

			for (int i = 0; i < pointCount; i++) {

				ContactPoint point = points[i];

				if (point.state == PointState.ADD_STATE) {
					model.getDebugDraw().drawPoint(point.position, 10f, color1);
				} else if (point.state == PointState.PERSIST_STATE) {
					model.getDebugDraw().drawPoint(point.position, 5f, color2);
				}

				if (settings.getSetting(SwimSettings.DrawNormals).enabled) {
					p1.set(point.position);
					p2.set(point.normal).mulLocal(axisScale).addLocal(p1);
					model.getDebugDraw().drawSegment(p1, p2, color3);
				}
			}
		}
		
		//Draw controller debug info
		for (DynamicsController controller : scenario.getDynamicsControllers()) {
			controller.draw(model.getDebugDraw(), settings);
		}
		
		//Draw character debug info
		if (settings.getSetting(SwimSettings.DrawDebugChars).enabled) {
			for (SwimCharacter character : scenario.getCharacters())
				character.debugDraw(model.getDebugDraw());
		}
	}

	public void queueShiftMouseDown(Vec2 p) {
		synchronized (inputQueue) {
			inputQueue.addLast(new QueueItem(QueueItemType.ShiftMouseDown, p));
		}
	}
	
	public void queueCtrlMouseDown(Vec2 p) {
		synchronized (inputQueue) {
			inputQueue.addLast(new QueueItem(QueueItemType.CtrlMouseDown, p));
		}
	}

	public void queueMouseUp(Vec2 p) {
		synchronized (inputQueue) {
			inputQueue.addLast(new QueueItem(QueueItemType.MouseUp, p));
		}
	}

	public void queueMouseDown(Vec2 p) {
		synchronized (inputQueue) {
			inputQueue.addLast(new QueueItem(QueueItemType.MouseDown, p));
		}
	}

	public void queueMouseMove(Vec2 p) {
		synchronized (inputQueue) {
			inputQueue.addLast(new QueueItem(QueueItemType.MouseMove, p));
		}
	}

	public void queueKeyPressed(char c, int code) {
		synchronized (inputQueue) {
			inputQueue
					.addLast(new QueueItem(QueueItemType.KeyPressed, c, code));
		}
	}

	public void queueKeyReleased(char c, int code) {
		synchronized (inputQueue) {
			inputQueue
					.addLast(new QueueItem(QueueItemType.KeyReleased, c, code));
		}
	}

	/**
	 * Called when shift-mouse down occurs
	 * 
	 * @param p
	 */
	public void shiftMouseDown(Vec2 p) {
		mouseWorld.set(p);

		if (mouseJoint != null) {
			return;
		}

		spawnBomb(p);
	}
	
	/**
	 * Called when ctrl-mouse down occurs
	 * 
	 * @param p
	 */
	public void ctrlMouseDown(Vec2 p) {
		mouseWorld.set(p);

		if (mouseJoint != null) {
			return;
		}

		queryAABB.lowerBound.set(p.x - .001f, p.y - .001f);
		queryAABB.upperBound.set(p.x + .001f, p.y + .001f);
		callback.point.set(p);
		callback.fixture = null;
		getWorld().queryAABB(callback, queryAABB);

		//Swap between dynamic and static
		if (callback.fixture != null) {
			Body body = callback.fixture.getBody();
			switch (body.m_type) {
				case DYNAMIC: body.setType(BodyType.STATIC); break;
				case STATIC: body.setType(BodyType.DYNAMIC); break;
			}
		}
	}

	/**
	 * Called for mouse-up
	 * 
	 * @param p
	 */
	public void mouseUp(Vec2 p) {
		if (mouseJoint != null) {
			getWorld().destroyJoint(mouseJoint);
			mouseJoint = null;
		}

		if (bombSpawning) {
			completeBombSpawn(p);
		}
	}

	private final AABB queryAABB = new AABB();
	private final TestQueryCallback callback = new TestQueryCallback();

	/**
	 * Called for mouse-down
	 * 
	 * @param p
	 */
	public void mouseDown(Vec2 p) {
		mouseWorld.set(p);

		if (mouseJoint != null) {
			return;
		}

		queryAABB.lowerBound.set(p.x - .001f, p.y - .001f);
		queryAABB.upperBound.set(p.x + .001f, p.y + .001f);
		callback.point.set(p);
		callback.fixture = null;
		getWorld().queryAABB(callback, queryAABB);

		if (callback.fixture != null) {
			Body body = callback.fixture.getBody();
			MouseJointDef def = new MouseJointDef();
			def.bodyA = groundBody;
			def.bodyB = body;
			def.target.set(p);
			def.maxForce = 1000f * body.getMass();
			mouseJoint = (MouseJoint) getWorld().createJoint(def);
			body.setAwake(true);
		}
	}

	/**
	 * Called when mouse is moved
	 * 
	 * @param p
	 */
	public void mouseMove(Vec2 p) {
		mouseWorld.set(p);

		if (mouseJoint != null) {
			mouseJoint.setTarget(p);
		}
	}

	/**
	 * Sets the title of the test
	 * 
	 * @param argTitle
	 */
	public void setTitle(String argTitle) {
		title = argTitle;
	}

	/**
	 * Adds a text line to the reporting area
	 * 
	 * @param argTextLine
	 */
	public void addTextLine(String argTextLine) {
		textList.add(argTextLine);
	}

	private final Vec2 p = new Vec2();
	private final Vec2 v = new Vec2();

	public void lanchBomb() {
		p.set((float) (Math.random() * 30 - 15), 30f);
		v.set(p).mulLocal(-5f);
		launchBomb(p, v);
	}

	private final AABB aabb = new AABB();

	public synchronized void launchBomb(Vec2 position, Vec2 velocity) {
		if (bomb != null) {
			getWorld().destroyBody(bomb);
			bomb = null;
		}
		// todo optimize this
		BodyDef bd = new BodyDef();
		bd.type = BodyType.DYNAMIC;
		bd.position.set(position);
		bd.bullet = true;
		bomb = getWorld().createBody(bd);
		bomb.setLinearVelocity(velocity);

		CircleShape circle = new CircleShape();
		circle.m_radius = 0.3f;

		FixtureDef fd = new FixtureDef();
		fd.shape = circle;
		fd.density = 20f;
		fd.restitution = 0;

		Vec2 minV = new Vec2(position);
		Vec2 maxV = new Vec2(position);

		minV.subLocal(new Vec2(.3f, .3f));
		maxV.addLocal(new Vec2(.3f, .3f));

		aabb.lowerBound.set(minV);
		aabb.upperBound.set(maxV);

		bomb.createFixture(fd);
	}

	public void setDrawingEnabled(boolean enabled) {
		this.drawingEnabled = enabled;
	}
	
	public synchronized void spawnBomb(Vec2 worldPt) {
		bombSpawnPoint.set(worldPt);
		bombSpawning = true;
	}

	private final Vec2 vel = new Vec2();

	public synchronized void completeBombSpawn(Vec2 p) {
		if (bombSpawning == false) {
			return;
		}

		float multiplier = 30f;
		vel.set(bombSpawnPoint).subLocal(p);
		vel.mulLocal(multiplier);
		launchBomb(bombSpawnPoint, vel);
		bombSpawning = false;
	}

	/**
	 * Override to enable saving and loading. Remember to also override the
	 * {@link ObjectListener} and {@link ObjectSigner} methods if you need to
	 * 
	 * @return
	 */
	public boolean isSaveLoadEnabled() {
		return false;
	}

	public void jointDestroyed(Joint joint) {
	}

	public void beginContact(Contact contact) {
	}

	public void endContact(Contact contact) {
	}

	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	private final PointState[] state1 = new PointState[Settings.maxManifoldPoints];
	private final PointState[] state2 = new PointState[Settings.maxManifoldPoints];
	private final WorldManifold worldManifold = new WorldManifold();

	public void preSolve(Contact contact, Manifold oldManifold) {
		Manifold manifold = contact.getManifold();

		if (manifold.pointCount == 0) {
			return;
		}

		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();

		Collision.getPointStates(state1, state2, oldManifold, manifold);

		contact.getWorldManifold(worldManifold);

		for (int i = 0; i < manifold.pointCount
				&& pointCount < MAX_CONTACT_POINTS; i++) {
			ContactPoint cp = points[pointCount];
			cp.fixtureA = fixtureA;
			cp.fixtureB = fixtureB;
			cp.position.set(worldManifold.points[i]);
			cp.normal.set(worldManifold.normal);
			cp.state = state2[i];
			++pointCount;
		}
	}

	public void keyPressed(char argKeyChar, int argKeyCode) {
	}

	public void keyReleased(char argKeyChar, int argKeyCode) {
	}
}

class TestQueryCallback implements QueryCallback {

	public final Vec2 point;
	public Fixture fixture;

	public TestQueryCallback() {
		point = new Vec2();
		fixture = null;
	}

	/**
	 * @see org.jbox2d.callbacks.QueryCallback#reportFixture(org.jbox2d.dynamics.Fixture)
	 */
	public boolean reportFixture(Fixture argFixture) {
		Body body = argFixture.getBody();
		if (body.getType() == BodyType.DYNAMIC) {
			boolean inside = argFixture.testPoint(point);
			if (inside) {
				fixture = argFixture;

				return false;
			}
		}

		return true;
	}
}

enum QueueItemType {
	MouseDown, MouseMove, MouseUp, ShiftMouseDown, KeyPressed, KeyReleased, CtrlMouseDown
}

class QueueItem {
	public QueueItemType type;
	public Vec2 p;
	public char c;
	public int code;

	public QueueItem(QueueItemType t, Vec2 pt) {
		type = t;
		p = pt;
	}

	public QueueItem(QueueItemType t, char cr, int cd) {
		type = t;
		c = cr;
		code = cd;
	}
}