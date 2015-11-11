/*
 * Copyright (C) 2014 Peter Gregus for GravityBox Project (C3C076@xda)
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
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

        if (mHash == null) {
            result.setStatus(WebServiceResult.ResultStatus.ERROR);
            result.setMessage(mContext.getString(R.string.wsc_hash_creation_failed));
            return result;
        }

        try {
            params[0].addParam("hash", mHash);
            if (Build.SERIAL != null) {
                params[0].addParam("serial", Build.SERIAL);
            }
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);

            SchemeRegistry registry = new SchemeRegistry();
            URL url = new URL(params[0].getUrl());
            int port = url.getPort();
            registry.register(new Scheme("http", new PlainSocketFactory(), (port == -1) ? 80 : port));

            ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, registry);
            DefaultHttpClient httpClient = new DefaultHttpClient(cm, httpParams);

            HttpPost httpPost = new HttpPost(params[0].getUrl());
            httpPost.setEntity(new UrlEncodedFormEntity(params[0].getParams())); 
            HttpResponse httpResponse = httpClient.execute(httpPost); 
            HttpEntity httpEntity = httpResponse.getEntity(); 
            String json = EntityUtils.toString(httpEntity, HTTP.UTF_8);
            result.setData(new JSONObject(json));
        } catch (UnsupportedEncodingException e) { 
            result.setStatus(ResultStatus.ERROR);
            result.setMessage(String.format(mContext.getString(R.string.wsc_error), e.getMessage()));
            e.printStackTrace();
        } catch (ClientProtocolException e) { 
            result.setStatus(ResultStatus.ERROR);
            result.setMessage(String.format(mContext.getString(R.string.wsc_error), e.getMessage()));
            e.printStackTrace();
        } catch (IOException e) { 
            result.setStatus(ResultStatus.ERROR);
            result.setMessage(String.format(mContext.getString(R.string.wsc_error), e.getMessage()));
            e.printStackTrace();
        } catch (JSONException e) {
            result.setStatus(ResultStatus.ERROR);
            result.setMessage(String.format(mContext.getString(R.string.wsc_error), e.getMessage()));
            e.printStackTrace();
        } catch (Exception e) {
            result.setStatus(ResultStatus.ERROR);
            result.setMessage(String.format(mContext.getString(R.string.wsc_error), e.getMessage()));
            e.printStackTrace();
        }

        return result;
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
        try {
            return "9a19d753c105b4c40135f3e63d55b84c";
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
