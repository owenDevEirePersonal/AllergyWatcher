package com.deveire.dev.allergywatcher;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.acs.bluetooth.BluetoothReaderGattCallback;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class OrderFoodActivity extends AppCompatActivity implements DownloadCallback<String>, RecognitionListener
{


    final static int PAIR_READER_REQUESTCODE = 9;

    private SharedPreferences savedData;

    private ImageView foodImage;
    private ImageView alertImage;
    private TextView alertText;
    private TextView alertBigText;

    private boolean hasState;

    private Timer resetImageTimer;
    private TimerTask resetImageTimerTask;

    private int savedTotalNumberOfUsers;
    private ArrayList<String> savedUsersIDs;
    private ArrayList<String> savedUserAllergies;
    private ArrayList<Integer> savedUserMaxSalt;
    private ArrayList<Integer> savedUserCurrentSalt;
    private ArrayList<Boolean> savedUserIsNearSighted;
    private ArrayList<Boolean> savedUserWantsSuggestions;

    private String[] currentOrderedFoodAllergies;
    private int currentOrderedFoodSalt;
    private String currentOrderedFoodDinnerSuggestion;
    private String currentOrderedFoodTextDescription;


    private TextToSpeech toSpeech;
    private String speechInText;
    private HashMap<String, String> endOfSpeakIndentifier;
    private final String textToSpeechID_Order = "Order";
    private final String textToSpeechID_Confirmation = "Confirmation";
    private final String textToSpeechID_Nothing = "Nothing";
    private final String textToSpeechID_Clarification = "Clarification";
    private final String textToSpeechID_Allergy = "Allergy";
    private final String textToSpeechID_Suggestion = "Suggestion";
    private final String textToSpeechID_OrderDespiteSalt = "OrderWithSalt";


    private SpeechRecognizer recog;
    private Intent recogIntent;
    private int pingingRecogFor;
    private int previousPingingRecogFor;
    private final int pingingRecogFor_Order = 1;
    private final int pingingRecogFor_Confirmation = 2;
    private final int pingingRecogFor_Clarification = 3;
    private final int pingingRecogFor_Suggestion = 4;
    private final int pingingRecogFor_OrderDespiteSalt = 5;
    private final int pingingRecogFor_Nothing = -1;

    private String[] currentPossiblePhrasesNeedingClarification;


    //[BLE Variables]
    private String storedScannerAddress;
    private final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"; //UUID for changing notify or not
    private int REQUEST_ENABLE_BT;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothAdapter.LeScanCallback leScanCallback;

    private BluetoothDevice btDevice;

    private BluetoothGatt btGatt;
    private BluetoothReaderGattCallback btLeGattCallback;
    //[/BLE Variables]

    //[Retreive Alert Data Variables]
    private Boolean pingingServerFor_alertData;
    //private TextView alertDataText;
    private String currentUID;
    private String currentStationID;
    //[/Retreive Alert Data Variables]


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

    private Timer resumeLoadSavedDataConnectionDelayer;

    //[/Tile Reader Variables]

    //[Headset Variables]
    /*private ArrayList<String> allHeadsetMacAddresses;
    private BluetoothDevice currentHeadsetDevice;

    private Timer headsetTimer;

    private BluetoothA2dp currentHeadsetProfile;
    private Method connectMethod;*/
    //[/Headset Variables]

    /*[Bar Reader Variables]
    private String barReaderInput;
    private Boolean barReaderInputInProgress;
    private Timer barReaderTimer;



    //[/Bar Reader Variables] */

    //[Scanner Variables]

    /* Default master key. */
    /*private static final String DEFAULT_1255_MASTER_KEY = "ACR1255U-J1 Auth";

    private static final byte[] AUTO_POLLING_START = { (byte) 0xE0, 0x00, 0x00, 0x40, 0x01 };
    private static final byte[] AUTO_POLLING_STOP = { (byte) 0xE0, 0x00, 0x00, 0x40, 0x00 };
    private static final byte[] GET_UID_APDU_COMMAND = {(byte)0xFF , (byte)0xCA, (byte)0x00, (byte)0x00, (byte)0x00};

    private int scannerConnectionState = BluetoothReader.STATE_DISCONNECTED;
    private BluetoothReaderManager scannerManager;
    private BluetoothReader scannerReader;

    private Timer scannerTimer;

    private static final int MAX_AUTHENTICATION_ATTEMPTS_BEFORE_TIMEOUT = 20;
    private boolean scannerIsAuthenticated;*/

    //[/Scanner Variables]

    //[Network and periodic location update, Variables]
    private boolean pingingServer;
    private final String serverIPAddress = "http://192.168.1.188:8080/InstructaConServlet/ICServlet";
    //private final String serverIPAddress = "http://api.eirpin.com/api/TTServlet";
    private String serverURL;
    private NetworkFragment aNetworkFragment;
    //[/Network and periodic location update, Variables]


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_food);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        foodImage = (ImageView) findViewById(R.id.foodImageView);
        alertImage = (ImageView) findViewById(R.id.alertImageView);
        alertText = (TextView) findViewById(R.id.alertText);
        alertBigText = (TextView) findViewById(R.id.alertBigText);

        resetImageTimer = new Timer("ResetImageTimer");
        resetImageTimerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        foodImage.setImageResource(R.drawable.menu_ad);
                    }
                });

            }
        };

        /*scanKegButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //scanKeg();
                //Log.i("Scanner Connection", "current card status = " + currentCardStatus);
                //transmitApdu();
            }
        });*/

        /*pairReaderButton = (Button) findViewById(R.id.pairReaderButton);
        pairReaderButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                storedScannerAddress = null;
                Intent pairReaderIntent = new Intent(getApplicationContext(), PairingActivity.class);
                startActivityForResult(pairReaderIntent, PAIR_READER_REQUESTCODE);
                /*if(btAdapter != null)
                {
                    btAdapter.startLeScan(leScanCallback);
                }*
            }
        });*/

        hasState = true;


        loadSavedUserData();

        //balanceText.setText("Balance: " + savedBalance);


        pingingServer = false;

        //aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "https://192.168.1.188:8080/smrttrackerserver-1.0.0-SNAPSHOT/hello?isDoomed=yes");
        serverURL = serverIPAddress + "?request=storelocation" + Settings.Secure.ANDROID_ID.toString() + "&name=" + "&lat=" + 0000 + "&lon=" + 0000;



        pingingServerFor_alertData = false;
        //alertDataText = (TextView) findViewById(R.id.mapText);

        toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status)
            {
                Log.i("Text To Speech Update", "onInit Complete");
                toSpeech.setLanguage(Locale.ENGLISH);
                endOfSpeakIndentifier = new HashMap();
                endOfSpeakIndentifier.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "endOfSpeech");
                toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener()
                {
                    @Override
                    public void onStart(String utteranceId)
                    {
                        Log.i("Text To Speech Update", "onStart called");
                    }

                    @Override
                    public void onDone(String utteranceId)
                    {
                        Log.i("Speech", utteranceId + " DONE!");
                        final String idOfUtterance = utteranceId;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                switch (idOfUtterance)
                                {
                                    case textToSpeechID_Order: recog.startListening(recogIntent); break;
                                    case textToSpeechID_Confirmation: recog.startListening(recogIntent); break;
                                    case textToSpeechID_Clarification: recog.startListening(recogIntent); break;
                                    case textToSpeechID_Suggestion: recog.startListening(recogIntent); break;
                                    case textToSpeechID_OrderDespiteSalt: recog.startListening(recogIntent); break;
                                    case textToSpeechID_Allergy:
                                        pingingRecogFor = pingingRecogFor_Order;
                                        recog.startListening(recogIntent);
                                        runOnUiThread(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                foodImage.setVisibility(View.VISIBLE);
                                            }
                                        });
                                        break;
                                }
                            }
                        });


                        //toSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId)
                    {
                        Log.i("Text To Speech Update", "ERROR DETECTED");
                    }
                });
            }
        });


        currentUID = "";
        currentStationID = "bathroom1";



        //setupBluetoothScanner();
        /*
        barReaderTimer = new Timer();
        barReaderInput = "";
        barReaderInputInProgress = false;
        kegIDEditText.requestFocus();*/


        //setupHeadset();

        recog = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        recog.setRecognitionListener(this);
        recogIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
        recogIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        currentPossiblePhrasesNeedingClarification = new String[]{};


        Log.i("Scanner Buggery", "orderFoodActivity OnCreate.");
        setupTileScanner();

        restoreSavedValues(savedInstanceState);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        hasState = true;


        final IntentFilter intentFilter = new IntentFilter();

        Log.i("Scanner Buggery", "orderFoodActivity onResume resuming connection to scanner.");
        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = true;
        tileReaderTimer = new Timer();
        connectToTileScanner();





        //barReaderTimer = new Timer();



        /*
        /* Start to monitor bond state change /
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);

        setupBluetoothScanner();
        */
    }

    @Override
    protected void onPause()
    {
        Log.i("Pause", "onpause called");
        hasState = false;
        if(aNetworkFragment != null)
        {
            aNetworkFragment.cancelDownload();
        }

        tileReaderTimer.cancel();
        tileReaderTimer.purge();

        resetImageTimer.cancel();
        resetImageTimer.purge();


        //if scanner is connected, disconnect it
        if(deviceManager.isConnection())
        {
            Log.i("Scanner Buggery", "Driver OnPause is connected now disconnecting");
            stopAllScans = true;
            deviceManager.requestDisConnectDevice();
        }

        if(mScanner.isScanning())
        {
            Log.i("Scanner Buggery", "Driver OnPause is scanning now stopping scanning");
            mScanner.stopScan();
        }

        SharedPreferences.Editor edit = savedData.edit();


        edit.commit();

        recog.stopListening();
        recog.cancel();
        recog.destroy();


        //headsetTimer.cancel();
        //headsetTimer.purge();

        /*
        barReaderTimer.cancel();
        barReaderTimer.purge();
        */

        //[Scanner onPause]
        /*
        /* Stop to monitor bond state change /
        unregisterReceiver(mBroadcastReceiver);

        scannerIsAuthenticated = false;

        /* Disconnect Bluetooth reader /
        disconnectReader();

        scannerTimer.cancel();
        scannerTimer.purge();
        */
        //[/Scanner On pause]

        super.onPause();
        //finish();
    }

    @Override
    protected void onStop()
    {
        hasState = false;

        //if scanner is connected, disconnect it
        /*if(deviceManager.isConnection())
        {
            Log.i("Scanner Buggery", "Driver OnStop is connected now disconnecting");
            stopAllScans = true;
            deviceManager.requestDisConnectDevice();
        }

        if(mScanner.isScanning())
        {
            Log.i("Scanner Buggery", "Driver OnStop is scanning now stopping scanning");
            mScanner.stopScan();
        }*/

        SharedPreferences.Editor edit = savedData.edit();

        //edit.putString("ScannerMacAddress", storedScannerAddress);

        edit.commit();




        /*
        if(btGatt != null)
        {
            btGatt.disconnect();
            btGatt.close();
        }
        */

        super.onStop();
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

    //returns true if no allerigies found
    private boolean runAllergyCheck(String userUIDin, String[] currentFoodAllergens)
    {
        Log.i("Allergens", "Running Check on " + userUIDin);
        boolean userFound = false;
        int indexOfUser = 0;
        for (String aUserID: savedUsersIDs)
        {
            if(aUserID.matches(userUIDin))
            {
                Log.i("Allergens", "User Found");
                userFound = true;
                break;
            }
            indexOfUser++;
        }

        if(userFound)
        {
            ArrayList<String> currentUserAllergies = new ArrayList<String>(Arrays.asList(savedUserAllergies.get(indexOfUser).split(",")));

            for (String anAllergen : currentFoodAllergens)
            {
                Log.i("Allergens", "Current Food Allegen: " + anAllergen);
                for (String bAllergen : currentUserAllergies)
                {
                    Log.i("Allergens", "Current User Allegen: " + bAllergen);
                    if (anAllergen.matches(bAllergen))
                    {
                        Log.i("Allergens", "Allergy Match found: " + anAllergen + " : " + bAllergen);
                        foodImage.setVisibility(View.INVISIBLE);
                        alertImage.setImageResource(R.drawable.allergyalerticon);
                        alertBigText.setText("Allergen Alert");
                        alertText.setText("This item may contain " + anAllergen + ". Consuming it may cause an Allergic Reaction, please choose another item.");
                        speakAlert();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean runSaltCheck(String userUIDin, int currentFoodSalt)
    {
        Log.i("Salt", "Running Check on " + userUIDin);
        boolean userFound = false;
        int indexOfUser = 0;
        for (String aUserID: savedUsersIDs)
        {
            if(aUserID.matches(userUIDin))
            {
                Log.i("Allergens", "User Found");
                userFound = true;
                break;
            }
            indexOfUser++;
        }

        if(userFound)
        {
            if(currentFoodSalt + savedUserCurrentSalt.get(indexOfUser) > savedUserMaxSalt.get(indexOfUser))
            {
                Log.i("Allergens", "Excessive Salt: " + currentFoodSalt + " : " + currentFoodSalt + savedUserCurrentSalt.get(indexOfUser) + " :of: " + savedUserMaxSalt.get(indexOfUser));
                foodImage.setVisibility(View.INVISIBLE);
                alertImage.setImageResource(R.drawable.red_smiley);
                alertBigText.setText("Diet Alert");
                alertText.setText("Warning: This food contains more salt than your diet's salt budget will allow. Do you still wish to order this meal?");
                speakDietAlert();
                return false;
            }
        }
        return true;
    }

    private boolean runNearSightedCheck(String userUIDin)
    {
        Log.i("Salt", "Running Check on " + userUIDin);
        boolean userFound = false;
        int indexOfUser = 0;
        for (String aUserID: savedUsersIDs)
        {
            if(aUserID.matches(userUIDin))
            {
                Log.i("Allergens", "NearSighedCheck User Found");
                userFound = true;
                break;
            }
            indexOfUser++;
        }

        if(userFound)
        {
            if(savedUserIsNearSighted.get(indexOfUser))
            {
                Log.i("Allergens", "User is nearsighted, displying text description: " + currentOrderedFoodTextDescription);
                foodImage.setVisibility(View.INVISIBLE);
                alertImage.setImageResource(R.drawable.infoicon);
                alertBigText.setText(currentOrderedFoodTextDescription);
                alertText.setText("");

                return false;
            }
            Log.i("Allergens", "User is not nearsighted");
        }
        return true;
    }

    private void speakAlert()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            toSpeech.speak("ALLERGY ALERT!" + alertText.getText().toString(), TextToSpeech.QUEUE_ADD , null, textToSpeechID_Allergy);
            //toSpeech.speak("", TextToSpeech.QUEUE_ADD , null, "End");
        }
    }

    private void speakDietAlert()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            pingingRecogFor = pingingRecogFor_OrderDespiteSalt;
            toSpeech.speak("Diet Alert!" + alertText.getText().toString(), TextToSpeech.QUEUE_ADD , null, textToSpeechID_OrderDespiteSalt);
            //toSpeech.speak("", TextToSpeech.QUEUE_ADD , null, "End");
        }
    }



    private void retrieveAlerts(String stationIDin)
    {
        if(!stationIDin.matches(""))
        {
            serverURL = serverIPAddress + "?request=getalertsfor" + "&stationid=" + stationIDin;
            //lat and long are doubles, will cause issue? nope
            pingingServerFor_alertData = true;
            Log.i("Network Update", "Attempting to start download from retrieveAlerts. " + serverURL);
            aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
        }
        else
        {
            Log.e("Network Update", "Error in RetreiveAlters, invalid uuid entered");
        }
    }


    /*public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        Log.i("BarReader   ", "OnKeyUp Triggered");

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_0: barReaderInput += "0"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_1: barReaderInput += "1"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_2: barReaderInput += "2"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_3: barReaderInput += "3"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_4: barReaderInput += "4"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_5: barReaderInput += "5"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_6: barReaderInput += "6"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_7: barReaderInput += "7"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_8: barReaderInput += "8"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_9: barReaderInput += "9"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_Q: barReaderInput += "Q"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_W: barReaderInput += "W"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_E: barReaderInput += "E"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_R: barReaderInput += "R"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_T: barReaderInput += "T"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_Y: barReaderInput += "Y"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_U: barReaderInput += "U"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_I: barReaderInput += "I"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_O: barReaderInput += "O"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_P: barReaderInput += "P"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_A: barReaderInput += "A"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_S: barReaderInput += "S"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_D: barReaderInput += "D"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_F: barReaderInput += "F"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_G: barReaderInput += "G"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_H: barReaderInput += "H"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_J: barReaderInput += "J"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_K: barReaderInput += "K"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_L: barReaderInput += "L"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_Z: barReaderInput += "Z"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_X: barReaderInput += "X"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_C: barReaderInput += "C"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_V: barReaderInput += "V"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_B: barReaderInput += "B"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_N: barReaderInput += "N"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_M: barReaderInput += "M"; barScannerSheduleUpload(); Log.i("BarReader   ", "Current Input equals: " + barReaderInput); break;
            case KeyEvent.KEYCODE_BACK: Log.i("BarReader   ", "Current Input equals Back"); finish(); break;
            default: Log.i("BarReader   ", "Unidentified symbol: " + keyCode); break;
        }

        return true;
    }*/

    /*private void barScannerSheduleUpload()
    {
        int delay = 1500;
        if(!barReaderInputInProgress)
        {
            barReaderInputInProgress = true;
            barReaderTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    scanKeg(barReaderInput);
                    Log.i("BarReader   ", "Final Input equals: " + barReaderInput);
                    barReaderInputInProgress = false;
                    final String barReaderInputToRead = barReaderInput;
                    barReaderInput = "";
                    barReaderTimer.schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            Log.i("BarReader  ", "Launching Data Request");
                            retrieveKegData(barReaderInputToRead);
                        }
                    }, 2000);
                }
            }, delay);
        }
    }*/

    private void retrieveKegData(String kegIDin)
    {
        if(!kegIDin.matches(""))
        {
            kegIDin = kegIDin.replace(' ', '_');
            serverURL = serverIPAddress + "?request=getkegdata" + "&kegid=" + kegIDin;
            //lat and long are doubles, will cause issue? nope
            Log.i("Network Update", "Attempting to start download from retrieveKegData " + serverURL);
            pingingServerFor_alertData = true;
            aNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), serverURL);
        }
        else
        {
            Log.e("kegData Error", "invalid uuid entered. " + kegIDin);
        }
    }


    //++++++++[Recognition Listener Code]
    @Override
    public void onReadyForSpeech(Bundle bundle)
    {
        Log.e("Recog", "ReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech()
    {
        Log.e("Recog", "BeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float v)
    {
        Log.e("Recog", "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] bytes)
    {
        Log.e("Recog", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech()
    {
        Log.e("Recog", "End ofSpeech");
        recog.stopListening();
    }

    @Override
    public void onError(int i)
    {
        switch (i)
        {
            //case RecognizerIntent.RESULT_AUDIO_ERROR: Log.e("Recog", "RESULT AUDIO ERROR"); break;
            //case RecognizerIntent.RESULT_CLIENT_ERROR: Log.e("Recog", "RESULT CLIENT ERROR"); break;
            //case RecognizerIntent.RESULT_NETWORK_ERROR: Log.e("Recog", "RESULT NETWORK ERROR"); break;
            //case RecognizerIntent.RESULT_SERVER_ERROR: Log.e("Recog", "RESULT SERVER ERROR"); break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: Log.e("Recog", "SPEECH TIMEOUT ERROR"); break;
            case SpeechRecognizer.ERROR_SERVER: Log.e("Recog", "SERVER ERROR"); break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: Log.e("Recog", "BUSY ERROR"); break;
            case SpeechRecognizer.ERROR_NO_MATCH: Log.e("Recog", "NO MATCH ERROR");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        toSpeech.speak("No Response Detected, aborting order.", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    foodImage.setImageResource(R.drawable.menu_ad);
                    foodImage.setVisibility(View.VISIBLE);
                    break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: Log.e("Recog", "NETWORK TIMEOUT ERROR"); break;
            case SpeechRecognizer.ERROR_NETWORK: Log.e("Recog", "TIMEOUT ERROR"); break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: Log.e("Recog", "INSUFFICENT PERMISSIONS ERROR"); break;
            case SpeechRecognizer.ERROR_CLIENT: Log.e("Recog", "CLIENT ERROR"); break;
            case SpeechRecognizer.ERROR_AUDIO: Log.e("Recog", "AUDIO ERROR"); break;
            default: Log.e("Recog", "UNKNOWN ERROR: " + i); break;
        }
    }

    /* Old onResults Method, does not support user clarification for multiple keywords in 1 response.
    @Override
    public void onResults(Bundle bundle)
    {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String[] phrases;
        Log.i("Recog", "Results recieved: " + matches);
        String response = "-Null-";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            switch (pingingRecogFor)
            {
                case pingingRecogFor_Confirmation:
                    phrases = new String[]{"Yes", "No"};
                    response = sortThroughRecognizerResults(matches, phrases);
                    if(response.matches(""))
                    {
                        Log.i("Recog", "Unrecongised response: " + response);
                        pingingRecogFor = pingingRecogFor_Confirmation;
                        toSpeech.speak("Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);
                    }
                    else
                    {
                        Log.i("Recog", "Confirmation Returned: " + response);
                        if(response.matches("Yes"))
                        {
                            pingingRecogFor = pingingRecogFor_Nothing;
                            toSpeech.speak("Order Confirmed.", TextToSpeech.QUEUE_FLUSH, null, null);
                            foodImage.setImageResource(R.drawable.menu_ad);
                        }
                        else if(response.matches("No"))
                        {
                            pingingRecogFor = pingingRecogFor_Order;
                            toSpeech.speak("Order Canceled. What would you like to order instead?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                            foodImage.setImageResource(R.drawable.menu_ad);
                        }
                    }
                    break;

                case pingingRecogFor_Order:
                    phrases = new String[]{"veggie burger", "beef burgundy", "pan fried chicken", "what's on the menu"};
                    response = sortThroughRecognizerResults(matches, phrases);
                    if(response.matches(""))
                    {
                        Log.i("Recog", "Unrecongised response: " + response);
                        pingingRecogFor = pingingRecogFor_Order;
                        toSpeech.speak("I do not recognise: " + matches.get(0) + ". Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                    }
                    else if(response.matches("what's on the menu"))
                    {
                        Log.i("Recog", "Unrecongised response: " + response);
                        pingingRecogFor = pingingRecogFor_Order;
                        toSpeech.speak("Today's Menu includes: Beef Burgundy; Pan Fried Chicken; Veggie Burger. Which would you like?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                    }
                    else
                    {
                        Log.i("Recog", "Order Returned: " + response);
                        pingingRecogFor = pingingRecogFor_Confirmation;
                        toSpeech.speak("You have ordered the " + response + ". Is this correct?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);

                        switch (response)
                        {
                            case "beef burgundy": foodImage.setImageResource(R.drawable.beefburgany); break;
                            case "veggie burger": foodImage.setImageResource(R.drawable.veggie_burger); break;
                            case "pan fried chicken": foodImage.setImageResource(R.drawable.chicken); break;
                        }
                    }
                    break;

                case pingingRecogFor_Clarification:
                    phrases = currentPossiblePhrasesNeedingClarification;
                    response = sortThroughRecognizerResults(matches, phrases);
                    if(response.matches(""))
                    {
                        Log.i("Recog", "Unrecongised response: " + response);
                        pingingRecogFor = pingingRecogFor_Confirmation;
                        toSpeech.speak("Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);
                    }
                    else
                    {
                        Log.i("Recog", "Confirmation Returned: " + response);
                        if(response.matches("Yes"))
                        {
                            pingingRecogFor = pingingRecogFor_Nothing;
                            toSpeech.speak("Order Confirmed.", TextToSpeech.QUEUE_FLUSH, null, null);
                            foodImage.setImageResource(R.drawable.menu_ad);
                        }
                        else if(response.matches("No"))
                        {
                            pingingRecogFor = pingingRecogFor_Order;
                            toSpeech.speak("Order Canceled. What would you like to order instead?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                            foodImage.setImageResource(R.drawable.menu_ad);
                        }
                    }
                    break;

                default:

                    break;
            }
        }
    }*/

    @Override
    public void onResults(Bundle bundle)
    {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        recogResultLogic(matches);

    }

    @Override
    public void onPartialResults(Bundle bundle)
    {
        Log.e("Recog", "Partial Result");
    }

    @Override
    public void onEvent(int i, Bundle bundle)
    {
        Log.e("Recog", "onEvent");
    }
//++++++++[/Recognition Listener Code]

//++++++++[Recognition Other Code]
    private String sortThroughRecognizerResults(ArrayList<String> results, String[] matchablePhrases)
    {
        for (String aResult: results)
        {
            Log.i("Recog", "Sorting results for result: " + aResult);
            for (String aPhrase: matchablePhrases)
            {
                Log.i("Recog", "Sorting results for result: " + aResult.toLowerCase().replace("-", " ") + " and Phrase: " + aPhrase.toLowerCase());
                if((aResult.toLowerCase().replace("-"," ")).contains(aPhrase.toLowerCase()))
                {
                    Log.i("Recog", "Match Found");
                    return aPhrase;
                }
            }
        }
        Log.i("Recog", "No matches found, returning empty string \"\" .");
        return "";
    }



    private void sortThroughRecognizerResultsForAllPossiblities(ArrayList<String> results, String[] matchablePhrases)
    {
        ArrayList<String> possibleResults = new ArrayList<String>();
        for (String aResult: results)
        {
            Log.i("Recog", "All Possiblities, Sorting results for result: " + aResult);
            for (String aPhrase: matchablePhrases)
            {
                Boolean isDuplicate = false;
                Log.i("Recog", "All Possiblities, Sorting results for result: " + aResult.toLowerCase().replace("-", " ") + " and Phrase: " + aPhrase.toLowerCase());
                for (String b: possibleResults)
                {
                    if(b.matches(aPhrase)){isDuplicate = true; break;}
                }

                if((aResult.toLowerCase().replace("-"," ")).contains(aPhrase.toLowerCase()) && !isDuplicate)
                {
                    Log.i("Recog", "All Possiblities, Match Found");
                    possibleResults.add(aPhrase);
                }
            }
        }

        currentPossiblePhrasesNeedingClarification = possibleResults.toArray(new String[possibleResults.size()]);
        //if there is more than 1 keyword in the passed phrase, the method will list those keywords back to the user and ask them to repeat  the correct 1.
        //This in turn will call recogResult from the utterance listener and trigger the pinging for Clarification case where the repeated word will then be used
        //to resolve the logic of the previous call to recogResult.
        if(possibleResults.size() > 1)
        {
            String clarificationString = "I'm sorry but did you mean.";

            for (String a: possibleResults)
            {
                clarificationString += (". " + a);
                if(!possibleResults.get(possibleResults.size() - 1).matches(a))
                {
                    clarificationString += ". or";
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                pingingRecogFor = pingingRecogFor_Clarification;
                toSpeech.speak(clarificationString, TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Clarification);
            }
        }
        //if there is only 1 keyword in the passed phrase, the method skips speech confirmation and immediately calls it's own listener in recogResults,
        // which(given that there is only 1 possible match, will skip to resolving the previous call to recogResult's logic)
        else if (possibleResults.size() == 1)
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                pingingRecogFor = pingingRecogFor_Clarification;
                recogResultLogic(possibleResults);
                //toSpeech.speak("h", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Clarification);
            }
        }
        else
        {
            Log.i("Recog", "No matches found, Requesting Repetition .");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                toSpeech.speak("Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Clarification);
            }
        }
    }

    private String sortThroughRecognizerResults(ArrayList<String> results, String matchablePhrase)
    {
        for (String aResult: results)
        {
            Log.i("Recog", "Sorting results for result: " + aResult.replace("-", " ") + " and Phrase: " + matchablePhrase.toLowerCase());
            if((aResult.replace("-", " ")).contains(matchablePhrase.toLowerCase()))
            {
                Log.i("Recog", "Match Found");
                return matchablePhrase;
            }
        }
        Log.i("Recog", "No matches found, returning empty string \"\" .");
        return "";
    }


    //CALLED FROM: RecogListener onResults()
    private void recogResultLogic(ArrayList<String> matches)
    {
        String[] phrases;
        Log.i("Recog", "Results recieved: " + matches);
        String response = "-Null-";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            Log.i("Recog", "Pinging For: " + pingingRecogFor);
            switch (pingingRecogFor)
            {
                case pingingRecogFor_Clarification:

                    Log.i("Recog", "onResult for Clarification");
                    phrases = currentPossiblePhrasesNeedingClarification;
                    response = sortThroughRecognizerResults(matches, phrases);
                    Log.i("Recog", "onClarification: Response= " + response);
                    if(response.matches(""))
                    {
                        Log.i("Recog", "Unrecongised response: " + response);
                        pingingRecogFor = pingingRecogFor_Clarification;
                        ArrayList<String> copyOfCurrentPossiblePhrases = new ArrayList<String>(Arrays.asList(currentPossiblePhrasesNeedingClarification));
                        sortThroughRecognizerResultsForAllPossiblities(copyOfCurrentPossiblePhrases, phrases);
                    }
                    else
                    {
                        Log.i("Recog", "Clarification Returned: " + response);
                        switch (previousPingingRecogFor)
                        {
                            case pingingRecogFor_Order:
                                if(response.matches(""))
                                {
                                    Log.i("Recog", "Unrecongised response: " + response);
                                    pingingRecogFor = pingingRecogFor_Order;
                                    toSpeech.speak("I do not recognise: " + matches.get(0) + ". Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                                }
                                else if(response.matches("what's on the menu"))
                                {
                                    Log.i("Recog", "Alternate response: " + response);
                                    pingingRecogFor = pingingRecogFor_Order;
                                    toSpeech.speak("Today's Menu includes: Beef Burgundy; Pan Fried Chicken; Veggie Burger. Which would you like?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                                }
                                else if(response.matches("no") || response.matches("cancel"))
                                {
                                    Log.i("Recog", "Alternate response: " + response);
                                    toSpeech.speak("Canceling Order", TextToSpeech.QUEUE_FLUSH, null, null);
                                    foodImage.setImageResource(R.drawable.menu_ad);
                                    foodImage.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    Log.i("Recog", "Order Returned: " + response);
                                    pingingRecogFor = pingingRecogFor_Confirmation;
                                    toSpeech.speak("You have ordered the " + response + ". Is this correct?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);

                                    switch (response)
                                    {
                                        case "beef burgundy": foodImage.setImageResource(R.drawable.meal1); foodImage.setVisibility(View.VISIBLE); currentOrderedFoodAllergies = new String[]{"Mushrooms"}; currentOrderedFoodSalt = 50; currentOrderedFoodDinnerSuggestion = "An email has been sent to you with the details.";
                                            currentOrderedFoodTextDescription = "Beef Burgundy served with pickling onions, button mushrooms, mashed potatoes and green beans.\n\n Allergens: Mushrooms."; runNearSightedCheck(currentUID);
                                            break;
                                        case "pan fried chicken": foodImage.setImageResource(R.drawable.meal2); foodImage.setVisibility(View.VISIBLE); currentOrderedFoodAllergies = new String[]{"Peanuts", "Celery", "Sesame Seeds"}; currentOrderedFoodSalt = 150; currentOrderedFoodDinnerSuggestion = "An email has been sent to you with the details.";
                                            currentOrderedFoodTextDescription = "Pan fried chicken in peanut oil, coated in sesame seeds to give it a good crunch. Served with a side dish of celery and carrots. \n\n Allergens: Peanut Oil, Celery, Sesame Seeds."; runNearSightedCheck(currentUID);
                                            break;
                                        case "veggie burger": foodImage.setImageResource(R.drawable.vegitarian); foodImage.setVisibility(View.VISIBLE); currentOrderedFoodAllergies = new String[]{"Eggs", "Mushrooms"}; currentOrderedFoodSalt = 20; currentOrderedFoodDinnerSuggestion = "An email has been sent to you with the details.";
                                            currentOrderedFoodTextDescription = "Veggie Burger served with tomatoes and lettuce. \n\n Allergens: Eggs, Sesame Seed, Mushrooms."; runNearSightedCheck(currentUID);
                                            break;
                                    }
                                }
                                break;

                            case pingingRecogFor_Confirmation:
                                if(response.matches(""))
                                {
                                    Log.i("Recog", "Unrecongised response: " + response);
                                    pingingRecogFor = pingingRecogFor_Confirmation;
                                    toSpeech.speak("Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);
                                }
                                else
                                {
                                    Log.i("Recog", "Confirmation Returned: " + response);
                                    if(response.matches("Yes"))
                                    {
                                        pingingRecogFor = pingingRecogFor_Nothing;
                                        //returns true if no allergies
                                        if(runAllergyCheck(currentUID, currentOrderedFoodAllergies))//allergy found logic handled inside runAllergyCheck
                                        {
                                            if(runSaltCheck(currentUID, currentOrderedFoodSalt))//salt exceeds diet logic handled inside runSaltCheck
                                            {

                                                int indexOfUser = 0;
                                                for (String aUserID : savedUsersIDs)
                                                {
                                                    if (aUserID.matches(currentUID))
                                                    {
                                                        savedUserCurrentSalt.set(indexOfUser, currentOrderedFoodSalt + savedUserCurrentSalt.get(indexOfUser));
                                                        break;
                                                    }
                                                    indexOfUser++;
                                                }

                                                toSpeech.speak("Order Confirmed.", TextToSpeech.QUEUE_FLUSH, null, null);
                                                foodImage.setVisibility(View.VISIBLE);

                                                //Dinner Suggesting Code
                                                if (!currentOrderedFoodDinnerSuggestion.matches("") && savedUserWantsSuggestions.get(indexOfUser))
                                                {
                                                    pingingRecogFor = pingingRecogFor_Suggestion;
                                                    toSpeech.speak("This meal comes with a suggested dinner for tonight. Would you like to order it?", TextToSpeech.QUEUE_ADD, null, textToSpeechID_Suggestion);
                                                }
                                                foodImage.setImageResource(R.drawable.order_complete);
                                                resetImageTimer.schedule(resetImageTimerTask, 10000);
                                            }
                                            else
                                            {
                                                foodImage.setImageResource(R.drawable.menu_ad);
                                            }
                                        }
                                        else
                                        {
                                            foodImage.setImageResource(R.drawable.menu_ad);
                                        }


                                    }
                                    else if(response.matches("No"))
                                    {
                                        pingingRecogFor = pingingRecogFor_Order;
                                        toSpeech.speak("Order Canceled. What would you like to order instead?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                                        foodImage.setImageResource(R.drawable.menu_ad);
                                        foodImage.setVisibility(View.VISIBLE);
                                    }
                                }
                            break;

                            case pingingRecogFor_Suggestion:
                                if(response.matches(""))
                                {
                                    Log.i("Recog", "Unrecongised response: " + response);
                                    pingingRecogFor = pingingRecogFor_Confirmation;
                                    toSpeech.speak("Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);
                                }
                                else
                                {
                                    Log.i("Recog", "Suggestion Confirmation Returned: " + response);
                                    if(response.matches("Yes"))
                                    {
                                        pingingRecogFor = pingingRecogFor_Nothing;
                                        //returns true if no allergies
                                        toSpeech.speak(/*"For Dinner we suggest . " +*/ currentOrderedFoodDinnerSuggestion /*+ ". Would you like to order it? "*/, TextToSpeech.QUEUE_FLUSH, null, "");
                                        sendEmail();
                                    }
                                    else if(response.matches("No"))
                                    {
                                        pingingRecogFor = pingingRecogFor_Order;
                                        toSpeech.speak("Ok", TextToSpeech.QUEUE_FLUSH, null, "");
                                    }
                                }
                                break;

                            case pingingRecogFor_OrderDespiteSalt:
                                if(response.matches(""))
                                {
                                    Log.i("Recog", "Unrecongised response: " + response);
                                    pingingRecogFor = pingingRecogFor_Confirmation;
                                    toSpeech.speak("Can you please repeat that?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Confirmation);
                                }
                                else
                                {
                                    Log.i("Recog", "Confirmation Returned: " + response);
                                    if(response.matches("Yes"))
                                    {
                                        pingingRecogFor = pingingRecogFor_Nothing;
                                        //returns true if no allergies

                                        int indexOfUser = 0;
                                        for (String aUserID : savedUsersIDs)
                                        {
                                            if (aUserID.matches(currentUID))
                                            {
                                                savedUserCurrentSalt.set(indexOfUser, currentOrderedFoodSalt + savedUserCurrentSalt.get(indexOfUser));
                                                break;
                                            }
                                            indexOfUser++;
                                        }

                                        toSpeech.speak("Order Confirmed.", TextToSpeech.QUEUE_FLUSH, null, null);
                                        if (!currentOrderedFoodDinnerSuggestion.matches(""))
                                        {
                                            pingingRecogFor = pingingRecogFor_Suggestion;
                                            toSpeech.speak("We have a dinner suggestion for you. Would you like to hear it?", TextToSpeech.QUEUE_ADD, null, textToSpeechID_Suggestion);
                                        }


                                        foodImage.setImageResource(R.drawable.order_complete);
                                        resetImageTimer.schedule(resetImageTimerTask, 10000);
                                        foodImage.setVisibility(View.VISIBLE);
                                    }
                                    else if(response.matches("No"))
                                    {
                                        pingingRecogFor = pingingRecogFor_Order;
                                        toSpeech.speak("Order Canceled. What would you like to order instead?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
                                        foodImage.setImageResource(R.drawable.menu_ad);
                                        foodImage.setVisibility(View.VISIBLE);
                                    }
                                }
                                break;
                        }
                    }
                    break;

                case pingingRecogFor_Order:
                    previousPingingRecogFor = pingingRecogFor_Order;
                    phrases = new String[]{"veggie burger", "beef burgundy", "pan fried chicken", "what's on the menu", "no", "cancel"};
                    sortThroughRecognizerResultsForAllPossiblities(matches, phrases);
                    break;

                case pingingRecogFor_Confirmation:
                    previousPingingRecogFor = pingingRecogFor_Confirmation;
                    phrases = new String[]{"Yes", "No"};
                    sortThroughRecognizerResultsForAllPossiblities(matches, phrases);
                    break;

                case pingingRecogFor_Suggestion:
                    previousPingingRecogFor = pingingRecogFor_Suggestion;
                    phrases = new String[]{"Yes", "No"};
                    sortThroughRecognizerResultsForAllPossiblities(matches, phrases);
                    break;

                case pingingRecogFor_OrderDespiteSalt:
                    previousPingingRecogFor = pingingRecogFor_OrderDespiteSalt;
                    phrases = new String[]{"Yes", "No"};
                    sortThroughRecognizerResultsForAllPossiblities(matches, phrases);
                    break;
            }
        }
    }
//++++++++[/Recognition Other Code]




    //+++[TileScanner Code]
    private void setupTileScanner()
    {
        dialog = new ProgressDialog(OrderFoodActivity.this);
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
        mScanner = new Scanner(OrderFoodActivity.this, scannerCallback);
        deviceManager = new DeviceManager(OrderFoodActivity.this);
        deviceManager.setCallBack(deviceManagerCallback);


        tileReaderTimer = new Timer();
        uidIsFound = false;
        hasSufferedAtLeastOneFailureToReadUID = false;

        //connect called from OnResume which triggers after on create anyway
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
            Log.i("TileScanner", "Disconnect Complete");
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
                        //alertDataText.setText(outUID);

                        currentUID = outUID.toString();
                        //retrieveAlerts(currentStationID);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        {
                            pingingRecogFor = pingingRecogFor_Order;
                            toSpeech.speak("What food do you wish to order?", TextToSpeech.QUEUE_FLUSH, null, textToSpeechID_Order);
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
                        //alertDataText.setText("UID found without a prior failure, assuming its a tag left on the scanner");
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
                        Handler readSfzHandler = new Handler(OrderFoodActivity.this.getMainLooper()) {
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
                                        Handler readSztHandler = new Handler(OrderFoodActivity.this.getMainLooper()) {
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
            Log.e("TileScanner", "Timer has been canceled, aborting the call for uid loop due to illegal state exception: " + e);

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



//**********[Location Update and server pinging Code]





    @Override
    public void onSaveInstanceState(Bundle savedState)
    {
        super.onSaveInstanceState(savedState);
    }

    private void restoreSavedValues(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {


        }
    }

    //Update activity based on the results sent back by the servlet.
    @Override
    public void updateFromDownload(String result) {
        //intervalTextView.setText("Interval: " + result);

        if(result != null)
        {


            // matches uses //( as matches() takes a regular expression, where ( is a special character.
            if(!result.matches("failed to connect to /192.168.1.188 \\(port 8080\\) after 3000ms: isConnected failed: ECONNREFUSED \\(Connection refused\\)"))
            {
                Log.e("Download", result);
                try
                {
                    JSONArray jsonResultFromServer = new JSONArray(result);

                    Log.i("Network UPDATE", "Non null result received.");
                    //mapText.setText("We're good");
                    if (pingingServerFor_alertData)
                    {
                        pingingServerFor_alertData = false;
                        //alertDataText.setText(result);
                        ArrayList<String> results = new ArrayList<String>();
                        for (int i = 0; i < jsonResultFromServer.length(); i++)
                        {
                            results.add(jsonResultFromServer.getJSONObject(i).getString("alert"));
                        }


                    }
                    else
                    {
                        /*if (itemID == 0 && !result.matches(""))//if app has no assigned id, receive id from servlet.
                        {
                            try
                            {
                                JSONArray jin = new JSONArray(result);
                                JSONObject obj = jin.getJSONObject(0);
                                itemID = obj.getInt("id");
                            } catch (JSONException e)
                            {
                                Log.e("JSON ERROR", "Error retrieving id from servlet with exception: " + e.toString());
                            }
                        }*/
                    }
                }
                catch (JSONException e)
                {
                    Log.e("Network Update", "ERROR in Json: " + e.toString());
                }

            }
            else
            {
                //mapText.setText("Error: network unavaiable");
                Log.e("Network UPDATE", "Error: network unavaiable, error: " + result);
            }
        }
        else
        {
            //mapText.setText("Error: network unavaiable");
            Log.e("Network UPDATE", "Error: network unavaiable");
        }

        Log.e("Download Output", "" + result);
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case DownloadCallback.Progress.ERROR:
                Log.e("Progress Error", "there was an error during a progress report at: " + percentComplete + "%");
                break;
            case DownloadCallback.Progress.CONNECT_SUCCESS:
                Log.i("Progress ", "connection successful during a progress report at: " + percentComplete + "%");
                break;
            case DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS:
                Log.i("Progress ", "input stream acquired during a progress report at: " + percentComplete + "%");
                break;
            case DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                Log.i("Progress ", "input stream in progress during a progress report at: " + percentComplete + "%");
                break;
            case DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS:
                Log.i("Progress ", "input stream processing successful during a progress report at: " + percentComplete + "%");
                break;
        }
    }

    @Override
    public void finishDownloading() {
        pingingServer = false;
        Log.i("Network Update", "finished Downloading");
        if (aNetworkFragment != null) {
            Log.e("Network Update", "network fragment found, canceling download");
            aNetworkFragment.cancelDownload();
        }
    }

    class AddressResultReceiver extends ResultReceiver
    {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            resultData.getString(Constants.RESULT_DATA_KEY);


            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT)
            {
                Log.i("Success", "Address found");
            }
            else
            {
                Log.e("Network Error:", "in OnReceiveResult in AddressResultReceiver: " +  resultData.getString(Constants.RESULT_DATA_KEY));
            }

        }
    }
//**********[/Location Update and server pinging Code]

    private void sendEmail()
    {
        //GMailSender sm = new GMailSender(this, "dan@deveire.com", "New Trouble Ticket", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");

        SimpleDateFormat format = new SimpleDateFormat(" HH:mm  dd/MM/yyyy");
        Calendar aCalendar = Calendar.getInstance();
        Calendar bCalendar = Calendar.getInstance();
        bCalendar.setTime(aCalendar.getTime());
        bCalendar.add(Calendar.HOUR, 2);

        GMailSender sm = new GMailSender(this, "dan@deveire.com", "Soxgo Dinner Suggestion", "For dinner today on" + format.format(aCalendar.getTime()) +  " we suggest the Caesar Salad. \n To order at this time and location, click below: \n\t Location: Dan's House \n\t Time: " + format.format(bCalendar.getTime()));
        //Executing sendmail to send email
        sm.execute();
    }

}




/*



 */