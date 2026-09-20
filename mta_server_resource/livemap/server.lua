--[[
    MTA:SA Live Map Server Resource - Core Server Logic
    File: server.lua
    Author: MTA Live Map System
    Description:
      - Tracks all online players and their real-time telemetry:
        name, account ID, rank, X/Y/Z, interior, dimension, money,
        health, armor, playtime, session duration, vehicle name/health,
        heading/rotation, and speed.
      - Broadcasts player_join, player_quit, player_update, and server_status
        using MTA's native fetchRemote (to external WebSocket bridge/gateway)
        and responds to inbound HTTP queries via MTA's built-in web server.
      - Uses delta-based updates to eliminate spam and keep CPU usage negligible.
]]

local serverStartTime = getRealTime().timestamp
local playerSessions = {}    -- [playerElement] = { joinTime = timestamp, lastPos = {x, y, z}, lastVehicle = vehicleName }
local lastStatusPushTime = 0

-- ============================================================================
-- 1. UTILITY FUNCTIONS (Math, Formats, Vehicle Names, Ranks)
-- ============================================================================

-- Format float to 2 decimal places to minimize bandwidth and JSON payload size
local function round2(num)
    if not num then return 0 end
    return math.floor(num * 100 + 0.5) / 100
end

-- Calculate 3D speed in km/h from element velocity vectors
local function getElementSpeedKmh(element)
    if not isElement(element) then return 0 end
    local vx, vy, vz = getElementVelocity(element)
    if not vx then return 0 end
    -- Standard MTA velocity to km/h formula: speed * 180 (or * 111.847 for mph)
    local speed = (vx^2 + vy^2 + vz^2) ^ 0.5 * 180
    return round2(speed)
end

-- Resolve player rank from MTA ACL groups or ElementData
local function resolvePlayerRank(player)
    if not isElement(player) then return Config.Player.defaultRank end
    
    -- Check element data first if configured
    local customRank = getElementData(player, Config.Player.elementDataKeys.rank)
    if customRank and type(customRank) == "string" and #customRank > 0 then
        return customRank
    end
    
    if Config.Player.checkAclForRank then
        local account = getPlayerAccount(player)
        if account and not isGuestAccount(account) then
            local accountName = getAccountName(account)
            if isObjectInACLGroup("user." .. accountName, aclGetGroup("Admin")) then
                return "Admin"
            elseif isObjectInACLGroup("user." .. accountName, aclGetGroup("Moderator")) then
                return "Moderator"
            elseif isObjectInACLGroup("user." .. accountName, aclGetGroup("SuperModerator")) then
                return "SuperMod"
            end
        end
    end
    
    return Config.Player.defaultRank
end

-- Resolve account ID / Name
local function resolveAccountId(player)
    local account = getPlayerAccount(player)
    if account and not isGuestAccount(account) then
        local customId = getElementData(player, Config.Player.elementDataKeys.accountId)
        if customId then return tostring(customId) end
        return getAccountName(account)
    end
    return "guest_" .. tostring(getElementID(player) or getPlayerName(player))
end

-- Resolve total playtime in seconds
local function resolvePlayerPlaytime(player)
    -- Check common element data or account data
    local ptime = getElementData(player, Config.Player.elementDataKeys.playtime)
    if ptime and tonumber(ptime) then
        return tonumber(ptime)
    end
    
    local account = getPlayerAccount(player)
    if account and not isGuestAccount(account) then
        local accPlaytime = getAccountData(account, "playtime")
        if accPlaytime and tonumber(accPlaytime) then
            return tonumber(accPlaytime)
        end
    end
    
    -- Fallback to current session duration
    local session = playerSessions[player]
    if session and session.joinTime then
        return getRealTime().timestamp - session.joinTime
    end
    return 0
end

-- Calculate current online session duration in seconds
local function getSessionDuration(player)
    local session = playerSessions[player]
    if session and session.joinTime then
        return getRealTime().timestamp - session.joinTime
    end
    return 0
end

-- ============================================================================
-- 2. SERIALIZATION: Exact Match for Android App Data Models
-- ============================================================================

