package huji.ac.il.stick_defence;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;


public class SlaveActivity extends Activity {
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private ArrayAdapter adapter;
    private ListView list;
    private ArrayList<WifiP2pDevice> devices = new ArrayList<>();
    private String name = "test";
    private final int TIME_TO_REFRESH_PEERS = 20000;
    private Client master;
    private Activity slaveActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        slaveActivity=this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slave);

        mManager = (WifiP2pManager) getSystemService(getApplicationContext().WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(this, mManager, mChannel);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        list = (ListView) findViewById(R.id.devices);


        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                boolean running = true;
                while (running) {
                    Log.w("custom", "searching peers");
                    mManager.discoverPeers(mChannel, null);
                    try {
                        Thread.sleep(TIME_TO_REFRESH_PEERS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        adapter = new peersAdapter(this, android.R.layout.simple_list_item_1, devices);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = devices.get(position);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.w("custom", "connections success!");
                        mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {

                            @Override
                            public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                                Log.w("custom", "groupInfo:");
                                Log.w("custom", info.toString());
                                if (info.groupOwnerAddress != null) {
                                    Log.w("custom", info.groupOwnerAddress.getHostAddress());
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            try {
                                                Socket socket = new Socket(info.groupOwnerAddress.getHostAddress(), MasterActivity.PORT);
                                                master = new Client(socket);
                                                master.setCurrentActivity(slaveActivity);
                                                master.startListening();
                                                Log.w("custom", "saying hello to server");
                                                master.protocol.send(Protocol.Command.NAME, name);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            return null;
                                        }
                                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                                    ;
                                }

                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.w("custom", "connections failed!");
                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void addDevices(Collection<WifiP2pDevice> devices) {
        this.devices.clear();
        this.devices.addAll(devices);
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }
    }
public void switchToLeagueActivity(){
    Log.w("custom", "going to league");
}

    //======================================Adapter classs==============================
    private class peersAdapter extends ArrayAdapter<WifiP2pDevice> {

        public peersAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public peersAdapter(Context context, int resource, ArrayList<WifiP2pDevice> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                convertView = vi.inflate(android.R.layout.simple_list_item_1, null);
            }

            String title = getItem(position).deviceName;

            TextView titleView = (TextView) convertView.findViewById(android.R.id.text1);
            titleView.setText(title);
            return convertView;

        }
    }
}