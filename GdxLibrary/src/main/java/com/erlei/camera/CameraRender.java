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
import com.erlei.gdx.graphics.glutils.ShaderProgram;
import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.utils.Logger;

/**
 * Created by lll on 2018/10/8
 * Email : lllemail@foxmail.com
 * Describe : 使用相机的数据作为纹理渲染到renderView
 */
public class CameraRender extends Gdx implements SurfaceTexture.OnFrameAvailableListener {
    private Logger mLogger = new Logger("CameraRender");
    private final CameraControl mControl;
    private CameraTexture mCameraTexture;
    private ShaderProgram mProgram2d;
    private ShaderProgram mProgramOES;
    private float[] mTexMatrixOES = new float[16];
    private Matrix4 mMatrix4 = new Matrix4();
    private Mesh mMesh;

    public CameraRender(IRenderView renderView, CameraControl cameraControl) {
        super(renderView);
        mControl = cameraControl;
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        initSurfaceTexture();
        initShaderProgram();
        initMesh();
        mControl.open(mCameraTexture.getSurfaceTexture());
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void render() {
        super.render();
        clear();
        mCameraTexture.getSurfaceTexture().updateTexImage();
        mCameraTexture.getSurfaceTexture().getTransformMatrix(mTexMatrixOES);
        mMesh.transform(mMatrix4.set(mTexMatrixOES));

        mProgramOES.begin();
        mMesh.render(mProgramOES, GL20.GL_TRIANGLE_FAN);
        mProgramOES.end();
    }

    @Override
    public void dispose() {
        super.dispose();
        mControl.close();
    }

    protected void initSurfaceTexture() {
        mLogger.debug("initSurfaceTexture");
        mCameraTexture = new CameraTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, new CameraTexture.CameraTextureData(getWidth(), getHeight()));
        mCameraTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        mCameraTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        mCameraTexture.getSurfaceTexture().setOnFrameAvailableListener(this);
    }

    protected void initShaderProgram() {
        mProgram2d = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_2d.glsl"));
        mProgramOES = new ShaderProgram(files.internal("shader/vertex_shader.glsl"), files.internal("shader/fragment_shader_oes.glsl"));
    }

    protected void initMesh() {
        float[] verts = new float[20];
        int i = 0;

        verts[i++] = -1; // x1
        verts[i++] = -1; // y1
        verts[i++] = 0;
        verts[i++] = 0f; // u1
        verts[i++] = 0f; // v1

        verts[i++] = 1f; // x2
        verts[i++] = -1; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u2
        verts[i++] = 0f; // v2

        verts[i++] = 1f; // x3
        verts[i++] = 1f; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u3
        verts[i++] = 1f; // v3

        verts[i++] = -1; // x4
        verts[i++] = 1f; // y4
        verts[i++] = 0;
        verts[i++] = 0f; // u4
        verts[i] = 1f; // v4

        mMesh = new Mesh(true, 4, 0,
                VertexAttribute.Position(), VertexAttribute.TexCoords(0));
        mMesh.setVertices(verts);

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        getRenderView().requestRender();
    }

    public interface CameraControl {

        void open(SurfaceTexture surfaceTexture);

        void close();

    }
}
