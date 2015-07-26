package com.badlogic.gdx.backends.android;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class CardboardCamera extends Camera {

    public void setProjection(Matrix4 projection) {
        this.projection.set(projection);
    }

    public Matrix4 getViewMatrix() {
        view.setToLookAt(position, tmp.set(position).add(direction), up);
        return view;
    }

    @Override
    public void update() {
        update(true);
    }

    final Vector3 tmp = new Vector3();
    @Override
    public void update(boolean updateFrustum) {
        combined.set(projection);
        Matrix4.mul(combined.val, view.val);

        if (updateFrustum) {
            invProjectionView.set(combined);
            Matrix4.inv(invProjectionView.val);
            frustum.update(invProjectionView);
        }
    }

}
