
package com.annie.dictionary;

import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.WebViewClientCallback;
import com.mmt.widget.M2tToast;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DictWebViewClient extends WebViewClient {

    private WebViewClientCallback mCallback = null;

    private Context mContext;

    public DictWebViewClient(Context context, WebViewClientCallback wvCB) {
        mCallback = wvCB;
        mContext = context;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(Def.BWORD_URL)) {
            String keyword = url.substring(Def.BWORD_URL.length());
            if (null != mCallback) {
                mCallback.shouldOverrideUrlLoading(keyword);
            }
        } else if (url.startsWith(Def.HTTP_URL) || url.startsWith(Def.HTTPS_URL)) {
            try {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(myIntent);
            } catch (ActivityNotFoundException e) {
                M2tToast.makeText(mContext, "No application can handle this request. Please install a webbrowser",
                        M2tToast.LENGTH_LONG).show();
            }
        }
        return true;
    }
}
