package com.badlogic.gdx.backends.android;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class CardboardCamera extends Camera {
    Matrix4 eyeViewAdjustMatrix = new Matrix4();

    public void setProjection(Matrix4 projection) {
        this.projection.set(projection);
    }

    @Override
    public void update() {
        update(true);
    }

    public void setEyeViewAdjustMatrix(Matrix4 eyeViewAdjustMatrix) {
        this.eyeViewAdjustMatrix.set(eyeViewAdjustMatrix);
    }

    public void setEyeProjection(Matrix4 projection) {
        this.projection.set(projection);
    }

    final Matrix4 tmpMatrix = new Matrix4();
    final Vector3 tmpVec = new Vector3();
    @Override
    public void update(boolean updateFrustum) {
        view.setToLookAt(position, tmpVec.set(position).add(direction), up);
        tmpMatrix.set(eyeViewAdjustMatrix);
        Matrix4.mul(tmpMatrix.val, view.val);
        combined.set(projection);
        Matrix4.mul(combined.val, tmpMatrix.val);

        if (updateFrustum) {
            invProjectionView.set(combined);
            Matrix4.inv(invProjectionView.val);
            frustum.update(invProjectionView);
        }
    }

}
