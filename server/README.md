# HugoService presence server

Tiny Cloudflare Worker that lets HugoService Style show an icon next to the names of players who use the mod.

```bash
npx wrangler login
npx wrangler kv namespace create ONLINE   # put the id into wrangler.toml
npx wrangler deploy
```

Then set the printed `https://hugoservice-presence.<you>.workers.dev` URL in the mod (Extras → Name icon).
