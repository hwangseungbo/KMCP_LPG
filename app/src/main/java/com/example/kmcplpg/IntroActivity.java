package com.example.kmcplpg;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class IntroActivity extends AppCompatActivity {

    static final int PERMISSIONS_REQUEST = 2;

    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    private Handler handler;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //intro 안에 IntroActivity 클래스와 MainActivity 클래스를 전달
            //화면간 페이지 전환시 이용
            Intent intent = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    };

    //블루투스 지원 유무 확인
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        /*상태바 제거*/
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //handler 변수에 Handler 객체를 넣음
        init();


        //포어그라운드 위치권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(getApplicationContext(), "앱실행을 위해선 위치권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST);

        }


        //블루투스를 지원하는 기기인지 아닌지 검사수행
        if (mBluetoothAdapter == null) {
            //장치가 블루투스를 지원하지 않는 경우,
            Toast.makeText(getApplicationContext(), "블루투스 이용이 불가하여 애플리케이션을 종료합니다.", Toast.LENGTH_SHORT).show();
            // 깔끔종료
            moveTaskToBack(true); // 태스크를 백그라운드로 이동
            finishAndRemoveTask(); // 액티비티 종료 + 태스크 리스트에서 지우기
            android.os.Process.killProcess(android.os.Process.myPid()); // 앱 프로세스 종료
        } else {
            //장치가 블루투스를 지원하는경우 블루투스가 활성화상태인지 검사수행
            if (!mBluetoothAdapter.isEnabled()) {
                //비활성 상태일시 활성상태로
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(intent, 1);

            } else if (mBluetoothAdapter.isEnabled() && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //블루투스가 활성상태이면서 위치권한이 있을경우 블루투스 권한이 있는지 없는지 판별하여 메인화면진입 혹은 깔끔종료
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_ADVERTISE,
                                    Manifest.permission.BLUETOOTH_CONNECT
                            },
                            1);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH
                            },
                            1);
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1: // 83번 줄에서 requestCode 값 1
                if(resultCode==RESULT_OK){
                    // 블루투스 기능을 활성화 시켰을 때
                    Toast.makeText(getApplicationContext(), "블루투스를 활성화합니다.", Toast.LENGTH_SHORT).show();
                    //2초 뒤에 화면 전환을 하게함
                    handler.postDelayed(runnable, 2000);
                }else{
                    // 활성화 여부를 거부할경우 종료
                    Toast.makeText(getApplicationContext(), "블루트스 비활성화 시 애플리케이션을 이용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    moveTaskToBack(true); // 태스크를 백그라운드로 이동
                    finishAndRemoveTask(); // 액티비티 종료 + 태스크 리스트에서 지우기
                    android.os.Process.killProcess(android.os.Process.myPid()); // 앱 프로세스 종료
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @Nullable int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {

            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getApplicationContext(), "블루투스 권한이 활성화 되었습니다.", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(runnable, 2000);
                }else {
                    Toast.makeText(getApplicationContext(), "블루트스 비활성화 시 애플리케이션을 이용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    moveTaskToBack(true); // 태스크를 백그라운드로 이동
                    finishAndRemoveTask(); // 액티비티 종료 + 태스크 리스트에서 지우기
                    android.os.Process.killProcess(android.os.Process.myPid()); // 앱 프로세스 종료
                }

                break;
            case PERMISSIONS_REQUEST:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Toast.makeText(getApplicationContext(), "블루투스 권한 설정을 진행해야 정상동작합니다.", Toast.LENGTH_SHORT).show();
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.BLUETOOTH,
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_ADVERTISE,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                },
                                1);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Toast.makeText(getApplicationContext(), "블루투스 권한 설정을 진행해야 정상동작합니다.2", Toast.LENGTH_SHORT).show();
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.BLUETOOTH
                                },
                                1);
                    }

                    //handler.postDelayed(runnable, 2000);

                } else {
                    Toast.makeText(getApplicationContext(), "앱실행을 위해 위치권한을 설정해 주세요.", Toast.LENGTH_SHORT).show();

                    AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
                    localBuilder.setTitle("위치권한 설정").setMessage("권한 미허용시 앱사용이 불가합니다.").setPositiveButton("권한설정하로가기", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int pramAnonymousInt) {
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).setData(Uri.parse("package:" + getPackageName()));
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .setNegativeButton("취소하기", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface paramAnoymousDialoginterface, int paraAnonymousInt) {
                                    Toast.makeText(getApplicationContext(), "위치권한이 허가되지 않아 앱을 종료합니다.", Toast.LENGTH_LONG).show();
                                    // 깔끔종료
                                    moveTaskToBack(true); // 태스크를 백그라운드로 이동
                                    finishAndRemoveTask(); // 액티비티 종료 + 태스크 리스트에서 지우기
                                    android.os.Process.killProcess(android.os.Process.myPid()); // 앱 프로세스 종료
                                }
                            })
                            .create()
                            .show();
                }
                break;
        }
    }

    //init 메소드는 handler 변수에 Handler 객체를 넣는 함수
    public void init(){
        handler = new Handler();
    }

    //앱이 다시 호출되는 경우를 막기위해 호출
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        handler.removeCallbacks(runnable);

    }
}