# Banner Pack format (v1)

Banner packs are loaded from:

- `.minecraft/bannerpacks/`

Each pack is either:

- a directory (editable), or
- a `.zip` file (read-only).

## Root layout

Every pack must contain:

- `bannerpack.mcmeta`
- `banners/` directory

Example:

```text
my-pack/
  bannerpack.mcmeta
  banners/
    <namespace>/
      letter_a.json
      letter_b.json
```

## `bannerpack.mcmeta`

`bannerpack.mcmeta` is inspired by vanilla `pack.mcmeta` and uses format version `1`.

The pack id is derived from the directory or zip filename — it is not stored in the file.

Example (minimal):

```json
{
  "pack": {
    "pack_format": 1,
    "description": "Letters"
  }
}
```

Example (with optional metadata):

```json
{
  "pack": {
    "pack_format": 1,
    "description": "Letters and symbols"
  },
  "bannerpack": {
    "author": "magicus",
    "url": "https://example.com"
  }
}
```

Notes:

- `pack.pack_format` must be `1` for this version.
- `pack.description` is the display name shown in the UI.
- The `bannerpack` section is optional; omit it if there is no author or url.

## Banner design files

Each file under `banners/<namespace>/` is one banner design. The full design id is `<namespace>:<filename_without_ext>`.

Example (`banners/magicus/letter_a.json`):

```json
{
  "description": "Letter A",
  "author": "magicus",
  "url": "https://example.com/letters",
  "banner_color": "white",
  "layers": [
    {
      "pattern": "minecraft:small_stripes",
      "color": "blue"
    }
  ]
}
```

Optional fields (`author`, `url`) are omitted when empty.

## Special pack: `root`

The repository always ensures an editable `root` pack exists:

```text
.minecraft/bannerpacks/root/
  bannerpack.mcmeta
  banners/
    root/
```

`root` cannot be deleted.

## Give string conversion

The backend supports importing give commands in both the legacy and modern object formats:

- `/give ...`
- `give ...`
- `minecraft:<color>_banner`
- `<color>_banner`
- `/give @p {id:<color>_banner,components:{banner_patterns:[...],custom_name:"..."},count:...}`

Exported give strings use the modern object format:

- `/give @p {id:<color>_banner}`
- `/give @p {id:<color>_banner,components:{banner_patterns:[...]}}`
- `/give @p {id:<color>_banner,components:{banner_patterns:[...],custom_name:"..."}}`

`custom_name` is included when the banner has a non-default name.
