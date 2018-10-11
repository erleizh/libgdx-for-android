package com.erlei.camera;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Mesh;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.VertexAttribute;
import com.erlei.gdx.graphics.VertexAttributes;
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.utils.Logger;

import java.util.Locale;

/**
 * Created by lll on 2018/10/8
 * Email : lllemail@foxmail.com
 * Describe : 使用相机的数据作为纹理渲染到renderView
 */
public class CameraRender extends Gdx implements SurfaceTexture.OnFrameAvailableListener {
    private Logger mLogger = new Logger("CameraRender", Logger.DEBUG);
    private final CameraControl mControl;
    private CameraTexture mCameraTexture;
    private ShaderProgram mProgram2d;
    private ShaderProgram mProgramOES;
    private float[] mTexMatrixOES = new float[16];
    private Matrix4 mMatrix4 = new Matrix4();
    private Matrix4 mProjectionViewMatrix = new Matrix4();
    private Mesh mMesh;

    public CameraRender(IRenderView renderView, CameraControl cameraControl) {
        super(renderView);
        mControl = cameraControl;
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        mLogger.debug("create");
        initSurfaceTexture();
        initShaderProgram();
        initMesh();
        mControl.open(mCameraTexture.getSurfaceTexture());
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
        adjustTextureSize(width, height, mControl.getPreviewSize().width, mControl.getPreviewSize().height);
    }

    /**
     * 调整纹理大小比例
     *
     * @param viewWidth    视图宽度
     * @param viewHeight   视图高度
     * @param cameraWidth  相机宽度
     * @param cameraHeight 相机高度
     */
    protected void adjustTextureSize(float viewWidth, float viewHeight, float cameraWidth, float cameraHeight) {
        mLogger.debug(String.format(Locale.getDefault(), "viewWidth=%s viewHeight=%s cameraWidth=%s cameraHeight=%s", viewWidth, viewHeight, cameraWidth, cameraHeight));
        mProjectionViewMatrix.scale(cameraWidth / viewWidth, cameraHeight / viewHeight, 0F);
        mProjectionViewMatrix.scale(0.5F, 0.5F, 0F);
    }

    @Override
    public void pause() {
        super.pause();
        mLogger.debug("pause");
    }

    @Override
    public void render() {
        super.render();
        clear();
        mCameraTexture.getSurfaceTexture().updateTexImage();
        mCameraTexture.getSurfaceTexture().getTransformMatrix(mTexMatrixOES);

        mCameraTexture.bind();
        mProgramOES.begin();
        mProgramOES.setUniformMatrix("u_texMatrix", mMatrix4.set(mTexMatrixOES));
        mProgramOES.setUniformMatrix("u_projectionViewMatrix", mProjectionViewMatrix);
        mProgramOES.setUniformi("u_texture", 0);
        mMesh.render(mProgramOES, GL20.GL_TRIANGLE_FAN);
        mProgramOES.end();
    }

    @Override
    public void dispose() {
        super.dispose();
        mLogger.debug("dispose");
        mControl.close();
        mProgramOES.dispose();
        mMesh.dispose();
        mCameraTexture.dispose();
        mProgram2d.dispose();
    }

    protected void initSurfaceTexture() {
        mLogger.debug("initSurfaceTexture");
        mCameraTexture = new CameraTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, new CameraTexture.CameraTextureData());
        mCameraTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mCameraTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        mCameraTexture.getSurfaceTexture().setOnFrameAvailableListener(this);
    }

    protected void initShaderProgram() {
        mProgram2d = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_2d.glsl"));
        mProgramOES = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_oes.glsl"));
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

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        getRenderView().requestRender();
    }

    public interface CameraControl {

        Size getPreviewSize();

        void open(SurfaceTexture surfaceTexture);

        void close();

    }
}
