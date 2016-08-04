/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.lollipop.gravitybox.webserviceclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

import com.ceco.lollipop.gravitybox.R;
import com.ceco.lollipop.gravitybox.webserviceclient.WebServiceResult.ResultStatus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class WebServiceClient<T extends WebServiceResult> extends AsyncTask<RequestParams, Void, T> {
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int SOCKET_TIMEOUT = 30000;

    private Context mContext;
    private WebServiceTaskListener<T> mListener;
    private ProgressDialog mProgressDialog;
    private String mHash;

    public interface WebServiceTaskListener<T> {
        public void onWebServiceTaskCompleted(T result);
        public void onWebServiceTaskError(T result);
        public void onWebServiceTaskCancelled();
        public T obtainWebServiceResultInstance();
    }

    public WebServiceClient(Context context, WebServiceTaskListener<T> listener) {
        mContext = context;
        mListener = listener;
        if (mContext == null || mListener == null) { 
            throw new IllegalArgumentException();
        }

        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.wsc_please_wait));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dlgInterface) {
                abortTaskIfRunning();
            }
        });

        mHash = getAppSignatureHash(mContext);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.show();
    }

    @Override
    protected T doInBackground(RequestParams... params) {
        T result = mListener.obtainWebServiceResultInstance();
        result.setAction(params[0].getAction());
        HttpURLConnection con = null;

        if (mHash == null) {
            result.setStatus(WebServiceResult.ResultStatus.ERROR);
            result.setMessage(mContext.getString(R.string.wsc_hash_creation_failed));
            return result;
        }

        params[0].addParam("hash", mHash);
        if (Build.SERIAL != null) {
            params[0].addParam("serial", Build.SERIAL);
        }

        try {
            URL url = new URL(params[0].getUrl());
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(CONNECTION_TIMEOUT);
            con.setReadTimeout(SOCKET_TIMEOUT);
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);

            stringToStream(params[0].getEncodedQuery(), con.getOutputStream());
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result.setData(new JSONObject(streamToString(con.getInputStream())));
            } else {
                result.setStatus(ResultStatus.ERROR);
                result.setMessage(String.format(mContext.getString(R.string.wsc_error),
                        con.getResponseMessage()));
            }
        } catch (Exception e) {
            result.setStatus(ResultStatus.ERROR);
            result.setMessage(String.format(mContext.getString(R.string.wsc_error), e.getMessage()));
            e.printStackTrace();
        } finally {
            if (con != null) con.disconnect();
        }

        return result;
    }

    private void stringToStream(String value, OutputStream os) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(value);
        } finally {
            if (writer != null) writer.close();
            if (os != null) os.close();
        }
    }

    private String streamToString(InputStream is) throws IOException {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) reader.close();
            if (is != null) is.close();
        }
        return sb.toString();
    }

    @Override
    protected void onCancelled(T result) {
        mListener.onWebServiceTaskCancelled();
    }

    @Override
    protected void onPostExecute(T result) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (result.getStatus() == ResultStatus.ERROR) {
            mListener.onWebServiceTaskError(result);
        } else {
            mListener.onWebServiceTaskCompleted(result);
        }
    }

    public void abortTaskIfRunning() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (getStatus() == AsyncTask.Status.RUNNING) {
            cancel(true);
        }
    }

    public static String getAppSignatureHash(Context context) {
        return "9a19d753c105b4c40135f3e63d55b84c";
    }
}
