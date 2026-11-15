// The Contraption Diagram's ink pass: the lit blocks become a cyanotype drawing. A pixel is a line where
// the depth or the brightness jumps against a neighbour, a faint wash where there is a block, and
// nothing where there is not, so the page shows through.
uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform vec4 LineColor;
uniform vec4 WashColor;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float lum(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 here = texture(DiffuseSampler0, texCoord);
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    float edge = 0.0;
    vec2 taps[4] = vec2[](vec2(1, 0), vec2(-1, 0), vec2(0, 1), vec2(0, -1));
    for (int i = 0; i < 4; i++) {
        vec2 at = texCoord + taps[i] * oneTexel;
        vec4 other = texture(DiffuseSampler0, at);
        float d = texture(DiffuseDepthSampler, at).r;
        if (other.a < 0.5 && here.a >= 0.5) {
            edge = 1.0;
        } else if (here.a >= 0.5) {
            edge = max(edge, step(0.0006, abs(d - depth)));
            edge = max(edge, step(0.09, abs(lum(other.rgb) - lum(here.rgb))));
        }
    }
    if (here.a < 0.5) {
        fragColor = vec4(0.0);
        return;
    }
    float shade = 0.55 + 0.45 * lum(here.rgb);
    vec4 wash = vec4(WashColor.rgb * shade, WashColor.a);
    fragColor = mix(wash, LineColor, edge);
}
