#version 150

// The fluid caster's flame: fast scrolling noise along the stream, mapped through the same temperature ramp as the plain flame
// (white-yellow core, orange body, red, soot rim). u runs across the strip (0.5 is its axis), v along it from the nozzle (0) to the tip (1).
// The vertex colour's red carries how hot the fuel burns (0 to 1) and its alpha the strip's own fade.
// A jet at the nozzle, tearing into licks and wisps toward the tip: the noise grows stronger and the whole body sways sideways with distance.

uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

vec3 ramp(float f, float hot) {
    vec3 core = vec3(1.0, mix(0.78, 1.0, hot), mix(0.25, 0.88, hot));
    if (f < 0.3) {
        return mix(core, vec3(1.0, 0.68, 0.18), f / 0.3);
    } else if (f < 0.65) {
        return mix(vec3(1.0, 0.68, 0.18), vec3(0.95, 0.38, 0.1), (f - 0.3) / 0.35);
    }
    return mix(vec3(0.95, 0.38, 0.1), vec3(0.5, 0.2, 0.12), min(1.0, (f - 0.65) / 0.35));
}

void main() {
    // GameTime is a day's fraction: 24000 ticks to the unit, 20 to the second.
    float t = GameTime * 1200.0;
    float along = texCoord0.y;
    // The body sways sideways with distance, slowly: tongues of flame peel off to one side then the other.
    float sway = (noise(vec2(along * 3.0 - t * 2.5, 3.7)) - 0.5) * along * 1.1;
    float across = abs(texCoord0.x - 0.5 + sway) * 2.0;
    // Streaks: stretched along the stream and racing down it, a fast flicker over a broad roll.
    vec2 p = vec2(texCoord0.x * 2.5, along * 3.0 - t * 9.0);
    float n = 0.55 * noise(p) + 0.45 * noise(p * vec2(2.6, 2.2) + vec2(7.1, -t * 6.0));
    // Noise gets stronger toward the tip, which opens gaps and frays the edge into wisps; the edge also closes in a little less.
    float heat = clamp(1.0 - across * (1.0 + along * 0.4) - along * 0.7 + 0.12 + (n - 0.5) * (0.7 + along * 1.3), 0.0, 1.0);
    float alpha = smoothstep(0.0, 0.3, heat) * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    fragColor = vec4(ramp(1.0 - heat, vertexColor.r), alpha);
}
