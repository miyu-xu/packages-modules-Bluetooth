/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.channelsoundingtestapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

class CanvasView extends View {
    private final String LOG_TAG = "CanvasView";
    int height = 750;
    int width = 1000;
    String mTitle = "";

    int maxYValue = 5;

    int startX = 100;
    int endX = width - 50;
    int startY = 80;
    int endY = height - 100;

    int nodeCount = 1;
    int maxNodeSize = 20;

    int previousY = endY;

    ArrayList<Node> mDataList;

    CanvasView(Context context, String title) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(width, height));
        mDataList = new ArrayList<Node>();
        mTitle = title;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = canvas.getHeight();
        int width = canvas.getWidth();

        Paint paint = new Paint();
        Paint textPaint = new Paint();
        textPaint.setTextSize(24);

        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(3);
        canvas.drawLine(startX, startY, startX, endY, paint);
        canvas.drawLine(startX, endY, endX, endY, paint);

        // Draw line
        paint.setStrokeWidth(1);
        paint.setColor(Color.GRAY);
        int intervalY = (endY - startY) / 5;
        for (int i = 1; i <= 5; i++) {
            int y = endY - intervalY * i;
            int yValue = maxYValue / 5 * i;
            canvas.drawLine(startX, y, endX, y, paint);
            canvas.drawText(yValue + "", 40, y, textPaint);
        }
        canvas.drawText("0", 40, endY, textPaint);

        // DrawTitle
        textPaint.setTextSize(32);
        canvas.drawText(mTitle, width / 2 - mTitle.length() * 6, startY - 30, textPaint);

        // DrawNode
        int intervalX = (endX - startX) / maxNodeSize;
        int currentX = startX + intervalX;
        paint.reset();
        ;
        paint.setTextSize(16);
        textPaint.setTextSize(24);
        Paint pointPaint = new Paint();
        pointPaint.setColor(Color.BLUE);
        pointPaint.setStrokeWidth(3);

        // Draw first node
        for (int i = 0; i < mDataList.size(); i++) {
            if (mDataList.get(i).abort) {
                pointPaint.setColor(Color.RED);
                canvas.drawLine(currentX - intervalX, previousY, currentX, previousY, pointPaint);
                canvas.drawCircle(currentX, previousY, 5, pointPaint);
                canvas.drawText("abort", currentX - 15, previousY - 10, paint);
            } else {
                pointPaint.setColor(Color.BLUE);
                double distance = mDataList.get(i).value;
                int y = endY - (int) ((endY - startY) * (distance / maxYValue));
                canvas.drawLine(currentX - intervalX, previousY, currentX, y, pointPaint);
                canvas.drawCircle(currentX, y, 5, pointPaint);
                canvas.drawText(distance + "", currentX - 15, y - 10, paint);
                previousY = y;
            }

            // number text
            String number = mDataList.get(i).number + "";
            if (mDataList.get(i).number % (maxNodeSize / 20 * 5) == 0) {
                if (number.length() > 2) {
                    canvas.rotate(-60, currentX - 15, endY + 50);
                    canvas.drawText(number, currentX - 15, endY + 50, textPaint);
                    canvas.rotate(60, currentX - 15, endY + 50);
                } else {
                    canvas.drawText(number, currentX, endY + 30, textPaint);
                }
            }
            currentX += intervalX;
        }
    }

    void cleanUp() {
        mDataList.clear();
        invalidate();
    }

    void addNode(double distance, boolean abort) {
        Log.d(LOG_TAG, "Add Node " + nodeCount + " with distance:" + distance);
        if (abort && !mDataList.isEmpty()) {
            distance = mDataList.get(mDataList.size() - 1).value;
        }
        mDataList.add(new Node(distance, nodeCount++, abort));
        if (distance > maxYValue) {
            maxYValue = (int) (distance / 10) * 10 + 10;
        }

        if (mDataList.size() > maxNodeSize) {
            previousY = endY - (int) ((endY - startY) * (mDataList.get(0).value / maxYValue));
            mDataList.remove(0);
        } else {
            previousY = endY;
        }
        invalidate();
    }
}

class Node {
    Node(double value, int number, boolean abort) {
        this.value = value;
        this.number = number;
        this.abort = abort;
    }

    double value;
    int number;
    boolean abort;
}
