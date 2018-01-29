using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class AutoNext : MonoBehaviour {

    //Is auto-next functionality activated?
    public static bool autoNextActivated = false;
    public float secondsToHoldForAutoNext = 1.5f;
    private static float counter;
    private static float tempCounter;
    
    // Use this for initialization
	void Start ()
    {
        counter = 0f;	
	}
	
	// Update is called once per frame
	void Update ()
    {
        if (counter >= secondsToHoldForAutoNext) //calculate activation
        {
            autoNextActivated = true;
        }
        else
        {
            autoNextActivated = false;
        }

        if(autoNextActivated)
        {

            gameObject.GetComponent<UserInputHandler>().NextStep(); //go to the next step

        }
	}

    public void IncrementCounterForAutoNext()
    {
        counter += Time.deltaTime;
    }
    public void ResetIfNotActivated()
    {
        if(!autoNextActivated)
            counter = 0f;
    }
    public void ResetIfActivated() //On click
    {
        if (autoNextActivated)
            counter = 0f;
    }
    public static void ResetEverything()
    {
        autoNextActivated = false;
        counter = 0f;
    }

}
