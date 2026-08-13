#version 300 es
#ifdef GL_ES
precision highp float; // mediump can cause banding/jitter on large world coords
#endif

in vec2 v_worldPos;
uniform sampler2D u_texture;
uniform float zoom;
out vec4 fragColor;

const float baseCellSize = 20.0;

const float lineThickness = 2.0;

const vec4 gridXColor = vec4(0.851, 0.239, 0.337, 1.0);
const vec4 gridYColor = vec4(0.435, 0.671, 0.169, 1.0);

const vec4 primaryGridColor = vec4(0.247, 0.247, 0.247, 0.5);
const int devisions = 5;

int floorMod(int x, int n) {
    return x - n * int(floor(float(x) / float(n)));
}

void main() {
    vec2 worldPos = v_worldPos;
    
    float zoomLog = log(zoom) / log(float(devisions));
    float zoomFrac = fract(zoomLog);
    float sizeLevel = pow(float(devisions), floor(zoomLog));

    float primaryCellSize = baseCellSize * sizeLevel;

    vec2 pixelDerivatives = fwidth(worldPos);
    vec2 pixelDist = max(lineThickness * pixelDerivatives, vec2(1e-6));

    ivec2 lineIndex = ivec2(round(worldPos / primaryCellSize));
    vec2 dist = worldPos - floor(worldPos / primaryCellSize) * primaryCellSize;
    dist = min(dist, primaryCellSize - dist);

    bool vertical = dist.x <= dist.y ;
    bool horizontal = dist.y <= dist.x ;

    // bool corner = md && (length(dist) <= (10.0));
    bool corner = false;
    bool xAxis = lineIndex.y == 0 && horizontal;
    bool yAxis = lineIndex.x == 0 && vertical;

    pixelDist *= (corner && !(xAxis || yAxis)) ? 3.0 : 1.0;
    vertical = dist.x <= pixelDist.x;
    horizontal = dist.y <= pixelDist.y;

    int devSq = devisions * devisions;
    bool isLgX = floorMod(lineIndex.x, devSq) == 0;
    bool isMdX = floorMod(lineIndex.x, devisions) == 0;
    bool isLgY = floorMod(lineIndex.y, devSq) == 0;
    bool isMdY = floorMod(lineIndex.y, devisions) == 0;

    bool lg = (isLgX && vertical) || (isLgY && horizontal);
    bool md = !lg && ((isMdX && vertical) || (isMdY && horizontal));
    bool sm = !md && !lg;

    vec2 aa = 1.0 - smoothstep(vec2(0.0), pixelDist, dist);
    float alpha = max(aa.x, aa.y);

    vec4 color = sm ? ((1.0 - zoomFrac) * primaryGridColor * 0.2) : md ? ((1.0 - zoomFrac) * primaryGridColor + zoomFrac * primaryGridColor * 0.2) : primaryGridColor;
    color = (yAxis) ? gridYColor : (xAxis) ? gridXColor : color;

    float dummy = texture(u_texture, vec2(0.0)).a;
    fragColor = vec4(color.rgb, color.a * alpha * (0.999 + 0.001 * dummy));
}
