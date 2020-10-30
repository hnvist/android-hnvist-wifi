package com.tzmax.hnvist.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {

    String TAG = "binDebug";
    Context mContext;
    boolean isConnection, isGetAnnouncement;
    LinearLayout mConnection, mToast;
    TextView mConnectionText, mToastText, mOpenWeb, mResultText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        initView();
        getAnnouncement();
        showAnnouncement();
    }

    private void initView() {

        mConnection = findViewById(R.id.connection);
        mConnectionText = findViewById(R.id.connectionText);
        mResultText = findViewById(R.id.main_result);

        mConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                run();
                isConnection = !isConnection;
                setConnection(isConnection);
            }
        });

    }

    void run() {
        String Url = "http://172.16.0.30:801/eportal/?c=Portal&a=login&callback=tzmax&login_method=1&user_account=aqxy@test&user_password=123";
        int time = 2000;
        isGetAnnouncement = false;

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (isConnection) {

                    try {
                        setResultText("开始请求认证…");
                        OkHttpClient okHttpClient = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(Url)
                                .build();
                        okHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            }

                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    String data = response.body().string();
                                    if (response.code() == 200) {
                                        Log.d(TAG, "认证成功！" + data);
                                        setResultText("认证成功！");

                                        // 判断是否获取公告，每次连接只获取一次
                                        if(!isGetAnnouncement) {
                                            isGetAnnouncement = true;
                                            getAnnouncement();
                                        }

                                    } else {
                                        // 出现错误，断开连接
                                        onApiError("ErrorCode:1001，认证失败！请检查是否连接 wifi，也可能是免费认证方法失效！");
                                    }
                                }
                            }
                        });
                    } catch (Error error) {
                        // 出现错误，断开连接
                        onApiError("ErrorCode:1002，认证失败！请检查是否连接 wifi，也可能是免费认证方法失效！");
                    }

                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // 出现错误，断开连接
                        onApiError("ErrorCode:1003，认证失败！请检查是否连接 wifi，也可能是免费认证方法失效！");
                    }
                }

            }
        }).start();
    }

    // 设置连接状态
    void setConnection(final boolean b) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        isConnection = b;

                        // 设置点击圈颜色
                        mConnectionText.setBackground(getDrawable(b ? R.drawable.back_circle_ok : R.drawable.back_circle));
                        // 设置点击圈文本
                        mConnectionText.setText(b ? "断 开" : "连 接");

                    }
                });
            }
        }).start();


    }

    // 设置状态值文本
    void setResultText(String msg) {
        if (mResultText == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        mResultText.setText(msg);
                    }
                });

            }
        }).start();
    }

    // 认证失效通知
    void onApiError() {
        isConnection = false;
        setConnection(isConnection);
        setResultText("认证失败！请检查是否连接 wifi，也可能是免费认证方法失效！");
    }

    void onApiError(String msg) {
        isConnection = false;
        setConnection(isConnection);
        setResultText(msg);
    }

    // 获取公告
    void getAnnouncement() {
        String Url = "http://hnvist1.zmorg.cn/data/hnvist_wifi_announcement.json";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    OkHttpClient okHttpClient = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(Url)
                            .build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String data = response.body().string();
                                if (response.code() == 200) {
                                    Log.d(TAG, "获取公告成功！" + data);
                                    try {
                                        JSONObject json = new JSONObject(data);
                                        String msg = json.getString("msg");
                                        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
                                        String announcement = pref.getString("announcement", "");
                                        if (!msg.equals("") && !msg.equals(announcement)) {
                                            // 公告相同就不更新了
                                            setAnnouncement(msg);
                                            readingAnnouncement(false);
                                            showAnnouncement();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    });
                } catch (Error error) {
                }
            }
        }).start();
    }

    // 设置公告
    void setAnnouncement(String msg) {
        SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        editor.putString("announcement", msg);
        editor.putBoolean("isReading", false);
        editor.commit();
    }

    // 阅读公告
    void readingAnnouncement(boolean isReading) {
        SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        editor.putBoolean("isReading", isReading);
        editor.commit();
    }

    // 显示公告
    void showAnnouncement() {
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        String announcement = pref.getString("announcement", "该软件仅用于技术学习，任何人不得用于盈利，开发者不承担任何法律责任");
        boolean isReading = pref.getBoolean("isReading", false);

        if (!isReading && announcement != null) {
            // 未阅读公告，弹出最新公告

            runOnUiThread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    // 创建dialog构造器
                    AlertDialog.Builder normalDialog = new AlertDialog.Builder(mContext);
                    // 设置title
                    normalDialog.setTitle("友情提示！");
                    // 设置内容
                    normalDialog.setMessage(announcement);
                    // 设置按钮
                    normalDialog.setPositiveButton("我知道了"
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    readingAnnouncement(true);
                                }
                            });
                    // 创建并显示
                    AlertDialog dialog = normalDialog.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                }
            });

        }

    }


}
