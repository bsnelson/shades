/* eslint-disable */
var http = require("http");
var https = require("https");
var url = require("url");
var fs = require("fs");
var path = require("path");
var mqtt = require("mqtt");

function getEnv(name, fallback) {
  return process.env[name] || fallback;
}

var PORT = parseInt(getEnv("PORT", "8081"), 10);
var SOMA_CONNECT_IP = getEnv("SOMA_CONNECT_IP", "");
var SUNSA_BASE_URL = getEnv("SUNSA_BASE_URL", "");
var SUNSA_API_KEY = getEnv("SUNSA_API_KEY", "");
var SUNSA_ID_USER = getEnv("SUNSA_ID_USER", "");
var MQTT_BROKER_URL = getEnv("MQTT_BROKER_URL", "");
var MQTT_USERNAME = getEnv("MQTT_USERNAME", "");
var MQTT_PASSWORD = getEnv("MQTT_PASSWORD", "");
var RETRIES = parseInt(getEnv("RETRIES", "2"), 10);

if (!SOMA_CONNECT_IP || !SUNSA_BASE_URL || !SUNSA_API_KEY || !SUNSA_ID_USER || !MQTT_BROKER_URL || !MQTT_USERNAME || !MQTT_PASSWORD) {
  console.error("Missing required downstream config in environment (SOMA_CONNECT_IP, SUNSA_BASE_URL, SUNSA_API_KEY, SUNSA_ID_USER, MQTT_BROKER_URL, MQTT_USERNAME, MQTT_PASSWORD).");
  process.exit(1);
}

var deviceConfig = JSON.parse(fs.readFileSync(path.join(__dirname, "devices.json"), "utf8"));
var DEVICES = deviceConfig.devices;
var SOMA_PATHS = deviceConfig.api.soma;
var SUNSA_PATHS = deviceConfig.api.sunsa;
var ZIGBEE_BASE_TOPIC = deviceConfig.api.zigbee.baseTopic;

// ---- generic helpers ----

function sleep(ms) {
  return new Promise(function (resolve) { setTimeout(resolve, ms); });
}

function deviceByName(name) {
  return DEVICES.find(function (d) { return d.name === name; }) || null;
}

// Fills {placeholder} tokens by name, URI-encoding each value.
function fillTemplate(tmpl, params) {
  return tmpl.replace(/\{(\w+)\}/g, function (_, key) {
    return encodeURIComponent(params[key]);
  });
}

// Runs fn(device) over every device in parallel. A single device's network/HTTP
// failure never rejects the whole batch - it's turned into an { result: "error" }
// entry so callers (especially the retry loop) see it as a normal failed result.
function fanOut(devices, fn) {
  return Promise.all(devices.map(function (device) {
    return Promise.resolve().then(function () { return fn(device); }).catch(function (err) {
      return { result: "error", name: device.name, error: String((err && err.message) || err) };
    });
  }));
}

function requestJson(method, targetUrl, body) {
  return new Promise(function (resolve, reject) {
    var u = url.parse(targetUrl);
    var isHttps = u.protocol === "https:";
    var mod = isHttps ? https : http;
    var bodyStr = (body !== undefined && body !== null) ? JSON.stringify(body) : null;

    var headers = { "Accept": "application/json" };
    if (bodyStr !== null) {
      headers["Content-Type"] = "application/json";
      headers["Content-Length"] = Buffer.byteLength(bodyStr);
    }

    var req = mod.request({
      protocol: u.protocol,
      hostname: u.hostname,
      port: u.port || (isHttps ? 443 : 80),
      path: u.path,
      method: method,
      headers: headers
    }, function (res) {
      var chunks = [];
      res.on("data", function (c) { chunks.push(c); });
      res.on("end", function () {
        var txt = Buffer.concat(chunks).toString("utf8");
        var json = null;
        try { json = txt ? JSON.parse(txt) : null; } catch (e) { json = null; }
        resolve({ statusCode: res.statusCode, bodyText: txt, json: json });
      });
    });

    req.on("error", function (err) { reject(err); });
    req.setTimeout(15000, function () {
      req.destroy(new Error("timeout"));
    });
    if (bodyStr !== null) req.write(bodyStr);
    req.end();
  });
}

// ---- Soma client ----

function somaUrl(pathTemplate, params) {
  return SOMA_CONNECT_IP + fillTemplate(pathTemplate, params || {});
}

function somaGetDeviceList() {
  return requestJson("GET", somaUrl(SOMA_PATHS.listDevices, {})).then(function (r) {
    return r.json || { result: "error", shades: [] };
  });
}

