package com.erlei.videorecorder.camera;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.VertexAttributes;
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.widget.BaseRender;
import com.erlei.gdx.widget.EGLCore;
import com.erlei.gdx.widget.GLContext;

/**
 * Created by lll on 2018/10/8
 * Email : lllemail@foxmail.com
 * Describe : 使用相机的数据作为纹理渲染到renderView
 */
public class CameraRender extends BaseRender  {
    private Logger mLogger = new Logger("CameraRender", Logger.DEBUG);
    private CameraTexture mCameraTexture;
    private ShaderProgram mProgram;
    private float[] mTexMatrix = new float[16];
    private Matrix4 mMatrix4 = new Matrix4();
    private Matrix4 mProjectionViewMatrix = new Matrix4();
    private Mesh mMesh;
    private final CameraControl mCameraControl;

    public CameraRender(CameraControl cameraControl) {
        mCameraControl = cameraControl;
    }

    @Override
    public void create(EGLCore egl, GL20 gl) {
        super.create(egl, gl);
        mLogger.debug("create");
        initShaderProgram();
        initMesh();
        mCameraTexture = new CameraTexture(new CameraTextureData(mCameraControl));
        Size cameraSize = new Size(mCameraTexture.getWidth(),mCameraTexture.getHeight());
        adjustTextureSize(new Size(getWidth(), getHeight()), cameraSize);
    }


    @Override
    public void resume() {
        super.resume();
        mLogger.debug("resume");
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mLogger.debug("resize " + width + "x" + height);

        Size viewSize = new Size(width, height);
        Size cameraSize = new Size(mCameraTexture.getWidth(),mCameraTexture.getHeight());
        adjustTextureSize(viewSize, cameraSize);
    }

    /**
     * 调整纹理大小比例
     *
     * @param viewSize   视图大小
     * @param cameraSize 相机大小
     */
    private void adjustTextureSize(Size viewSize, Size cameraSize) {
        if (!GLContext.get().isLandscape())
            cameraSize = new Size(cameraSize.getHeight(), cameraSize.getWidth());
        mProjectionViewMatrix.idt();
        float cameraWidth = cameraSize.width;
        float viewWidth = viewSize.width;
        float cameraHeight = cameraSize.height;
        float viewHeight = viewSize.height;

        //1 . 恢复纹理比例
//        mProjectionViewMatrix.scale(cameraWidth / viewWidth, cameraHeight / viewHeight, 0F);

        //2 . CENTER_CROP (see ImageView CENTER_CROP)
        float scale;
        float dx = 0, dy = 0;
        if (cameraWidth * viewHeight > viewWidth * cameraHeight) {
            scale = viewHeight / cameraHeight;
            dx = (viewWidth - cameraWidth * scale) * 0.5f;
        } else {
            scale = viewWidth / cameraWidth;
            dy = (viewHeight - cameraHeight * scale) * 0.5f;
        }
        mLogger.debug("viewSize = " + viewSize.toString() + "\t\t cameraSize = " + cameraSize.toString() + "\t\tscale = " + scale + "\t\t dx = " + (dx / cameraWidth) + "\t\t dy = " + (dy / cameraHeight));
        //mProjectionViewMatrix.translate(dx,dy,0);
//        mProjectionViewMatrix.scale(scale, scale, 0f);
//        mProjectionViewMatrix.scale(0.5F, 0.5F, 0f);
    }

    @Override
    public void pause() {
        super.pause();
        mLogger.debug("pause");
    }


    @Override
    public void render(GL20 gl) {
        super.render(gl);
        clear();
        mCameraTexture.getSurfaceTexture().updateTexImage();
        mCameraTexture.getSurfaceTexture().getTransformMatrix(mTexMatrix);

        mCameraTexture.bind();
        mProgram.begin();
        mProgram.setUniformMatrix("u_texMatrix", mMatrix4.set(mTexMatrix));
        mProgram.setUniformMatrix("u_projectionViewMatrix", mProjectionViewMatrix);
        mProgram.setUniformi("u_texture", 0);
        mMesh.render(mProgram, GL20.GL_TRIANGLE_FAN);
        mProgram.end();
    }

    @Override
    public void dispose() {
        mLogger.debug("dispose");
        mProgram.dispose();
        mMesh.dispose();
        mCameraTexture.dispose();
    }


    protected void initShaderProgram() {
        mProgram = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_oes.glsl"));
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

}
