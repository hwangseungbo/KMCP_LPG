package com.example.kmcplpg;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ficat.easyble.BleDevice;
import com.ficat.easyble.BleManager;
import com.ficat.easyble.gatt.callback.BleCallback;
import com.ficat.easyble.gatt.callback.BleConnectCallback;
import com.ficat.easyble.gatt.callback.BleNotifyCallback;
import com.ficat.easyble.scan.BleScanCallback;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements LocationListener {

    public static LocationManager locationManager;
    public static Location mLastlocation = null;
    public static double speed;
    public static boolean sounds = false;
    BleManager bleManager;
    BleConnectCallback bleConnectCallback;

    TextView tv_Hour, tv_Minute, tv_Second, tv_Sub, tv_SystemRunningTime, tv_Connect, tv_Disconnect, tv_FuelValue, tv_gasStatus, tv_FuelValue2,
            tv_CoolantTempValue, tv_OilTempValue, tv_OilPressValue, Enable_GPS, tv_EngineRunningTime, tv_gasvalue;
    ImageView iv_RpmGradation, iv_AlarmSound, iv_BlowerStatus, iv_ValveShutdown;


    ProgressBar progressBar_RPM1, progressBar_RPM2, progressBar_Fuel, progressBar_CoolantTemp, progressBar_OilTemp, progressBar_OilPress, progressBar_GasValue;
    EditText et_RPM, et_KNOT;
    String wakelevel = "0";
    String RPM = "0";
    String KNOT = "0";
    String BTConnect = "1";  // 0 : 커넥트, 1 : 디스커넥트


    static String SystemRunningTime = "0";
    double RPMvalue = 0;

    float Rpm;
    int Fuel;
    int Ctemp;
    int Opress;
    int Otemp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BleManager.ScanOptions scanOptions = BleManager.ScanOptions
                .newInstance()
                .scanPeriod(10000)
                .scanDeviceName(null);

        BleManager.ConnectOptions connectOptions = BleManager.ConnectOptions
                .newInstance()
                .connectTimeout(12000);

        bleManager = BleManager
                .getInstance()
                .setScanOptions(scanOptions)//it is not necessary
                .setConnectionOptions(connectOptions)//like scan options
                .setLog(true, "TAG")
                .init(this.getApplication());//Context is needed here,do not use Activity,which can cause Activity leak


        bleManager.startScan(new BleScanCallback() {
            @Override
            public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {
                String name = device.name;
                String address = device.address;
                //tv_FuelValue.append("\r\n" + address + " - " + name);
            }

            @Override
            public void onStart(boolean startScanSuccess, String info) {
                if (startScanSuccess) {
                    //start scan successfully
                } else {
                    //fail to start scan, you can see details from 'info'
                    String failReason = info;
                }
            }

            @Override
            public void onFinish() {

            }
        });


        bleConnectCallback = new BleConnectCallback() {
            @Override
            public void onStart(boolean startConnectSuccess, String info, BleDevice device) {
                if (startConnectSuccess) {
                    //start to connect successfully
                    bleManager.stopScan();
                    BTConnect = "0";
                    tv_Connect.setTextColor(Color.parseColor("#01F8E0"));
                    tv_Disconnect.setTextColor(Color.parseColor("#5D5D5D"));
                    tv_Connect.setEnabled(false);
                    tv_Disconnect.setEnabled(true);

                    Toast.makeText(getApplicationContext(), "BLE 장비와 연결되었습니다.", Toast.LENGTH_SHORT).show();


                } else {
                    //fail to start connection, see details from 'info'
                    String failReason = info;
                }
            }

            @Override
            public void onFailure(int failCode, String info, BleDevice device) {
                if (failCode == BleConnectCallback.FAIL_CONNECT_TIMEOUT) {
                    //connection timeout

                    BTConnect = "1";
                    tv_Connect.setTextColor(Color.parseColor("#5D5D5D"));
                    tv_Disconnect.setTextColor(Color.parseColor("#01F8E0"));
                    tv_Connect.setEnabled(true);
                    tv_Disconnect.setEnabled(false);

                    Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                } else {
                    //connection fail due to other reasons

                    BTConnect = "1";
                    tv_Connect.setTextColor(Color.parseColor("#5D5D5D"));
                    tv_Disconnect.setTextColor(Color.parseColor("#01F8E0"));
                    tv_Connect.setEnabled(true);
                    tv_Disconnect.setEnabled(false);

                    Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onConnected(BleDevice device) {
                bleManager.notify(device, "0000FFE0-0000-1000-8000-00805F9B34FB", "0000FFE1-0000-1000-8000-00805F9B34FB", new BleNotifyCallback() {
                    @Override
                    public void onCharacteristicChanged(byte[] data, BleDevice device) {
                        String ecuData = null;
                        try {
                            ecuData = new String(data, StandardCharsets.UTF_8);
                            Log.d("DATA", ecuData);
                            tv_FuelValue2.setText(ecuData);

                            String[] subdata = ecuData.split("\\n");
                            for (int i = 0; subdata.length > i; i++) {
                                //Log.d("DATA",subdata[i]);
                                subdata[i] = subdata[i].trim();

                                if (subdata[i].length() >= 24) {   //정확한 데이터일 경우
                                    Log.d("DATA", subdata[i]);
                                    ecuData = subdata[i];


                                    String ecuSubData = ecuData.substring(3, 7);
                                    if (ecuSubData.equals("f004")) {
                                        //61444

                                        //190 RPM
                                        ecuSubData = ecuData.substring(18, 20);
                                        ecuSubData = ecuSubData + ecuData.substring(16, 18);
                                        int temp = Integer.parseInt(ecuSubData, 16);
                                        temp = temp * 125;
                                        temp = temp / 1000;
                                        Rpm = temp;

                                        temp = temp / 100;
                                        double rpm = temp / 10.0;
                                        et_RPM.setText(String.valueOf(rpm));

                                        if (Rpm > 1000) {
                                            progressBar_RPM1.setProgress(1000);
                                            progressBar_RPM2.setProgress(((int) Rpm - 1000));
                                        } else {
                                            progressBar_RPM1.setProgress((int) Rpm);
                                            progressBar_RPM2.setProgress(0);
                                        }

                                        if ((Rpm * 1000) >= 4000) {
                                            iv_RpmGradation.setImageResource(R.drawable.rpm_redzone);
                                        } else {
                                            iv_RpmGradation.setImageResource(R.drawable.rpm_normal);
                                        }

                                        if (rpm == 0) {
                                            et_RPM.setText("0.0");
                                        }

                                    } else if (ecuSubData.equals("fee5")) {
                                        //65253

                                        //247 Engine Running Time
                                        ecuSubData = ecuData.substring(16, 18);
                                        ecuSubData = ecuSubData + ecuData.substring(14, 16);
                                        ecuSubData = ecuSubData + ecuData.substring(12, 14);
                                        ecuSubData = ecuSubData + ecuData.substring(10, 12);
                                        int temp = Integer.parseInt(ecuSubData, 16);
                                        temp = temp * 5;
                                        temp = temp / 100;
                                        ecuSubData = String.valueOf(temp);

                                        tv_EngineRunningTime.setText(ecuSubData + " Hhr");

                                    } else if (ecuSubData.equals("feee")) {
                                        //65262

                                        //110   Coolant Temp
                                        ecuSubData = ecuData.substring(10, 12);
                                        int temp = Integer.parseInt(ecuSubData, 16);
                                        temp = temp - 40;

                                        //화씨로변환
                                        temp = ((temp * 9) / 5) + 32;

                                        if (ecuSubData.equals("ff")) {
                                            ecuSubData = "?";
                                        } else {
                                            ecuSubData = String.valueOf(temp);
                                        }

                                        progressBar_CoolantTemp.setProgress(temp);
                                        tv_CoolantTempValue.setText(ecuSubData + "ºF");

                                        //175   Oil Temp
                                        ecuSubData = ecuData.substring(16, 18);
                                        ecuSubData = ecuSubData + ecuData.substring(14, 16);
                                        double temp2 = Integer.parseInt(ecuSubData, 16);
                                        temp2 = temp2 * 0.03125;
                                        temp2 = temp2 - 273;
                                        if (ecuSubData.equals("ffff")) {
                                            ecuSubData = "?";
                                        } else {
                                            ecuSubData = String.valueOf((int) temp2);
                                        }

                                        progressBar_OilTemp.setProgress((int) temp2);
                                        tv_OilTempValue.setText(ecuSubData + "ºF");


                                    } else if (ecuSubData.equals("feef")) {
                                        //65263

                                        //100   Oil Press
                                        ecuSubData = ecuData.substring(16, 18);
                                        int temp = Integer.parseInt(ecuSubData, 16);
                                        temp = temp * 4;
                                        if (ecuSubData.equals("ff")) {
                                            ecuSubData = "?";
                                        } else {
                                            ecuSubData = String.valueOf(temp);
                                        }

                                        progressBar_OilPress.setProgress(temp);
                                        tv_OilPressValue.setText(ecuSubData + " PSI");


                                    } else if (ecuSubData.equals("fefc")) {
                                        //65276

                                        //96    Fuel Level
                                        ecuSubData = ecuData.substring(12, 14);
                                        int temp = Integer.parseInt(ecuSubData, 16);
                                        temp = temp * 4;
                                        temp = temp / 10;
                                        if (ecuSubData.equals("ff")) {
                                            ecuSubData = "?";
                                        } else {
                                            ecuSubData = String.valueOf(temp);
                                        }

                                        progressBar_Fuel.setProgress(temp);
                                        tv_FuelValue.setText(ecuSubData + "%");

                                    }

                                } else if (subdata[i].contains("$")) {
                                    String[] Gas = subdata[i].split("\\$");
                                    //tv_gasvalue.setText(Gas[1]);
                                    if (Gas[1].contains("*")) {
                                        //tv_gasvalue.setText("*들어있다.");
                                        String[] GasSub = Gas[1].split("\\*");
                                        //tv_gasvalue.setText(GasSub[0] + " " + GasSub[1]);
                                        String realGasValue = GasSub[0].substring(5);
                                        //tv_gasvalue.setText(realGasValue);
                                        double gasVal = Double.parseDouble(realGasValue);
                                        int GasVal = (int) gasVal;
                                        tv_gasvalue.setText(realGasValue + "%");
                                        progressBar_GasValue.setProgress(GasVal);
                                        if (GasVal < 30) {
                                            tv_gasStatus.setText("NORMAL");
                                            iv_AlarmSound.setImageResource(R.drawable.off);
                                            iv_BlowerStatus.setImageResource(R.drawable.off);
                                            iv_ValveShutdown.setImageResource(R.drawable.off);
                                            if(sounds == true) {
                                                MySoundPlayer.stop();
                                                sounds = false;
                                            }

                                        } else if (GasVal >= 30 && GasVal < 60) {
                                            tv_gasStatus.setText("BLOWER ON");
                                            iv_AlarmSound.setImageResource(R.drawable.on);
                                            iv_BlowerStatus.setImageResource(R.drawable.on);
                                            iv_ValveShutdown.setImageResource(R.drawable.off);
                                            if(sounds == false){
                                                MySoundPlayer.play(MySoundPlayer.Pager_Beeps);
                                                sounds = true;
                                            }
                                        } else if (GasVal >= 60) {
                                            tv_gasStatus.setText("VALVE OFF");
                                            iv_AlarmSound.setImageResource(R.drawable.on);
                                            iv_BlowerStatus.setImageResource(R.drawable.on);
                                            iv_ValveShutdown.setImageResource(R.drawable.on);
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {
                            Log.d("received", e.getMessage());
                        }
                    }

                    @Override
                    public void onNotifySuccess(String notifySuccessUuid, BleDevice device) {

                    }

                    @Override
                    public void onFailure(int failCode, String info, BleDevice device) {
                        switch (failCode) {
                            case BleCallback.FAIL_DISCONNECTED://connection has disconnected
                                Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
                                break;
                            case BleCallback.FAIL_OTHER://other reason
                                break;
                            default:
                                break;
                        }

                    }
                });
            }

            @Override
            public void onDisconnected(String info, int status, BleDevice device) {
                BTConnect = "1";
                tv_Connect.setTextColor(Color.parseColor("#5D5D5D"));
                tv_Disconnect.setTextColor(Color.parseColor("#01F8E0"));
                tv_Connect.setEnabled(true);
                tv_Disconnect.setEnabled(false);
                Toast.makeText(getApplicationContext(), "장비와 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();
            }
        };



        MySoundPlayer.initSounds(getApplicationContext());
        /*
        //경고음 관련 하여 아래와 같이 사용한다.
        MySoundPlayer.initSounds(getApplicationContext());

        findViewById(R.id.tv_Sub).setOnClickListener(v -> {
            iv_AlarmSound.setImageResource(R.drawable.on);
            MySoundPlayer.play(MySoundPlayer.Pager_Beeps);
            //MySoundPlayer.play(MySoundPlayer.Alarm_Clock);
        });

        findViewById(R.id.textView7).setOnClickListener(v -> {
            iv_AlarmSound.setImageResource(R.drawable.off);
            MySoundPlayer.stop();
        });
        */


        Intent intent = getIntent();
        if (intent.getStringExtra("BTConnect") != null) {
            BTConnect = intent.getStringExtra("BTConnect");
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        tv_FuelValue2 = findViewById(R.id.tv_FuelValue2);
        String last = "e0cf003008df0061ff00fffaff";
        tv_FuelValue2.setText(String.valueOf(last.length()));

        tv_gasStatus = findViewById(R.id.tv_gasStatus);
        tv_gasvalue = findViewById(R.id.tv_gasvalue);
        tv_Connect = findViewById(R.id.tv_Connect);
        tv_Disconnect = findViewById(R.id.tv_Disconnect);
        tv_Sub = findViewById(R.id.tv_Sub);
        tv_Hour = findViewById(R.id.tv_Hour);
        tv_Minute = findViewById(R.id.tv_Minute);
        tv_Second = findViewById(R.id.tv_Second);
        tv_SystemRunningTime = findViewById(R.id.tv_SystemRunningTime);
        tv_FuelValue = findViewById(R.id.tv_FuelValue);
        tv_CoolantTempValue = findViewById(R.id.tv_CoolantTempValue);
        tv_OilTempValue = findViewById(R.id.tv_OilTempValue);
        tv_OilPressValue = findViewById(R.id.tv_OilPressValue);
        tv_EngineRunningTime = findViewById(R.id.tv_EngineRunningTime);
        Enable_GPS = findViewById(R.id.textView20);

        iv_RpmGradation = findViewById(R.id.iv_RpmGradation);
        iv_AlarmSound = findViewById(R.id.iv_AlarmSound);
        iv_BlowerStatus = findViewById(R.id.iv_BlowerStatus);
        iv_ValveShutdown = findViewById(R.id.iv_ValveShutdown);

        et_RPM = findViewById(R.id.et_RPM);
        et_KNOT = findViewById(R.id.et_KNOT);

        progressBar_GasValue = findViewById(R.id.progressBar_GasValue);
        progressBar_RPM1 = findViewById(R.id.progressBar_RPM1);
        progressBar_RPM2 = findViewById(R.id.progressBar_RPM2);
        progressBar_Fuel = findViewById(R.id.progressBar_Fuel);
        progressBar_CoolantTemp = findViewById(R.id.progressBar_CoolantTemp);
        progressBar_OilTemp = findViewById(R.id.progressBar_OilTemp);
        progressBar_OilPress = findViewById(R.id.progressBar_OilPress);


        if (intent.getStringExtra("RPM") != null) {
            RPM = intent.getStringExtra("RPM");
            et_RPM.setText(RPM);
            RPM = et_RPM.getText().toString();
            RPMvalue = Double.parseDouble(RPM) * 1000;
            if (RPMvalue >= 4000) {
                iv_RpmGradation.setImageResource(R.drawable.rpm_redzone);
            } else {
                iv_RpmGradation.setImageResource(R.drawable.rpm_normal);
            }
            //Toast myToast = Toast.makeText(this.getApplicationContext(),RPM, Toast.LENGTH_SHORT);
            //myToast.show();
        }
        if (intent.getStringExtra("KNOT") != null) {
            KNOT = intent.getStringExtra("KNOT");
            et_KNOT.setText(KNOT);
            //Toast myToast = Toast.makeText(this.getApplicationContext(),KNOT, Toast.LENGTH_SHORT);
            //myToast.show();
        }


        //현재시간 실시간으로 가져오기
        SimpleDateFormat simple = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String time = simple.format(date);
        String[] splitTime = time.split(":");
        if (SystemRunningTime.equals("0")) {
            //static 변수에 초기에 한번만 초기실행시간을 담는다.
            SystemRunningTime = time;
        }
        tv_Hour.setText(splitTime[0]);
        tv_Minute.setText(splitTime[1]);
        tv_Second.setText(splitTime[2]);
        ShowTimeMethod();


        //전달 받은 값을 토대로 버튼활성화
        if (BTConnect.equals("0")) {
            tv_Connect.setTextColor(Color.parseColor("#01F8E0"));
            tv_Disconnect.setTextColor(Color.parseColor("#5D5D5D"));
            tv_Connect.setEnabled(false);
            tv_Disconnect.setEnabled(true);
        } else if (BTConnect.equals("1")) {
            tv_Connect.setTextColor(Color.parseColor("#5D5D5D"));
            tv_Disconnect.setTextColor(Color.parseColor("#01F8E0"));
            tv_Connect.setEnabled(true);
            tv_Disconnect.setEnabled(false);
        }

        // GPS관련 권한체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lastKnowLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        // GPS 사용 가능 여부 확인
        boolean isEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isEnable) {
            Enable_GPS.setTextColor(Color.parseColor("#FF0000"));   // GPS사용이 불가할 경우 KNOT글자색이 붉은색으로 변한다.
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);


        //EditText 설정이후 커서없애기
        et_RPM.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String inText = textView.getText().toString();

                RPM = textView.getText().toString();
                RPMvalue = Double.parseDouble(RPM) * 1000;
                if (RPMvalue >= 4000) {
                    iv_RpmGradation.setImageResource(R.drawable.rpm_redzone);
                } else {
                    iv_RpmGradation.setImageResource(R.drawable.rpm_normal);
                }
                //Do Something...
                textView.setCursorVisible(false);
                return true;
            }
        });
        et_RPM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText) view).setCursorVisible(true);
            }
        });
        //EditText 설정이후 커서없애기
        et_KNOT.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String inText = textView.getText().toString();
                KNOT = textView.getText().toString();
                //Do Something...
                textView.setCursorVisible(false);
                return true;
            }
        });
        et_KNOT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText) view).setCursorVisible(true);
            }
        });

        tv_Connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                tv_Connect.setEnabled(false);
                tv_Disconnect.setEnabled(false);

                Toast.makeText(getApplicationContext(), "블루투스 연결을 시도합니다.", Toast.LENGTH_SHORT).show();
                ShowProgressDialog spd = new ShowProgressDialog();
                spd.execute();

                //connectSelectedDevice("SD1000v2.0.8-77FBF5");   // "SD1000v2.0.8-77FC4B"

            }
        });

        tv_Disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (BTConnect.equals("0")) {
                    tv_Connect.setEnabled(false);
                    tv_Disconnect.setEnabled(false);

                    Toast.makeText(getApplicationContext(), "블루투스 연결을 해제합니다.", Toast.LENGTH_SHORT).show();
                    ShowProgressDialog2 spd2 = new ShowProgressDialog2();
                    spd2.execute();
                }
            }
        });


    }//onCreate().................................................................................................................................................................


    @Override
    protected void onDestroy() {
        //unregisterReceiver(mBluetoothStateReceiver);
        super.onDestroy();
    }


    //블루투스 connect 버튼 클릭시 발생하는 프로그래스 다이얼로그
    public class ShowProgressDialog extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            asyncDialog.setCancelable(false);

            asyncDialog.show();
            super.onPreExecute();
        }

        // 백그라운드에서 실행
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                //connectSelectedDevice("SD1000v2.0.8-77FC4B");   // "SD1000v2.0.8-77FC4B" "SD1000v2.0.8-77FBF5"

                //bleManager.connect(bled,bleConnectCallback);

                bleManager.connect("CF:CA:E2:11:12:CF", bleConnectCallback);
                //bleManager.connect("E3:B2:4C:9C:94:1B", bleConnectCallback);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        // 백그라운드가 모드 끝난 후 실행
        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            //Toast.makeText(MainActivity.this,String.valueOf(tv_Connect.getCurrentTextColor()), Toast.LENGTH_LONG).show();   //-10658467
            if (tv_Connect.getCurrentTextColor() == (-10658467)) {
                tv_Connect.setEnabled(true);
                tv_Disconnect.setEnabled(false);
                Toast.makeText(getApplicationContext(), "연결에 실패하였습니다. 장비상태를 확인해주세요.", Toast.LENGTH_LONG).show();
            }
            super.onPostExecute(result);
        }
    }

    //블루투스 disconnect 버튼 클릭시 발생하는 프로그래스 다이얼로그
    public class ShowProgressDialog2 extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            asyncDialog.setCancelable(false);

            asyncDialog.show();
            super.onPreExecute();
        }

        // 백그라운드에서 실행
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                //BTS.getInstance().cancel();
                bleManager.disconnect("CF:CA:E2:11:12:CF");
                //bleManager.disconnect("E3:B2:4C:9C:94:1B");
                //bleManager.disconnectAll();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        // 백그라운드가 모드 끝난 후 실행
        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }


    public void ShowTimeMethod() {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                SimpleDateFormat simple = new SimpleDateFormat("HH:mm:ss");
                Date date = new Date();
                String time = simple.format(date);
                String[] splitTime = time.split(":");
                tv_Hour.setText(splitTime[0]);
                tv_Minute.setText(splitTime[1]);
                tv_Second.setText(splitTime[2]);

                try {
                    Date AppStartTime = simple.parse(SystemRunningTime);
                    Date CurrentTime = simple.parse(time);
                    long minute = (CurrentTime.getTime() - AppStartTime.getTime()) / (60 * 1000);
                    tv_SystemRunningTime.setText(String.valueOf(minute) + " " + "Min");

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable task = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    handler.sendEmptyMessage(1); //핸들러 호출 = 시간 갱신
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }


    // GPS 관련 함수 오버라이드
    @Override
    public void onLocationChanged(Location location) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        double deltaTime = 0;

        try {
            // getSpeed() 함수를 이용하여 속도 계산
            double getSpeed = Double.parseDouble((String.format("%f", location.getSpeed())));
            // m/s 를 km/h로 바꾸려면 3.6을 곱해주면된다.
            getSpeed = (getSpeed * 3.6) / 1.852;
            int getspeed = (int) getSpeed;
            et_KNOT.setText(String.valueOf(getspeed));
            KNOT = et_KNOT.getText().toString();
        } catch (Exception e) {
        }


        String formatDate = sdf.format(new Date(location.getTime()));

        // 위치변경이 두번째로 변경된 경우 계산에 의해 속도 계산
        if (mLastlocation != null) {
            deltaTime = (location.getTime() - mLastlocation.getTime()) / 1000;
            // 속도 계산
            speed = mLastlocation.distanceTo(location) / deltaTime;
            String formatLastDate = sdf.format(new Date(mLastlocation.getTime()));

            try {
                double calSpeed = Double.parseDouble(String.format("%f, speed"));
                // m/s 를 km/h로 바꾸려면 3.6을 곱해주면된다.
                calSpeed = (calSpeed * 3.6) / 1.852;
                int calspeed = (int) calSpeed;
                et_KNOT.setText(String.valueOf(calspeed));
                KNOT = et_KNOT.getText().toString();
            } catch (Exception e) {
            }
        }
        //현재위치를 지난위치로 변경
        mLastlocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        // 권한체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // 위치정보 업데이트
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        //권한체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //위치정보 업데이트
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 위치정보 가져오기 제거
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //권한이 없을 경우 최초 권한 요청 또는 사용자에 의한 재용청 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // 권한 재요청
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기버튼 비활성화
    }

}