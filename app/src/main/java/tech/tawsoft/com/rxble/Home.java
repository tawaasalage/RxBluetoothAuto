package tech.tawsoft.com.rxble;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Subscription nfcSubscription;

    BluetoothSocket bSocket;

    @Bind(R.id.btnScan)
    Button btnScan;

    @Bind(R.id.btnStop)
    Button btnStop;

    @Bind(R.id.btnConnect)
    Button btnConnect;

    @Bind(R.id.lblValue)
    TextView lblValue;

    @Bind(R.id.lblNFCID)
    TextView lblNFCID;

    byte [] data=new byte[16];
    int count=0;
    String value="";


    // list of NFC technologies detected:
    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(), Ndef.class.getName()
            }
    };

    BluetoothDevice bluetoothDev;

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }


    @Override
    protected void onPause() {
        super.onPause();
        // disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {

            String type = intent.getType();
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            nfcReader(tag);
            lblNFCID.setText("Tag Detected");
        }
    }

    public String nfcRead(Tag t)
    {
        Tag tag = t;
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            return null;
        }
        NdefMessage ndefMessage = ndef.getCachedNdefMessage();
        NdefRecord[] records = ndefMessage.getRecords();
        for (NdefRecord ndefRecord : records)
        {
            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT))
            {
                try {return readText(ndefRecord);} catch (UnsupportedEncodingException e) {}
            }
        }

        return null;

    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        byte[] payload = record.getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    public void nfcReader(Tag tag)
    {
        nfcSubscription=Observable.just(nfcRead(tag))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {
                        if (s != null) {
                            lblNFCID.setText(s);
                        }
                    }
                });
    }




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
                    .filter(Action.isEqualTo(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF,BluetoothAdapter.STATE_TURNING_ON))
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

            UUID uuid = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

            bluetoothConnectionSubscription=Observable.just(getConnection(bluetoothDev))
            .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Boolean aBoolean) {

                            if(aBoolean)
                            {
                                readData();
                            }
                            else
                            {
                                reconnect();
                            }
                        }
                    });

        }
    }

    public void reconnect()
    {
        reconnectionnSubscription = Observable.timer(3, TimeUnit.SECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {
                        Log.d("DeviceInfoRe", "Called again");
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
    public boolean getConnection(BluetoothDevice device)
    {
        UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // bluetooth serial port service
        try {
            bSocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
        } catch (Exception e) {Log.e("","Error creating socket");}

        try {
            bSocket.connect();
            return true;

        } catch (IOException e) {
            Log.e("",e.getMessage());
            try {
                bSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                bSocket.connect();
                return true;
            }
            catch (Exception e2) {
               return false;
            }
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

            //Read Strings

            bluetoothreadDataSubscription=bluetoothConnection.observeStringStream()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String string) {

                            String contents = string;

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

           //Read Bytes
            /*
            bluetoothreadDataSubscription=bluetoothConnection.observeByteStream()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<Byte>() {
                        @Override
                        public void call(Byte aByte) {

                                if(count==16)
                                {
                                    count=0;
                                    value=new String(data);

                                    if(value.substring(0, 1).contentEquals("="))
                                    {

                                        String s = value.substring(1, 5);
                                        s=reverse(s);
                                        float count = Float.parseFloat(s);

                                        lblValue.setText(count+"");
                                        Log.d("ValueFinal",s);
                                    }
                                    else
                                    {

                                    }

                                }
                                else{
                                    data[count]=aByte;
                                    count++;
                                }

                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            lblValue.setText("Connection lost");
                            ConnectFunc();
                        }
                    });
                    */
        }

    }

    public  String reverse(String input){
        char[] in = input.toCharArray();
        int begin=0;
        int end=in.length-1;
        char temp;
        while(end>begin){
            temp = in[begin];
            in[begin]=in[end];
            in[end] = temp;
            end--;
            begin++;
        }
        return new String(in);
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
        unsubscribe(nfcSubscription);
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
