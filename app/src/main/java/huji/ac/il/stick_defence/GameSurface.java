package huji.ac.il.stick_defence;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.HashSet;
import java.util.Set;


/**
 * The game surface. During the game, everything takes place on this surface.
 */
public class GameSurface extends SurfaceView implements
        SurfaceHolder.Callback {

    private static final int END_GAME_MSG_FACTOR = 7;
    private GameLoopThread gameLoopThread;
    private GameState gameState = GameState.getInstance();
    private SimpleGestureDetector simpleGestureDetector =
            new SimpleGestureDetector();
    private Context context;
    private Bitmap scaledBackground;
    private Set<Pair<Integer, Soldier.SoldierType>> soldiersX = new HashSet<>();

    public GameSurface(Context context) {

        super(context);

        // Adding the callback (this) to the surface holder to intercept events
        getHolder().addCallback(this);

        // Create the GameLoopThread
        gameLoopThread = new GameLoopThread(getHolder(), this);

        // Make the GameSurface focusable so it can handle events
        setFocusable(true);

        this.context = context;

    }

    public void writeEndGameMessage(Canvas canvas) {
        Sounds.getInstance().stopAllSound();
        Paint paint = new Paint();
        paint.setTextSize(canvas.getWidth() / END_GAME_MSG_FACTOR);
        paint.setTypeface(Typeface.SERIF);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.RIGHT);
        int y = canvas.getHeight() / 2;
        int x = canvas.getWidth() / 2;
        String message;
        if(gameState.isLeftPlayerWin()){
            message= "You won!";
        }else{
            message= "You lost!";
        }

        String half1 = message.substring(0, message.length() / 2);
        String half2 = message.substring(message.length() / 2);

        canvas.drawText(half1, x, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(half2, x, y, paint);
        Sounds.getInstance().streamSound(Sounds.END_TRUMPET,false);

    }

    public void goToLeagueInfo(){
        Client.getClientInstance().
                send(Protocol.stringify(Protocol.Action.GAME_OVER,
                        String.valueOf(GameState.getInstance().isLeftPlayerWin())));
        gameState.finishGame();
        stopGameLoop();
        Intent intent = new Intent(context, LeagueInfoActivity.class);
        intent.putExtra("NewGame", false);
        String leagueInfo = Market.getLeagueInfo();
        if (leagueInfo != null) {
            Log.w("custom","goint to league form market and sending league info");
            intent.putExtra("info", leagueInfo);
        }
        context.startActivity(intent);
        ((Activity) context).finish();
    }

    public void goToMarket() {
        gameState.finishGame();
        stopGameLoop();
        Intent intent = new Intent(context, Market.class);
        intent.putExtra("isMultiplayer", gameState.isMultiplayer());
        context.startActivity(intent);
        ((Activity) context).finish();
    }

    public void stopGameLoop() {
        if (null != gameLoopThread) {
            gameLoopThread.setRunning(false);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        super.onTouchEvent(event);
        this.simpleGestureDetector.detect(event);
        return true;

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Bitmap background = BitmapFactory.decodeResource(getResources(),
                R.drawable.background);
        float scale = (float) background.getHeight() / (float) getHeight();
        int newWidth = Math.round(background.getWidth() / scale);
        int newHeight = Math.round(background.getHeight() / scale);
        scaledBackground = Bitmap.createScaledBitmap(background, newWidth,
                                                     newHeight, true);

        if (gameLoopThread.getState() == Thread.State.NEW) {
            gameLoopThread.setRunning(true);
            gameLoopThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                gameLoopThread.setRunning(false);
                gameLoopThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                // try again shutting down the gameLoopThread
            }
        }
    }

    public void render(Canvas canvas) {
        canvas.drawBitmap(scaledBackground, 0, 0, null); // draw the background
        for (Tower tower : gameState.getTowers()) {
            tower.render(canvas);
        }

        for (Bullet bullet : gameState.getBullets()) {
            bullet.render(canvas);
        }

        //Optimization - don't draw 2 same soldiers with same x
        for (Soldier soldier : gameState.getSoldiers()) {
            Pair<Integer, Soldier.SoldierType> soldierPair =
                    new Pair<>(soldier.getSoldierX(), soldier.getSoldierType());
            if (!soldiersX.contains(soldierPair)){
                soldiersX.add(soldierPair);
                soldier.render(canvas);
            } // else - don't render the soldier
        }

        for (Bow bow : gameState.getBows()) {
            bow.render(canvas);
        }
        for (Arrow arrow : gameState.getArrows()) {
            arrow.render(canvas);
        }
        for (DrawableObject drawableObject :gameState.getMiscellaneous()){
            drawableObject.render(canvas);
        }
        soldiersX.clear();
    }

}




