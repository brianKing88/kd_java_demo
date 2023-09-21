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
            sessionStart("确认");
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
        paramBuilder.param("vflytek/ivw/keyword.txtadEndGap",60);
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

    @JavascriptInterface
    public void setP(String str){
        int ret = 0;

        String path = ""; // 例如"/sdcard/iflytek/ivw/keyword.txt"
        //处理数据存入指定路径
        AiRequest.Builder customBuilder = AiRequest.builder();
        //key：数据标识 value： 唤醒词配置文件 index： 数据索引,用户可自定义设置
        customBuilder.customText("key_word",path , 0);
        ret = AiHelper.getInst().loadData("e867a88f2", customBuilder.build());
        if (ret != 0) {
            Log.e("TAG", "loadData 失败：" + ret);
        }
        //指定要使用的个性化数据集合，未调用，则默认使用所有loadData加载的数据

        int[] indexs = {0};
        /*
         * indexs 个性化数据索引数组
         */
        AiHelper.getInst().specifyDataSet("e867a88f2", "key_word", indexs);
        if (ret != 0) {
            Log.e("TAG", "specifyDataSet 失败：" + ret);
        }
    }

    // Android本地方法，供JavaScript调用
    @JavascriptInterface
    public void speak(String message) {
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
                .workDir("/sdcard/iflytekAikit")//SDK工作路径，这里为绝对路径，此处仅为示例
                .iLogMaxCount(1)
                .authInterval(333)
                .ability("e75f07b62;e2e44feff")
                .build();

        AiHelper.getInst().registerListener((errType, code) -> {
            Log.d("IFlytekAbilityManager", "引擎初始化状态 " + (errType == ErrType.AUTH && code == 0));
        });

        AiHelper.getInst().registerListener("e75f07b62",aiRespListener);

        new Thread(() -> AiHelper.getInst().init(this, params)).start();



    }

    public boolean initEngine(int language) {
        AiRequest.Builder engineBuilder = AiRequest.builder();
        // 解码类型 fsa:命令词, wfst:wfst解码, wfst_fsa:混合解码
        engineBuilder.param("decNetType", "fsa");
        // fsa惩罚分数 最小值:0, 最大值:10
        engineBuilder.param("punishCoefficient", 0.0);
        // 选择加载wfst资源 0中文，1英文
        engineBuilder.param("wfst_addType", language);
        // 初始化引擎
        int ret = AiHelper.getInst().engineInit("e75f07b62", engineBuilder.build());
        Log.i("TAG", "引擎初始化结果：" + ret);
        if (ret != 0) {
            Log.d("TAG", "initEngine: "+"引擎初始化结果 ===> " + ret);
            return false;
        }
        return true;
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
                    Log.e("aiRespListener", key);
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