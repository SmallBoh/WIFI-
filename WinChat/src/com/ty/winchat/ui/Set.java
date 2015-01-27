package com.ty.winchat.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ty.winchat.R;
import com.ty.winchat.adapter.WiFiAadapter;
import com.ty.winchat.util.LocalMemoryCache;
import com.ty.winchat.util.Util;
import com.ty.winchat.util.WifiAdmin;

/**
 * 设置界面
 * 
 * @creation 2013-6-6
 */
public class Set extends Base implements OnClickListener {

	private EditText nickNameEdt, optionEdt;
	private Button nickNameBtn, optionBtn, iconBtn;
	private Button wifi_enable;
	private ImageView icon;
	private ListView listview;
	private WiFiAadapter adapter;

	private final int CUT_PHOTO_REQUEST_CODE = 201;
	private final int SELECT_PHOTO_REQUEST_CODE = 200;
	private WifiAdmin mWifiAdmin;
	private String iconPath;// 头像保存路径
	public static String iconName = "me";
	// 扫描结果列表
	private List<ScanResult> list;
	private StringBuffer sb = new StringBuffer();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set);
		mWifiAdmin = new WifiAdmin(Set.this);
		
		
		iconPath = getFilesDir() + File.separator + iconName;
		findViews();
	}
	private void findViews() {
		listview = (ListView) findViewById(R.id.set_listview);
		adapter = new WiFiAadapter(Set.this);
		getAllNetWorkList();
		nickNameEdt = (EditText) findViewById(R.id.set_nick_name_edt);
		optionEdt = (EditText) findViewById(R.id.set_option_edt);
		nickNameBtn = (Button) findViewById(R.id.set_nick_name_btn);
		optionBtn = (Button) findViewById(R.id.set_option_btn);
		iconBtn = (Button) findViewById(R.id.set_icon_btn);
		icon = (ImageView) findViewById(R.id.set_icon);
		wifi_enable = (Button) findViewById(R.id.wifi_enable);

		// 设置头像
		Bitmap bitmap = LocalMemoryCache.getInstance().get(iconName);
		if (bitmap == null) {
			bitmap = BitmapFactory.decodeFile(iconPath);
			if (bitmap != null) {
				icon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap));
				LocalMemoryCache.getInstance().put(iconName, bitmap);
			} else {
				icon.setImageResource(R.drawable.ic_launcher);
			}
		} else {
			icon.setImageBitmap(Util.getRoundedCornerBitmap(bitmap));
		}

		nickNameEdt.setText(getSharedPreferences("me", 0).getString("name",
				"新手"));
		TextView view = (TextView) findViewById(R.id.toptextView);
		view.setText("设置");
		nickNameBtn.setOnClickListener(this);
		optionBtn.setOnClickListener(this);
		iconBtn.setOnClickListener(this);
		wifi_enable.setOnClickListener(this);
		listview.setOnItemClickListener(item);
	}
	
	AdapterView.OnItemClickListener item = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			ScanResult result = list.get(position);
			setWifi(result);
		}

	};
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.set_nick_name_btn:
			String nickName = nickNameEdt.getText().toString().trim();
			if ("".equals(nickName)) {
				showToast("请输入昵称");
				return;
			}
			Editor editor = getSharedPreferences("me", 0).edit();
			editor.putString("name", nickName);
			editor.commit();
			showToast("保存成功");
			break;

		case R.id.set_option_btn:
			String option = optionEdt.getText().toString().trim();
			if ("".equals(option)) {
				showToast("请输入意见");
				return;
			}
			showToast("发送成功");
			break;

		case R.id.set_icon_btn:// 调用系统图片库
			try {
				Intent i = new Intent(Intent.ACTION_PICK,
						Media.EXTERNAL_CONTENT_URI);
				i.setType("image/*");
				startActivityForResult(i, SELECT_PHOTO_REQUEST_CODE);
			} catch (Exception e) {
				showToast("抱歉，您的手机不支持头像设置");
			}

			break;

		case R.id.wifi_enable:
			setWifiFound();
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK
				&& data != null) {
			Uri uri = data.getData();
			if (uri != null) {
				final Intent intent = new Intent(
						"com.android.camera.action.CROP");
				intent.setDataAndType(uri, "image/*");
				intent.putExtra("crop", "true");
				intent.putExtra("aspectX", 1);
				intent.putExtra("aspectY", 1);
				intent.putExtra("outputX", 100);
				intent.putExtra("outputY", 100);
				intent.putExtra("return-data", true);
				startActivityForResult(intent, CUT_PHOTO_REQUEST_CODE);
			}
		} else if (requestCode == CUT_PHOTO_REQUEST_CODE
				&& resultCode == RESULT_OK && data != null) {
			try {
				Bitmap bitmap = data.getParcelableExtra("data");
				bitmap = Util.getRoundedCornerBitmap(bitmap);
				icon.setImageBitmap(bitmap);
				File file = new File(iconPath);
				file.delete();
				file.createNewFile();
				FileOutputStream outputStream = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
				outputStream.flush();
				outputStream.close();
				LocalMemoryCache.getInstance().put(iconName, bitmap);
				showToast("头像保存成功");
			} catch (IOException e) {
				e.printStackTrace();
				showToast("头像保存失败");
			}
		}
	}
		
	public void getAllNetWorkList() {
		// 每次点击扫描之前清空上一次的扫描结果
		if (sb != null) {
			sb = new StringBuffer();
		}
		// 开始扫描网络
		mWifiAdmin.startScan();
		list = mWifiAdmin.getWifiList();
		if (list != null) {
			adapter.setList(list);
			listview.setAdapter(adapter);
			listview.setOnItemClickListener(item);
		}
	}
	
	EditText pass,name;

	private void setWifiFound() {
		View found = LayoutInflater.from(Set.this).inflate(R.layout.wifi_found,
				null);
		pass = (EditText) found.findViewById(R.id.wifi_found_edit_p);
		name = (EditText) found.findViewById(R.id.wifi_found_edit_n);
		new AlertDialog.Builder(Set.this).setTitle("创建热点")
				.setIcon(R.drawable.all_people_icon).setView(found)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

						 boolean apEnabled = mWifiAdmin.setWifiApEnabled(true,
								name.getText().toString(), pass.getText().toString());

						if (apEnabled) {
							showToast("wifi创建成功");
						} else {
							showToast("wifi创建失败");
						} 
						//mWifiAdmin.AddWifiConfig(list, name.getText().toString(), pass.getText().toString());

					}
				}).setNegativeButton("取消", null).create().show();

	}
	
	EditText passWod;

	private void setWifi(final ScanResult result) {
		View found = LayoutInflater.from(Set.this).inflate(
				R.layout.wifi_found_lj, null);
		passWod = (EditText) found.findViewById(R.id.wifi_found_lj_edit_passWod);
		new AlertDialog.Builder(Set.this).setTitle("连接 热点："+result.SSID.trim())
				.setIcon(R.drawable.all_people_icon)
				.setView(found)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						boolean b = mWifiAdmin.addNetWork(mWifiAdmin.CreateWifiInfo(
								result.SSID.trim(), passWod.getText().toString()
										.trim(), 3));
						if(b){
							showToast("连接成功");
							finish();
						}else{
							showToast("连接失败");
						}
					}
				}).setNegativeButton("取消", null).create().show();
	}
	
}
