#version 150

// The fluid caster's flame: scrolling two-octave noise along the stream, mapped through the same temperature ramp as the plain flame
// (white-yellow core, orange body, deep red, soot rim). u runs across the strip (0.5 is its axis), v along it from the nozzle (0) to the tip (1).
// The vertex colour's red carries how hot the fuel burns (0 to 1) and its alpha the strip's own fade.

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
        return mix(core, vec3(1.0, 0.52, 0.1), f / 0.3);
    } else if (f < 0.65) {
        return mix(vec3(1.0, 0.52, 0.1), vec3(0.75, 0.12, 0.04), (f - 0.3) / 0.35);
    }
    return mix(vec3(0.75, 0.12, 0.04), vec3(0.12, 0.1, 0.1), min(1.0, (f - 0.65) / 0.35));
}

void main() {
    // GameTime is a day's fraction: 24000 ticks to the unit, 20 to the second.
    float t = GameTime * 1200.0;
    float across = abs(texCoord0.x - 0.5) * 2.0;
    float along = texCoord0.y;
    // Noise rising along the stream and drifting with it, two octaves: a slow billow and a quick flicker.
    vec2 p = vec2(texCoord0.x * 3.0, along * 5.0 - t * 3.0);
    float n = 0.6 * noise(p) + 0.4 * noise(p * 2.3 + vec2(7.1, -t * 5.0));
    float heat = clamp(1.0 - across * 1.05 - along * 0.8 + (n - 0.5) * 0.9, 0.0, 1.0);
    float alpha = smoothstep(0.0, 0.35, heat) * vertexColor.a;
    if (alpha < 0.01) {
        discard;
    }
    fragColor = vec4(ramp(1.0 - heat, vertexColor.r), alpha);
}
