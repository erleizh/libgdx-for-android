#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoord;
uniform sampler2D s_texture;
uniform sampler2D s_texture2;
void main() {
    gl_FragColor = texture2D( s_texture, v_texCoord ) * texture2D( s_texture2, v_texCoord);
}