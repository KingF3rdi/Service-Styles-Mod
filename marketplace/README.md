# Marketplace

Shared mixes for 7auction Styles. The in-game **Marketplace** page (sidebar, under
Crosshair) lists everything in [`index.json`](index.json). **Install** downloads the mix into the player's packs
folder and puts it on at once; after that it is also in **Mixes**.

## Share your mix

1. In the menu, name your mix in the box at the bottom, then press **Export**. That saves a `.zip`.
2. Send the `.zip` (and a screenshot if you like) in the Discord, or open a GitHub issue here with the file attached.

## Add a mix (repo owner)

1. Pick an id: lowercase letters, digits and `_`, 2–40 characters, for example `purple_pvp`.
2. Put the exported pack here as `<id>.zip`. Optional: a square preview as `<id>.png` (under 2 MB).
3. Get its size and SHA-256 in PowerShell:

   ```powershell
   $f = "marketplace\purple_pvp.zip"; (Get-Item $f).Length; (Get-FileHash $f -Algorithm SHA256).Hash.ToLower()
   ```

4. Add an entry to `presets` in `index.json`, then commit and push:

   ```json
   { "id": "purple_pvp", "name": "Purple PvP", "author": "Someone", "description": "Purple crystals, dark obsidian",
     "size": 123456, "sha256": "<the hash>" }
   ```

The game checks the size and hash, so a changed zip needs a new entry (size and hash), or players get
"file does not match the marketplace list". An entry with a bad id, hash or size is skipped; the rest still show.

To try a catalog before pushing it, serve this folder (for example `python -m http.server 8765` inside it) and
start the game with `-Dhugoservice.market=http://127.0.0.1:8765/`.
