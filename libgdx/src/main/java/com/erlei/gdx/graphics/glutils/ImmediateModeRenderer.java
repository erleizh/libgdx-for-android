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

package com.erlei.gdx.graphics.glutils;

import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.math.Matrix4;

public interface ImmediateModeRenderer {
	void begin(Matrix4 projModelView, int primitiveType);

	void flush();

	void color(Color color);

	void color(float r, float g, float b, float a);

	void color(float colorBits);

	void texCoord(float u, float v);

	void normal(float x, float y, float z);

	void vertex(float x, float y, float z);

	void end();

	int getNumVertices();

	int getMaxVertices();

	void dispose();
}