--[[
    Serializes a player element into a Lua table matching:
    MtaPlayer (com.example.data.model.MtaPlayer):
        id: Int
        name: String
        x: Float
        y: Float
        z: Float
        interior: Int
        dimension: Int
        money: Long
        health: Float
        armor: Float
        vehicle: String?
        vehicleHealth: Float?
        rank: String?
        playtime: Long?
        ping: Int?
        skin: Int?
        ip: String?
        heading: Float? (used for radar direction)
        speed: Float?
        sessionDuration: Long?
        accountId: String?
]]
function serializePlayer(player)
    if not isElement(player) or getElementType(player) ~= "player" then
        return nil
    end

    local x, y, z = getElementPosition(player)
    local interior = getElementInterior(player) or 0
    local dimension = getElementDimension(player) or 0
    local health = getElementHealth(player) or 100
    local armor = getPedArmor(player) or 0
    local money = getPlayerMoney(player) or 0
    local ping = getPlayerPing(player) or 0
    local skin = getElementModel(player) or 0
    
    local rotX, rotY, rotZ = getElementRotation(player)
    local heading = round2(rotZ or 0)
    
    -- Vehicle data
    local vehicle = getPedOccupiedVehicle(player)
    local vehicleName = nil
    local vehicleHealth = nil
    local speed = 0
    
    if vehicle and isElement(vehicle) then
        vehicleName = getVehicleName(vehicle)
        vehicleHealth = round2(getElementHealth(vehicle) or 1000)
        speed = getElementSpeedKmh(vehicle)
    else
        speed = getElementSpeedKmh(player)
    end
    
    -- Telemetry object
    local pData = {
        id = tonumber(getElementID(player)) or getPlayerPing(player) + math.random(100, 999), -- Stable ID or fallback
        name = getPlayerName(player),
        x = round2(x),
        y = round2(y),
        z = round2(z),
        interior = interior,
        dimension = dimension,
        money = money,
        health = round2(health),
        armor = round2(armor),
        vehicle = vehicleName,
        vehicleHealth = vehicleHealth,
        rank = resolvePlayerRank(player),
        playtime = resolvePlayerPlaytime(player),
        ping = ping,
        skin = skin,
        heading = heading,
        speed = speed,
        sessionDuration = getSessionDuration(player),
        accountId = resolveAccountId(player)
    }
    
    -- Use MTA player serial hash / index if element ID isn't an integer
    -- (Ensures clean numeric ID mapping for the Android client)
    local numericId = 0
    for idx, p in ipairs(getElementsByType("player")) do
        if p == player then
            numericId = idx
            break
        end
    end
    pData.id = numericId
    
    if Config.Sync.exposePlayerIp then
        pData.ip = getPlayerIP(player)
    end
    
    return pData
end

--[[
    Serializes server status matching:
    ServerStatus (com.example.data.model.ServerStatus):
        serverName: String
        serverIp: String?
        serverPort: Int
        gameType: String
        mapName: String
        playerCount: Int
        maxPlayers: Int
        version: String
        uptime: Long
        passwordProtected: Boolean
]]
function serializeServerStatus()
    local players = getElementsByType("player")
    local playerCount = #players
    local maxPlayers = getMaxPlayers()
    local currentUptime = getRealTime().timestamp - serverStartTime
    
    return {
        serverName = getServerName() or "MTA:SA Server",
        serverIp = getServerHttpPort and get("serverip") or nil,
        serverPort = getServerPort() or 22003,
        gameType = getGameType() or "Play",
        mapName = getMapName() or "San Andreas",
        playerCount = playerCount,
        maxPlayers = maxPlayers,
        version = getVersion().sortable or "1.6.0",
        uptime = currentUptime,
        passwordProtected = (getServerPassword() ~= nil and getServerPassword() ~= "")
    }
end

--[[
    Serializes all map blips matching:
    ServerBlip (com.example.data.model.ServerBlip):
        id: Int
        icon: Int
        x: Float
        y: Float
        z: Float
        name: String?
        color: String?
]]
function serializeBlips()
    local blipsList = {}
    local allBlips = getElementsByType("blip")
    
    for idx, blip in ipairs(allBlips) do
        -- Ignore blips attached to players (player blips are rendered directly from player coords)
        local attachedTo = getElementAttachedTo(blip)
        if not attachedTo or getElementType(attachedTo) ~= "player" then
            local x, y, z = getElementPosition(blip)
            local icon = getBlipIcon(blip) or 0
            local r, g, b, a = getBlipColor(blip)
            local hexColor = string.format("#%02X%02X%02X", r or 255, g or 255, b or 255)
            local name = getElementData(blip, "name") or getElementData(blip, "text") or ("Blip #" .. tostring(idx))
            
            table.insert(blipsList, {
                id = idx,
                icon = icon,
                x = round2(x),
                y = round2(y),
                z = round2(z),
                name = tostring(name),
                color = hexColor
            })
        end
    end
    
    return blipsList
end

