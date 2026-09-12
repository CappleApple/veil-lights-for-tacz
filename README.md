# Veil Lights for TaCZ

Veil Lights for TaCZ adds proper Veil-powered weapon lights to TaCZ on NeoForge 1.21.1.

Instead of faking a flashlight as a simple texture or screen effect, the addon follows the actual animated attachment on the gun and feeds that transform into [Veil Volume Lights](https://github.com/CappleApple/veil-volume-lights). The beam therefore follows ADS, hipfire, reloads, recoil, sprinting, inspection animations, and third-person weapon movement.

Press **L** to toggle your weapon light.

## Requirements

Client:

- Minecraft 1.21.1
- NeoForge
- TaCZ
- Veil
- Veil Volume Lights

Installing this addon on a dedicated server is optional, but recommended for multiplayer. The server copy handles synchronized flashlight state and datapack-defined attachment profiles. TaCZ and Veil themselves do not need to be installed server-side for that synchronization.

## What it supports

- First-person and third-person flashlights
- Animated attachment transforms from TaCZ
- Remote-player flashlight state in multiplayer
- Configurable beam length, width, color, intensity, shadows, and volumetric strength
- Third-person lens flares
- Datapack profiles for addon weapon packs
- Automatic recognition of suitable TaCZ laser attachments when they expose a flashlight-style emitter bone
- Local TOML fallback profiles for attachments without datapack definitions

The addon only adds lighting. TaCZ's normal laser rendering is left alone.

## Attachment profiles

The preferred way to configure third-party attachments is with datapacks.

Profiles live at:

```text
data/<attachment namespace>/veiltaczlights/attachment_profiles/<attachment path>.json
```

Example:

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

`length` is measured in blocks. `width` and `inner_width` describe the beam diameter at that distance; the addon converts them to the cone angles Veil needs.

Profiles reload with `/reload` and take priority over the legacy local TOML configuration.

Bundled defaults are included for the standard PEQ-15, PEQ-6, and Nightstick attachments.

## Local fallback configuration

Client-wide settings are written to:

```text
config/veiltaczlights-client.toml
```

Attachment-specific fallback entries are written to:

```text
config/veiltaczlights-attachments.toml
```

Example:

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

Use `enabled = false` when an automatically detected attachment should not act as a flashlight.

## Multiplayer

With the addon installed on the server, pressing **L** sends the player's flashlight state through the server so other modded clients see the same result.

The server also distributes its datapack attachment profiles to clients. Without the server addon, local lights still work, but remote-state/profile behavior falls back to the client's local information.

## Third-person flare

Active third-person lights can draw a small lens flare at the lamp itself. It is strongest when looking toward the emitter and fades from the side.

The relevant client options are:

```text
thirdPersonFlare
thirdPersonFlareSize
thirdPersonFlareIntensity
thirdPersonLightDistance
```

The flare is not drawn over the first-person weapon view.

## How attachment tracking works

TaCZ already knows the final animated transform of every attachment. The addon reads the configured emitter bone after TaCZ has applied the gun, animation, and attachment hierarchy, then converts that transform into a world-space position and direction for the Veil spotlight.

That is why the light follows custom animations without needing a separate animation table in this mod.

If an attachment does not expose a usable emitter bone, give it an explicit profile.

## Veil Volume Lights

The generic light implementation lives in the separate Veil Volume Lights library. This addon only handles TaCZ-specific attachment discovery, transforms, profiles, and flashlight state.

The library supplies point, spot, and area lights; this addon currently uses spotlights.

## Target versions

The current integration targets the MUKSC TaCZ NeoForge 1.21.1 port (`1.1.8-hotfix-r6`) and Veil 4.4.1.

TaCZ does not currently expose a flashlight-state API in that port, which is why this addon owns the **L** toggle. If TaCZ exposes one later, the compatibility layer can use it without changing the lighting library.

## Building from source

Clone with the Veil Volume Lights submodule:

```bash
git clone --recurse-submodules https://github.com/CappleApple/veil-lights-for-tacz.git
```

Then build with:

```bash
./gradlew test build
```

Windows:

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
```

The build produces the TaCZ addon and the Veil Volume Lights library as separate jars.
