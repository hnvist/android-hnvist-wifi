package com.tzmax.hnvist.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {

    String TAG = "binDebug";
    Context mContext;
    boolean isConnection, isGetAnnouncement;
    LinearLayout mConnection, mToast, mSelectAccount;
    TextView mConnectionText, mToastText, mOpenWeb, mResultText, mAccountStr, mAccountType;

    AccountData accountData = new AccountData("aqxy@test", "免费账号", "123");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        initView();
        getAnnouncement();
        showAnnouncement();
        loadAccount(accountData);
        addDataAccount(accountData);
    }

    private void initView() {

        mConnection = findViewById(R.id.connection);
        mConnectionText = findViewById(R.id.connectionText);
        mResultText = findViewById(R.id.main_result);
        mSelectAccount = findViewById(R.id.main_select_account);

        mAccountStr = findViewById(R.id.main_text_account_str);
        mAccountType = findViewById(R.id.main_text_account_type);

        // 连接按钮点击事件
        mConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isConnection = !isConnection;
                setConnection(isConnection);
                run();

            }
        });

        // 绑定切换账号点击事件
        mSelectAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountList();
            }
        });

    }

    void run() {
        String Url = "http://172.16.0.30:801/eportal/?c=Portal&a=login&callback=tzmax&login_method=1&user_account=" + accountData.account + "&user_password=" + accountData.password;
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
                                        if (!isGetAnnouncement) {
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

                        // 设置状态文本
                        setResultText(b ? "连接中…" : "待连接!");

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

    // 加载账号信息到页面
    void loadAccount(AccountData account) {
        this.accountData = account;

        if (mAccountStr != null) {
            mAccountStr.setText(accountData.getAccount());
        }
        if (mAccountType != null) {
            mAccountType.setText(accountData.getType());
        }

    }

    // 获取全部账号列表
    ArrayList<AccountData> getDataAccountList() {
        ArrayList<AccountData> data = new ArrayList<AccountData>();
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        String json = pref.getString("accountList", "[]");

        if (json != null) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonElements = jsonParser.parse(json).getAsJsonArray();
            for (JsonElement item : jsonElements) {
                AccountData accountData = new Gson().fromJson(item, AccountData.class);
                data.add(accountData);
            }
        }

        return data;
    }

    // 添加一个账号到列表
    void addDataAccount(AccountData account) {

        ArrayList<AccountData> listData = getDataAccountList();

        // 判断账号存在的话就是修改相关数据
        boolean isEdit = false;
        for (int i = 0; i < listData.size(); i++) {
            AccountData item = listData.get(i);
            if (item != null && item.account != null && item.account.equals(account.account)) {
                listData.set(i, account);
                isEdit = true;
            }
        }
        if (!isEdit) {
            // 不是修改数据的话就是添加数据
            listData.add(account);
        }

        String json = new Gson().toJson(listData);
        SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        editor.putString("accountList", json);
        editor.commit();
    }

    // 显示选择账号列表
    void showAccountList() {

        if (isConnection) {
            toast("需要先断开连接才能切换账号！");
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_list);
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;

        int dHeight = getWindowManager().getDefaultDisplay().getHeight();
        lp.height = (dHeight / 10) * 6;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().setBackgroundDrawable(null);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // 去除黑框

        Button mFlatshare = dialog.findViewById(R.id.dialog_button_flatshare);
        Button mAddaccunt = dialog.findViewById(R.id.dialog_button_addaccount);

        // 合租账号点击事件绑定
        mFlatshare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("安院校园网合租平台即将上线，敬请期待…");
            }
        });

        mAddaccunt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("点击了添加本地账号");
            }
        });

        ListView mList = dialog.findViewById(R.id.dialog_list);
        ArrayList<AccountData> listData = getDataAccountList();

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AccountData data = listData.get(position);
                loadAccount(data);
                dialog.cancel();
            }
        });
        AccountListAdapter listAdapter = new AccountListAdapter(listData, this);


        if (mList != null) {
            mList.setAdapter(listAdapter);
        } else {
            toast("出错啦，请反馈给我们。error: -10967");
        }


    }

    void toast(String msg) {
        runOnUiThread(new Thread() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class AccountListAdapter extends BaseAdapter {

        ArrayList<AccountData> list;
        Context context;

        public AccountListAdapter(ArrayList<AccountData> list, Context context) {
            this.list = list;
            this.context = context;
        }


        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public AccountData getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            AccountData data = list.get(position);
            View view;
            ViewHolder viewHolder;

            if (convertView == null) {
                // inflate出子项布局，实例化其中的图片控件和文本控件
                view = LayoutInflater.from(getBaseContext()).inflate(R.layout.list_item, null);

                viewHolder = new ViewHolder();
                // 通过id得到图片控件实例
                viewHolder.type = view.findViewById(R.id.list_item_text_type);
                // 通过id得到文本空间实例
                viewHolder.account = view.findViewById(R.id.list_item_text_account);
                // 缓存图片控件和文本控件的实例
                view.setTag(viewHolder);
            } else {
                view = convertView;
                // 取出缓存
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.type.setText(data.type);
            viewHolder.account.setText(data.account);


            return view;
        }


        class ViewHolder {
            TextView type;
            TextView account;
        }
    }

    class AccountData {
        String account;
        String type;
        String password;

        public AccountData(String account, String type, String password) {
            this.account = account;
            this.type = type;
            this.password = password;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAccount() {
            return account;
        }

        public String getType() {
            return type;
        }

        public String getPassword() {
            return password;
        }
    }

}
