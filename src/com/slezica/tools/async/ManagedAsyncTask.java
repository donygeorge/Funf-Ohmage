/*
 * Copyright (C) 2011 Santiago Lezica
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

package com.slezica.tools.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public abstract class ManagedAsyncTask<Params, Progress, Result> {

    private TaskManagerFragment mManager;

    private InternalAsyncTask mTask;

    public ManagedAsyncTask(FragmentActivity activity) {
        this(activity, TaskManagerFragment.DEFAULT_TAG);
    }

    public ManagedAsyncTask(FragmentActivity activity, String fragmentTag) {

        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        mManager = (TaskManagerFragment) fragmentManager
                .findFragmentByTag(fragmentTag);

        if (mManager == null) {
            mManager = new TaskManagerFragment();

            fragmentManager.beginTransaction().add(mManager, fragmentTag)
                    .commit();
        }

        mTask = new InternalAsyncTask();
    }

    protected void onPreExecute() {}

    protected abstract Result doInBackground(Params... params);

    protected void onProgressUpdate(Progress... values) {}

    protected void onPostExecute(Result result) {}

    protected void onCancelled() {}

    public ManagedAsyncTask<Params, Progress, Result> execute(Params... params) {
        mTask.execute(params);

        return this;
    }

    public FragmentActivity getActivity() {
        return mManager.getActivity();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return mTask.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return mTask.isCancelled();
    }

    public Result get() throws InterruptedException, ExecutionException {
        return mTask.get();
    }

    public Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mTask.get(timeout, unit);
    }

    public AsyncTask.Status getStatus() {
        return mTask.getStatus();
    }

    protected class InternalAsyncTask extends
            AsyncTask<Params, Progress, Result> {

        @Override
        protected void onPreExecute() {
        	mManager.runWhenReady(new Runnable() {
                public void run() {
                	ManagedAsyncTask.this.onPreExecute();
                }
            });

            return;
        }

        @Override
        protected Result doInBackground(Params... params) {
            return ManagedAsyncTask.this.doInBackground(params);
        }

        protected void onProgressUpdate(final Progress... values) {
            mManager.runWhenReady(new Runnable() {
                public void run() {
                    ManagedAsyncTask.this.onProgressUpdate(values);
                }
            });

            return;
        };

        protected void onPostExecute(final Result result) {
            mManager.runWhenReady(new Runnable() {
                public void run() {
                    ManagedAsyncTask.this.onPostExecute(result);
                }
            });

            return;
        }

        @Override
        protected void onCancelled() {
            mManager.runWhenReady(new Runnable() {
                public void run() {
                    ManagedAsyncTask.this.onCancelled();
                }
            });
        }
    }
}
