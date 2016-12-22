package com.bihe0832.getsignature;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private Button mGetSigButton;
    private Button mShakeButton;

    private EditText mPkgNameEdit;
    private TextView mResultView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGetSigButton = (Button) findViewById(R.id.getsig_getSigBtn);
        mGetSigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSigByPkgName();
            }
        });

        mShakeButton = (Button) findViewById(R.id.getsig_shareResultBtn);
        mShakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareResult();
            }
        });

        mPkgNameEdit = (EditText)findViewById(R.id.getsig_pkgNameEditText);
        mResultView = (TextView)findViewById(R.id.getsig_resultTextView);
    }

    private void getSigByPkgName(){
        String pkgName = mPkgNameEdit.getText().toString();
        if(null != pkgName && pkgName.length() > 0){
            try {

                Signature sig = this.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures[0];
                MessageDigest md5;
                md5 = MessageDigest.getInstance("MD5");
                String result = bytesToHexString(md5.digest(sig.toByteArray()));
                if(null != result && result.length() > 0){
                    showResult("包名：" + pkgName + "\n 签名：" + result);
                    mShakeButton.setVisibility(View.VISIBLE);
                }else{
                    showResult("读取失败，请重试或者检查应用是否有签名！");
                }
            }catch (PackageManager.NameNotFoundException e){
                e.printStackTrace();
                showResult("应用未安装，请检查输入的包名是否正确！");
                mShakeButton.setVisibility(View.GONE);
            }catch (NoSuchAlgorithmException e){
                e.printStackTrace();
                showResult("系统错误，请重启应用后重试！");
                mShakeButton.setVisibility(View.GONE);
            }
        }else{
            showResult("请先在输入框输入需要查询签名应用的包名！");
            mShakeButton.setVisibility(View.GONE);
        }
    }

    private void showResult(String tips){
        mResultView.setText(tips);
    }

    private void shareResult(){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mResultView.getText().toString());
        sendIntent.setType("text/plain");
        this.startActivity(Intent.createChooser(sendIntent, "分享结果"));
    }

    private String bytesToHexString(byte md5Bytes[]) {
       final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        if (md5Bytes != null && md5Bytes.length == 16) {
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = md5Bytes[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } else {
            return "";
        }
    }
}
