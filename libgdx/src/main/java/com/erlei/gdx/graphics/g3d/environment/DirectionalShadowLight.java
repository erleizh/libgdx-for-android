/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.erlei.gdx.graphics.g3d.environment;

import com.erlei.gdx.widget.GLContext;
import com.erlei.gdx.graphics.Camera;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.OrthographicCamera;
import com.erlei.gdx.graphics.Pixmap.Format;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g3d.utils.TextureDescriptor;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.math.Matrix4;
import com.erlei.gdx.math.Vector3;
import com.erlei.gdx.utils.Disposable;

/** @deprecated Experimental, likely to change, do not use!
 * @author Xoppa */
public class DirectionalShadowLight extends DirectionalLight implements ShadowMap, Disposable {
	protected FrameBuffer fbo;
	protected Camera cam;
	protected float halfDepth;
	protected float halfHeight;
	protected final Vector3 tmpV = new Vector3();
	protected final TextureDescriptor textureDesc;

	/** @deprecated Experimental, likely to change, do not use! */
	public DirectionalShadowLight (int shadowMapWidth, int shadowMapHeight, float shadowViewportWidth, float shadowViewportHeight,
		float shadowNear, float shadowFar) {
		fbo = new FrameBuffer(Format.RGBA8888, shadowMapWidth, shadowMapHeight, true);
		cam = new OrthographicCamera(shadowViewportWidth, shadowViewportHeight);
		cam.near = shadowNear;
		cam.far = shadowFar;
		halfHeight = shadowViewportHeight * 0.5f;
		halfDepth = shadowNear + 0.5f * (shadowFar - shadowNear);
		textureDesc = new TextureDescriptor();
		textureDesc.minFilter = textureDesc.magFilter = Texture.TextureFilter.Nearest;
		textureDesc.uWrap = textureDesc.vWrap = Texture.TextureWrap.ClampToEdge;
	}

	public void update (final Camera camera) {
		update(tmpV.set(camera.direction).scl(halfHeight), camera.direction);
	}

	public void update (final Vector3 center, final Vector3 forward) {
		// cam.position.set(10,10,10);
		cam.position.set(direction).scl(-halfDepth).add(center);
		cam.direction.set(direction).nor();
		// cam.up.set(forward).nor();
		cam.normalizeUp();
		cam.update();
	}

	public void begin (final Camera camera) {
		update(camera);
		begin();
	}

	public void begin (final Vector3 center, final Vector3 forward) {
		update(center, forward);
		begin();
	}

	public void begin () {
		final int w = fbo.getWidth();
		final int h = fbo.getHeight();
		fbo.begin();
		GLContext.getGL20().glViewport(0, 0, w, h);
		GLContext.getGL20().glClearColor(1, 1, 1, 1);
		GLContext.getGL20().glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		GLContext.getGL20().glEnable(GL20.GL_SCISSOR_TEST);
		GLContext.getGL20().glScissor(1, 1, w - 2, h - 2);
	}

	public void end () {
		GLContext.getGL20().glDisable(GL20.GL_SCISSOR_TEST);
		fbo.end();
	}

	public FrameBuffer getFrameBuffer () {
		return fbo;
	}

	public Camera getCamera () {
		return cam;
	}

	@Override
	public Matrix4 getProjViewTrans () {
		return cam.combined;
	}

	@Override
	public TextureDescriptor getDepthMap () {
		textureDesc.texture = fbo.getColorBufferTexture();
		return textureDesc;
	}

	@Override
	public void dispose () {
		if (fbo != null) fbo.dispose();
		fbo = null;
	}
}