function somaGetShadeState(device) {
  return requestJson("GET", somaUrl(SOMA_PATHS.getShadeState, { id: device.id })).then(function (r) {
    if (!r.json) return { result: "error", name: device.name, mac: device.id };
    var resp = r.json;
    resp.name = device.name;
    return resp;
  });
}

function somaGetBatteryState(device) {
  return requestJson("GET", somaUrl(SOMA_PATHS.getBatteryLevel, { id: device.id })).then(function (r) {
    if (!r.json) return { result: "error", name: device.name };
    return { name: device.name, battery_percentage: r.json.battery_percentage };
  });
}

function somaSetShadePosition(device, position) {
  var target = somaUrl(SOMA_PATHS.setShadePosition, { id: device.id, position: position }) + "?close_upwards=" + device.closeUpward;
  return requestJson("GET", target).then(function (r) {
    if (!r.json) return { result: "error", name: device.name, mac: device.id };
    var resp = r.json;
    resp.name = device.name;
    return resp;
  });
}

// ---- Sunsa client ----
// The device list is cached in memory (evicted on any position change, and on
// an hourly timer below) so /getStates and /getBattery don't hammer the Sunsa
// API once per device on every call.

var sunsaListCache = null;

function sunsaInvalidateCache() {
  sunsaListCache = null;
}
setInterval(sunsaInvalidateCache, 60 * 60 * 1000);

function sunsaGetDeviceList() {
  if (sunsaListCache) return Promise.resolve(sunsaListCache);
  var target = SUNSA_BASE_URL + fillTemplate(SUNSA_PATHS.listDevices, { idUser: SUNSA_ID_USER, apiKey: SUNSA_API_KEY });
  return requestJson("GET", target).then(function (r) {
    sunsaListCache = r.json || { devices: [], deviceCount: 0 };
    return sunsaListCache;
  });
}

function sunsaGetShadeState(device) {
  return sunsaGetDeviceList().then(function (list) {
    var match = (list.devices || []).find(function (d) { return String(d.idDevice) === String(device.id); });
    return match || null;
  });
}

function sunsaGetBatteryState(device) {
  return sunsaGetShadeState(device).then(function (state) {
    if (!state) return { result: "error", name: device.name };
    return { name: device.name, battery_percentage: parseInt(state.batteryPercentage, 10) };
  });
}

function sunsaSetShadePosition(device, position) {
  var target = SUNSA_BASE_URL + fillTemplate(SUNSA_PATHS.setShadePosition, { idUser: SUNSA_ID_USER, idDevice: device.id, apiKey: SUNSA_API_KEY });
  // Sunsa uses negative positions for closing upward.
  var body = { Position: "-" + position };
  return requestJson("PUT", target, body).then(function (r) {
    sunsaInvalidateCache();
    if (!r.json) return { result: "error", device: { idDevice: device.id, name: device.name } };
    return r.json;
  });
}

// ---- Zigbee (Zigbee2MQTT) client ----
// Zigbee2MQTT publishes device state as retained MQTT messages, so subscribing
// on startup immediately backfills each device's last-known state - there's no
// synchronous "get" round-trip the way Soma/Sunsa's HTTP APIs have, so reads
// below are served straight from this cache.

var zigbeeStateCache = {};
var zigbeeSetWaiters = {}; // device.id -> [{ targetPosition, timer, resolve }]
var zigbeeDeviceIds = DEVICES.filter(function (d) { return d.type === "zigbee"; }).map(function (d) { return d.id; });

var mqttClient = mqtt.connect(MQTT_BROKER_URL, {
  username: MQTT_USERNAME,
  password: MQTT_PASSWORD,
  reconnectPeriod: 5000
});

mqttClient.on("connect", function () {
  mqttClient.subscribe(ZIGBEE_BASE_TOPIC + "/+");
});

mqttClient.on("error", function (err) {
  console.error("MQTT error:", err);
});

mqttClient.on("message", function (topic, payload) {
  var id = topic.slice(ZIGBEE_BASE_TOPIC.length + 1);
  if (zigbeeDeviceIds.indexOf(id) === -1) return; // ignores bridge/other-device topics on the same broker

  var parsed;
  try { parsed = JSON.parse(payload.toString("utf8")); } catch (e) { return; }
  zigbeeStateCache[id] = parsed;

  var waiters = zigbeeSetWaiters[id];
  if (!waiters || !waiters.length || parsed.position === undefined) return;
  zigbeeSetWaiters[id] = waiters.filter(function (w) {
    if (parsed.position !== w.targetPosition) return true;
    clearTimeout(w.timer);
    w.resolve(parsed);
    return false;
  });
});

