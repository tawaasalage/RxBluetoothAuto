package tech.tawsoft.com.rxble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ivbaranov.rxbluetooth.Action;
import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.jakewharton.rxbinding.view.RxView;
import com.polidea.rxandroidble.RxBleClient;

import com.polidea.rxandroidble.RxBleScanResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class Home extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    RxBluetooth rxBluetooth;
    private Subscription deviceSubscription;
    private Subscription discoverySubscription;
    private Subscription bluetoothStateOnSubscription;
    private Subscription bluetoothStateOtherSubscription;

    private Subscription bluetoothreadDataSubscription;
    private Subscription bluetoothConnectionSubscription;
    private Subscription reconnectionnSubscription;

    BluetoothSocket bSocket;

    @Bind(R.id.btnScan)
    Button btnScan;

    @Bind(R.id.btnStop)
    Button btnStop;

    @Bind(R.id.btnConnect)
    Button btnConnect;

    @Bind(R.id.lblValue)
    TextView lblValue;

    BluetoothDevice bluetoothDev;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rxBluetooth = new RxBluetooth(this);

        ButterKnife.bind(this);

        RxView.clicks(btnScan).subscribe(conclickEvent);
        RxView.clicks(btnStop).subscribe(stopclickEvent);
        RxView.clicks(btnConnect).subscribe(connectclickEvent);

        rxBluetooth = new RxBluetooth(this);
        if (!rxBluetooth.isBluetoothEnabled()) {
            rxBluetooth.enableBluetooth(this, REQUEST_ENABLE_BT);
        }


        initBluetoothOperation();

    }

    public void initBluetoothOperation()
    {
        observeDevices();
        observeDiscovery();
        observeStateOn();
        observeStateOff();
    }



    private void observeStateOff() {

        if(bluetoothStateOtherSubscription!=null)
        {

        }
        else
        {
            bluetoothStateOtherSubscription = rxBluetooth.observeBluetoothState()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.computation())
                    .filter(Action.isEqualTo(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF,
                            BluetoothAdapter.STATE_TURNING_ON))
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            Log.d("DeviceInfoList", "OFF " + integer);

                        }
                    });
        }




    }

    private void observeStateOn() {

        if(bluetoothStateOnSubscription!=null)
        {

        }
        else{

            bluetoothStateOnSubscription = rxBluetooth.observeBluetoothState()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.computation())
                    .filter(Action.isEqualTo(BluetoothAdapter.STATE_ON))
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            Log.d("DeviceInfoList", "ON " + integer);

                        }
                    });

        }


    }

    private void observeDiscovery() {

        if(discoverySubscription!=null)
        {

        }
        else{
            discoverySubscription = rxBluetooth.observeDiscovery()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(new Action1<String>() {
                        @Override public void call(String action) {
                            Log.d("DeviceInfoList",action);
                        }
                    });
        }


    }

    private void observeDevices() {

        if(deviceSubscription!=null)
        {

        }
        else{
            deviceSubscription = rxBluetooth.observeDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(new Observer<BluetoothDevice>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(BluetoothDevice bluetoothDevice) {

                            if(bluetoothDevice.getBondState()==12)
                            {
                                bluetoothDev=bluetoothDevice;
                                Log.d("DeviceInfo", "Pared Device");
                            }
                            Log.d("DeviceInfo", bluetoothDevice.getAddress()+"  "+bluetoothDevice.getName()+"    :"+bluetoothDevice.getBondState());

                        }
                    });
        }


    }


    Action1 conclickEvent=new Action1<Void>() {
        @Override
        public void call(Void aVoid) {

            rxBluetooth.startDiscovery();

        }
    };

    Action1 stopclickEvent=new Action1<Void>() {
        @Override
        public void call(Void aVoid) {

            rxBluetooth.cancelDiscovery();

        }
    };

    Action1 connectclickEvent=new Action1<Void>() {
        @Override
        public void call(Void aVoid) {

            ConnectFunc();

        }
    };

    public void ConnectFunc()
    {
        BluetoothAdapter bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices= bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDev=device;
            }
        }

        if(bluetoothDev==null)
        {
            Log.d("DeviceInfoDevice", "Null Device");

        }
        else
        {
            UUID uuid;
            String uuidS="";
            ParcelUuid [] puuid=bluetoothDev.getUuids();
            for (ParcelUuid u:puuid)
            {
                uuidS=u.getUuid().toString();
            }
            uuid = UUID.fromString(uuidS);

            bluetoothConnectionSubscription=rxBluetooth.observeConnectDevice(bluetoothDev,uuid )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<BluetoothSocket>() {
                        @Override
                        public void call(BluetoothSocket bluetoothSocket) {
                            bSocket = bluetoothSocket;
                            Log.d("DeviceInfoList", "Connection Success ");
                            readData();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.d("DeviceInfoList", "Connection Faileds " + throwable.getMessage());

                            reconnectionnSubscription=Observable.timer(3, TimeUnit.SECONDS)
                                     .subscribe(new Observer<Long>() {
                                         @Override
                                         public void onCompleted() {
                                             Log.d("DeviceInfoRe","Called again");
                                             ConnectFunc();

                                         }

                                         @Override
                                         public void onError(Throwable e) {

                                         }

                                         @Override
                                         public void onNext(Long number) {

                                         }
                                     });

                        }
                    });


        }
    }

    public void readData()
    {
        BluetoothConnection bluetoothConnection=null;
        try {
            bluetoothConnection = new BluetoothConnection(bSocket);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if(bluetoothConnection!=null) {
            bluetoothreadDataSubscription=bluetoothConnection.observeStringStream()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String string) {

                            String contents =string;

                            Log.d("DeviceInfoListData", contents);
                            try{
                                String val=contents.substring(11, 13);
                                int weight=Integer.parseInt(val);
                                if(weight<10)
                                {
                                    if(contents.substring(8, 9).equals("1"))
                                    {
                                        lblValue.setText("0");
                                    }
                                    else
                                    {
                                        String o_val_txt=contents.substring(12, 13)+"."+contents.substring(13, 15);
                                        lblValue.setText(o_val_txt);
                                    }
                                }
                                else
                                {
                                    if(contents.substring(8, 9).equals("1"))
                                    {
                                        lblValue.setText("0");
                                    }
                                    else
                                    {
                                        String o_val_txt=contents.substring(11, 13)+"."+contents.substring(13, 15);
                                        lblValue.setText(o_val_txt);
                                    }
                                }

                            }
                            catch(Exception e)
                            {

                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            // Error occured
                            //lblValue.setText(throwable.getMessage());
                            lblValue.setText("Connection lost");
                            ConnectFunc();
                        }
                    });
        }

    }

    @Override protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (rxBluetooth != null) {
            // Make sure we're not doing discovery anymore
            rxBluetooth.cancelDiscovery();
        }

        unsubscribe(deviceSubscription);
        unsubscribe(discoverySubscription);
        unsubscribe(bluetoothStateOnSubscription);
        unsubscribe(bluetoothStateOtherSubscription);
        unsubscribe(bluetoothreadDataSubscription);
        unsubscribe(bluetoothConnectionSubscription);
        unsubscribe(reconnectionnSubscription);
    }

    private static void unsubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void onBackPressed() {
        System.exit(0);
    }
}
