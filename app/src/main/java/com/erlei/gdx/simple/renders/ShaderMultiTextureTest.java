package com.erlei.gdx.simple.renders;

import com.erlei.gdx.android.widget.EglHelper;
import com.erlei.gdx.android.widget.GLContext;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.VertexAttributes;
import com.erlei.gdx.graphics.glutils.ShaderProgram;

public class ShaderMultiTextureTest extends GLContext {

    ShaderProgram shader;
    Texture texture;
    Texture texture2;
    Mesh mesh;

    public ShaderMultiTextureTest(IRenderView renderView) {
        super(renderView);
    }

    @Override
    public void create(EglHelper egl, GL20 gl) {
        super.create(egl, gl);
        shader = new ShaderProgram(
                files.internal("shader/ShaderMultiTextureTest_vertex.glsl"),
                files.internal("shader/ShaderMultiTextureTest_fragment.glsl"));
        mesh = new Mesh(true, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"));
        float[] vertices = {
                -0.5f, 0.5f, // Position 0
                0.0f, 0.0f, // TexCoord 0
                -0.5f, -0.5f, // Position 1
                0.0f, 1.0f, // TexCoord 1
                0.5f, -0.5f, // Position 2
                1.0f, 1.0f, // TexCoord 2
                0.5f, 0.5f, // Position 3
                1.0f, 0.0f // TexCoord 3
        };
        short[] indices = {0, 1, 2, 0, 2, 3};
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        createTexture();
    }

    private void createTexture() {
        Pixmap pixmap = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixmap.setColor(0, 0, 0, 1);
        pixmap.drawLine(0, 0, 256, 256);
        pixmap.drawLine(256, 0, 0, 256);
        texture = new Texture(pixmap);
        pixmap.dispose();

        pixmap = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixmap.setColor(0, 0, 0, 1);
//        pixmap.drawLine(128, 0, 128, 256);
        texture2 = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void render() {
        super.render();
        gl.glViewport(0, 0, getBackBufferWidth(), getBackBufferHeight());
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gl.glActiveTexture(GL20.GL_TEXTURE0);
        texture.bind();

        gl.glActiveTexture(GL20.GL_TEXTURE1);
        texture2.bind();

        shader.begin();
        shader.setUniformi("s_texture", 0);
        shader.setUniformi("s_texture2", 1);

        mesh.render(shader, GL20.GL_TRIANGLES);

        shader.end();
    }

    @Override
    public void dispose() {
        super.dispose();
        texture.dispose();
        texture2.dispose();
        shader.dispose();
        mesh.dispose();
    }

    @Override
    public void resume() {
        super.resume();
        createTexture();
    }
}