-- ============================================================================
-- 3. NETWORK TRANSMISSION (External Bridge / Webhook)
-- ============================================================================

-- Outbound sender using MTA's native fetchRemote
local function sendToBridge(eventType, payloadTable)
    if not Config.Bridge.enabled or not Config.Bridge.url or Config.Bridge.url == "" then
        return
    end

    local wrapper = {
        type = eventType,
        timestamp = getRealTime().timestamp,
        token = Config.Bridge.apiKey
    }

    -- Merge event payload according to Android client expectations
    if eventType == "player_update" then
        wrapper.player = payloadTable
    elseif eventType == "player_join" then
        wrapper.player = payloadTable
    elseif eventType == "player_quit" then
        wrapper.id = payloadTable.id
        wrapper.name = payloadTable.name
        wrapper.reason = payloadTable.reason
    elseif eventType == "player_vehicle_update" then
        wrapper.id = payloadTable.id
        wrapper.vehicle = payloadTable.vehicle
        wrapper.vehicleHealth = payloadTable.vehicleHealth
        wrapper.speed = payloadTable.speed
    elseif eventType == "server_status" then
        wrapper.server = payloadTable
    elseif eventType == "server_blips" then
        wrapper.blips = payloadTable
    else
        wrapper.data = payloadTable
    end

    local jsonPayload = toJSON(wrapper)
    -- MTA's toJSON wraps arrays with brackets or root JSON. Remove MTA toJSON quirks if needed:
    if jsonPayload:sub(1, 1) == "[" and jsonPayload:sub(-1) == "]" then
        -- In some MTA versions toJSON wraps objects in [ ... ]
        -- Check if it's a single object:
        local inner = jsonPayload:sub(2, -2)
        if inner:sub(1, 1) == "{" and inner:sub(-1) == "}" then
            jsonPayload = inner
        end
    end

    local requestOptions = {
        headers = {
            ["Content-Type"] = "application/json",
            ["Authorization"] = "Bearer " .. tostring(Config.Bridge.apiKey)
        },
        postData = jsonPayload,
        connectTimeout = Config.Bridge.timeout or 8000
    }

    fetchRemote(Config.Bridge.url, requestOptions, function(responseData, responseInfo)
        -- responseInfo: 0 or 200 = Success, other = HTTP error
        if responseInfo and responseInfo.statusCode and responseInfo.statusCode >= 400 then
            outputDebugString(string.format("[LiveMap] Bridge returned HTTP %s for event '%s'", tostring(responseInfo.statusCode), eventType), 2)
        end
    end)
end

-- ============================================================================
-- 4. EVENT LISTENERS (Joins, Quits, Wasted, Vehicles)
-- ============================================================================

-- On Player Join
addEventHandler("onPlayerJoin", root, function()
    local player = source
    local now = getRealTime().timestamp
    playerSessions[player] = {
        joinTime = now,
        lastPos = { x = 0, y = 0, z = 0 },
        lastVehicle = nil
    }
    
    -- Wait 1 frame so spawn/position data initializes
    setTimer(function()
        if isElement(player) then
            local data = serializePlayer(player)
            if data then
                sendToBridge("player_join", data)
            end
        end
    end, 500, 1)
end)

-- On Player Quit
addEventHandler("onPlayerQuit", root, function(quitType, reason, responsibleElement)
    local player = source
    local playerId = 0
    for idx, p in ipairs(getElementsByType("player")) do
        if p == player then
            playerId = idx
            break
        end
    end

    local quitPayload = {
        id = playerId,
        name = getPlayerName(player),
        reason = reason or quitType or "Quit"
    }

    sendToBridge("player_quit", quitPayload)
    playerSessions[player] = nil
end)

-- On Vehicle Enter
addEventHandler("onPlayerVehicleEnter", root, function(vehicle, seat, jacked)
    local player = source
    local pData = serializePlayer(player)
    if pData then
        sendToBridge("player_vehicle_update", {
            id = pData.id,
            vehicle = pData.vehicle,
            vehicleHealth = pData.vehicleHealth,
            speed = pData.speed
        })
    end
end)

-- On Vehicle Exit
addEventHandler("onPlayerVehicleExit", root, function(vehicle, seat, jacker)
    local player = source
    local pData = serializePlayer(player)
    if pData then
        sendToBridge("player_vehicle_update", {
            id = pData.id,
            vehicle = nil,
            vehicleHealth = nil,
            speed = pData.speed
        })
    end
end)

-- ============================================================================
-- 5. HIGH-EFFICIENCY TELEMETRY LOOP (Delta-based Position Tracker)
-- ============================================================================

