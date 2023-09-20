package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.ErrType;
import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionX.init(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .onExplainRequestReason((scope, deniedList) -> {
                    scope.showRequestReasonDialog(deniedList, "需要您同意以下权限才能正常使用", "Allow", "Deny");
                })
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        initSdk();
                    }
                });


        WebView webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 处理页面加载事件
                return false;
            }
        });

        findViewById(R.id.button).setOnClickListener(view -> {
            sessionStart("");
        });

        webView.loadUrl("http://10.100.68.51:8000/index.html");

    }

    // Android本地方法，供JavaScript调用
    @JavascriptInterface
    public void sessionStart(String message) {
        AiRequest.Builder paramBuilder = AiRequest.builder();
        paramBuilder.param("languageType", 0);
        paramBuilder.param("vadOn", true);
        //paramBuilder.param("vadLinkOn", false);
        paramBuilder.param("vadEndGap",60);
        //paramBuilder.param("beamThreshold", 20);
        //paramBuilder.param("hisGramThreshold", 3000);
        //paramBuilder.param("vadSpeechEnd", 80);
        //paramBuilder.param("vadResponsetime", 1000);
        //paramBuilder.param("postprocOn", false);
        AiHandle handle = AiHelper.getInst().start("e75f07b62", paramBuilder.build(), null);
        if (!handle.isSuccess()) {
            Log.e("TAG", "ERROR::START | handle code:" + handle.getCode());
            return;
        }
    }

    private void initSdk() {
        File file = getApplicationContext().getFilesDir();
        File customDir = new File(file, "iflytekAikit");
        if (!customDir.exists()) {
            customDir.mkdirs();
        }
        BaseLibrary.Params params = BaseLibrary.Params.builder()
                .appId("4a5d4d1d")
                .apiKey("53b2a37fdbedf2aac5b5fd60130102ba")
                .apiSecret("ODc3Mzc2ZjBhNTAwYzcxNjZhMDkwZDFh")
                .ability("e75f07b62")
                .workDir(customDir.getAbsolutePath())//SDK工作路径，这里为绝对路径，此处仅为示例
                .build();
        AiHelper.getInst().init(this, params);
        AiHelper.getInst().registerListener((errType, code) -> {
            Log.i("TAG", "core listener code:" + code);
            switch (errType) {
                case AUTH:
                    Log.i("TAG", "SDK状态：授权结果码" + code);
                    break;
                case HTTP:
                    Log.i("TAG", "SDK状态：HTTP认证结果" + code);
                    break;
                default:
                    Log.i("TAG", "SDK状态：其他错误");
            }
        });

        new Thread(() -> {
            AiHelper.getInst().init(this, params);
        }).start();

    }

    private AiListener aiRespListener = new AiListener() {
        @Override
        public void onResult(int handleID, List<AiResponse> outputData, Object usrContext) {

            if (null != outputData && outputData.size() > 0) {
                for (int i = 0; i < outputData.size(); i++) {
                    byte[] bytes = outputData.get(i).getValue();
                    if (bytes == null) {
                        continue;
                    }
                    String key = outputData.get(i).getKey();
                    //获取到结果的key及value，可根据业务保存存储结果或其他处理

                }
            }
        }

        @Override
        public void onEvent(int i, int i1, List<AiResponse> list, Object o) {

        }

        @Override
        public void onError(int i, int i1, String s, Object o) {

        }
    };
}