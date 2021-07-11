/*
 * Copyright (c) 2017, Kronos <https://github.com/KronosDesign>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.devtools;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static java.lang.Math.min;

@PluginDescriptor(
		name = "Developer Tools",
		tags = {"panel"},
		developerPlugin = true
)
@Getter
public class DevToolsPlugin extends Plugin
{

	private boolean track = false;
	private JsonArray array = new JsonArray();
	private final List<String> lines = new ArrayList<>();

	private static final List<MenuAction> EXAMINE_MENU_ACTIONS = ImmutableList.of(MenuAction.EXAMINE_ITEM,
			MenuAction.EXAMINE_ITEM_GROUND, MenuAction.EXAMINE_NPC, MenuAction.EXAMINE_OBJECT);

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DevToolsOverlay overlay;

	@Inject
	private LocationOverlay locationOverlay;

	@Inject
	private SceneOverlay sceneOverlay;

	@Inject
	private CameraOverlay cameraOverlay;

	@Inject
	private WorldMapLocationOverlay worldMapLocationOverlay;

	@Inject
	private WorldMapRegionOverlay mapRegionOverlay;

	@Inject
	private SoundEffectOverlay soundEffectOverlay;

	@Inject
	private EventBus eventBus;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	private DevToolsButton toggleTracking;
	private DevToolsButton saveTrackedData;
	private DevToolsButton actorAnimationsAndGraphics;
	private DevToolsButton players;
	private DevToolsButton npcs;
	private DevToolsButton groundItems;
	private DevToolsButton groundObjects;
	private DevToolsButton gameObjects;
	private DevToolsButton graphicsObjects;
	private DevToolsButton walls;
	private DevToolsButton decorations;
	private DevToolsButton inventory;
	private DevToolsButton projectiles;
	private DevToolsButton location;
	private DevToolsButton chunkBorders;
	private DevToolsButton mapSquares;
	private DevToolsButton validMovement;
	private DevToolsButton movementFlags;
	private DevToolsButton lineOfSight;
	private DevToolsButton cameraPosition;
	private DevToolsButton worldMapLocation;
	private DevToolsButton tileLocation;
	private DevToolsButton interacting;
	private DevToolsButton examine;
	private DevToolsButton detachedCamera;
	private DevToolsButton widgetInspector;
	private DevToolsButton varInspector;
	private DevToolsButton soundEffects;
	private DevToolsButton scriptInspector;
	private DevToolsButton inventoryInspector;
	private DevToolsButton roofs;
	private DevToolsButton shell;
	private NavigationButton navButton;
	private boolean switchedOn;

	@Provides
	DevToolsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DevToolsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		actorAnimationsAndGraphics = new DevToolsButton("Animations and Graphics");

		players = new DevToolsButton("Players");
		npcs = new DevToolsButton("NPCs");

		groundItems = new DevToolsButton("Ground Items");
		groundObjects = new DevToolsButton("Ground Objects");
		gameObjects = new DevToolsButton("Game Objects");
		graphicsObjects = new DevToolsButton("Graphics Objects");
		walls = new DevToolsButton("Walls");
		decorations = new DevToolsButton("Decorations");

		inventory = new DevToolsButton("Inventory");
		projectiles = new DevToolsButton("Projectiles");

		location = new DevToolsButton("Location");
		worldMapLocation = new DevToolsButton("World Map Location");
		tileLocation = new DevToolsButton("Tile Location");
		cameraPosition = new DevToolsButton("Camera Position");

		chunkBorders = new DevToolsButton("Chunk Borders");
		mapSquares = new DevToolsButton("Map Squares");

		lineOfSight = new DevToolsButton("Line Of Sight");
		validMovement = new DevToolsButton("Valid Movement");
		movementFlags = new DevToolsButton("Movement Flags");
		interacting = new DevToolsButton("Interacting");
		examine = new DevToolsButton("Examine");

		detachedCamera = new DevToolsButton("Detached Camera");
		widgetInspector = new DevToolsButton("Widget Inspector");
		varInspector = new DevToolsButton("Var Inspector");
		soundEffects = new DevToolsButton("Sound Effects");
		scriptInspector = new DevToolsButton("Script Inspector");
		inventoryInspector = new DevToolsButton("Inventory Inspector");
		roofs = new DevToolsButton("Roofs");
		shell = new DevToolsButton("Shell");

		toggleTracking = new DevToolsButton("Toggle Tracking");
		saveTrackedData = new DevToolsButton("Save Tracked Data");

		overlayManager.add(overlay);
		overlayManager.add(locationOverlay);
		overlayManager.add(sceneOverlay);
		overlayManager.add(cameraOverlay);
		overlayManager.add(worldMapLocationOverlay);
		overlayManager.add(mapRegionOverlay);
		overlayManager.add(soundEffectOverlay);

		overlayManager.add(new Overlay() {
			@Override
			public Dimension render(Graphics2D graphics) {

				if(track) {
					graphics.setColor(Color.GREEN);
					graphics.drawString("Tracking", 0, 20);
				} else {
					graphics.setColor(Color.RED);
					graphics.drawString("Not tracking", 0, 20);
				}

				return null;
			}
		});
		toggleTracking.addActionListener(e -> {
			track = !track;

			resetJsonArrays();
		});

		saveTrackedData.addActionListener(e -> {

			if (switchedOn) {
				switchedOn = false;
				return;
			}
			Date date = new Date() ;
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH-mm") ;

			String fileName = JOptionPane.showInputDialog("Please input filename: ");

			if ((fileName == null) || (fileName.length() == 0)) {
				return;
			}

			File jsonFile = Paths.get("json_dumps", fileName+".json").toFile();
			File txtFile = Paths.get("txt_dumps", fileName+".txt").toFile();

			jsonFile.getParentFile().mkdir();
			txtFile.getParentFile().mkdir();

			int i = 1;
			while (jsonFile.exists()){
				jsonFile = Paths.get("json_dumps", fileName+"_"+i+".json").toFile();
				i++;
			}
			i = 1;
			while (txtFile.exists()){
				txtFile = Paths.get("txt_dumps", fileName+"_"+i+".txt").toFile();
				i++;
			}
			try {

				jsonFile.createNewFile();
				final FileWriter writer = new FileWriter(jsonFile);
				final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
				gson.toJson(array, writer);
				writer.flush();
				writer.close();

				txtFile.createNewFile();
				final PrintWriter printWriter = new PrintWriter(new FileWriter(txtFile));
				for(String line : lines)
					printWriter.println(line);
				printWriter.flush();
				printWriter.close();;

			} catch (IOException ex) {
				ex.printStackTrace();
			}
			resetJsonArrays();
			switchedOn = true;
		});

		final DevToolsPanel panel = injector.getInstance(DevToolsPanel.class);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "devtools_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Developer Tools")
				.icon(icon)
				.priority(1)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		eventBus.register(soundEffectOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		eventBus.unregister(soundEffectOverlay);
		overlayManager.remove(overlay);
		overlayManager.remove(locationOverlay);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(cameraOverlay);
		overlayManager.remove(worldMapLocationOverlay);
		overlayManager.remove(mapRegionOverlay);
		overlayManager.remove(soundEffectOverlay);
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		String[] args = commandExecuted.getArguments();

		switch (commandExecuted.getCommand())
		{
			case "logger":
			{
				final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
				String message;
				Level currentLoggerLevel = logger.getLevel();

				if (args.length < 1)
				{
					message = "Logger level is currently set to " + currentLoggerLevel;
				}
				else
				{
					Level newLoggerLevel = Level.toLevel(args[0], currentLoggerLevel);
					logger.setLevel(newLoggerLevel);
					message = "Logger level has been set to " + newLoggerLevel;
				}

				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
				break;
			}
			case "getvarp":
			{
				int varp = Integer.parseInt(args[0]);
				int[] varps = client.getVarps();
				int value = varps[varp];
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "VarPlayer " + varp + ": " + value, null);
				break;
			}
			case "setvarp":
			{
				int varp = Integer.parseInt(args[0]);
				int value = Integer.parseInt(args[1]);
				int[] varps = client.getVarps();
				varps[varp] = value;
				client.queueChangedVarp(varp);
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Set VarPlayer " + varp + " to " + value, null);
				VarbitChanged varbitChanged = new VarbitChanged();
				varbitChanged.setIndex(varp);
				eventBus.post(varbitChanged); // fake event
				break;
			}
			case "getvarb":
			{
				int varbit = Integer.parseInt(args[0]);
				int value = client.getVarbitValue(client.getVarps(), varbit);
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Varbit " + varbit + ": " + value, null);
				break;
			}
			case "setvarb":
			{
				int varbit = Integer.parseInt(args[0]);
				int value = Integer.parseInt(args[1]);
				client.setVarbitValue(client.getVarps(), varbit, value);
				VarbitComposition varbitComposition = client.getVarbit(varbit);
				client.queueChangedVarp(varbitComposition.getIndex());
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Set varbit " + varbit + " to " + value, null);
				eventBus.post(new VarbitChanged()); // fake event
				break;
			}
			case "addxp":
			{
				Skill skill = Skill.valueOf(args[0].toUpperCase());
				int xp = Integer.parseInt(args[1]);

				int totalXp = client.getSkillExperience(skill) + xp;
				int level = min(Experience.getLevelForXp(totalXp), 99);

				client.getBoostedSkillLevels()[skill.ordinal()] = level;
				client.getRealSkillLevels()[skill.ordinal()] = level;
				client.getSkillExperiences()[skill.ordinal()] = totalXp;

				client.queueChangedSkill(skill);

				StatChanged statChanged = new StatChanged(
						skill,
						totalXp,
						level,
						level
				);
				eventBus.post(statChanged);
				break;
			}
			case "setstat":
			{
				Skill skill = Skill.valueOf(args[0].toUpperCase());
				int level = Integer.parseInt(args[1]);

				level = Ints.constrainToRange(level, 1, Experience.MAX_REAL_LEVEL);
				int xp = Experience.getXpForLevel(level);

				client.getBoostedSkillLevels()[skill.ordinal()] = level;
				client.getRealSkillLevels()[skill.ordinal()] = level;
				client.getSkillExperiences()[skill.ordinal()] = xp;

				client.queueChangedSkill(skill);

				StatChanged statChanged = new StatChanged(
						skill,
						xp,
						level,
						level
				);
				eventBus.post(statChanged);
				break;
			}
			case "anim":
			{
				int id = Integer.parseInt(args[0]);
				Player localPlayer = client.getLocalPlayer();
				localPlayer.setAnimation(id);
				localPlayer.setAnimationFrame(0);
				break;
			}
			case "gfx":
			{
				int id = Integer.parseInt(args[0]);
				Player localPlayer = client.getLocalPlayer();
				localPlayer.setGraphic(id);
				localPlayer.setSpotAnimFrame(0);
				break;
			}
			case "transform":
			{
				int id = Integer.parseInt(args[0]);
				Player player = client.getLocalPlayer();
				player.getPlayerComposition().setTransformedNpcId(id);
				player.setIdlePoseAnimation(-1);
				player.setPoseAnimation(-1);
				break;
			}
			case "cape":
			{
				int id = Integer.parseInt(args[0]);
				Player player = client.getLocalPlayer();
				player.getPlayerComposition().getEquipmentIds()[KitType.CAPE.getIndex()] = id + 512;
				player.getPlayerComposition().setHash();
				break;
			}
			case "sound":
			{
				int id = Integer.parseInt(args[0]);
				client.playSoundEffect(id);
				break;
			}
			case "msg":
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", String.join(" ", args), "");
				break;
			}
			case "setconf":
			{
				// setconf group.key name = value
				String group = args[0];
				String key = args[1], value = "";
				for (int i = 2; i < args.length; ++i)
				{
					if (args[i].equals("="))
					{
						value = String.join(" ", Arrays.copyOfRange(args, i + 1, args.length));
						break;
					}

					key += " " + args[i];
				}
				String current = configManager.getConfiguration(group, key);
				final String message;
				if (value.isEmpty())
				{
					configManager.unsetConfiguration(group, key);
					message = String.format("Unset configuration %s.%s (was: %s)", group, key, current);
				}
				else
				{
					configManager.setConfiguration(group, key, value);
					message = String.format("Set configuration %s.%s to %s (was: %s)", group, key, value, current);
				}
				chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.GAMEMESSAGE)
						.runeLiteFormattedMessage(new ChatMessageBuilder().append(message).build())
						.build());
				break;
			}
			case "getconf":
			{
				String group = args[0], key = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				String value = configManager.getConfiguration(group, key);
				final String message = String.format("%s.%s = %s", group, key, value);
				chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.GAMEMESSAGE)
						.runeLiteFormattedMessage(new ChatMessageBuilder().append(message).build())
						.build());
				break;
			}
			case "modicons":
			{
				final ChatMessageBuilder builder = new ChatMessageBuilder();
				final IndexedSprite[] modIcons = client.getModIcons();
				for (int i = 0; i < modIcons.length; i++)
				{
					builder.append(i + "=").img(i);

					if (i != modIcons.length - 1)
					{
						builder.append(", ");
					}
				}
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(builder.build())
					.build());
				break;
			}
		}
	}

	private void resetJsonArrays() {
		array = new JsonArray();
		playerSounds = new JsonArray();
		areaSounds = new JsonArray();
		lines.clear();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (!examine.isActive()) {
			return;
		}

		MenuAction action = MenuAction.of(event.getType());

		if (EXAMINE_MENU_ACTIONS.contains(action)) {
			MenuEntry[] entries = client.getMenuEntries();
			MenuEntry entry = entries[entries.length - 1];

			final int identifier = event.getIdentifier();
			String info = "ID: ";

			if (action == MenuAction.EXAMINE_NPC) {
				NPC npc = client.getCachedNPCs()[identifier];
				info += npc.getId();
			} else {
				info += identifier;

				if (action == MenuAction.EXAMINE_OBJECT) {
					WorldPoint point = WorldPoint.fromScene(client, entry.getParam0(), entry.getParam1(), client.getPlane());
					info += " X: " + point.getX() + " Y: " + point.getY();
				}
			}

			entry.setTarget(entry.getTarget() + " " + ColorUtil.prependColorTag("(" + info + ")", JagexColors.MENU_TARGET));
			client.setMenuEntries(entries);
		}
	}

	final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	volatile WorldPoint lastWp = null;
	JsonArray playerSounds = new JsonArray();
	JsonArray npcSounds = new JsonArray();
	JsonArray areaSounds = new JsonArray();
	JsonArray groundItemArray = new JsonArray();

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		final TileItem tileItem = itemSpawned.getItem();
		final JsonObject groundItem = new JsonObject();
		groundItem.addProperty("id, quantity, spawnTime", tileItem.getId()+", "+tileItem.getQuantity()+", "+tileItem.getSpawnTime());
		groundItemArray.add(groundItem);
		save("GROUND_ITEM "+tileItem.getId()+"\t quantity "+tileItem.getQuantity()+"\t spawnTime "+tileItem.getSpawnTime());
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed soundEffectPlayed){
		final JsonObject sound = new JsonObject();
		sound.addProperty("id, delay", soundEffectPlayed.getSoundId()+", "+soundEffectPlayed.getDelay());
		playerSounds.add(sound);
		save("SOUND "+soundEffectPlayed.getSoundId()+"\t delay: "+soundEffectPlayed.getDelay());
	}

	@Subscribe
	public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed areaSoundEffectPlayed){
		final JsonObject sound = new JsonObject();
		sound.addProperty("id", areaSoundEffectPlayed.getSoundId());
		sound.addProperty("delay", areaSoundEffectPlayed.getDelay());
		sound.addProperty("range", areaSoundEffectPlayed.getRange());
		sound.addProperty("sceneX", areaSoundEffectPlayed.getSceneX());
		sound.addProperty("sceneY", areaSoundEffectPlayed.getSceneY());
		String details = "AREA_SOUND "+areaSoundEffectPlayed.getSoundId()+"\t delay: "+areaSoundEffectPlayed.getDelay()+", range: "+areaSoundEffectPlayed.getRange()+", sceneX: "+areaSoundEffectPlayed.getSceneX()+", sceneY: "+areaSoundEffectPlayed.getSceneY();

		if(areaSoundEffectPlayed.getSource() instanceof NPC){
			final Player localPlayer = client.getLocalPlayer();
			final NPC npc = ((NPC) areaSoundEffectPlayed.getSource());
			if(npc.getInteracting() == localPlayer || Objects.requireNonNull(localPlayer).getInteracting() == npc) {
				sound.addProperty("source", npc.getName() + " (" + npc.getId() + ")");
				areaSounds.add(sound);
				details+=", source: " + npc.getName() + " (" + npc.getId() + ")";
			}
		} else if(areaSoundEffectPlayed.getSource() instanceof Player){
			sound.addProperty("source",  areaSoundEffectPlayed.getSource().getName());
			areaSounds.add(sound);
			details+=", source: " + areaSoundEffectPlayed.getSource().getName();
		}
		save(details);
	}

	GameState gameState = GameState.UNKNOWN;
	@Subscribe
	public void onGameStateChanged(final GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			save("---------| LOADING |----------");
		}
		gameState = event.getGameState();
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {


		final int tick = client.getTickCount();
		final Player local = Objects.requireNonNull(client.getLocalPlayer());
		final Actor interacting = local.getInteracting();

		final JsonObject object = new JsonObject();

		final WorldPoint wp = local.getWorldLocation();

		object.addProperty("server-tick", tick);
		object.addProperty("game-state", gameState.name()+" ("+gameState.getState()+")");

		String details = "";

		if (local.getAnimation() != -1) {
			object.addProperty("animation", local.getAnimation());
			details+="animation: "+local.getAnimation();
		}
		if (local.getGraphic() != -1) {
			object.addProperty("gfx", local.getGraphic());
			if(!details.isEmpty())
				details+=", ";
			details+="gfx: "+local.getGraphic();
		}
		if (lastWp == null || !lastWp.equals(wp))
			object.addProperty("pos", wp.getX() + ", " + wp.getY() + ", " + wp.getPlane() + "");

		if(!details.isEmpty()){
			save("PLAYER '"+local.getName()+"'\t "+details);
		}
		lastWp = wp;

		if (interacting != null)
			saveInteractingActor(interacting, object);

		saveSounds(object);
		saveGroundItems(object);

		playerSounds = new JsonArray();
		areaSounds = new JsonArray();
		groundItemArray = new JsonArray();

		saveAddedProjectiles(object);

		saveAddedGraphics(object);

		if (object.keySet().size() > 1 && track) {
			array.add(object);
		}
	}

	private void saveInteractingActor(Actor interacting, JsonObject object) {

		final JsonObject interactingObject = new JsonObject();

		String string = "";

		if (interacting instanceof Player) {
			interactingObject.addProperty("player", interacting.getName());
			string+="PLAYER '"+interacting.getName()+"'\t";
		} else if (interacting instanceof NPC) {
			interactingObject.addProperty("npc", interacting.getName() + " (" + ((NPC) interacting).getId() + ")");
			string+="NPC '"+interacting.getName()+ " (" + ((NPC) interacting).getId() + ")"+"'\t";
		}

		boolean save = false;
		if (interacting.getAnimation() != -1) {
			interactingObject.addProperty("animation", interacting.getAnimation());
			string+="animation: "+interacting.getAnimation();
			save = true;
		}

		if (interacting.getGraphic() != -1) {
			interactingObject.addProperty("gfx", interacting.getGraphic());
			if(save)
				string+=", ";
			string+="gfx: "+interacting.getGraphic();
			save = true;
		}

		if(save){
			save(string);
		}

		object.add("interacting", interactingObject);
	}

	private void saveSounds(JsonObject object) {
		if (areaSounds.size() > 0)
			object.add("area-sounds", areaSounds);

		if (playerSounds.size() > 0)
			object.add("sounds", playerSounds);
	}

	private void saveGroundItems(JsonObject object) {
		if(groundItemArray.size() > 0)
			object.add("ground-items", groundItemArray);
	}

	private void saveAddedProjectiles(JsonObject object) {
		final JsonArray addedProjectiles = new JsonArray();
		for (Projectile projectile : client.getProjectiles()) {
			final JsonObject proj = new JsonObject();
			proj.addProperty("id", projectile.getId());
			proj.addProperty("heights", projectile.getStartHeight() + ", " + projectile.getEndHeight());
			proj.addProperty("slope, duration", projectile.getSlope() + ", " + projectile.getRemainingCycles());
			final int projTick = projectile.getStartMovementCycle();
			final int clientCycle = client.getGameCycle();
			if (projTick > clientCycle && projTick < clientCycle + 15) {
				addedProjectiles.add(proj);
				save("PROJECTILE "+projectile.getId()+"\tstartHeight: "+projectile.getStartHeight()+", endHeight: "+projectile.getEndHeight()+", slope: "+projectile.getSlope()+", duration: "+projectile.getRemainingCycles()+", x: "+projectile.getX1()+", y: "+projectile.getY1());
			}
		}

		if (addedProjectiles.size() > 0)
			object.add("projectiles", addedProjectiles);
	}

	private void saveAddedGraphics(JsonObject object) {
		final JsonArray addedGraphics = new JsonArray();
		for (GraphicsObject graphicsObject : client.getGraphicsObjects()) {
			final JsonObject grap = new JsonObject();
			grap.addProperty("id", graphicsObject.getId());
			grap.addProperty("level", graphicsObject.getLevel());
			grap.addProperty("start", graphicsObject.getStartCycle());
			final int projTick = graphicsObject.getStartCycle();
			final int clientCycle = client.getGameCycle();

			if (projTick > clientCycle) {
				addedGraphics.add(grap);
				final LocalPoint localPoint = graphicsObject.getLocation();
				save("GFX "+graphicsObject.getId()+"\tlevel: "+graphicsObject.getLevel()+", start_cycle: "+graphicsObject.getStartCycle()+", x: "+localPoint.getX()+", y: "+localPoint.getY());
			}
		}

		if (addedGraphics.size() > 0)
			object.add("graphics", addedGraphics);

	}

	private void save(String... lines){
		Collections.addAll(this.lines, lines);
	}

}