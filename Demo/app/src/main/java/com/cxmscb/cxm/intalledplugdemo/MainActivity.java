package com.cxmscb.cxm.intalledplugdemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.PathClassLoader;

public class MainActivity extends Activity {

    String skinPackageName; //当前皮肤的包名
    RelativeLayout rl ;
    SharedPreferences skinType ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rl = (RelativeLayout) findViewById(R.id.relativeLayout);

        skinType = getPreferences(Context.MODE_PRIVATE);
        String skin = skinType.getString("skin",null);
        if(skin!=null) installSkin(skin);
    }

    public void changeSkin1(View view) {
        installSkin("Dog");
    }

    public void changeSkin2(View view) {
        installSkin("Girl");
    }

    public void installSkin(String skinName){
        String packageName = findPlugins(skinName);
        if (packageName==null) {
            Toast.makeText(this, "请先安装皮肤", Toast.LENGTH_SHORT).show();
            // 皮肤插件被删除时，清空存储
            if (skinType.getString("skin", skinName).equals(skinName))
                skinType.edit().clear().commit();
        }
        else {
            try {
                //获取插件的上下文:忽略安全警告且可访问代码
                Context plugContext = this.createPackageContext(packageName,Context.CONTEXT_IGNORE_SECURITY|Context.CONTEXT_INCLUDE_CODE);
                //获取插件背景的资源文件id
                int bgId = getSkinBackgroundId(packageName,plugContext);

                rl.setBackgroundDrawable(plugContext.getResources().getDrawable(bgId));
                skinType.edit().putString("skin",skinName).commit();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }


        }
    }

    private int getSkinBackgroundId(String packageName,Context plugContext) {

        int id = 0;
        try {
            // 在插件R文件中寻找插件资源的id (R也是一个java文件，插件被安装后可以用类加载器PathClassLoader来取得)
            PathClassLoader pathClassLoader = new PathClassLoader(plugContext.getPackageResourcePath(),ClassLoader.getSystemClassLoader());
            // 运用反射：
            Class<?> forName = Class.forName(packageName + ".R$drawable", true, pathClassLoader);
            // 获取成员变量的值
            for (Field field:forName.getDeclaredFields()){
                if(field.getName().contains("main_bg")){
                   id = field.getInt(R.drawable.class);
                   return id;
                }
            }

        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return id;
    }

    private String findPlugins(String plugName) {
        PackageManager pm = this.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo info : installedPackages) {
            String packageName = info.packageName;
            String sharedUserId = info.sharedUserId;
            if (sharedUserId == null || !sharedUserId.equals("cxm.scb.skin") || packageName.equals(getPackageName())) {
                continue;
            }
            String appLabel = pm.getApplicationLabel(info.applicationInfo).toString();

            if (appLabel.equals(plugName)) {
                return info.packageName;
            }
        }
        return null;
    }
}