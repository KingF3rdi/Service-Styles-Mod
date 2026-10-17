// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.MipmapStrategy;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws the worn cosmetics on a player model. {@link CosmeticRendering} adds it to every player renderer.
 *
 * <p>Boxes: each box cosmetic is one custom geometry draw. Its boxes are placed relative to the anchor point of the model
 * and then moved by its {@link Motion}. Geometry is given in model units, 16 units to a block, and textured with the
 * cosmetic texture laid out like a vanilla cube, or with a vanilla texture for a pet that names one. Shields are drawn
 * translucent, the others opaque. A pet at its shoulder anchor is drawn at half size.
 *
 * <p>Overlays: an overlay is the whole player model drawn a second time with the overlay texture. The model parts,
 * their poses and the slim or wide arms are the model's own, so the overlay lines up with every part. Glints scroll with
 * an energy swirl offset and shimmer in brightness. Auras have no boxes and are spawned by {@link CosmeticAuras}.
 */
public final class CosmeticLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
	/** Model units per block. */
	private static final float UNIT = 1.0f / 16.0f;
	private static final int WHITE = 0xFFFFFFFF;
	/** Steps of the scroll offset of an overlay over one repeat of its texture. */
	private static final int SCROLL_STEPS = 128;
	/** Overlays farther away than this are not drawn: a second pass over the model is not worth it for them. */
	private static final double OVERLAY_RANGE_SQ = 48.0 * 48.0;
	/** Most overlays kept at once. The catalog has ten, so this bound is never reached. */
	private static final int MAX_OVERLAYS = 16;
	private static final String NAMESPACE = "hugoservice";
	private static final Logger LOGGER = LoggerFactory.getLogger(CosmeticLayer.class);

	/** Texture of each cosmetic by id. A null value marks a cosmetic whose texture could not be made. */
	private static final Map<String, Identifier> TEXTURES = new HashMap<>();
	/** Render type of each box cosmetic by id. A null value marks a cosmetic whose texture could not be made. */
	private static final Map<String, RenderLayer> RENDER_TYPES = new HashMap<>();
	/** Overlays by id, made on first use. A null value marks an overlay whose texture could not be made. */
	private static final Map<String, Overlay> OVERLAYS = new HashMap<>();

	public CosmeticLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> parent) {
		super(parent);
	}

	@Override
	public void render(MatrixStack poseStack, OrderedRenderCommandQueue commands, int light, PlayerEntityRenderState state,
			float limbAngle, float limbDistance) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (state.invisible || client.world == null) {
			return;
		}
		Entity entity = client.world.getEntityById(state.id);
		if (entity == null) {
			return;
		}
		Loadout loadout = CosmeticRendering.loadoutOf(entity);
		if (loadout.isEmpty()) {
			return;
		}
		PlayerEntityModel model = getContextModel();
		boolean local = entity == client.player;
		// Copied out of the state now, the draw runs later and must not read the state again.
		Animation animation = new Animation(state.age, state.field_53536, state.field_53537, state.field_53538);
		for (Cosmetic cosmetic : loadout.all()) {
			if (cosmetic.anchor() == Anchor.WHOLE) {
				submitOverlay(poseStack, commands, light, state, model, cosmetic, local);
				continue;
			}
			if (cosmetic.boxes().isEmpty()) {
				continue;
			}
			RenderLayer type = renderTypeOf(cosmetic);
			if (type == null) {
				continue;
			}
			Anchor anchor = cosmetic.anchor();
			ModelPart part = anchor == Anchor.HEAD ? model.head : model.body;
			poseStack.push();
			// Same order as the vanilla head layer: the root first, then the part the cosmetic hangs from.
			model.getRootPart().applyTransform(poseStack);
			part.applyTransform(poseStack);
			if (anchor.x() != 0 || anchor.y() != 0 || anchor.z() != 0) {
				poseStack.translate(anchor.x() * UNIT, anchor.y() * UNIT, anchor.z() * UNIT);
			}
			if (anchor.scale() != 1.0f) {
				// A pet at its shoulder is drawn at half size. Its motions are scaled with it.
				poseStack.scale(anchor.scale(), anchor.scale(), anchor.scale());
			}
			commands.getBatchingQueue(0).submitCustom(poseStack, type,
					(pose, consumer) -> draw(pose, consumer, cosmetic, animation, light));
			poseStack.pop();
		}
	}

	/**
	 * Draws the player model again with the overlay texture. The model submits itself and sets its own pose from the
	 * render state when it is drawn, so the overlay follows the animation exactly. Coats are translucent and sorted with
	 * the other translucent models. Only the local player's overlays use the speed and strength settings: other players
	 * show the defaults.
	 */
	private void submitOverlay(MatrixStack poseStack, OrderedRenderCommandQueue commands, int light, PlayerEntityRenderState state,
			PlayerEntityModel model, Cosmetic cosmetic, boolean local) {
		// The second pass costs time, and an overlay this far away is too small to matter.
		if (state.squaredDistanceToCamera > OVERLAY_RANGE_SQ) {
			return;
		}
		Overlay overlay = overlayOf(cosmetic);
		if (overlay == null) {
			return;
		}
		float age = state.age;
		float speed = overlay.pace * (local ? OverlayTuning.speed(cosmetic.id()) : 1.0f);
		float strength = local ? OverlayTuning.strength(cosmetic.id()) : 1.0f;
		int step = Math.round(MotionMath.scroll(age * speed) * SCROLL_STEPS) % SCROLL_STEPS;
		int colour;
		int lightLevel = light;
		if (overlay.coat) {
			float opacity = cosmetic.motion() == Motion.DRIFT
					? MotionMath.coatAlpha(age)
					: MotionMath.shimmerBrightness(age);
			colour = alpha(opacity * strength);
		} else {
			float brightness = cosmetic.motion() == Motion.FLICKER
					? MotionMath.flickerBrightness(age)
					: MotionMath.shimmerBrightness(age);
			colour = grey(brightness * strength);
			// Light is emitted by a glint, so it shows in the dark as well.
			lightLevel = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		}
		commands.getBatchingQueue(0).submitModel(model, state, poseStack, overlay.at(step), lightLevel,
				OverlayTexture.DEFAULT_UV, colour, null, 0, null);
	}

	/** Time and cape motion of one frame. */
	private record Animation(float age, float flap, float lean, float sideLean) {
	}

	/** Emits all boxes of a cosmetic. The collector hands over a copy of the pose, so the local stack starts from it. */
	private static void draw(MatrixStack.Entry base, VertexConsumer consumer, Cosmetic cosmetic, Animation animation,
			int light) {
		MatrixStack local = new MatrixStack();
		local.peek().copy(base);
		// Motions of the whole cosmetic come first, so they move every box alike. Box motions follow per box.
		rigid(local, cosmetic.motion(), animation);
		for (CosmeticBox box : cosmetic.boxes()) {
			local.push();
			move(local, cosmetic.motion(), box, animation);
			emitBox(local.peek(), consumer, box, light, CosmeticTextures.uvSize(cosmetic));
			local.pop();
		}
	}

	/** Applies a motion that moves the whole cosmetic around its anchor point. */
	private static void rigid(MatrixStack pose, Motion motion, Animation animation) {
		switch (motion) {
			case HOVER, FLUTTER -> {
				// The pet bobs and turns a little around its own point, which the anchor puts beside the body.
				pose.translate(0.0f, MotionMath.hoverBob(animation.age()) * UNIT, 0.0f);
				pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MotionMath.hoverYawDegrees(animation.age())));
				pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MotionMath.hoverRollDegrees(animation.age())));
			}
			case PULSE -> {
				float scale = MotionMath.pulseScale(animation.age());
				pose.scale(scale, scale, scale);
			}
			default -> {
				// The other motions are per box, see move().
			}
		}
	}

	/** Applies the motion of a cosmetic to one of its boxes. */
	private static void move(MatrixStack pose, Motion motion, CosmeticBox box, Animation animation) {
		switch (motion) {
			case NONE -> {
				// Stays on the anchor.
			}
			case FLOAT -> pose.translate(0.0f, (float) Math.sin(animation.age() * 0.08) * 1.5f * UNIT, 0.0f);
			case SPIN -> pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animation.age() * 3.0f));
			case FLAP -> {
				// Wings open and close by a few degrees around the spine. The mirrored wing turns the other way, so
				// both wings move up and down together.
				float angle = 10.0f * (float) Math.sin(animation.age() * 0.2);
				boolean leftSide = box.x() + box.w() / 2 < 0;
				pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(leftSide ? angle : -angle));
			}
			case SWAY -> {
				// Hangs from its top edge on the back. The tilt and the roll come from the cape values of the
				// render state, the same values the vanilla cape uses: 6 degrees back at rest.
				float px = 0.0f;
				float py = box.y() * UNIT;
				float pz = box.z() * UNIT;
				// The tilt is the outer rotation, as in the vanilla cape: roll first, then tilt.
				pose.translate(px, py, pz);
				pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0f + animation.lean() / 2.0f + animation.flap()));
				pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(animation.sideLean() / 2.0f));
				pose.translate(-px, -py, -pz);
			}
			case FLUTTER -> {
				if (box.wing()) {
					// Wings beat around the pet's point: the side of the wing decides the direction of the turn.
					float angle = MotionMath.flutterDegrees(animation.age());
					boolean leftSide = box.x() + box.w() / 2 < 0;
					pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(leftSide ? angle : -angle));
				}
			}
			case WAVE -> {
				float phase = MotionMath.wavePhase(box.x() + box.w() / 2, box.z() + box.d() / 2);
				pose.translate(0.0f, MotionMath.waveOffset(animation.age(), phase) * UNIT, 0.0f);
			}
			default -> {
				// HOVER, PULSE and the overlay motions act on the whole cosmetic, see rigid().
			}
		}
	}

	/** Emits the six faces of a box. Each face is unfolded as seen from outside, like a vanilla cube. */
	private static void emitBox(MatrixStack.Entry pose, VertexConsumer c, CosmeticBox box, int light, int texels) {
		float x0 = box.x() * UNIT;
		float x1 = (box.x() + box.w()) * UNIT;
		float y0 = box.y() * UNIT;
		float y1 = (box.y() + box.h()) * UNIT;
		float z0 = box.z() * UNIT;
		float z1 = (box.z() + box.d()) * UNIT;
		// Texture sizes in whole pixels. The box offset in the texture is (ou, ov). A texture of fewer pixels than the
		// cosmetic texture, such as a vanilla parrot, is scaled up to the 64 pixels that face() divides by, so its UVs
		// come out right.
		float k = CosmeticTextures.SIZE / (float) texels;
		float w = Math.round(box.w()) * k;
		float h = Math.round(box.h()) * k;
		float d = Math.round(box.d()) * k;
		float ou = box.u() * k;
		float ov = box.v() * k;

		// Front (-z): the side the player looks at.
		face(pose, c, light, 0, 0, -1,
				x0, y0, z0, ou + d, ov + d,
				x1, y0, z0, ou + d + w, ov + d,
				x1, y1, z0, ou + d + w, ov + d + h,
				x0, y1, z0, ou + d, ov + d + h);
		// Back (+z): from the left edge at +x to the right edge at -x.
		face(pose, c, light, 0, 0, 1,
				x1, y0, z1, ou + 2 * d + w, ov + d,
				x0, y0, z1, ou + 2 * d + 2 * w, ov + d,
				x0, y1, z1, ou + 2 * d + 2 * w, ov + d + h,
				x1, y1, z1, ou + 2 * d + w, ov + d + h);
		// Right (-x): from the back edge to the front edge.
		face(pose, c, light, -1, 0, 0,
				x0, y0, z1, ou, ov + d,
				x0, y0, z0, ou + d, ov + d,
				x0, y1, z0, ou + d, ov + d + h,
				x0, y1, z1, ou, ov + d + h);
		// Left (+x): from the front edge to the back edge.
		face(pose, c, light, 1, 0, 0,
				x1, y0, z0, ou + d + w, ov + d,
				x1, y0, z1, ou + 2 * d + w, ov + d,
				x1, y1, z1, ou + 2 * d + w, ov + d + h,
				x1, y1, z0, ou + d + w, ov + d + h);
		// Top (-y): the back edge at the top of its texture.
		face(pose, c, light, 0, -1, 0,
				x0, y0, z1, ou + d, ov,
				x1, y0, z1, ou + d + w, ov,
				x1, y0, z0, ou + d + w, ov + d,
				x0, y0, z0, ou + d, ov + d);
		// Bottom (+y): the front edge at the top of its texture.
		face(pose, c, light, 0, 1, 0,
				x0, y1, z0, ou + d + w, ov,
				x1, y1, z0, ou + d + 2 * w, ov,
				x1, y1, z1, ou + d + 2 * w, ov + d,
				x0, y1, z1, ou + d + w, ov + d);
	}

	/**
	 * Emits one quad. The corners follow in order around the quad, and each one is x, y, z and the texture pixel
	 * it shows, five numbers per corner.
	 */
	private static void face(MatrixStack.Entry pose, VertexConsumer c, int light, float nx, float ny, float nz,
			float... corners) {
		for (int i = 0; i < corners.length; i += 5) {
			c.vertex(pose, corners[i], corners[i + 1], corners[i + 2])
					.color(WHITE)
					.texture(corners[i + 3] / CosmeticTextures.SIZE, corners[i + 4] / CosmeticTextures.SIZE)
					.overlay(OverlayTexture.DEFAULT_UV)
					.light(light)
					.normal(pose, nx, ny, nz);
		}
	}

	/** Render type of a box cosmetic, made on first use. Null when its texture could not be made. Render thread only. */
	private static RenderLayer renderTypeOf(Cosmetic cosmetic) {
		if (RENDER_TYPES.containsKey(cosmetic.id())) {
			return RENDER_TYPES.get(cosmetic.id());
		}
		RenderLayer type = null;
		// A pet with a vanilla texture, such as a parrot, is drawn with that texture. Its own texture is not made.
		Identifier id = cosmetic.texture() == null ? textureOf(cosmetic) : Identifier.tryParse(cosmetic.texture());
		if (id != null) {
			// Shields are translucent, and their back faces are culled, so each cell shows its front only.
			// Cutout without culling: both sides of a cape or a wing are visible.
			type = cosmetic.category() == CosmeticCategory.SHIELD
					? RenderLayers.itemEntityTranslucentCull(id)
					: RenderLayers.entityCutoutNoCull(id);
		}
		RENDER_TYPES.put(cosmetic.id(), type);
		return type;
	}

	/** The overlay of a cosmetic, made on first use. Null when its texture cannot be made. Render thread only. */
	private static Overlay overlayOf(Cosmetic cosmetic) {
		if (OVERLAYS.containsKey(cosmetic.id())) {
			return OVERLAYS.get(cosmetic.id());
		}
		if (OVERLAYS.size() >= MAX_OVERLAYS) {
			return null;
		}
		Identifier texture = textureOf(cosmetic);
		Overlay overlay = texture == null
				? null
				: new Overlay(texture, CosmeticTextures.isCoat(cosmetic), CosmeticTextures.flowsDown(cosmetic));
		OVERLAYS.put(cosmetic.id(), overlay);
		return overlay;
	}

	/** An overlay ready to draw: its texture, whether it is a coat, and its render type at each scroll step. */
	private static final class Overlay {
		private final Identifier texture;
		/** A coat is translucent, with its alpha as the opacity. Any other overlay is light, added to the skin. */
		final boolean coat;
		private final boolean flowsDown;
		/** Pace of the overlay before the player's setting: slime drips, so a flowing coat moves at half the glints' pace. */
		final float pace;
		/** Render types by scroll step. Scrolling needs one type per offset, made the first time it is drawn. */
		private final RenderLayer[] steps = new RenderLayer[SCROLL_STEPS];

		Overlay(Identifier texture, boolean coat, boolean flowsDown) {
			this.texture = texture;
			this.coat = coat;
			this.flowsDown = flowsDown;
			this.pace = flowsDown ? 0.5f : 1.0f;
		}

		/** The render type at a scroll step. The light scrolls across the body, and the venom flows down it. */
		RenderLayer at(int step) {
			RenderLayer type = steps[step];
			if (type == null) {
				float offset = step / (float) SCROLL_STEPS;
				float across = flowsDown ? 0.0f : offset;
				// A texture moves towards smaller coordinates when its offset grows, so a falling flow takes a negative one.
				float down = flowsDown ? -offset : 0.0f;
				type = coat
						? RenderLayers.breezeWind(texture, across, down)
						: RenderLayers.energySwirl(texture, across, down);
				steps[step] = type;
			}
			return type;
		}
	}

	/** Texture of a cosmetic, made and registered on first use. Null when it could not be made. Render thread only. */
	private static Identifier textureOf(Cosmetic cosmetic) {
		if (TEXTURES.containsKey(cosmetic.id())) {
			return TEXTURES.get(cosmetic.id());
		}
		Identifier id = Identifier.of(NAMESPACE, "cosmetic/" + cosmetic.id());
		Identifier result = null;
		try {
			TextureManager textures = MinecraftClient.getInstance().getTextureManager();
			textures.registerTexture(id, new CosmeticTexture(id, encode(CosmeticTextures.of(cosmetic))));
			result = id;
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Could not make the texture of cosmetic {}", cosmetic.id(), e);
		}
		TEXTURES.put(cosmetic.id(), result);
		return result;
	}

	/** An opaque grey of the given brightness, from 0 to 1. */
	private static int grey(float brightness) {
		int v = Math.max(0, Math.min(255, Math.round(brightness * 255)));
		return 0xFF000000 | (v << 16) | (v << 8) | v;
	}

	/** A white colour with the given opacity, from 0 to 1. */
	private static int alpha(float opacity) {
		int a = Math.max(0, Math.min(255, Math.round(opacity * 255)));
		return (a << 24) | 0xFFFFFF;
	}

	private static byte[] encode(BufferedImage image) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		if (!ImageIO.write(image, "png", bytes)) {
			throw new IOException("no PNG writer for the cosmetic texture");
		}
		return bytes.toByteArray();
	}

	/** Cosmetic texture kept as PNG bytes, read back by the texture manager like a pack texture. */
	private static final class CosmeticTexture extends ReloadableTexture {
		private final byte[] png;

		CosmeticTexture(Identifier id, byte[] png) {
			super(id);
			this.png = png;
		}

		@Override
		public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
			NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
			return new TextureContents(image, new TextureResourceMetadata(false, false, MipmapStrategy.AUTO, 0.1f));
		}
	}
}
