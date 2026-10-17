// HugoService Style presence server (Cloudflare Worker + KV).
// POST /heartbeat {"uuid": "..."}        -> marks a player as using the mod for 3 minutes
// POST /check     {"uuids": ["...", ...]} -> returns which of them use the mod
// Nothing but random UUIDs is stored, and entries expire on their own.

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
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
      await env.ONLINE.put(uuid, "1", { expirationTtl: TTL });
      return json({ ok: true });
    }

    if (url.pathname === "/check") {
      const list = Array.isArray(body.uuids) ? body.uuids.slice(0, MAX_CHECK) : [];
      const uuids = list.map((u) => String(u).toLowerCase()).filter((u) => UUID.test(u));
      const hits = await Promise.all(uuids.map((u) => env.ONLINE.get(u)));
      return json({ users: uuids.filter((_, i) => hits[i] !== null) });
    }

    return json({ error: "not found" }, 404);
  },
};
