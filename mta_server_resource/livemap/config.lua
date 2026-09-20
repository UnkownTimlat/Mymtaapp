--[[
    MTA:SA Live Map Server Resource - Configuration
    Resource: livemap
    Author: MTA Live Map System
    Compatible with: MTA Live Map Android Client
]]

Config = {}

-- ============================================================================
-- 1. HTTP / REST BRIDGE CONFIGURATION (Outbound to external Relay/API)
-- ============================================================================
-- If you use an external bridge or webhook (e.g. Free Cloudflare Worker, Supabase Edge Function,
-- or Webhook proxy) to relay events to the Android WebSocket, configure it here:
Config.Bridge = {
    -- Set to true if you are forwarding live events via fetchRemote to an external endpoint
    enabled = false,
    
    -- Target URL of your bridge endpoint
    url = "https://your-livemap-bridge.workers.dev/api/mta-push",
    
    -- Secret API Key/Token used to authenticate outbound requests
    apiKey = "CHANGE_ME_SECRET_KEY_123456",
    
    -- Request timeout in milliseconds
    timeout = 8000
}

-- ============================================================================
-- 2. SECURITY & AUTHENTICATION (For Inbound MTA Built-in Web Server)
-- ============================================================================
-- When the Android App connects directly to MTA via MTA's built-in HTTP server:
-- URL format: http://YOUR_SERVER_IP:HTTP_PORT/livemap/call/...
Config.Auth = {
    -- Require secret token authentication for all data requests
    requireAuth = true,
    
    -- The shared secret token. Enter the SAME token in your Android App's Settings screen!
    apiToken = "SECURE_LIVE_MAP_TOKEN_2026",
    
    -- Allowed IP whitelist (leave empty {} to allow connections from any client IP that has the token)
    ipWhitelist = {}
}

-- ============================================================================
-- 3. TELEMETRY & SYNC FREQUENCY
-- ============================================================================
Config.Sync = {
    -- Interval in milliseconds for polling player coordinate changes (default: 1000ms = 1s)
    -- 1000ms provides smooth map tracking without straining MTA or your Optiklink server CPU
    positionUpdateIntervalMs = 1000,
    
    -- Minimum movement distance (in 3D GTA units) required to trigger a position update broadcast
    -- Ignores tiny idle jitter when players are standing still
    minPositionDelta = 0.5,
    
    -- Interval in milliseconds to sync full server status (player count, uptime, etc.)
    statusUpdateIntervalMs = 30000,
    
    -- Include player IP address in player details (set to false for privacy)
    exposePlayerIp = false
}

-- ============================================================================
-- 4. PLAYER DATA MAPPINGS & CUSTOM METRICS
-- ============================================================================
Config.Player = {
    -- Rank calculation: By default, checks MTA ACL groups (Admin, Moderator, etc.)
    -- or element data (e.g. "rank", "role", "group").
    checkAclForRank = true,
    
    -- Fallback rank name if not in any special group
    defaultRank = "Player",
    
    -- Custom Element Data keys (if your gamemode uses specific element data):
    elementDataKeys = {
        rank = "rank",
        money = "money",
        playtime = "playtime",
        accountId = "account:id"
    }
}
