# Veil Lights for TaCZ

A NeoForge 1.21.1 compatibility addon that renders configured TaCZ weapon lights through the separate [Veil Volume Lights](https://github.com/CappleApple/veil-volume-lights) library. Client installations need both `veiltaczlights` and `veilvolumelights`; installing only the TaCZ addon JAR on a server remains optional and enables datapack-profile synchronization. TaCZ integration code is under `com.cappleapple.veiltaczlights`, while generic rendering lives under `com.cappleapple.veilvolumelights` in the library artifact.

## What it does

- Captures the real animated TaCZ attachment bone matrix during rendering. Hipfire, ADS, reload, sprint, inspect, sway, recoil, custom animations, and per-gun attachment placement therefore flow through TaCZ's own transform hierarchy.
- Updates one reusable library `VolumeLight.Spot` per rendered player every frame.
- Supports first-person and third-person rendering, distance-culls remote lights, and removes stale lights at the end of the frame and on disconnect/world unload.
- Renders a colored, camera-facing lens flare at active third-person emitters. The flare is depth-tested, brightest while looking into the lamp, faint from the side, and never drawn over the first-person weapon view.
- Uses the library's native Veil point, spot, and rectangular area-light handles. TaCZ submits only spotlights.
- Preserves opaque-only world depth and applies continuous, subtractive transparent-medium transmission before stopping at opaque geometry.
- Automatically recognizes TaCZ `LASER` attachments whose models contain a flashlight-like emitter bone, with explicit third-party profiles available in TOML.
- Loads per-attachment beam profiles from datapack JSON, with bundled defaults for the standard PEQ-15, PEQ-6, and Nightstick attachments. Nightstick needs an explicit profile because TaCZ declares it only as a laser and supplies no flashlight marker.

Press **L** to toggle the local player's weapon light.

The library adds three independent test blocks to the Functional Blocks tab:

```text
/give @s veilvolumelights:test_point_light
/give @s veilvolumelights:test_spot_light
/give @s veilvolumelights:test_area_light
```

Point lights radiate in every direction. Spot and area lights point away from the placer; the area block emits from a 4-by-2 rectangular plane. All three use white light so colored glass, clear glass, water, attenuation, and overlapping sources can be compared without source-color contamination.

The client config includes `thirdPersonFlare`, `thirdPersonFlareSize`, and `thirdPersonFlareIntensity`. Flare distance uses the existing `thirdPersonLightDistance` limit.

## Configuration

NeoForge creates `config/veiltaczlights-client.toml` for global fallback behavior. Bounds are enforced for every numeric option.

Attachment profiles are datapack files at:

```text
data/<attachment namespace>/veiltaczlights/attachment_profiles/<attachment path>.json
```

For example, `data/addonpack/veiltaczlights/attachment_profiles/weapon_light.json` configures `addonpack:weapon_light`:

```json
{
  "enabled": true,
  "emitter_bone": "flashlight",
  "beam": {
    "length": 40.0,
    "width": 14.0,
    "inner_width": 7.0,
    "color": "#F2FAFF",
    "intensity": 1.2,
    "shadows": true,
    "volumetric_strength": 0.4
  }
}
```

`length` is the maximum distance in blocks. `width` and optional `inner_width` are full beam diameters in blocks at that distance, which the addon converts to Veil cone angles. `color` accepts `#RRGGBB` or a three-number RGB array. Datapack profiles take priority and reload with `/reload`.

In singleplayer, the integrated server loads and synchronizes these files automatically. For a dedicated multiplayer server, install the addon JAR on the server to synchronize server datapack profiles to clients. TaCZ and Veil are not required server-side, and clients can still join servers that do not install this addon; in that case only the client's legacy TOML fallback is available.

The addon still creates `config/veiltaczlights-attachments.toml` as a legacy/local fallback when no datapack profile exists for an attachment:

```toml
[attachments."addonpack:weapon_light"]
enabled = true
emitterBone = "flashlight"
range = 40.0
intensity = 1.2
outerConeAngle = 30.0
innerConeAngle = 16.0
red = 1.0
green = 0.97
blue = 0.90
shadows = true
volumetricStrength = 0.0
```

Set `emitterBone = "@attachment"` only for a pack whose attachment origin itself is authored at the lamp. Set `enabled = false` to override automatic recognition for an attachment.

The bundled JSON defaults use a clearly visible volumetric strength. Veil Volume Lights excludes transparent surfaces from the terminating depth snapshot, coalesces adjacent identical media, and integrates the actual distance traveled inside each volume. Colored media multiply their wavelength transmission along a light path, while separate light sources remain framebuffer-additive. The same behavior applies to point, spot, and area lights.

## Veil Volume Lights library

The reusable library is built from `veil-volume-lights/` and exposes three snapshot definitions plus a persistent handle:

```java
VolumeLightHandle handle = VeilVolumeLights.create(
        new VolumeLight.Point(position, color, intensity, range, shadows, volumetricStrength)
);

handle.update(new VolumeLight.Spot(
        position, forward, up, color, intensity, range,
        outerConeDegrees, innerConeDegrees, shadows, volumetricStrength
));

handle.free();
```

`VolumeLight.Area` additionally accepts emitter width, height, and spread angle. Creating or updating handles should happen on the client render thread; integrations own their handle lifecycle. The library owns all Veil shader overrides, framebuffer hooks, opaque-depth capture, transparent-medium scanning, and GPU upload.

## Integration details and honest API limits

The implementation targets the MUKSC TaCZ NeoForge 1.21.1 port `1.1.8-hotfix-r6` and Veil `4.4.1`.

TaCZ's public API exposes attachment ID/category and its renderer exposes the final animated model hierarchy, but this port has no flashlight state API or networked flashlight toggle. It renders laser attachments continuously. Consequently this addon's client rendering owns the local **L** toggle; remote recognized lights appear while their attachment is rendered. A future TaCZ state API can be connected in `compat/tacz` without changing Veil lifecycle code.

TaCZ also does not create competing dynamic/local illumination in the inspected 1.21.1 source, so no unrelated TaCZ rendering is disabled. Its laser beam remains intact.

TaCZ enables stencil rendering by upgrading Minecraft's main depth attachment to `DEPTH24_STENCIL8`. Veil Volume Lights centrally supplies the matching depth-stencil framebuffer overrides and first-person composite guard, so integrations do not each patch Veil independently.

Veil 4.4.1 exposes voxel occlusion rather than a per-light shadow-map resolution. `shadows` maps to that supported feature; no fake shadow-resolution option is presented.

The transform hook is intentionally narrow:

1. `BedrockAttachmentModel.render` establishes the attachment/gun/render-owner context.
2. `BedrockPart.translateAndRotateAndScale` captures the configured emitter after TaCZ applies the complete parent hierarchy.
3. The resulting camera-relative matrix is converted to a world position plus forward/up basis.
4. `VeilFlashlightManager` updates or removes a persistent Veil Volume Lights spotlight handle.

If an attachment lacks a recognized/configured emitter bone, it is skipped. Debug mode logs its resolved gun, attachment, bone, position, forward vector, and Veil handle state at most once per second per player.

## Development

Requires Java 21.

Clone with the Veil Volume Lights submodule:

```powershell
git clone --recurse-submodules https://github.com/CappleApple/veil-lights-for-tacz.git
```

```powershell
./gradlew.bat test build
./gradlew.bat runClient
```

The build produces two independent client artifacts:

```text
build/libs/veiltaczlights-1.0.2.jar
veil-volume-lights/build/libs/veilvolumelights-1.0.jar
```

Runtime verification needs both dependencies and a TaCZ gun pack. In-game transform checks should cover first-person hipfire/ADS/sprint/reload/inspect/recoil, third-person players, weapon switching, repeated toggles, and world transitions.
