using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SpeedControl : MonoBehaviour
{
    public static int speed; //Larger the value, greater the speed of playback
    //larger num for greater speed
    void Awake()
    {
        speed = 3;
        CalibrateVars();
    }
	// Use this for initialization
	public static void IncreaseSpeed()
    {
        if (speed < 6)
        {
            speed++;
            CalibrateVars();
        }
        else
            Debug.Log("Already at top speed");
    }

    public static void DecreaseSpeed()
    {
        if (speed > 0)
        {
            speed--;
            CalibrateVars();
        }
        else
            Debug.Log("Already at lowest speed");
    }

    private static void CalibrateVars()
    {
        switch(speed)
        {
            case 1:
                //Slowest
                TraceImplement.timeDelay = 2.5f;
                TraceImplement.timeDelayForOutline = 1.85f;
                VisualizationHandler.outlineTime = 2.15f;
                MessageFunctionality.bezierPointResolution = 300;
                break;
            case 2:
                //Slow
                TraceImplement.timeDelay = 2f;
                TraceImplement.timeDelayForOutline = 1.45f;
                VisualizationHandler.outlineTime = 1.65f;
                MessageFunctionality.bezierPointResolution = 250;
                break;
            case 3:
                //Medium
                TraceImplement.timeDelay = 1.65f;
                TraceImplement.timeDelayForOutline = 1f;
                VisualizationHandler.outlineTime = 1.25f;
                MessageFunctionality.bezierPointResolution = 150;
                break;
            case 4:
                //Fast
                TraceImplement.timeDelay = 1.25f;
                TraceImplement.timeDelayForOutline = 0.75f;
                VisualizationHandler.outlineTime = 0.95f;
                MessageFunctionality.bezierPointResolution = 100;
                break;
            case 5:
                //Faster
                TraceImplement.timeDelay = .80f;
                TraceImplement.timeDelayForOutline = 0.5f;
                VisualizationHandler.outlineTime = 0.65f;
                MessageFunctionality.bezierPointResolution = 50;
                break;
            case 6:
                //Fastest
                TraceImplement.timeDelay = .35f;
                TraceImplement.timeDelayForOutline = 0.2f;
                VisualizationHandler.outlineTime = 0.3f;
                MessageFunctionality.bezierPointResolution = 20;
                break;
            default:
                Debug.LogError("Unknown speed value");
                break;
        }
    }
}
