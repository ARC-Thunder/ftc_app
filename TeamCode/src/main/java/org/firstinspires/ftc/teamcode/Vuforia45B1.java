package org.firstinspires.ftc.teamcode;

import android.app.Activity;
import android.view.View;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.SwitchableLight;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

/**
 * Created by daniv on 3/2/18.
 */
@Autonomous(name="Vuforia 45 degrees B1", group="Autonomous")
public class Vuforia45B1 extends AutonomousMethodMaster{

    View relativeLayout;

    public void runOpMode()
    {
        initElectronics(0);

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        // the licence key that vuforia needs to work
        parameters.vuforiaLicenseKey = "AQRacK7/////AAAAGea1bsBsYEJvq6S3KuXK4PYTz4IZmGA7SV88bdM7l26beSEWkZTUb8H352Bo/ZMC6krwmfEuXiK7d7qdFkeBt8BaD0TZAYBMwHoBkb7IBgMuDF4fnx2KiQPOvwBdsIYSIFjiJgGlSj8pKZI+M5qiLb3DG3Ty884EmsqWQY0gjd6RNhtSR+6oiXazLhezm9msyHWZtX5hQFd9XoG5npm4HoGaZNdB3g5YCAQNHipjTm3Vkf71rG/Fffif8UTCI1frmKYtb4RvqiixDSPrD6OG6YmbsPOYUt2RZ6sSTreMzVL76CNfBTzmpo2V0E6KKP2y9N19hAum3GZu3G/1GEB5D+ckL/CXk4JM66sJw3PGucCs";

        // indicates camera direction
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.FRONT;
        this.vuforia = ClassFactory.createVuforiaLocalizer(parameters);

        // loads data for vumarks
        VuforiaTrackables relicTrackables = this.vuforia.loadTrackablesFromAsset("RelicVuMark");
        VuforiaTrackable relicTemplate = relicTrackables.get(0);
        relicTemplate.setName("relicVuMarkTemplate"); // can help in debugging; otherwise not necessary

        telemetry.addData(">", "Press Play to start");
        telemetry.update();
        waitForStart();

        relicTrackables.activate();

        RelicRecoveryVuMark vuMark = RelicRecoveryVuMark.from(relicTemplate);


        waitForStart();

        int timesScanned = 0;
        double move_inches = 0;
        double distanceFromWall = 4 + 4.5 * Math.sqrt(2.0);             //Distance from the wall that the robot will attempt
        // to reach. It is necessary to stop the robot from driving into the cryptobox or getting sutck.
        // identify which vumark. If it doesn't pick one up after 1,000 tries, it defaults to simple parking.
        while (vuMark == RelicRecoveryVuMark.UNKNOWN){
            timesScanned++;
            //motorL.setPower(0.25);
            //motorR.setPower(0.25);
            telemetry.addData("Vumark not found, retrying. Retry attempt: ", timesScanned );
            telemetry.update();
            vuMark = RelicRecoveryVuMark.from(relicTemplate);
            if(timesScanned >= 100000)
            {
                parkR1();
                return;
            }
        }
        if(vuMark == RelicRecoveryVuMark.LEFT){
            telemetry.addData("VuMark", "%s visible", vuMark);
            telemetry.update();

            move_inches = distanceFromWall; //-7.63;
        }
        else if(vuMark == RelicRecoveryVuMark.CENTER){
            telemetry.addData("VuMark", "%s visible", vuMark);
            telemetry.update();

            move_inches = distanceFromWall + 8;
        }
        else if(vuMark == RelicRecoveryVuMark.RIGHT) {
            telemetry.addData("VuMark", "%s visible", vuMark);
            telemetry.update();

            move_inches = distanceFromWall + 16; //7.63;
        }
        else
        {
            telemetry.addData("VuMark", "Couldn't be captured");
            telemetry.update();

            move_inches = distanceFromWall + 8;
        }

        //sleep(5000);

        /* We further illustrate how to decompose the pose into useful rotational and
         * translational components */
        double tX = 0, tY = 0, tZ = 0;                                                                //Translation X, Y, and Z.
        double phone_displacement = (double) 6.5;
        double pictograph_displacement = (double) 3 + 5.5;
        boolean isOnStone = true;                                                                     //Is it on the balancing stone? Defaults to true.
        boolean isMovingOffStone = false; //Is it moving off the stone? Defaults to false.
        int inchesAdjusted = 0;

        while (vuMark != RelicRecoveryVuMark.UNKNOWN /*&& (tY > -((double) 36 - phone_displacement - pictograph_displacement) * inchToMm)*/) // 36 as in 36 inches
        {
            vuMark = RelicRecoveryVuMark.from(relicTemplate);
            OpenGLMatrix pose = ((VuforiaTrackableDefaultListener)relicTemplate.getListener()).getPose();
            telemetry.addData("Pose", format(pose));
            if(pose != null) {
                VectorF trans = pose.getTranslation();

                Orientation rot = Orientation.getOrientation(pose, AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES);

                // Extract the X, Y, and Z components of the offset of the target relative to the robot
                tX = trans.get(0);
                tY = trans.get(1);
                tZ = trans.get(2);

                // Extract the rotational components of the target relative to the robot
                double rX = rot.firstAngle;                                                             //Rotation along X-axis
                double rY = rot.secondAngle;                                                            //Rotation along Y-axis
                double rZ = rot.thirdAngle;                                                             //Rotation along Z-axis

                telemetry.addData("X translation", tX);
                telemetry.addData("Y translation", tY);
                telemetry.addData("Z translation", tZ);

                telemetry.addData("X rotation", rX);
                telemetry.addData("Y rotation", rY);
                telemetry.addData("Z rotation", rZ);

                telemetry.update();

                if(isFlat(rZ) && !isOnStone && !isParallel(rY))
                {
                    encoderRotateDegrees((rY < 90 ? 0:1), 0.5, (int)Math.round(Math.abs(rY)));
                    continue;
                }
                if(!isFlat(rZ))
                {
                    isMovingOffStone = true;
                }
                if(isFlat(rZ) && isMovingOffStone)
                {
                    isOnStone = false;
                    isMovingOffStone = false;
                }
                if(!isOnStone)
                {
                    double distanceToDestination = Math.abs(tY+ (move_inches)); //The distance to the destination
                    distanceToDestination /= inchToMm;
                    telemetry.addData("tY: (inches)", (tY / inchToMm));
                    telemetry.addData("inches to move: ", distanceToDestination);
                    telemetry.update();
                    sleep(5000);
                    encoderMove(0.5, -distanceToDestination, -distanceToDestination); //Move to the destination
                    break;
                }
                else if(isOnStone)
                {
                    encoderMove(0.2, -0.5, -0.5); // just move...
                }
            }
        }
        double halfCryptoboxWallLength = 1.75/2.54; //1.75cm in inches
        double halfCryptoboxColumnLength = 7.63/2;
        double verticalFromCryptobox = Math.sqrt(Math.pow(9.5, 2) - Math.pow(halfCryptoboxColumnLength, 2));
        double lengthToTurn = (24 + halfCryptoboxWallLength + halfCryptoboxColumnLength) - (24 - verticalFromCryptobox);
        double lengthAfterTurn = (8 - verticalFromCryptobox) * Math.sqrt(2.0);

        double horizontalBackUp = 3.0 / Math.sqrt(2);
        double verticalBackUp = 5.0;

        encoderMove(0.4, -lengthToTurn, -lengthToTurn);
        encoderRotateDegrees(1, 0.25, 45);
        encoderMove(0.4, -lengthAfterTurn, -lengthAfterTurn);
        liftPlatform();
        lowerPlatform();
        encoderMove(0.4, horizontalBackUp, horizontalBackUp);
        encoderRotateDegrees(0, 0.25, 45);
        encoderMove(0.4, verticalBackUp, verticalBackUp);
        encoderRotateDegrees(0, 0.25, 90);
        encoderMove(0.4, -horizontalBackUp, -horizontalBackUp);
        encoderRotateDegrees(1, 0.25, 90);
        liftPlatform();
        encoderMove(0.4, -(verticalBackUp + verticalFromCryptobox - 2.25), -(verticalBackUp + verticalFromCryptobox - 2.25));
        sleep(500);
        int timesPushed = 0;
        while (timesPushed < 2) {

            encoderMove(0.25, -6, -6);

            sleep(500);

            encoderMove(0.25, 5.75, 5.75);

            sleep(500);

            timesPushed++;
        }
        stopMotion(); //Stops all motors - a failsafe for our failsafe.

    }

    String format(OpenGLMatrix transformationMatrix) {
        return (transformationMatrix != null) ? transformationMatrix.formatAsTransform() : "null";
    }

}