function zigbeeGetShadeState(device) {
  var cached = zigbeeStateCache[device.id];
  if (!cached) return Promise.resolve(null);
  var resp = Object.assign({}, cached);
  resp.name = device.name;
  return Promise.resolve(resp);
}

function zigbeeGetBatteryState(device) {
  var cached = zigbeeStateCache[device.id];
  if (!cached || cached.battery === undefined) return Promise.resolve({ result: "error", name: device.name });
  return Promise.resolve({ name: device.name, battery_percentage: cached.battery });
}

// Publishes the target position and waits (up to 15s) for Zigbee2MQTT to report
// the shade actually reaching it, so durablePosition's retry logic can tell a
// real failure (jammed motor, radio drop) from a normal successful move, the
// same guarantee Soma/Sunsa already get from their synchronous HTTP responses.
// Z2M's own position convention (0=closed/100=open) already matches this app's
// convention (confirmed empirically - low numbers are "close to closed upward"
// for Soma/Sunsa too), so position passes straight through, same as Soma.
function zigbeeSetShadePosition(device, position) {
  var targetPosition = parseInt(position, 10);

  return new Promise(function (resolve) {
    var timer = setTimeout(function () {
      zigbeeSetWaiters[device.id] = (zigbeeSetWaiters[device.id] || []).filter(function (w) { return w.timer !== timer; });
      resolve({ result: "error", name: device.name });
    }, 15000);

    if (!zigbeeSetWaiters[device.id]) zigbeeSetWaiters[device.id] = [];
    zigbeeSetWaiters[device.id].push({
      targetPosition: targetPosition,
      timer: timer,
      resolve: function (state) {
        resolve({ result: "success", name: device.name, position: state.position });
      }
    });

    mqttClient.publish(ZIGBEE_BASE_TOPIC + "/" + device.id + "/set", JSON.stringify({ position: targetPosition }));
  });
}

function zigbeeGetDeviceList() {
  return DEVICES.filter(function (d) { return d.type === "zigbee"; }).map(function (d) {
    var cached = zigbeeStateCache[d.id];
    var resp = cached ? Object.assign({}, cached) : {};
    resp.name = d.name;
    return resp;
  });
}

function dispatchGetState(device) {
  if (device.type === "sunsa") return sunsaGetShadeState(device);
  if (device.type === "soma") return somaGetShadeState(device);
  if (device.type === "zigbee") return zigbeeGetShadeState(device);
  return Promise.resolve(null);
}

function dispatchGetBattery(device) {
  if (device.type === "sunsa") return sunsaGetBatteryState(device);
  if (device.type === "soma") return somaGetBatteryState(device);
  if (device.type === "zigbee") return zigbeeGetBatteryState(device);
  return Promise.resolve(null);
}

function dispatchSetPosition(device, position) {
  if (device.type === "sunsa") return sunsaSetShadePosition(device, position);
  if (device.type === "soma") return somaSetShadePosition(device, position);
  if (device.type === "zigbee") return zigbeeSetShadePosition(device, position);
  return Promise.resolve(null);
}

function splitByType(devices, results) {
  var sunsaDevices = [];
  var somaDevices = [];
  var zigbeeDevices = [];
  devices.forEach(function (device, i) {
    var r = results[i];
    if (!r) return;
    if (device.type === "sunsa") sunsaDevices.push(r);
    else if (device.type === "soma") somaDevices.push(r);
    else if (device.type === "zigbee") zigbeeDevices.push(r);
  });
  return { sunsaDevices: sunsaDevices, somaDevices: somaDevices, zigbeeDevices: zigbeeDevices };
}

// ---- service ----

function listDevices() {
  return Promise.all([somaGetDeviceList(), sunsaGetDeviceList()]).then(function (r) {
    return { sunsaDevices: r[1].devices, somaDevices: r[0].shades, zigbeeDevices: zigbeeGetDeviceList() };
  });
}

function listNames() {
  var names = DEVICES.map(function (d) { return d.name; });
  var groupsSeen = {};
  var groups = [];
  DEVICES.forEach(function (d) {
    (d.groups || []).forEach(function (g) {
      if (!groupsSeen[g]) { groupsSeen[g] = true; groups.push(g); }
    });
  });
  return { names: names, groups: groups };
}

function getStates() {
  return fanOut(DEVICES, dispatchGetState).then(function (results) {
    return splitByType(DEVICES, results);
  });
}

function getBattery() {
  return fanOut(DEVICES, dispatchGetBattery).then(function (results) {
    return { devices: results.filter(Boolean) };
  });
}

function setPosition(position, group, name) {
  var filtered = DEVICES.filter(function (d) {
    return (!group || (d.groups || []).indexOf(group) !== -1) && (!name || d.name === name);
  });
  return fanOut(filtered, function (device) { return dispatchSetPosition(device, position); }).then(function (results) {
    return splitByType(filtered, results);
  });
}

