package huji.ac.il.stick_defence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

/**
 * Created by yahav on 18/08/15.
 */
public class Tank extends Soldier {

    //======================Tank's abilities==========================
    private static final double SEC_TO_SCREEN_WIDTH = 15;
    private static final int DAMAGE_PER_SEC = 20; // [Damage/Sec]
    //==========================================================================

    //==========================================================================
    //Soldier height in relation to the screen height.
    //0-1 double. For instance, 0.5 will cause the
    //soldier to span over a half of the screen height.
    private static final double SCREEN_HEIGHT_PORTION = 0.166;
    //==========================================================================

    //============================Sprite constants==============================
    private static final int NUMBER_OF_FRAMES = 6;
    private static final int SHOOT_NUMBER_OF_FRAMES = 6; //TODO
    private static final int WALK_FPS = 40;
    private static final int ATTACK_FPS = 2;
    private static final float TANK_HEIGHT_RELATIVE = 0.99f;
    private static final int ATTACK_PIC_INDEX = 1; //TODO
    //==========================================================================

    private static Bitmap leftSoldierPic = null;
    private static Bitmap rightSoldierPic = null;
    private static Bitmap leftAttackSoldierPic = null;
    private static Bitmap rightAttackSoldierPic = null;
    private static float tankY = 0;
    private boolean canShoot = false;
    private Sprite.Player player;
    private int attackFrame = 0;

    public Tank(Context context, Sprite.Player player, double delayInSec) {
        super(context, player, SEC_TO_SCREEN_WIDTH, DAMAGE_PER_SEC, delayInSec);
        if (null == leftSoldierPic) {
            leftSoldierPic = BitmapFactory.decodeResource(
                    context.getResources(),
                    R.drawable.tank);
        }

        if (null == rightSoldierPic) {
            rightSoldierPic = Sprite.mirrorBitmap(leftSoldierPic);
        }

        if (null == leftAttackSoldierPic) {
            leftAttackSoldierPic =
                    BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.tank); //TODO
        }

        if (null == rightAttackSoldierPic) {
            rightAttackSoldierPic = Sprite.mirrorBitmap(leftAttackSoldierPic);
        }

        if (Sprite.Player.LEFT == player) {
            super.initSprite(context, leftSoldierPic, NUMBER_OF_FRAMES,
                    SCREEN_HEIGHT_PORTION, WALK_FPS);
        } else {
            super.initSprite(context, rightSoldierPic, NUMBER_OF_FRAMES,
                    SCREEN_HEIGHT_PORTION, WALK_FPS);
        }

        Bullet.init(context, getScaledDownFactor());

        tankY = super.getSoldierY(); // TODO - verify

        this.player = player;

    }

    public void update(long gameTime) {
        if (!super.isAttack()) {
            if (player == Sprite.Player.LEFT) {
                if (getSoldierX() + getScaledFrameWidth() / 2 >=
                        getScreenWidth() * 0.25) {
                    super.attack(leftSoldierPic, SHOOT_NUMBER_OF_FRAMES,
                            ATTACK_FPS);
                    tankY = super.getSoldierY();
                    attackFrame = (getCurrentFrame() + 1) % NUMBER_OF_FRAMES;
                }
            } else {
                if (getSoldierX() + getScaledFrameWidth() / 2 <=
                        (float) getScreenWidth() * 0.75) {
                    super.attack(rightSoldierPic, SHOOT_NUMBER_OF_FRAMES,
                            ATTACK_FPS);
                    tankY = super.getSoldierY();
                    attackFrame = (getCurrentFrame() + 1) % NUMBER_OF_FRAMES;
                }
            }
        } else { // Attack
            if (super.getCurrentFrame() == attackFrame) {
                if (canShoot) {
                    float bulletX = getSoldierX();
                    if (Sprite.Player.LEFT == player) {
                        bulletX += (float) getScaledFrameWidth() / 2;
                    }
                    Bullet bullet = new Bullet(getContext(),
                            bulletX,
                            getSoldierY() / TANK_HEIGHT_RELATIVE,
                            getPlayer());

                    gameState.addBullet(bullet);
                    canShoot = false;
                }
            } else {
                canShoot = true;
            }
        }
        super.update(gameTime);

    }

    public void render(Canvas canvas) {
        super.render(canvas);
    }

    public boolean isHitByArrow(Arrow arrow) {
        return super.isHitByArrow(arrow);
    }

    public static float getTankY() {
        return tankY;
    }
}