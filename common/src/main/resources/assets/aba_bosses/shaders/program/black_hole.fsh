#version 150

// Black hole math adapted from ghostty-blackhole/blackhole.glsl.
// MIT License. Copyright (c) 2026 s13k <s13k@pm.me>.

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec2 OutSize;
uniform float BlackHoleActive;
uniform float BlackHoleCameraLocalX;
uniform float BlackHoleCameraLocalY;
uniform float BlackHoleCameraLocalZ;
uniform float BlackHoleCameraRightLocalX;
uniform float BlackHoleCameraRightLocalY;
uniform float BlackHoleCameraRightLocalZ;
uniform float BlackHoleCameraUpLocalX;
uniform float BlackHoleCameraUpLocalY;
uniform float BlackHoleCameraUpLocalZ;
uniform float BlackHoleCameraForwardLocalX;
uniform float BlackHoleCameraForwardLocalY;
uniform float BlackHoleCameraForwardLocalZ;
uniform float BlackHoleProjectionScaleX;
uniform float BlackHoleProjectionScaleY;
uniform float BlackHoleDepthA;
uniform float BlackHoleDepthB;
uniform float BlackHoleOcclusionRadius;
uniform float BlackHoleWorldScale;
uniform float BlackHoleTime;

in vec2 texCoord;

out vec4 fragColor;

const float STAR_GAIN = 0.0;

// Gargantua preset from the Ghostty tuner.
const float DISK_INNER = 2.2;
const float DISK_OUTER = 7.0;
const float DISK_GAIN = 1.4;
const float DISK_OPACITY = 0.85;
const float DISK_TEMP = 4500.0;
const float DOPPLER_MIX = 0.35;
const float DISK_BEAM = 2.0;
const float DISK_SPEED = 5.0;
const float DISK_WIND = 7.0;
const float DISK_CONTRAST = 0.5;
const float EXPOSURE = 1.2;

const float DILATION_MIN = 0.2;
const float VIEW_TANGENCY_FADE = 1.25;

#define N_STEPS 36

float hash21(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float vnoiseWrapY(vec2 p, float perY) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float y0 = mod(i.y, perY);
    float y1 = mod(i.y + 1.0, perY);
    return mix(
            mix(hash21(vec2(i.x, y0)), hash21(vec2(i.x + 1.0, y0)), f.x),
            mix(hash21(vec2(i.x, y1)), hash21(vec2(i.x + 1.0, y1)), f.x),
            f.y
    );
}

vec2 mirrorUV(vec2 u) {
    return 1.0 - abs(1.0 - mod(u, 2.0));
}

vec3 cameraLocal() {
    return vec3(BlackHoleCameraLocalX, BlackHoleCameraLocalY, BlackHoleCameraLocalZ);
}

vec3 cameraRightLocal() {
    return vec3(BlackHoleCameraRightLocalX, BlackHoleCameraRightLocalY, BlackHoleCameraRightLocalZ);
}

vec3 cameraUpLocal() {
    return vec3(BlackHoleCameraUpLocalX, BlackHoleCameraUpLocalY, BlackHoleCameraUpLocalZ);
}

vec3 cameraForwardLocal() {
    return vec3(BlackHoleCameraForwardLocalX, BlackHoleCameraForwardLocalY, BlackHoleCameraForwardLocalZ);
}

vec3 fragmentRayLocal(vec2 uv, vec3 right, vec3 up, vec3 forward) {
    vec2 ndc = uv * 2.0 - 1.0;
    vec3 ray = forward
            + right * (ndc.x / max(abs(BlackHoleProjectionScaleX), 1.0e-4))
            + up * (ndc.y / max(abs(BlackHoleProjectionScaleY), 1.0e-4));
    return normalize(ray);
}

vec2 rayToUv(vec3 ray, vec3 right, vec3 up, vec3 forward) {
    float z = max(dot(ray, forward), 1.0e-4);
    vec2 ndc = vec2(
            dot(ray, right) / z * BlackHoleProjectionScaleX,
            dot(ray, up) / z * BlackHoleProjectionScaleY
    );
    return ndc * 0.5 + 0.5;
}

float sceneForwardDistance(vec2 uv) {
    float rawDepth = texture(DepthSampler, uv).r;
    if (rawDepth >= 0.999999) {
        return 1.0e20;
    }

    float denom = rawDepth - BlackHoleDepthA;
    if (abs(denom) < 1.0e-6) {
        return 1.0e20;
    }

    return max(BlackHoleDepthB / denom, 0.0);
}

float blackHoleFrontDistance(vec3 cameraPosition, vec3 ray, vec3 forward) {
    float closestT = -dot(cameraPosition, ray);
    if (closestT <= 0.0) {
        return -1.0;
    }

    float cameraDistance2 = dot(cameraPosition, cameraPosition);
    float closestDistance2 = max(cameraDistance2 - closestT * closestT, 0.0);
    float radius = max(BlackHoleOcclusionRadius, 0.0);
    float radius2 = radius * radius;
    float forwardScale = max(dot(ray, forward), 1.0e-4);
    if (closestDistance2 <= radius2) {
        return max(closestT - sqrt(radius2 - closestDistance2), 0.0) * forwardScale;
    }

    return -1.0;
}

