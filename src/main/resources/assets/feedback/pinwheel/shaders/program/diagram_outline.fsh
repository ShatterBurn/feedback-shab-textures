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
    if (here.a < 0.5) {
        fragColor = vec4(0.0);
        return;
    }
    float depth = texture(DiffuseDepthSampler, texCoord).r;
    float edge = 0.0;
    vec2 taps[4] = vec2[](vec2(1, 0), vec2(-1, 0), vec2(0, 1), vec2(0, -1));
    float[4] ds;
    for (int i = 0; i < 4; i++) {
        vec2 at = texCoord + taps[i] * oneTexel;
        vec4 other = texture(DiffuseSampler0, at);
        ds[i] = texture(DiffuseDepthSampler, at).r;
        // The silhouette, and a face that is lit differently from its neighbour.
        if (other.a < 0.5)
            edge = 1.0;
        else
            edge = max(edge, step(0.14, abs(lum(other.rgb) - lum(here.rgb))));
    }
    // A flat slope has a straight depth ramp (second difference zero); a crease or an edge does not.
    float bend = max(abs(ds[0] + ds[1] - 2.0 * depth), abs(ds[2] + ds[3] - 2.0 * depth));
    edge = max(edge, step(0.004, bend));
    float shade = 0.55 + 0.45 * lum(here.rgb);
    vec4 wash = vec4(WashColor.rgb * shade, WashColor.a);
    fragColor = mix(wash, LineColor, edge);
}
