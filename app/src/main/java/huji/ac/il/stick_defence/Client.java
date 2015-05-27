package huji.ac.il.stick_defence;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A client for managing all the communication with the server.
 * It uses the singleton pattern so that it will be accessible from all the
 * different activities.
 */
public class Client {
    private static Client client;
    private String name; //each client as a name, dosen't have to be unique.
    private PrintWriter out;
    private DoProtocolAction currentActivity;

    private Client() {
    }

    /**
     * Constructs a new client with the given name
     *
     * @param name the name of the client
     */
    private Client(String name) {
        this.name = name;
    }

    /**
     * Creates a new client if the current static client is null
     *
     * @param name the name of the client
     * @return an instance of the client
     */
    public static Client createClient(String name) {
        if (client == null) {
            client = new Client(name);
        }
        return client;
    }

    /**
     * Returns an instance of the client. Could return null if no one called
     * "createClient" before
     *
     * @return an instance of the client.
     */
    public static Client getClientInstance() {
        return client;
    }

    /**
     * Sends data to the server
     *
     * @param out the data to send
     */
    public void send(String out) {
        this.out.println(out);
    }

    /**
     * Set the server the client is currently connected too.
     * The server could be changed during the game if needed so.
     *
     * @param server the current server
     */
    public void setServer(Socket server) {
        try {
            this.out = new PrintWriter(server.getOutputStream(), true);
            //save the output stream of the server.
        } catch (IOException e) {
            e.printStackTrace();
        }
        new ClientSocketListener().executeOnExecutor(AsyncTask
                .THREAD_POOL_EXECUTOR, server); //start listen to the server
                // on a different thread
        send(Protocol.stringify(Protocol.Action.NAME, name)); //send the
        // client name to the server.


    }

    /**
     * Sets the current activity so that the client could communicate with it.
     *
     * @param activity the current activity.
     */
    public void setCurrentActivity(DoProtocolAction activity) {
        this.currentActivity = activity;

    }

    /**
     * This method decide what to do on each data received from the server.
     *
     * @param action the action received from the server
     * @param data   the data received from the server
     */
    private void doAction(String action, String data) {
        this.currentActivity.doAction(action, data);
    }

    public void reportArrow(int distance) {
        send(Protocol.stringify(Protocol.Action.ARROW, Integer.toString
                (distance)));
    }

    public void reportSoldier() {
        send(Protocol.stringify(Protocol.Action.SOLDIER));
    }


    /**
     * This class represents a socket listener on a server node.
     */
    private class ClientSocketListener extends AsyncTask<Socket, Void, Void> {

        @Override
        protected Void doInBackground(Socket... params) {
            Socket server = params[0];
            Log.w("custom", "start listening to server");
            String inputLine;
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader
                        (server.getInputStream()));
                while ((inputLine = in.readLine()) != null) { //the readLine
                // is a blocking method.
                    Log.w("custom", "server says: " + inputLine);
                    String[] action = Protocol.parse(inputLine);
                    doAction(action[0], action[1]);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.w("custom", "finish socket listener");
            return null;
        }
    }

}