bool isOccludedByScene(vec2 uv, vec3 cameraPosition, vec3 ray, vec3 forward) {
    float blackHoleDistance = blackHoleFrontDistance(cameraPosition, ray, forward);
    if (blackHoleDistance <= 0.0) {
        return false;
    }

    float depthBias = max(0.08, BlackHoleOcclusionRadius * 0.03);
    return sceneForwardDistance(uv) < blackHoleDistance - depthBias;
}

vec3 blackbody(float T) {
    float t = clamp(T, 1500.0, 40000.0) / 100.0;
    float r = t <= 66.0 ? 1.0 : clamp(1.292936 * pow(t - 60.0, -0.1332047), 0.0, 1.0);
    float g = t <= 66.0 ? clamp(0.3900816 * log(t) - 0.6318414, 0.0, 1.0) : clamp(1.1298909 * pow(t - 60.0, -0.0755148), 0.0, 1.0);
    float b = t >= 66.0 ? 1.0 : (t <= 19.0 ? 0.0 : clamp(0.5432068 * log(t - 10.0) - 1.1962540, 0.0, 1.0));
    return vec3(r, g, b);
}

vec3 stars(vec3 d) {
    vec2 sph = vec2(atan(d.x, -d.z), asin(clamp(d.y, -1.0, 1.0)));
    vec2 g = sph * 40.0;
    vec2 id = floor(g);
    float h = hash21(id);
    if (h < 0.92) {
        return vec3(0.0);
    }

    vec2 f = fract(g) - 0.5;
    vec2 off = (vec2(hash21(id + 17.3), hash21(id + 31.7)) - 0.5) * 0.7;
    float spark = smoothstep(0.10, 0.0, length(f - off));
    float tw = 0.7 + 0.3 * sin(BlackHoleTime * (0.5 + 2.0 * hash21(id + 5.1)) + 40.0 * h);
    vec3 tint = mix(vec3(1.0, 0.82, 0.60), vec3(0.75, 0.85, 1.0), hash21(id + 2.9));
    return tint * spark * tw * ((h - 0.92) / 0.08);
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    if (BlackHoleActive < 0.5) {
        fragColor = original;
        return;
    }


    vec2 res = OutSize;
    vec2 uv = texCoord;

    float t = BlackHoleTime;
    float rin = max(DISK_INNER, 1.6);
    float rout = max(DISK_OUTER, rin + 0.5);
    float dil = mix(1.0, DILATION_MIN, 1.0);
    float shield = 1.0;

    float bmax = rout + 3.0;
    vec3 cameraRight = cameraRightLocal();
    vec3 cameraUp = cameraUpLocal();
    vec3 cameraForward = cameraForwardLocal();
    vec3 cameraPosition = cameraLocal();
    vec3 ray = fragmentRayLocal(uv, cameraRight, cameraUp, cameraForward);

    vec3 traceCameraPosition = cameraPosition / max(BlackHoleWorldScale, 1.0e-4);
    float cameraDistance = length(traceCameraPosition);
    float closestT = -dot(traceCameraPosition, ray);
    // Fade across tangent rays; a hard closestT cutoff creates screen-space half-plane edges.
    float viewWindow = smoothstep(-VIEW_TANGENCY_FADE, VIEW_TANGENCY_FADE, closestT);
    if (viewWindow < 0.0006) {
        fragColor = original;
        return;
    }

    vec3 closestApproach = traceCameraPosition + ray * closestT;
    vec3 angularMomentum = cross(traceCameraPosition, ray);
    float h2 = dot(angularMomentum, angularMomentum);
    float b = sqrt(max(h2, 0.0));
    float window = exp(-pow(max(b - bmax, 0.0) / max(bmax * 2.4, 1.0), 2.0)) * viewWindow;
    float windowCutoff = 0.0006 * clamp(720.0 / max(min(res.x, res.y), 1.0), 0.5, 2.0);
    if (b > bmax && window < windowCutoff) {
        fragColor = original;
        return;
    }

    if (isOccludedByScene(uv, cameraPosition, ray, cameraForward)) {
        fragColor = original;
        return;
    }

    if (b >= bmax) {
        vec3 bendDirection = normalize(closestApproach);
        float deflection = (2.0 / max(b, 1.0e-3)) * window * shield;
        vec3 sourceRay = normalize(ray - bendDirection * deflection);
        vec2 suv = mirrorUV(rayToUv(sourceRay, cameraRight, cameraUp, cameraForward));
        if (isOccludedByScene(suv, cameraPosition, fragmentRayLocal(suv, cameraRight, cameraUp, cameraForward), cameraForward)) {
            fragColor = original;
            return;
        }

        vec3 term = texture(DiffuseSampler, suv).rgb;
        fragColor = vec4(term + stars(sourceRay) * STAR_GAIN * window * shield, original.a);
        return;
    }

    vec3 x = traceCameraPosition;
    vec3 v = ray;

    vec3 n = vec3(0.0, 1.0, 0.0);
    vec3 e2 = vec3(0.0, 0.0, 1.0);
    float sdir = DISK_SPEED < 0.0 ? -1.0 : 1.0;
    float spd = abs(DISK_SPEED);

    vec3 emitc = vec3(0.0);
    float trans = 1.0;
    bool captured = false;
    float sPrev = dot(x, n);
    vec3 xPrev = x;
    float maxTraceRadius = max(cameraDistance + rout + 8.0, 20.0);
    float maxTraceRadius2 = maxTraceRadius * maxTraceRadius;
    float escapeRadius2 = max(cameraDistance * cameraDistance, maxTraceRadius2 * 0.35);

    for (int i = 0; i < N_STEPS; i++) {
        float r2 = dot(x, x);
        if (r2 < 1.0) {
            captured = true;
            break;
        }
        if (dot(x, v) > 0.0 && r2 > escapeRadius2) {
            break;
        }
        if (r2 > maxTraceRadius2) {
            break;
        }

        float r = sqrt(r2);
        float dt = clamp(0.18 * r, 0.04, 1.8);
        vec3 a = -1.5 * h2 * x / (r2 * r2 * r);
        v += a * (0.5 * dt);
        x += v * dt;
        r2 = dot(x, x);
        r = sqrt(r2);
        a = -1.5 * h2 * x / (r2 * r2 * r);
        v += a * (0.5 * dt);

        float s = dot(x, n);
        if (s * sPrev < 0.0 && trans > 0.02) {
            float tc = sPrev / (sPrev - s);
            vec3 xc = mix(xPrev, x, tc);
            float rc = length(xc);
            if (rc > rin && rc < rout) {
                float band = smoothstep(rin, rin * 1.25, rc)
                        * (1.0 - smoothstep(rout * 0.70, rout, rc));

                float phi = atan(dot(xc, e2), xc.x);
                float turns = phi / 6.2831853;
                float kep = pow(rin / rc, 1.5);
                float gloc = sqrt(max(1.0 - 1.5 / rc, 0.02));
                float swirl = rc * DISK_WIND * 0.12 - t * kep * spd * gloc * dil * sdir;
                float streaks = vnoiseWrapY(vec2(rc * 2.8, turns * 19.0 + swirl * 3.0), 19.0) * 0.65
                        + vnoiseWrapY(vec2(rc * 1.0, turns * 9.0 + swirl * 1.5 + 7.0), 9.0) * 0.35;
                streaks = 0.35 + DISK_CONTRAST * streaks * streaks;

                vec3 gasdir = normalize(cross(n, xc)) * sdir;
                float beta = clamp(inversesqrt(max(2.0 * (rc - 1.0), 0.2)), 0.0, 0.99);
                float g = gloc / max(1.0 + beta * dot(gasdir, normalize(v)), 0.05);
                g = mix(1.0, g, DOPPLER_MIX);

                float xpr = max(1.0 - sqrt(rin / rc), 0.0);
                float tprof = pow(rin / rc, 0.75) * pow(xpr, 0.25) / 0.488;
                vec3 cbb = blackbody(DISK_TEMP * tprof * g);
                float boost = pow(g, DISK_BEAM);

                float density = band * streaks;
                emitc += trans * cbb * (DISK_GAIN * 2.2 * density * tprof * tprof * boost);
                trans *= 1.0 - clamp(DISK_OPACITY * density, 0.0, 1.0);
            }
        }

        sPrev = s;
        xPrev = x;
    }

    if (!captured && dot(x, x) < 4.0) {
        captured = true;
    }

    vec3 bg = vec3(0.0);
    if (!captured) {
        vec3 d = normalize(v);
        bg += stars(d) * STAR_GAIN * window * shield;
        float forward = dot(d, cameraForward);
        if (forward > 0.02) {
            vec2 warpedUv = rayToUv(d, cameraRight, cameraUp, cameraForward);
            vec2 suv = mirrorUV(mix(uv, warpedUv, window * shield));
            float toward = smoothstep(0.02, 0.35, forward);
            vec3 sourceRay = fragmentRayLocal(suv, cameraRight, cameraUp, cameraForward);
            if (isOccludedByScene(suv, cameraPosition, sourceRay, cameraForward)) {
                bg += original.rgb * toward;
            } else {
                bg += texture(DiffuseSampler, suv).rgb * toward;
            }
        }
    }

    vec3 col = bg * trans + (vec3(1.0) - exp(-emitc * EXPOSURE));
    fragColor = vec4(mix(original.rgb, col, viewWindow), original.a);
}
