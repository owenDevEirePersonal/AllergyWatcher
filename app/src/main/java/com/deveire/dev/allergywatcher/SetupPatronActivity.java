package com.deveire.dev.allergywatcher;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.deveire.dev.allergywatcher.bleNfc.DeviceManager;
import com.deveire.dev.allergywatcher.bleNfc.DeviceManagerCallback;
import com.deveire.dev.allergywatcher.bleNfc.Scanner;
import com.deveire.dev.allergywatcher.bleNfc.ScannerCallback;
import com.deveire.dev.allergywatcher.bleNfc.card.CpuCard;
import com.deveire.dev.allergywatcher.bleNfc.card.FeliCa;
import com.deveire.dev.allergywatcher.bleNfc.card.Iso14443bCard;
import com.deveire.dev.allergywatcher.bleNfc.card.Mifare;
import com.deveire.dev.allergywatcher.bleNfc.card.Ntag21x;
import com.deveire.dev.allergywatcher.bleNfc.card.SZTCard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SetupPatronActivity extends AppCompatActivity
{

    private SharedPreferences savedData;
    private int savedTotalNumberOfUsers;
    private ArrayList<String> savedUsersIDs;
    private ArrayList<String> savedUserAllergies;
    private ArrayList<Integer> savedUserMaxSalt;
    private ArrayList<Integer> savedUserCurrentSalt;
    private ArrayList<Boolean> savedUserIsNearSighted;
    private ArrayList<Boolean> savedUserWantsSuggestions;
    private ArrayList<String> currentUserAllergies;



    private String currentUID;
    private float currentBalance;

    private TextView scannedCardUIDText;
    private EditText patronNameEditText;
    private EditText patronMaxSaltEditText;
    private Button cancelButton;
    private Button okButton;
    private CheckBox nearSightedCheckbox;
    private CheckBox suggestionsCheckbox;
    private ArrayList<CheckBox> allergenRadioButtons;


    //[Tile Reader Variables]
    private DeviceManager deviceManager;
    private Scanner mScanner;

    private ProgressDialog dialog = null;

    private BluetoothDevice mNearestBle = null;
    private int lastRssi = -100;

    private int readCardFailCnt = 0;
    private int disconnectCnt = 0;
    private int readCardCnt = 0;
    private String startTimeString;
    private static volatile Boolean getMsgFlag = false;


    private Timer tileReaderTimer;
    private boolean uidIsFound;
    private boolean hasSufferedAtLeastOneFailureToReadUID;

    private boolean stopAllScans;


    private Timer resumeTileScannerConnectionDelayer;

    //[/Tile Reader Variables]

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_patron);



        savedTotalNumberOfUsers = 0;

        currentUserAllergies = new ArrayList<String>();


        loadSavedUserData();


        currentUID = "";

        scannedCardUIDText = (TextView) findViewById(R.id.scannedCardIDTextView);
        //patronNameEditText = (EditText) findViewById(R.id.nameEditText);
        patronMaxSaltEditText = (EditText) findViewById(R.id.saltEditText);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        okButton = (Button) findViewById(R.id.okButton);

        allergenRadioButtons = new ArrayList<CheckBox>();
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox));
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox2));
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox3));
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox4));
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox5));
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox6));
        allergenRadioButtons.add((CheckBox)findViewById(R.id.checkBox7));

        nearSightedCheckbox = (CheckBox) findViewById(R.id.nearSightedCheckbox);
        suggestionsCheckbox = (CheckBox) findViewById(R.id.suggestionsCheckbox);




        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                addPatron();
                finish();
            }
        });



        loadLastUserDetails();

        Log.i("Scanner Buggery", "SetupPatronActivity OnCreate.");
        setupTileScanner();
    }

    protected  void onPause()
    {
        super.onPause();
        tileReaderTimer.cancel();
        tileReaderTimer.purge();

        resumeTileScannerConnectionDelayer.cancel();
        resumeTileScannerConnectionDelayer.purge();

        //if scanner is connected, disconnect it
        if(deviceManager.isConnection())
        {
            Log.i("Scanner Buggery", "SetupPatron OnPause is connected now disconnecting");
            stopAllScans = true;
            deviceManager.requestDisConnectDevice();
        }

        if(mScanner.isScanning())
        {
            Log.i("Scanner Buggery", "SetupPatron OnPause is scanning now not scanning");
            mScanner.stopScan();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.i("Scanner Buggery", "SetupPatron onResume resuming connection to scanner.");
        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = true;
        tileReaderTimer = new Timer();

        //TODO: Consider alternative solution to the onResume Problem
        resumeTileScannerConnectionDelayer = new Timer();
        resumeTileScannerConnectionDelayer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                Log.i("Scanner Buggery", "SetupPatronActivity OnResume resuming connection after delay.");
                connectToTileScanner();
            }
        }, 1000);

    }

    protected void onStop()
    {
        Log.i("State", "SetupPatronActivity On Stop");
        super.onStop();
        SharedPreferences.Editor edit = savedData.edit();
        edit.putInt("savedTotal", savedTotalNumberOfUsers);
        for(int i = 0; i < savedTotalNumberOfUsers; i++)
        {
            edit.putString("savedUserID" + i, savedUsersIDs.get(i));
            edit.putString("savedUserAllergies" + i, savedUserAllergies.get(i));
            edit.putInt("savedUserMaxSalt" + i, savedUserMaxSalt.get(i));
            Log.i("Salt", "Salt for " + i + ": " + savedUserMaxSalt.get(i));
            edit.putInt("savedUserCurrentSalt" + i, savedUserCurrentSalt.get(i));
            edit.putBoolean("savedUserIsNearSighted" + i, savedUserIsNearSighted.get(i));
            edit.putBoolean("savedUserWantsSuggestions" + i, savedUserWantsSuggestions.get(i));
        }

        saveLastUsedDetails(edit);

        edit.commit();

        Log.i("Scanner Buggery", "SetupPatron OnStop");
    }

    private void loadSavedUserData()
    {
        savedData = this.getApplicationContext().getSharedPreferences("AllergyWatcher SavedData", Context.MODE_PRIVATE);

        savedUserAllergies = new ArrayList<String>();
        savedUsersIDs = new ArrayList<String>();
        savedUserMaxSalt = new ArrayList<Integer>();
        savedUserCurrentSalt = new ArrayList<Integer>();
        savedUserIsNearSighted = new ArrayList<Boolean>();
        savedUserWantsSuggestions = new ArrayList<Boolean>();
        savedTotalNumberOfUsers = savedData.getInt("savedTotal", 0);

        for(int i = 0; i < savedTotalNumberOfUsers; i++)
        {
            savedUsersIDs.add(savedData.getString("savedUserID" + i, "Error"));
            savedUserAllergies.add(savedData.getString("savedUserAllergies" + i, "Error"));
            savedUserMaxSalt.add(savedData.getInt("savedUserMaxSalt" + i, 0));
            savedUserCurrentSalt.add(savedData.getInt("savedUserCurrentSalt" + i, 0));
            savedUserIsNearSighted.add(savedData.getBoolean("savedUserIsNearSighted" + i, false));
            savedUserWantsSuggestions.add(savedData.getBoolean("savedUserWantsSuggestions" + i, false));
        }
    }

    private void addPatron()
    {
        if(!currentUID.matches(""))
        {
            int i  = 0;
            boolean matchFound = false;
            //if user already exists
            for (String aUID: savedUsersIDs)
            {
                if(aUID.matches(currentUID))
                {
                    Log.i("Setup Patron", "Replacing old local patron " + i);
                    savedUsersIDs.set(i, scannedCardUIDText.getText().toString());

                    String allergens = "";
                    for (CheckBox anAllergen: allergenRadioButtons)
                    {
                        if(anAllergen.isChecked())
                        {
                            if(allergens.matches("")) //if first allergen added to list, do not precede with a comma
                            {
                                allergens += anAllergen.getText().toString();
                            }
                            else
                            {
                                allergens += "," + anAllergen.getText().toString();
                            }
                        }
                    }


                    savedUserAllergies.set(i, allergens);
                    savedUserMaxSalt.set(i, Integer.parseInt(patronMaxSaltEditText.getText().toString()));
                    Log.i("Salt", "Saving Salt: " + patronMaxSaltEditText.getText().toString() + " : " + i + " : " + Integer.parseInt(patronMaxSaltEditText.getText().toString()));
                    savedUserCurrentSalt.set(i, 0);

                    savedUserIsNearSighted.set(i, nearSightedCheckbox.isChecked());
                    savedUserWantsSuggestions.set(i, suggestionsCheckbox.isChecked());


                    matchFound = true;
                    break;
                }
                i++;
            }

            if(!matchFound)
            {
                Log.i("Setup Patron", "Creating new local patron");
                savedTotalNumberOfUsers++;
                //savedNames.add(patronNameEditText.getText().toString());
                savedUsersIDs.add(scannedCardUIDText.getText().toString());

                String allergens = "";
                for (CheckBox anAllergen: allergenRadioButtons)
                {
                    if(anAllergen.isChecked())
                    {
                        if(allergens.matches("")) //if first allergen added to list, do not precede with a comma
                        {
                            allergens += anAllergen.getText().toString();
                        }
                        else
                        {
                            allergens += "," + anAllergen.getText().toString();
                        }
                    }
                }


                savedUserAllergies.add(allergens);
                savedUserMaxSalt.add(Integer.parseInt(patronMaxSaltEditText.getText().toString()));
                Log.i("Salt", "Adding Salt: " + patronMaxSaltEditText.getText().toString() + " : " + Integer.parseInt(patronMaxSaltEditText.getText().toString()));
                savedUserCurrentSalt.add(0);

                savedUserIsNearSighted.add(nearSightedCheckbox.isChecked());
                savedUserWantsSuggestions.add(suggestionsCheckbox.isChecked());

            }
        }
    }

    private void loadLastUserDetails()
    {
        Log.i("Setup Patron", "Loading User Details");
        //patronNameEditText.setText(savedData.getString("lastUsedPatronName", "Error"));
        //preferedDrinksEditText.setText(savedData.getString("lastUsedPatronDrinks", "Error"));
        scannedCardUIDText.setText(savedData.getString("lastUsedPatronIDs", "Error"));
        patronMaxSaltEditText.setText("" + savedData.getInt("lastUsedPatronSalt", 0));
        currentUID = savedData.getString("lastUsedPatronIDs", "Error");

        //patronDrinksCountText.setText("Drinks Consumed: " + savedData.getInt("patronDrinksCount" +getPatronIndexFromUID(scannedCardUIDText.getText().toString()), 0));
        //patronBalanceText.setText("Balance: " + savedData.getFloat("savedBalance" + getPatronIndexFromUID(scannedCardUIDText.getText().toString()), 0.00f) + "€");
        //currentBalance = savedData.getFloat("savedBalance" + getPatronIndexFromUID(scannedCardUIDText.getText().toString()), 0.00f);
    }

    private void saveLastUsedDetails(SharedPreferences.Editor edit)
    {
        //edit.putString("lastUsedPatronName", patronNameEditText.getText().toString());
        //edit.putString("lastUsedPatronDrinks", preferedDrinksEditText.getText().toString());
        edit.putString("lastUsedPatronIDs", scannedCardUIDText.getText().toString());
        edit.putInt("lastUsedPatronSalt", Integer.parseInt(patronMaxSaltEditText.getText().toString()));
    }

    private int getPatronIndexFromUID(String inUID)
    {
        int i = 0;
        for (String aUID: savedUsersIDs)
        {
            if(inUID.matches(aUID))
            {
                return i;
            }
            i++;
        }
        return -1; //returns -1 if UID not found to match any, stored in shared preferences.
    }

    //+++[TileScanner Code]
    private void setupTileScanner()
    {
        dialog = new ProgressDialog(SetupPatronActivity.this);
        //Set processing bar style(round,revolving)
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //Set a Button for ProgressDialog
        dialog.setButton("Cancel", new ProgressDialog.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceManager.requestDisConnectDevice();
            }
        });
        //set if the processing bar of ProgressDialog is indeterminate
        dialog.setIndeterminate(false);

        //Initial device operation classes
        mScanner = new Scanner(SetupPatronActivity.this, scannerCallback);
        deviceManager = new DeviceManager(SetupPatronActivity.this);
        deviceManager.setCallBack(deviceManagerCallback);


        tileReaderTimer = new Timer();
        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = false;

        //connectToTileScanner is called from onResume
        //connectToTileScanner();
    }

    //Scanner CallBack
    private ScannerCallback scannerCallback = new ScannerCallback() {
        @Override
        public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            super.onReceiveScanDevice(device, rssi, scanRecord);
            System.out.println("Activity found a device：" + device.getName() + "Signal strength：" + rssi );
            //Scan bluetooth and record the one has the highest signal strength
            if ( (device.getName() != null) && (device.getName().contains("UNISMES") || device.getName().contains("BLE_NFC")) ) {
                if (mNearestBle != null) {
                    if (rssi > lastRssi) {
                        mNearestBle = device;
                    }
                }
                else {
                    mNearestBle = device;
                    lastRssi = rssi;
                }
            }
        }

        @Override
        public void onScanDeviceStopped() {
            super.onScanDeviceStopped();
        }
    };

    //Callback function for device manager
    private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback()
    {
        @Override
        public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
            super.onReceiveConnectBtDevice(blnIsConnectSuc);
            if (blnIsConnectSuc) {
                Log.i("TileScanner", "Activity Connection successful");
                Log.i("TileScanner", "Connection successful!\r\n");
                Log.i("TileScanner", "SDK version：" + deviceManager.SDK_VERSIONS + "\r\n");

                // Send order after 500ms delay
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(3);
            }
        }

        @Override
        public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
            super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
            Log.i("TileScanner", "Activity Unlink");
            Log.i("TileScanner", "Unlink!");
            handler.sendEmptyMessage(5);
        }

        @Override
        public void onReceiveConnectionStatus(boolean blnIsConnection) {
            super.onReceiveConnectionStatus(blnIsConnection);
            System.out.println("Activity Callback for Connection Status");
        }

        @Override
        public void onReceiveInitCiphy(boolean blnIsInitSuc) {
            super.onReceiveInitCiphy(blnIsInitSuc);
        }

        @Override
        public void onReceiveDeviceAuth(byte[] authData) {
            super.onReceiveDeviceAuth(authData);
        }

        @Override
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS)
        {
            super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
            if (!blnIsSus)
            {
                return;
            }
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < bytCardSn.length; i++)
            {
                stringBuffer.append(String.format("%02x", bytCardSn[i]));
            }

            StringBuffer stringBuffer1 = new StringBuffer();
            for (int i = 0; i < bytCarATS.length; i++)
            {
                stringBuffer1.append(String.format("%02x", bytCarATS[i]));
            }

            final StringBuffer outUID = stringBuffer;
            if (hasSufferedAtLeastOneFailureToReadUID)
            {
                uidIsFound = true;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.i("TileScanner", "callback received: UID = " + outUID.toString());

                        currentUID = outUID.toString();
                        Log.e("Setup Patron", "scanned UID: " + currentUID);


                        scannedCardUIDText.setText(currentUID);

                        int j = getPatronIndexFromUID(currentUID);
                        if(j != -1)
                        {
                            //patronNameEditText.setText(savedNames.get(j));
                            //currentBalance = savedData.getFloat("savedBalance" + getPatronIndexFromUID(currentUID), 0.00f);
                            //patronBalanceText.setText("Balance: " + currentBalance + "€");
                            for (String anAllergen: savedUserAllergies.get(j).split(","))
                            {
                                for (CheckBox aButton: allergenRadioButtons)
                                {
                                    if(aButton.getText().toString().matches(anAllergen))
                                    {
                                        aButton.setChecked(true);
                                    }
                                }
                            }
                            nearSightedCheckbox.setChecked(savedUserIsNearSighted.get(j));
                            suggestionsCheckbox.setChecked(savedUserWantsSuggestions.get(j));
                            patronMaxSaltEditText.setText("" + savedUserMaxSalt.get(j));
                        }
                        else
                        {
                            //patronNameEditText.setText("-Enter Name-");

                            for (CheckBox aButton: allergenRadioButtons)
                            {
                                aButton.setChecked(false);
                            }
                        }
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.i("TileScanner", "UID found without a prior failure, assuming its a tag left on the scanner");
                    }
                });

            }

            Log.i("TileScanner","Activity Activate card callback received：UID->" + stringBuffer + " ATS->" + stringBuffer1);
        }

        @Override
        public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
            super.onReceiveRfmSentApduCmd(bytApduRtnData);

            StringBuffer stringBuffer = new StringBuffer();
            for (int i=0; i<bytApduRtnData.length; i++) {
                stringBuffer.append(String.format("%02x", bytApduRtnData[i]));
            }
            Log.i("TileScanner", "Activity APDU callback received：" + stringBuffer);
        }

        @Override
        public void onReceiveRfmClose(boolean blnIsCloseSuc) {
            super.onReceiveRfmClose(blnIsCloseSuc);
        }
    };


    private void connectToTileScanner()
    {
        if (deviceManager.isConnection()) {
            deviceManager.requestDisConnectDevice();
            return;
        }
        Log.i("TileScanner", "connect To Update: Searching Devices");
        //handler.sendEmptyMessage(0);
        if (!mScanner.isScanning()) {
            mScanner.startScan(0);
            mNearestBle = null;
            lastRssi = -100;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    int searchCnt = 0;
                    while ((mNearestBle == null) && (searchCnt < 50000) && (mScanner.isScanning())) {
                        searchCnt++;
                        try {
                            //Log.i("TileScanner", "connect to Update: Sleeping Thread while scanning");
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Log.i("TileScanner", "connect to Update: Sleeping Thread after scan comeplete");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //mScanner.stopScan();
                    if (mNearestBle != null && !deviceManager.isConnection()) {
                        mScanner.stopScan();
                        Log.i("TileScanner", "connect To Update: Connecting to Device");
                        handler.sendEmptyMessage(0);
                        deviceManager.requestConnectBleDevice(mNearestBle.getAddress());
                    }
                    else {
                        Log.i("TileScanner", "connect To Update: Cannot Find Devices");
                        handler.sendEmptyMessage(0);
                    }
                }
            }).start();
        }
    }

    //Read card Demo
    private void readCardDemo() {
        readCardCnt++;
        Log.i("TileScanner", "Activity Send scan/activate order");
        deviceManager.requestRfmSearchCard((byte) 0x00, new DeviceManager.onReceiveRfnSearchCardListener() {
            @Override
            public void onReceiveRfnSearchCard(final boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
                deviceManager.mOnReceiveRfnSearchCardListener = null;
                if ( !blnIsSus ) {
                    Log.i("TileScanner", "No card is found！Please put ShenZhen pass on the bluetooth card reading area first");
                    handler.sendEmptyMessage(0);
                    Log.i("TileScanner", "No card is found！");
                    hasSufferedAtLeastOneFailureToReadUID = true;
                    readCardFailCnt++;

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //alertDataText.setText("No card detected");
                        }
                    });
                    //handler.sendEmptyMessage(4);
                    return;
                }
                if ( cardType == DeviceManager.CARD_TYPE_ISO4443_B ) {   //Find ISO14443-B card（identity card）
                    final Iso14443bCard card = (Iso14443bCard)deviceManager.getCard();
                    if (card != null) {

                        Log.i("TileScanner", "found ISO14443-B card->UID:(Identity card send 0036000008 order to get UID)\r\n");
                        handler.sendEmptyMessage(0);
                        //Order stream to get Identity card DN code
                        final byte[][] sfzCmdBytes = {
                                {0x00, (byte)0xa4, 0x00, 0x00, 0x02, 0x60, 0x02},
                                {0x00, 0x36, 0x00, 0x00, 0x08},
                                {(byte)0x80, (byte)0xB0, 0x00, 0x00, 0x20},
                        };
                        System.out.println("Send order stream");
                        Handler readSfzHandler = new Handler(SetupPatronActivity.this.getMainLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                final Handler theHandler = msg.getTarget();
                                if (msg.what < sfzCmdBytes.length) {  // Execute order stream recurrently
                                    final int index = msg.what;
                                    StringBuffer stringBuffer = new StringBuffer();
                                    for (int i=0; i<sfzCmdBytes[index].length; i++) {
                                        stringBuffer.append(String.format("%02x", sfzCmdBytes[index][i]));
                                    }
                                    Log.i("TileScanner", "Send：" + stringBuffer + "\r\n");
                                    handler.sendEmptyMessage(0);
                                    card.bpduExchange(sfzCmdBytes[index], new Iso14443bCard.onReceiveBpduExchangeListener() {
                                        @Override
                                        public void onReceiveBpduExchange(boolean isCmdRunSuc, byte[] bytBpduRtnData) {
                                            if (!isCmdRunSuc) {
                                                card.close(null);
                                                return;
                                            }
                                            StringBuffer stringBuffer = new StringBuffer();
                                            for (int i=0; i<bytBpduRtnData.length; i++) {
                                                stringBuffer.append(String.format("%02x", bytBpduRtnData[i]));
                                            }
                                            Log.i("TileScanner", "Return：" + stringBuffer + "\r\n");
                                            handler.sendEmptyMessage(0);
                                            theHandler.sendEmptyMessage(index + 1);
                                        }
                                    });
                                }
                                else{ //Order stream has been excuted,shut antenna down
                                    card.close(null);
                                    handler.sendEmptyMessage(4);
                                }
                            }
                        };
                        readSfzHandler.sendEmptyMessage(0);  //Start to execute the first order
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_ISO4443_A){  //Find ACPU card
                    Log.i("TileScanner", "Card activation status：" + blnIsSus);
                    Log.i("TileScanner", "Send APDU order - Select main file");

                    final CpuCard card = (CpuCard)deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "Found CPU card->UID:" + card.uidToString() + "\r\n");
                        handler.sendEmptyMessage(0);
                        card.apduExchange(SZTCard.getSelectMainFileCmdByte(), new CpuCard.onReceiveApduExchangeListener() {
                            @Override
                            public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                                if (!isCmdRunSuc) {
                                    Log.i("TileScanner", "Main file selection failed");
                                    card.close(null);
                                    readCardFailCnt++;
                                    handler.sendEmptyMessage(4);
                                    return;
                                }
                                Log.i("TileScanner", "Send APDU order- read balance");
                                card.apduExchange(SZTCard.getBalanceCmdByte(), new CpuCard.onReceiveApduExchangeListener() {
                                    @Override
                                    public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                                        if (SZTCard.getBalance(bytApduRtnData) == null) {
                                            Log.i("TileScanner", "This is not ShenZhen Pass！");
                                            handler.sendEmptyMessage(0);
                                            Log.i("TileScanner", "This is not ShenZhen Pass！");
                                            card.close(null);
                                            readCardFailCnt++;
                                            handler.sendEmptyMessage(4);
                                            return;
                                        }
                                        Log.i("TileScanner", "ShenZhen Pass balance：" + SZTCard.getBalance(bytApduRtnData));
                                        handler.sendEmptyMessage(0);
                                        System.out.println("Balance：" + SZTCard.getBalance(bytApduRtnData));
                                        System.out.println("Send APDU order -read 10 trading records");
                                        Handler readSztHandler = new Handler(SetupPatronActivity.this.getMainLooper()) {
                                            @Override
                                            public void handleMessage(Message msg) {
                                                final Handler theHandler = msg.getTarget();
                                                if (msg.what <= 10) {  //Read 10 trading records recurrently
                                                    final int index = msg.what;
                                                    card.apduExchange(SZTCard.getTradeCmdByte((byte) msg.what), new CpuCard.onReceiveApduExchangeListener() {
                                                        @Override
                                                        public void onReceiveApduExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                                                            if (!isCmdRunSuc) {
                                                                card.close(null);
                                                                readCardFailCnt++;
                                                                handler.sendEmptyMessage(4);
                                                                return;
                                                            }
                                                            Log.i("TileScanner", "\r\n" + SZTCard.getTrade(bytApduRtnData));
                                                            handler.sendEmptyMessage(0);
                                                            theHandler.sendEmptyMessage(index + 1);
                                                        }
                                                    });
                                                }
                                                else if (msg.what == 11){ //Shut antenna down
                                                    card.close(null);
                                                    handler.sendEmptyMessage(4);
                                                }
                                            }
                                        };
                                        readSztHandler.sendEmptyMessage(1);
                                    }
                                });
                            }
                        });
                    }
                    else {
                        readCardFailCnt++;
                        handler.sendEmptyMessage(4);
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_FELICA) { //find Felica card
                    FeliCa card = (FeliCa) deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "Read data block 0000 who serves 008b：\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] pServiceList = {(byte) 0x8b, 0x00};
                        byte[] pBlockList = {0x00, 0x00, 0x00};
                        card.read((byte) 1, pServiceList, (byte) 1, pBlockList, new FeliCa.onReceiveReadListener() {
                            @Override
                            public void onReceiveRead(boolean isSuc, byte pRxNumBlocks, byte[] pBlockData) {
                                if (isSuc) {
                                    StringBuffer stringBuffer = new StringBuffer();
                                    for (int i = 0; i < pBlockData.length; i++) {
                                        stringBuffer.append(String.format("%02x", pBlockData[i]));
                                    }
                                    Log.i("TileScanner", stringBuffer + "\r\n");
                                    handler.sendEmptyMessage(0);
                                }
                                else {
                                    Log.i("TileScanner", "\r\n READing FeliCa FAILED");
                                    handler.sendEmptyMessage(0);
                                }
                            }
                        });

//                        card.write((byte) 1, pServiceList, (byte) 1, pBlockList, new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x18, 0x19, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55}, new FeliCa.onReceiveWriteListener() {
//                            @Override
//                            public void onReceiveWrite(boolean isSuc, byte[] returnBytes) {
//                                msgBuffer.append("" + isSuc + returnBytes);
//                                handler.sendEmptyMessage(0);
//                            }
//                        });
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_ULTRALIGHT) { //find Ultralight卡
                    final Ntag21x card  = (Ntag21x) deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "find Ultralight card ->UID:" + card.uidToString() + "\r\n");
                        Log.i("TileScanner", "Read tag NDEFText\r\n");
                        handler.sendEmptyMessage(0);

                        card.NdefTextRead(new Ntag21x.onReceiveNdefTextReadListener() {
                            @Override
                            public void onReceiveNdefTextRead(String eer, String returnString) {
                                if (returnString != null) {
                                    Log.i("TileScanner", "read NDEFText successfully：\r\n" + returnString);
                                }
                                if (eer != null) {
                                    Log.i("TileScanner", "reading NDEFText failed：" + eer);
                                }
                                handler.sendEmptyMessage(0);
                                card.close(null);
                            }
                        });
                    }
                }
                else if (cardType == DeviceManager.CARD_TYPE_MIFARE) {
                    final Mifare card = (Mifare)deviceManager.getCard();
                    if (card != null) {
                        Log.i("TileScanner", "Found Mifare card->UID:" + card.uidToString() + "\r\n");
                        Log.i("TileScanner", "Start to verify the first password block\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] key = {(byte) 0xff, (byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,};
                        card.authenticate((byte) 1, Mifare.MIFARE_KEY_TYPE_A, key, new Mifare.onReceiveAuthenticateListener() {
                            @Override
                            public void onReceiveAuthenticate(boolean isSuc) {
                                if (!isSuc) {
                                    Log.i("TileScanner", "Verifying password failed\r\n");
                                    handler.sendEmptyMessage(0);
                                }
                                else {
                                    Log.i("TileScanner", "Verify password successfully\r\n");

                                    Log.i("TileScanner", "Charge e-Wallet block 1 1000 Chinese yuan\r\n");
                                    handler.sendEmptyMessage(0);
                                    card.decrementTransfer((byte) 1, (byte) 1, card.getValueBytes(1000), new Mifare.onReceiveDecrementTransferListener() {
                                        @Override
                                        public void onReceiveDecrementTransfer(boolean isSuc) {
                                            if (!isSuc) {
                                                Log.i("TileScanner", "e-Walle is not initialized!\r\n");
                                                handler.sendEmptyMessage(0);
                                                card.close(null);
                                            }
                                            else {
                                                Log.i("TileScanner", "Charge successfully！\r\n");
                                                handler.sendEmptyMessage(0);
                                                card.readValue((byte) 1, new Mifare.onReceiveReadValueListener() {
                                                    @Override
                                                    public void onReceiveReadValue(boolean isSuc, byte address, byte[] valueBytes) {
                                                        if (!isSuc || (valueBytes == null) || (valueBytes.length != 4)) {
                                                            Log.i("TileScanner", "Reading e-Wallet balance failed！\r\n");
                                                            handler.sendEmptyMessage(0);
                                                            card.close(null);
                                                        }
                                                        else {
                                                            int value = card.getValue(valueBytes);
                                                            Log.i("TileScanner", "e-Wallet balance is：" + (value & 0x0ffffffffl) + "\r\n");
                                                            handler.sendEmptyMessage(0);
                                                            card.close(null);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });

//                                    //Increase value
//                                    card.incrementTransfer((byte) 1, (byte) 1, card.getValueBytes(1000), new Mifare.onReceiveIncrementTransferListener() {
//                                        @Override
//                                        public void onReceiveIncrementTransfer(boolean isSuc) {
//                                            if (!isSuc) {
//                                                msgBuffer.append("e-Walle is not initialized!\r\n");
//                                                handler.sendEmptyMessage(0);
//                                                card.close(null);
//                                            }
//                                            else {
//                                                msgBuffer.append("Charge successfully！\r\n");
//                                                handler.sendEmptyMessage(0);
//                                                card.readValue((byte) 1, new Mifare.onReceiveReadValueListener() {
//                                                    @Override
//                                                    public void onReceiveReadValue(boolean isSuc, byte address, byte[] valueBytes) {
//                                                        if (!isSuc || (valueBytes == null) || (valueBytes.length != 4)) {
//                                                            msgBuffer.append("Reading e-Wallet balance failed！\r\n");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                        }
//                                                        else {
//                                                            int value = card.getValue(valueBytes);
//                                                            msgBuffer.append("e-Wallet balance is：" + (value & 0x0ffffffffl) + "\r\n");
//                                                            handler.sendEmptyMessage(0);
//                                                            card.close(null);
//                                                        }
//                                                    }
//                                                });
//                                            }
//                                        }
//                                    });

//                                    //Test read and write block
//                                    msgBuffer.append("write 00112233445566778899001122334455 to block 1\r\n");
//                                    handler.sendEmptyMessage(0);
//                                    card.write((byte) 1, new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55}, new Mifare.onReceiveWriteListener() {
//                                        @Override
//                                        public void onReceiveWrite(boolean isSuc) {
//                                            if (isSuc) {
//                                                msgBuffer.append("Write successfully！\r\n");
//                                                msgBuffer.append("read data from block 1\r\n");
//                                                handler.sendEmptyMessage(0);
//                                                card.read((byte) 1, new Mifare.onReceiveReadListener() {
//                                                    @Override
//                                                    public void onReceiveRead(boolean isSuc, byte[] returnBytes) {
//                                                        if (!isSuc) {
//                                                            msgBuffer.append("reading data from block 1 failed！\r\n");
//                                                            handler.sendEmptyMessage(0);
//                                                        }
//                                                        else {
//                                                            StringBuffer stringBuffer = new StringBuffer();
//                                                            for (int i=0; i<returnBytes.length; i++) {
//                                                                stringBuffer.append(String.format("%02x", returnBytes[i]));
//                                                            }
//                                                            msgBuffer.append("Block 1 data:\r\n" + stringBuffer);
//                                                            handler.sendEmptyMessage(0);
//                                                        }
//                                                        card.close(null);
//                                                    }
//                                                });
//                                            }
//                                            else {
//                                                msgBuffer.append("Write fails！\r\n");
//                                                handler.sendEmptyMessage(0);
//                                            }
//                                        }
//                                    });
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            getMsgFlag = true;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy MM dd HH:mm:ss ");
            Date curDate = new Date(System.currentTimeMillis());//Get current time
            String str = formatter.format(curDate);

            if (deviceManager.isConnection()) {
                Log.i("TileScanner", "Ble is connected");
            }
            else {
                Log.i("TileScanner", "Search device");
            }

            if (msg.what == 1) {
                dialog.show();
            }
            else if (msg.what == 2) {
                dialog.dismiss();
            }
            else if (msg.what == 3) {
                handler.sendEmptyMessage(4);
//                deviceManager.requestVersionsDevice(new DeviceManager.onReceiveVersionsDeviceListener() {
//                    @Override
//                    public void onReceiveVersionsDevice(byte versions) {
//                        msgBuffer.append("Device version:" + String.format("%02x", versions) + "\r\n");
//                        handler.sendEmptyMessage(0);
//                        deviceManager.requestBatteryVoltageDevice(new DeviceManager.onReceiveBatteryVoltageDeviceListener() {
//                            @Override
//                            public void onReceiveBatteryVoltageDevice(double voltage) {
//                                msgBuffer.append("Device battery voltage:" + String.format("%.2f", voltage) + "\r\n");
//                                if (voltage < 3.4) {
//                                    msgBuffer.append("Device has low battery, please charge！");
//                                }
//                                else {
//                                    msgBuffer.append("Device has enough battery！");
//                                }
//                                handler.sendEmptyMessage(4);
//                            }
//                        });
//                    }
//                });
            }
            else if (msg.what == 4) {
                if (deviceManager.isConnection()) {
                    getMsgFlag = false;

                    Log.i("TileScanner", "Stuff is happening");

                    scheduleCallForUID();

                    /*readCardDemo();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (getMsgFlag == false) {
                                handler.sendEmptyMessage(4);


                            }
                        }
                    }).start();*/
                }
            }
            else if (msg.what == 5) {
                disconnectCnt++;
                //searchButton.performClick();
                if(!stopAllScans)
                {
                    connectToTileScanner();
                }
            }
        }
    };

    //Recursive Method that schedules a call to the TileScanner to read the card currently on the scanner and return the UID. Then the method calls itself, creating a periodic call to the TileScanner.
    //  ceases calling itself if a UID has already been received and then calls scheduleRestartOfCallForUID().
    private void scheduleCallForUID()
    {
        try
        {
            Log.i("TileScanner", " scheduling the next cycle of the call for uid loop");
            tileReaderTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (!uidIsFound)
                    {
                        Log.i("TileScanner", " running the next cycle of the call for uid loop");
                        readCardDemo();
                        scheduleCallForUID();
                    }
                    else
                    {
                        scheduleRestartOfCallForUID();
                    }
                }
            }, 2000);
        }
        catch (IllegalStateException e)
        {
            Log.e("TileScanner", "Timer has been canceled, aborting the call for uid loop");

            if(deviceManager.isConnection())
            {
                stopAllScans = true;
                deviceManager.requestDisConnectDevice();
            }

            if(mScanner.isScanning())
            {
                mScanner.stopScan();
            }
        }
    }

    //Schedules a task to restart the recursive method scheduleCallForUID (and thus the periodic calling to the TileScanner to read the card) after a short delay.
    private void scheduleRestartOfCallForUID()
    {
        Log.i("TileScanner", " scheduling the restart of the call for uid loop");
        tileReaderTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if(uidIsFound)
                {
                    uidIsFound = false;
                    hasSufferedAtLeastOneFailureToReadUID = false; //is used to check if the previously read card was left on the scanner, preventing false positive readings.
                    // If the card was removed then the scanner will report a failure to read.
                    Log.i("TileScanner", " restarting the call for uid loop");
                    scheduleCallForUID();
                }
            }
        }, 3000);
    }

//+++[/TileScanner Code]


}
