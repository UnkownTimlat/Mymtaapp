<*
    --[[
        MTA:SA Live Map Server Resource - Inbound HTTP Endpoint Handler
        File: api.lua
        Author: MTA Live Map System
        Description:
          Executes inside MTA's built-in web server.
          Responds to GET/POST requests coming directly from the Android app or
          external HTTP clients.
          
          Supported queries via query string (?endpoint=...) or HTTP call:
            - ?endpoint=status        -> Returns ServerStatus JSON
            - ?endpoint=players       -> Returns List<MtaPlayer> JSON
            - ?endpoint=players&id=1  -> Returns MtaPlayer details JSON
            - ?endpoint=blips         -> Returns List<ServerBlip> JSON
            - ?endpoint=auth          -> Returns Auth status JSON
    --]]

    -- Set JSON HTTP response header
    httpSetResponseHeader("Content-Type", "application/json; charset=UTF-8")
    httpSetResponseHeader("Access-Control-Allow-Origin", "*")
    httpSetResponseHeader("Access-Control-Allow-Headers", "Authorization, Content-Type")
    httpSetResponseHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")

    -- Check Authentication
    local function isAuthorized()
        if not Config.Auth.requireAuth then
            return true
        end
        
        -- Check Bearer header or ?token= query param
        local authHeader = httpGetRequestHeader("Authorization") or ""
        local token = nil
        
        if authHeader:find("Bearer ") then
            token = authHeader:gsub("Bearer%s+", "")
        end
        
        if not token or token == "" then
            token = form["token"] or form["apiKey"]
        end
        
        if token and tostring(token) == tostring(Config.Auth.apiToken) then
            return true
        end
        
        return false
    end

    local endpoint = form["endpoint"] or form["action"] or "status"

    if not isAuthorized() then
        httpSetResponseCode(401)
        local errJson = toJSON({
            error = "Unauthorized",
            message = "Invalid or missing API authorization token. Provide 'Authorization: Bearer <token>' header or '?token=<token>'."
        })
        httpWrite(errJson)
        return
    end

    -- Route to appropriate data serializer
    if endpoint == "status" then
        local status = httpGetStatus()
        httpWrite(toJSON(status))
    elseif endpoint == "players" then
        if form["id"] then
            local p = httpGetPlayerDetails(form["id"])
            if p then
                httpWrite(toJSON(p))
            else
                httpSetResponseCode(404)
                httpWrite(toJSON({ error = "Player not found", id = form["id"] }))
            end
        else
            local playerList = httpGetPlayers()
            httpWrite(toJSON(playerList))
        end
    elseif endpoint == "blips" then
        local blips = httpGetBlips()
        httpWrite(toJSON(blips))
    elseif endpoint == "auth" then
        httpWrite(toJSON({ success = true, message = "Authenticated" }))
    else
        httpSetResponseCode(400)
        httpWrite(toJSON({ error = "Bad Request", message = "Unknown endpoint: " .. tostring(endpoint) }))
    end
*>
