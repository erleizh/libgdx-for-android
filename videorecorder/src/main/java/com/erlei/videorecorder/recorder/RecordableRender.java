package com.erlei.videorecorder.recorder;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.VertexAttributes;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.widget.BaseRender;
import com.erlei.gdx.widget.EGLCore;
import com.erlei.gdx.widget.GLContext;
import com.erlei.gdx.widget.IRenderView;


/**
 * Created by lll on 2018/10/19
 * Email : lllemail@foxmail.com
 * Describe : 一个易于记录的渲染器 ，它将数据渲染到FrameBuffer中 , 而不是直接渲染到屏幕上
 * https://stackoverflow.com/questions/45015398/explanation-on-libgdx-draw-method
 */
public class RecordableRender extends BaseRender {

    private final Recorder mRenderer;
    protected FrameBuffer mFrameBuffer;
    protected SpriteBatch mSpriteBatch;
    private ShaderProgram mProgram;
    private Mesh mMesh;
    private Matrix4 mMatrix4;
    private Matrix4 mProjectionViewMatrix;

    /**
     * @param renderer 需要Recorder对象返回一个
     * @see Recorder
     */
    public RecordableRender(Recorder renderer) {
        mRenderer = renderer;
    }

    @Override
    public void create(EGLCore egl, GL20 gl) {
        super.create(egl, gl);
        mSpriteBatch = new SpriteBatch();
        mRenderer.create(egl, gl);
        initFrameBuffer();
        GLContext.get().setBackBufferSize(mFrameBuffer.getWidth(), mFrameBuffer.getHeight());
        mMatrix4 = new Matrix4().idt();
        mProjectionViewMatrix = new Matrix4().idt();
        initShaderProgram();
        initMesh();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mRenderer.resize(mFrameBuffer.getWidth(), mFrameBuffer.getHeight());
//        mMatrix4.scale((float)mFrameBuffer.getWidth() / width,  (float) mFrameBuffer.getHeight() / height, 0);


    }

    @Override
    public void resume() {
        super.resume();
        mRenderer.resume();
    }

    @Override
    public void dispose() {
        super.dispose();
        mFrameBuffer.dispose();
        mRenderer.dispose();
        mProgram.dispose();
        mMesh.dispose();
    }

    @Override
    public void render(GL20 gl) {
        super.render(gl);
        mFrameBuffer.begin();
        mRenderer.render(gl);
        mFrameBuffer.end();

//        mSpriteBatch.begin();
//        Texture texture = mFrameBuffer.getColorBufferTexture();
//        mSpriteBatch.draw(texture, vertices, 0, vertices.length);
//        mSpriteBatch.end();
        Texture texture = mFrameBuffer.getColorBufferTexture();
        texture.bind();
        mProgram.begin();
        mProgram.setUniformMatrix("u_texMatrix", mMatrix4);
        mProgram.setUniformMatrix("u_projectionViewMatrix", mProjectionViewMatrix);
        mProgram.setUniformi("u_texture", 0);
        mMesh.render(mProgram, GL20.GL_TRIANGLE_FAN);
        mProgram.end();
    }


    protected void initShaderProgram() {
        mProgram = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_2d.glsl"));
    }

    protected void initMesh() {
        float[] vertices = {
                -1f, -1f, // Position 0
                0.0f, 0.0f, // TexCoord 0
                1f, -1f, // Position 1
                1f, 0.0f, // TexCoord 1
                1f, 1f, // Position 2
                1f, 1f, // TexCoord 2
                -1f, 1f, // Position 3
                0.0f, 1f // TexCoord 3
        };

        mMesh = new Mesh(true, 4, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
        mMesh.setVertices(vertices);

    }


    protected void initFrameBuffer() {
//        mFrameBuffer = mRenderer.generateFrameBuffer();
//        if (mFrameBuffer == null) {
//            throw new IllegalStateException("generateFrameBuffer can not return null");
//        }
        mFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 1080, 1440, false);
    }


    public FrameBuffer getFrameBuffer() {
        return mFrameBuffer;
    }


    /**
     * 返回一个FrameBuffer ， 用于存储每一次渲染的帧数据
     */
    public interface Recorder extends IRenderView.Renderer {

        FrameBuffer generateFrameBuffer();

    }
}
