package com.sunlin.weextest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.sunlin.weextest.common.GsonHelp;
import com.sunlin.weextest.common.HttpTask;
import com.sunlin.weextest.common.LogC;
import com.sunlin.weextest.common.RestTask;
import com.sunlin.weextest.common.ScreenUtil;
import com.sunlin.weextest.common.SharedData;
import com.sunlin.weextest.common.WeexJSVesion;
import com.taobao.weex.IWXRenderListener;
import com.taobao.weex.RenderContainer;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.common.WXRenderStrategy;
import com.taobao.weex.ui.component.NestedContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class WeexActivity extends MyActivtiyToolBar implements IWXRenderListener,WXSDKInstance.NestedInstanceInterceptor,RestTask.ResponseCallback {

    private WXSDKInstance mWeexInstance;
    private FrameLayout container;
    private String  url;
    private String  titleStr;
    private String  rTitle;
    private String  rColor;
    private String  rTag;
    private String  rImg;
    //////////////
    private Boolean isCatch=false;
    //////////////
    private Boolean isGet=false;
    private Boolean isLoading=false;
    private WeexJSVesion weexJSVesion;
    private String path="/weex/js";
    private String filePath="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = (FrameLayout) findViewById(R.id.container);
        mWeexInstance = new WXSDKInstance(this);
        mWeexInstance.registerRenderListener(this);
        String titleExt= getIntent().getStringExtra("title");
        String urlExt=getIntent().getStringExtra("url");
        String rTitleExt=getIntent().getStringExtra("rTitle");
        String rColorExt=getIntent().getStringExtra("rColor");
        String rTagExt=getIntent().getStringExtra("rTag");
        String rImgExt=getIntent().getStringExtra("rImg");

        url = urlExt==null?"":urlExt;
        titleStr = titleExt==null?"":titleExt;
        rTitle = rTitleExt==null?"":rTitleExt;
        rColor = rColorExt==null? "":rColorExt;
        rTag = rTagExt==null?"":rTagExt;
        rImg = rImgExt==null?"":rImgExt;

        //
        SharedPreferences sp = this.getSharedPreferences("WEEXTEST", Context.MODE_PRIVATE);
        isCatch=sp.getBoolean("isCatch",true);
        //导航栏
        ViewGroup.LayoutParams layoutParams= container.getLayoutParams();
        layoutParams.height=displayHeight();
        container.setLayoutParams(layoutParams);
        buildToolbar();
        if(isCatch){
            //JS版本判断
            getJSBundle();
        }else{
            render();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_show;
    }

    private  void getJSBundle(){
        if(isLoading){return;}
        isLoading=true;
        //版本地址
        String urlVesion=url;
        int num=url.lastIndexOf("/");
        StringBuilder sb = new StringBuilder(urlVesion);

        String fileName=sb.substring(num+1,url.length());
        num=url.lastIndexOf("dist");
        String pathName=sb.substring(0,num);

        //dist/test/index.weex.js,dist/v/v.index.weex.js

        urlVesion=pathName+"dist/v/v."+fileName;
        //获取版本信息
        HttpTask.Get(urlVesion,null,this);

    }
    private void buildToolbar(){
        Boolean isSet=true;
        if(rTitle.isEmpty() && rImg.isEmpty()){
            isSet=false;
        }
        if(rImg.isEmpty()){
            toolSet.setText(rTitle);
            String tempColor=rColor.isEmpty() ? "#000000":rColor;
            toolSet.setTextColor(Color.parseColor(tempColor));
        }else{
            toolSet.setBackgroundResource(getResourceByReflect(rImg));
        }
        int tagBtn=rTag.isEmpty()?0:Integer.parseInt(rTag);
        toolSet.setTag(tagBtn);
        ToolbarBuild(titleStr,true,isSet);

        if(isSet){
            ToolbarSetListense(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map data = new HashMap<String,Object>();
                    data.put("tag", toolSet.getTag());
                    mWeexInstance.fireEvent("_root","navclick",data,null);
                    mWeexInstance.fireGlobalEventCallback("navclickEvent",data);
                }
            });
        }
    }
    public void render(){
        render("");
    }
    public void  render(String template){
        if (mWeexInstance != null) {
            mWeexInstance.destroy();
        }
        mWeexInstance = new WXSDKInstance(this);
        mWeexInstance.setTrackComponent(true);
        mWeexInstance.registerRenderListener(this);
        //mWeexInstance?.renderByUrl("WXSample","http://10.66.48.190:8081/dist/index.weex.js",null, null, WXRenderStrategy.APPEND_ASYNC);
        Map options = new HashMap<String,Object>();
        options.put(WXSDKInstance.BUNDLE_URL, url);
        /**
         * pageName:自定义，一个标示符号。
         * url:远程bundle JS的下载地址
         * options:初始化时传入WEEX的参数，比如 bundle JS地址
         * flag:渲染策略。WXRenderStrategy.APPEND_ASYNC:异步策略先返回外层View，其他View渲染完成后调用onRenderSuccess。WXRenderStrategy.APPEND_ONCE 所有控件渲染完后后一次性返回。
         */
        if(isCatch){
            if(template.isEmpty()){
                template=readFile(filePath);
            }
            mWeexInstance.render("WXSample", template, options, null,WXRenderStrategy.APPEND_ONCE);
        }else{
            mWeexInstance.renderByUrl("WXSample", url, options, null, ScreenUtil.getScreenWidthPixels(this),ScreenUtil.getScreenHeightPixels(this),WXRenderStrategy.APPEND_ONCE);
        }

    }
    private String readFile(String filePath){
        String encoding = "UTF-8";
        File file = new File(filePath);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }
    private int  getResourceByReflect(String imageName){
        Class drawable = R.drawable.class;
        Field field = null ;
        int r_id;
        try {
            field = drawable.getField(imageName);
            r_id = field.getInt(field.getName());
        } catch (Exception e) {
            r_id = R.drawable.ios_app;
            LogC.e("ERROR", "PICTURE NOT　FOUND！");
        }
        return r_id;
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (mWeexInstance != null) {
            mWeexInstance.onActivityStart();
            mWeexInstance.fireEvent("_root","viewstart",null,null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWeexInstance != null) {
            mWeexInstance.onActivityResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWeexInstance != null) {
            mWeexInstance.onActivityPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWeexInstance != null) {
            mWeexInstance.onActivityStop();
            mWeexInstance.fireEvent("_root","viewstop",null,null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWeexInstance != null) {
            mWeexInstance.onActivityDestroy();
        }
    }

    @Override
    public void onViewCreated(WXSDKInstance instance, View view) {
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        container.addView(view);
    }

    @Override
    public void onRenderSuccess(WXSDKInstance instance, int width, int height) {

    }

    @Override
    public void onRefreshSuccess(WXSDKInstance instance, int width, int height) {

    }

    @Override
    public void onException(WXSDKInstance instance, String errCode, String msg) {

    }

    @Override
    public void onRequestSuccess(String response) {
        //判断是否是下载文件
        if(isGet){

            File file=new File(filePath);
            FileOutputStream fos = null;
            try{
                //创建下载文件夹
                isExistDir(path);
                if(!file.exists()){
                    file.createNewFile();
                }
                //保存文件
                fos = new FileOutputStream(file,false);
                byte[] buf = response.getBytes("UTF-8");
                fos.write(buf);
                render(response);
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                try {
                    if(fos!=null)
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            isLoading=false;
        }else{
            weexJSVesion= GsonHelp.getGsonObj().fromJson(response,WeexJSVesion.class);
            if(weexJSVesion != null){
                //获取本地版本
                String vesion= SharedData.getValue(this,weexJSVesion.getFileName());
                if(vesion!=null){
                    if(vesion.equals(weexJSVesion.getMd5())){
                        isGet=false;
                    }else{
                        //需更新
                        isGet=true;
                        SharedData.saveValue(this,weexJSVesion.getFileName(),weexJSVesion.getMd5());
                    }
                }else{
                    isGet=true;
                    SharedData.saveValue(this,weexJSVesion.getFileName(),weexJSVesion.getMd5());
                }
                isLoading=false;
                down();
            }
        }
    }

    @Override
    public void onRequestError(Exception error) {
        LogC.write("network error!","WeexActivity");
        isLoading=false;

        Toast.makeText(this, "服务器请求失败", Toast.LENGTH_LONG).show();
    }
    private void down(){
        if(isLoading){return;}
        //判断文件是否存在
        filePath=getCacheDir().getAbsolutePath() + path+"/"+weexJSVesion.getFileName();
        File file=new File(filePath);
        if(!file.exists()) {
            isGet=true;
        }
        if(isGet){
            isLoading=true;
            HttpTask.Get(url,null,this);

        }else{
            render();
        }
    }
    private String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(getCacheDir().getAbsolutePath()+ saveDir);
        if (!downloadFile.exists()) {
            downloadFile.mkdirs();
        }
        String savePath = downloadFile.getAbsolutePath();
        return savePath;
    }

    @Override
    public void onCreateNestInstance(WXSDKInstance wxsdkInstance, NestedContainer nestedContainer) {
        Log.d("Weex", "Nested Instance created.");
    }

}