// "Next highest multiple of ten" of the reported position - Sunsa's real API rounds
// position to steps of 10, so this is how a set-position call is judged successful.
function isNextHighestMultipleOfTen(positionStr, reportedStr) {
  var pos = parseInt(positionStr, 10);
  var reported = parseInt(reportedStr, 10);
  if (!isFinite(pos) || !isFinite(reported)) return false;
  var nextMultipleOfTen = Math.trunc((pos + 9) / 10) * 10;
  return reported === nextMultipleOfTen || reported === -nextMultipleOfTen;
}

// Retries a reopen/reclose across all devices until everyone succeeds or RETRIES
// is exhausted, backing off 1s * attempt number between passes. Only devices that
// failed the previous pass are retargeted on the next one.
function durablePosition(useSeasonal, position) {
  var retriable = RETRIES;
  var state = { result: "error", retries: 0, failedDevices: DEVICES.map(function (d) { return d.name; }) };

  function attempt() {
    var targets = state.failedDevices.map(deviceByName).filter(Boolean);

    return fanOut(targets, function (device) {
      return dispatchSetPosition(device, useSeasonal ? device.seasonalDefault : position);
    }).then(function (responses) {
      var failedNames = [];

      targets.forEach(function (device, i) {
        var r = responses[i];
        if (device.type === "soma") {
          if (!r || r.result === "error") failedNames.push(device.name);
        } else if (device.type === "sunsa") {
          var wantedPos = useSeasonal ? device.seasonalDefault : position;
          var reportedPos = r && r.device ? r.device.position : null;
          if (!r || !r.device || !isNextHighestMultipleOfTen(wantedPos, reportedPos)) {
            failedNames.push(device.name);
          }
        } else if (device.type === "zigbee") {
          if (!r || r.result === "error") failedNames.push(device.name);
        }
      });

      if (failedNames.length === 0) {
        state = { result: "success", retries: state.retries, failedDevices: null };
        return state;
      }

      state = { result: "error", retries: state.retries, failedDevices: failedNames };
      if (--retriable >= 0) {
        state.retries += 1;
        return sleep(1000 * state.retries).then(attempt);
      }
      return state;
    });
  }

  return attempt();
}

function reopen() {
  return durablePosition(true, "");
}

function reclose() {
  return durablePosition(false, "100");
}

// ---- HTTP server ----

function setCors(res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Headers", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
}

function sendJson(res, statusCode, obj) {
  setCors(res);
  res.statusCode = statusCode;
  res.setHeader("Content-Type", "application/json");
  res.end(JSON.stringify(obj));
}

function handle(res, resultPromise) {
  Promise.resolve(resultPromise).then(function (result) {
    sendJson(res, 200, result);
  }).catch(function (err) {
    console.error(err);
    sendJson(res, 500, { error: String((err && err.message) || err) });
  });
}

// Logs one line per request on completion (method, path, status, duration,
// remote address) to stdout - journald captures and rotates it for us since
// this runs under systemd, so no file handling needed here.
function logAccess(req, res, startTime) {
  res.on("finish", function () {
    var ms = Date.now() - startTime;
    var addr = (req.socket && req.socket.remoteAddress) || "-";
    console.log(new Date().toISOString() + " " + addr + " " + req.method + " " + req.url + " " + res.statusCode + " " + ms + "ms");
  });
}

var server = http.createServer(function (req, res) {
  logAccess(req, res, Date.now());

  if (req.method === "OPTIONS") {
    setCors(res);
    res.statusCode = 204;
    return res.end();
  }

  if (req.method !== "GET") {
    return sendJson(res, 404, { error: "Not found" });
  }

  var parsed = url.parse(req.url, true);
  var pathname = parsed.pathname;

  if (pathname === "/listDevices") return handle(res, listDevices());
  if (pathname === "/listNames") return handle(res, listNames());
  if (pathname === "/getStates") return handle(res, getStates());
  if (pathname === "/getBattery") return handle(res, getBattery());
  if (pathname === "/reopen") return handle(res, reopen());
  if (pathname === "/reclose") return handle(res, reclose());

  var setPositionMatch = pathname.match(/^\/setPosition\/([^\/]+)$/);
  if (setPositionMatch) {
    var position = decodeURIComponent(setPositionMatch[1]);
    return handle(res, setPosition(position, parsed.query.group, parsed.query.name));
  }

  sendJson(res, 404, { error: "Not found" });
});

server.listen(PORT, "0.0.0.0", function () {
  console.log("shades server listening on 0.0.0.0:" + PORT);
});
