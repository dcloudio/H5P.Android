package com.seu.magicfilter.filter.advanced;

import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.seu.magicfilter.filter.base.gpuimage.GPUImageFilter;
import com.seu.magicfilter.utils.OpenGlUtils;
import com.upyun.hardware.GlUtil;
import com.upyun.hardware.Watermark;
import com.upyun.hardware.WatermarkPosition;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class MagicWaterFilter extends GPUImageFilter {

    private static final String TAG = "MagicWaterFilter";
    private int mProgram = -1;
    private int maPositionHandle = -1;
    private int maTexCoordHandle = -1;
    private int muPosMtxHandle = -1;
    private int muSamplerHandle = -1;

    public static Watermark mWatermark;
    private FloatBuffer mWatermarkVertexBuffer;
    private int mWatermarkTextureId = -1;
    private float mWatermarkRatio = 1.0f;

    private final FloatBuffer mNormalTexCoordBuf = GlUtil.createTexCoordBuffer();

    private int mFboTexId;

    private final float[] mPosMtx = GlUtil.createIdentityMtx();

    private static final String vertexShader =
            //
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "uniform   mat4 uPosMtx;\n" +
                    "varying   vec2 textureCoordinate;\n" +
                    "void main() {\n" +
                    "  gl_Position = uPosMtx * position;\n" +
                    "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                    "}\n";
    private static final String fragmentShader =
            //
            "precision mediump float;\n" +
                    "uniform sampler2D uSampler;\n" +
                    "varying vec2  textureCoordinate;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                    "}\n";


    private void initGL() {
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");
    }

//    public MagicWaterFilter(Watermark watermark) {
//        super(vertexShader, fragmentShader);
//        this.mWatermark = watermark;
//    }

    public MagicWaterFilter() {
        super(vertexShader, fragmentShader);
    }


    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {
        mFboTexId = textureId;
        runPendingOnDrawTasks();

        if (mOutputWidth <= 0 || mOutputHeight <= 0) {
            return OpenGlUtils.NOT_INIT;
        }
//        GlUtil.checkGlError("draw_S");
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * 2, textureBuffer);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);

        GLES20.glUniform1i(muSamplerHandle, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);


       /* cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }*/

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        drawWatermark();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenGlUtils.ON_DRAWN;
    }

    private void drawWatermark() {
        if (mWatermark.markImg == null) {
            return;
        }
        mWatermarkVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * 3, mWatermarkVertexBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        mNormalTexCoordBuf.position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * 2, mNormalTexCoordBuf);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        if (mWatermarkTextureId == -1) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mWatermark.markImg, 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            mWatermarkTextureId = textures[0];
        }

        GLES20.glUniform1i(muSamplerHandle, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mWatermarkTextureId);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
    }


    private void initWatermarkVertexBuffer() {
        if (mOutputWidth <= 0 || mOutputHeight <= 0) {
            return;
        }

        int width = (int) (mWatermark.width * mWatermarkRatio);
        int height = (int) (mWatermark.height * mWatermarkRatio);
        int vMargin = (int) (mWatermark.vMargin * mWatermarkRatio);
        int hMargin = (int) (mWatermark.hMargin * mWatermarkRatio);

        boolean isTop, isRight;
        if (mWatermark.orientation == WatermarkPosition.WATERMARK_ORIENTATION_TOP_LEFT
                || mWatermark.orientation == WatermarkPosition.WATERMARK_ORIENTATION_TOP_RIGHT) {
            isTop = true;
        } else {
            isTop = false;
        }

        if (mWatermark.orientation == WatermarkPosition.WATERMARK_ORIENTATION_TOP_RIGHT
                || mWatermark.orientation == WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT) {
            isRight = true;
        } else {
            isRight = false;
        }

        float leftX = (mOutputWidth / 2.0f - hMargin - width) / (mOutputWidth / 2.0f);
        float rightX = (mOutputWidth / 2.0f - hMargin) / (mOutputWidth / 2.0f);

        float topY = (mOutputHeight / 2.0f - vMargin) / (mOutputHeight / 2.0f);
        float bottomY = (mOutputHeight / 2.0f - vMargin - height) / (mOutputHeight / 2.0f);

        float temp;

        if (!isRight) {
            temp = leftX;
            leftX = -rightX;
            rightX = -temp;
        }
        if (!isTop) {
            temp = topY;
            topY = -bottomY;
            bottomY = -temp;
        }
        final float watermarkCoords[] = {
                leftX, bottomY, 0.0f,
                leftX, topY, 0.0f,
                rightX, bottomY, 0.0f,
                rightX, topY, 0.0f
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(watermarkCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mWatermarkVertexBuffer = bb.asFloatBuffer();
        mWatermarkVertexBuffer.put(watermarkCoords);
        mWatermarkVertexBuffer.position(0);
    }

    public void onInit() {

        initGL();
//
//        //设置水印
//        Bitmap watermarkImg = BitmapFactory.decodeResource(MagicParams.context.getResources(), R.drawable.upyun_logo);
//        Watermark watermark = new Watermark(watermarkImg, 50, 25, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
//        setWatermark(watermark);

    }

//    public void setWatermark(Watermark watermark) {
//        mWatermark = watermark;
//        mWatermarkImg = watermark.markImg;
//        initWatermarkVertexBuffer();
//    }

    @Override
    public void onDisplaySizeChanged(int width, int height) {
        super.onDisplaySizeChanged(width, height);
        initWatermarkVertexBuffer();
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        initWatermarkVertexBuffer();
    }
}
