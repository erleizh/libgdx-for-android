package com.erlei.gdx.simple.renders;

import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.g2d.BitmapFont;
import com.erlei.gdx.graphics.g2d.Sprite;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.widget.BaseRender;
import com.erlei.gdx.widget.EGLCore;


/**
 * Create by erlei on 2020-01-03
 * <p>
 * Email : erleizh@gmail.com
 * <p>
 * Describe : 绘制fps
 */
public class FPSRenderer extends BaseRender {


    private SpriteBatch mBatch;
    private BitmapFont font;

    @Override
    public void create(EGLCore egl, GL20 gl) {
        super.create(egl, gl);
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        mBatch = new SpriteBatch();
    }

    @Override
    public void render(GL20 gl) {
        super.render(gl);
        mLogger.debug("render");
        clearColor(Color.WHITE);
//        mBatch.begin();
//        font.draw(mBatch, "某啊噗啊", 10, 10);
//        mBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        mBatch.dispose();
        font.dispose();
    }
}
