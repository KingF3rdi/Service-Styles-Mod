// HugoService Style presence server (Cloudflare Worker + KV).
// POST /heartbeat {"uuid": "...", "loadout": "CAPE=id;HAT=id"} -> marks a player as using the mod for 3 minutes
// POST /check     {"uuids": ["...", ...]}                        -> which of them use the mod, plus their cosmetics
// Only random Minecraft UUIDs and cosmetic ids are stored, and entries expire on their own.

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const LOADOUT = /^[A-Za-z0-9_=;]{0,600}$/;
const TTL = 180;          // seconds a heartbeat keeps you "online"
const MAX_CHECK = 100;    // uuids per check request

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method !== "POST") return json({ name: "HugoService Style presence" });

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "bad json" }, 400);
    }

    if (url.pathname === "/heartbeat") {
      const uuid = String(body.uuid || "").toLowerCase();
      if (!UUID.test(uuid)) return json({ error: "bad uuid" }, 400);
      const loadout = body.loadout === undefined ? "" : String(body.loadout);
      if (!LOADOUT.test(loadout)) return json({ error: "bad loadout" }, 400);
      await env.ONLINE.put(uuid, "L:" + loadout, { expirationTtl: TTL });
      return json({ ok: true });
    }

    if (url.pathname === "/check") {
      const list = Array.isArray(body.uuids) ? body.uuids.slice(0, MAX_CHECK) : [];
      const uuids = list.map((u) => String(u).toLowerCase()).filter((u) => UUID.test(u));
      const hits = await Promise.all(uuids.map((u) => env.ONLINE.get(u)));
      const users = [];
      const loadouts = {};
      uuids.forEach((u, i) => {
        if (hits[i] === null) return;
        users.push(u);
        if (hits[i].startsWith("L:") && hits[i].length > 2) loadouts[u] = hits[i].slice(2);
      });
      return json({ users, loadouts });
    }

    return json({ error: "not found" }, 404);
  },
};