local function positionTelemetryTick()
    local minDelta = Config.Sync.minPositionDelta or 0.5
    local minDeltaSq = minDelta * minDelta

    for _, player in ipairs(getElementsByType("player")) do
        if isElement(player) then
            local x, y, z = getElementPosition(player)
            local session = playerSessions[player]
            if not session then
                session = {
                    joinTime = getRealTime().timestamp,
                    lastPos = { x = x, y = y, z = z },
                    lastVehicle = nil
                }
                playerSessions[player] = session
            end

            local last = session.lastPos
            local dx = x - last.x
            local dy = y - last.y
            local dz = z - last.z
            local distSq = (dx * dx) + (dy * dy) + (dz * dz)

            -- Check if position moved more than threshold, or interior/dimension changed
            local currentInterior = getElementInterior(player)
            local currentDimension = getElementDimension(player)
            local currentVehicle = getPedOccupiedVehicle(player)
            local currentVehicleName = currentVehicle and getVehicleName(currentVehicle) or nil

            local hasStateChanged = distSq >= minDeltaSq or
                                   session.lastInterior ~= currentInterior or
                                   session.lastDimension ~= currentDimension or
                                   session.lastVehicle ~= currentVehicleName

            if hasStateChanged then
                session.lastPos.x = x
                session.lastPos.y = y
                session.lastPos.z = z
                session.lastInterior = currentInterior
                session.lastDimension = currentDimension
                session.lastVehicle = currentVehicleName

                local serialized = serializePlayer(player)
                if serialized then
                    sendToBridge("player_update", serialized)
                end
            end
        end
    end

    -- Periodic Server Status Broadcast
    local now = getRealTime().timestamp
    if (now - lastStatusPushTime) >= ((Config.Sync.statusUpdateIntervalMs or 30000) / 1000) then
        lastStatusPushTime = now
        sendToBridge("server_status", serializeServerStatus())
    end
end

-- Start position ticker
setTimer(positionTelemetryTick, Config.Sync.positionUpdateIntervalMs or 1000, 0)

-- ============================================================================
-- 6. EXPORTED FUNCTIONS (Called by api.lua or other resources)
-- ============================================================================

function httpGetStatus()
    return serializeServerStatus()
end

function httpGetPlayers()
    local list = {}
    for _, player in ipairs(getElementsByType("player")) do
        local data = serializePlayer(player)
        if data then
            table.insert(list, data)
        end
    end
    return list
end

function httpGetPlayerDetails(id)
    local targetId = tonumber(id)
    for idx, player in ipairs(getElementsByType("player")) do
        if idx == targetId then
            return serializePlayer(player)
        end
    end
    return nil
end

function httpGetBlips()
    return serializeBlips()
end

function httpAuthenticate(token)
    if not Config.Auth.requireAuth then
        return { success = true, message = "Authentication not required" }
    end
    if token and tostring(token) == tostring(Config.Auth.apiToken) then
        return { success = true, message = "Authorized" }
    end
    return { success = false, message = "Invalid API token" }
end

-- ============================================================================
-- 7. RESOURCE LIFECYCLE
-- ============================================================================

addEventHandler("onResourceStart", resourceRoot, function()
    outputServerLog("[LiveMap] ======================================================")
    outputServerLog("[LiveMap] MTA Live Map Companion Resource v1.0.0 Loaded")
    outputServerLog(string.format("[LiveMap] HTTP Port: %s | Auth Required: %s", tostring(getServerHttpPort and getServerHttpPort() or "default"), tostring(Config.Auth.requireAuth)))
    outputServerLog(string.format("[LiveMap] Position Update Interval: %sms", tostring(Config.Sync.positionUpdateIntervalMs)))
    if Config.Bridge.enabled then
        outputServerLog("[LiveMap] External Bridge enabled -> " .. tostring(Config.Bridge.url))
    else
        outputServerLog("[LiveMap] Inbound MTA HTTP Server ready for direct Android connection.")
    end
    outputServerLog("[LiveMap] ======================================================")
    
    -- Initialize session trackers for already-connected players (e.g. upon /restart livemap)
    local now = getRealTime().timestamp
    for _, player in ipairs(getElementsByType("player")) do
        local x, y, z = getElementPosition(player)
        playerSessions[player] = {
            joinTime = now,
            lastPos = { x = x, y = y, z = z },
            lastVehicle = nil
        }
    end
end)

addEventHandler("onResourceStop", resourceRoot, function()
    outputServerLog("[LiveMap] MTA Live Map resource stopped.")
end)
