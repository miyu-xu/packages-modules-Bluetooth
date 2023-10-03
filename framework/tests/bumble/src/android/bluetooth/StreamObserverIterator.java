/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.bluetooth;

import io.grpc.stub.StreamObserver;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StreamObserverIterator<T> implements Iterator<T>, StreamObserver<T> {
    private BlockingQueue<Object> mQueue = new LinkedBlockingQueue<>();
    private boolean mCompleted = false;

    @Override
    public void onNext(T value) {
        mQueue.add(value);
    }

    @Override
    public void onError(Throwable t) {
        mQueue.add(t);
    }

    @Override
    public void onCompleted() {
        mCompleted = true;
    }

    @Override
    public boolean hasNext() {
        if (!mQueue.isEmpty() && mQueue.peek() instanceof Throwable) {
            throw new RuntimeException((Throwable) mQueue.peek());
        }

        return !mCompleted || !mQueue.isEmpty();
    }

    @Override
    public T next() {
        try {
            Object item = mQueue.take();
            if (item instanceof Throwable) {
                throw new RuntimeException((Throwable) item);
            }
            return (T) item;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
